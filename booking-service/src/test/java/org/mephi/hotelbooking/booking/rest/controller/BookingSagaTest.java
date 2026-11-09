package org.mephi.hotelbooking.booking.rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mephi.hotelbooking.booking.BaseTest;
import org.mephi.hotelbooking.booking.client.HotelServiceGateway;
import org.mephi.hotelbooking.booking.client.dto.AvailabilityDto;
import org.mephi.hotelbooking.booking.client.dto.RoomDto;

import org.mephi.hotelbooking.booking.client.exception.RetryableHotelServiceException;
import org.mephi.hotelbooking.booking.client.exception.RoomUnavailableException;
import org.mephi.hotelbooking.booking.domain.entity.Booking;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mephi.hotelbooking.booking.TestSecurity.userJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingSagaTest extends BaseTest {

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
    }

    @Test
    void shouldConfirmBookingWhenHotelConfirmsAvailability() throws Exception {
        // given
        when(hotelGateway.confirmAvailability(eq(ROOM_ID), any(), any(), anyString()))
                .thenReturn(availability("HOLD"));

        Map<String, Object> request = bookingRequest("2027-06-01", "2027-06-05", "req-ok");

        // when & then
        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.roomId").value(ROOM_ID.toString()));

        Booking saved = bookingRepository.findAll().getFirst();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(saved.getFailureReason()).isNull();
    }

    @Test
    void shouldCompensateWhenRoomIsUnavailable() throws Exception {
        // given
        when(hotelGateway.confirmAvailability(eq(ROOM_ID), any(), any(), anyString()))
                .thenThrow(new RoomUnavailableException("Номер занят на запрошенные даты"));

        Map<String, Object> request = bookingRequest("2027-06-01", "2027-06-05", "req-conflict");

        // when & then
        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.failureReason").value("Номер занят на запрошенные даты"));

        Booking saved = bookingRepository.findAll().getFirst();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        // компенсация обязательно снимает блокировку
        verify(hotelGateway, times(1)).release(eq(ROOM_ID), any(), any(), anyString());
    }

    @Test
    void shouldCompensateWhenHotelServiceIsDown() throws Exception {
        // given Hotel Service недоступен после повторов
        when(hotelGateway.confirmAvailability(eq(ROOM_ID), any(), any(), anyString()))
                .thenThrow(new RetryableHotelServiceException("Hotel Service недоступен"));

        Map<String, Object> request = bookingRequest("2027-06-01", "2027-06-05", "req-down");

        // when & then
        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Booking saved = bookingRepository.findAll().getFirst();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(saved.getFailureReason()).contains("Hotel Service недоступен");
    }

    @Test
    void shouldBeIdempotentForRepeatedRequestId() throws Exception {
        // given
        when(hotelGateway.confirmAvailability(eq(ROOM_ID), any(), any(), anyString()))
                .thenReturn(availability("HOLD"));

        Map<String, Object> request = bookingRequest("2027-06-01", "2027-06-05", "req-repeat");

        // when два одинаковых запроса
        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // then создана ровно одна бронь, второй раз Hotel Service не вызывался
        assertThat(bookingRepository.count()).isEqualTo(1);
        verify(hotelGateway, times(1)).confirmAvailability(eq(ROOM_ID), any(), any(), anyString());
    }

    @Test
    void shouldUseAutoSelectedRoom() throws Exception {
        // given
        when(hotelGateway.confirmAvailability(any(), any(), any(), anyString()))
                .thenReturn(availability("HOLD"));

        Map<String, Object> request = Map.of(
                "autoSelect", true,
                "startDate", "2027-06-01",
                "endDate", "2027-06-05",
                "requestId", "req-auto");

        // when & then
        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.roomId").value(ROOM_ID.toString()));

        verify(hotelGateway, times(1)).getRecommendedRooms(any(), any(), any(), anyString());
    }

    @Test
    void shouldRejectBookingWithoutRoomAndAutoSelect() throws Exception {
        // given ни roomId, ни autoSelect
        Map<String, Object> request = Map.of(
                "startDate", "2027-06-01",
                "endDate", "2027-06-05");

        // when & then
        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(bookingRepository.count()).isZero();
        verify(hotelGateway, never()).confirmAvailability(any(), any(), any(), anyString());
    }

    @Test
    void shouldRejectInvalidDateRange() throws Exception {
        // given выезд раньше заезда
        Map<String, Object> request = bookingRequest("2027-06-10", "2027-06-05", "req-bad-dates");

        // when & then
        mockMvc.perform(post("/booking")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(bookingRepository.count()).isZero();
    }

    private Map<String, Object> bookingRequest(String start, String end, String requestId) {
        return Map.of(
                "roomId", ROOM_ID.toString(),
                "autoSelect", false,
                "startDate", start,
                "endDate", end,
                "requestId", requestId);
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
