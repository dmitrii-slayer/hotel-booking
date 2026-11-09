package org.mephi.hotelbooking.booking.rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mephi.hotelbooking.booking.BaseTest;
import org.mephi.hotelbooking.booking.client.HotelServiceGateway;
import org.mephi.hotelbooking.booking.client.dto.AvailabilityDto;
import org.mephi.hotelbooking.booking.client.dto.RoomDto;

import org.mephi.hotelbooking.booking.client.exception.RetryableHotelServiceException;
import org.mephi.hotelbooking.booking.domain.enums.BookingStatus;
import org.mephi.hotelbooking.booking.domain.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mephi.hotelbooking.booking.TestSecurity.userJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingEdgeCasesTest extends BaseTest {

    private static final UUID ROOM_ID = UUID.fromString("d473a3ef-f765-499f-9463-f2c101d94898");
    private static final UUID HOTEL_ID = UUID.fromString("40d95c36-c832-4581-851e-8c0cf6419754");

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoBean
    private HotelServiceGateway hotelGateway;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();

        when(hotelGateway.getRoom(eq(ROOM_ID), any(), anyString())).thenReturn(room());
        when(hotelGateway.getRecommendedRooms(any(), any(), any(), anyString())).thenReturn(List.of(room()));
        when(hotelGateway.confirmAvailability(any(), any(), any(), anyString()))
                .thenReturn(availability("HOLD"));
    }

    @Test
    void shouldGenerateRequestIdWhenClientOmitsIt() throws Exception {
        // given клиент не передал requestId
        Map<String, Object> request = Map.of(
                "roomId", ROOM_ID.toString(),
                "autoSelect", false,
                "startDate", "2027-06-01",
                "endDate", "2027-06-05");

        // when
        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // then сервер сгенерировал идентификатор и сохранил его
        assertThat(bookingRepository.findAll()).hasSize(1);
        assertThat(bookingRepository.findAll().getFirst().getRequestId()).isNotBlank();
    }

    @Test
    void shouldReturnExistingBookingForDuplicateRequestId() throws Exception {
        // given
        Map<String, Object> request = Map.of(
                "roomId", ROOM_ID.toString(),
                "autoSelect", false,
                "startDate", "2027-06-10",
                "endDate", "2027-06-15",
                "requestId", "duplicate-request");

        // when две одинаковые доставки
        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then создана одна бронь, Hotel Service вызван один раз
        assertThat(bookingRepository.count()).isEqualTo(1);
        verify(hotelGateway, times(1)).confirmAvailability(any(), any(), any(), anyString());
    }

    @Test
    void shouldCompensateWhenSlotConfirmationFails() throws Exception {
        // given
        doThrow(new RetryableHotelServiceException("Сервис отелей недоступен"))
                .when(hotelGateway).confirmBookingSlots(anyString(), any(), anyString());

        Map<String, Object> request = Map.of(
                "roomId", ROOM_ID.toString(),
                "autoSelect", false,
                "startDate", "2027-07-01",
                "endDate", "2027-07-05",
                "requestId", "slot-confirm-fails");

        // when
        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then бронь отменена, блокировка снята
        assertThat(bookingRepository.findAll().getFirst().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
        verify(hotelGateway).release(any(), any(), any(), anyString());
    }

    @Test
    void shouldCancelBookingWhenHotelUnavailable() throws Exception {
        // given
        when(hotelGateway.confirmAvailability(any(), any(), any(), anyString()))
                .thenThrow(new RetryableHotelServiceException("Сервис отелей недоступен"));

        Map<String, Object> request = Map.of(
                "roomId", ROOM_ID.toString(),
                "autoSelect", false,
                "startDate", "2027-08-01",
                "endDate", "2027-08-05",
                "requestId", "hotel-down");

        // when
        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // then причина сохранена в брони
        assertThat(bookingRepository.findAll().getFirst().getFailureReason()).isNotBlank();
    }

    @Test
    void shouldStoreFailureReasonWithoutDuplicatedPrefix() throws Exception {
        // given
        String message = "Сервис отелей недоступен при подтверждении";
        when(hotelGateway.confirmAvailability(any(), any(), any(), anyString()))
                .thenThrow(new RetryableHotelServiceException(message));

        Map<String, Object> request = Map.of(
                "roomId", ROOM_ID.toString(),
                "autoSelect", false,
                "startDate", "2027-09-01",
                "endDate", "2027-09-05",
                "requestId", "prefix-check");

        // when
        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then
        assertThat(bookingRepository.findAll().getFirst().getFailureReason()).isEqualTo(message);
    }

    private RoomDto room() {
        return RoomDto.builder()
                .id(ROOM_ID)
                .hotelId(HOTEL_ID)
                .hotelName("Тестовый отель")
                .number("101")
                .roomType("DOUBLE")
                .capacity(2)
                .pricePerNight(5000.0)
                .available(true)
                .timesBooked(0L)
                .build();
    }

    private AvailabilityDto availability(String status) {
        return AvailabilityDto.builder()
                .roomId(ROOM_ID)
                .requestId("req")
                .status(status)
                .replayed(false)
                .createdAt(Instant.now())
                .build();
    }
}
