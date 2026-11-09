package org.mephi.hotelbooking.hotel.rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mephi.hotelbooking.hotel.BaseTest;
import org.mephi.hotelbooking.hotel.domain.entity.Hotel;
import org.mephi.hotelbooking.hotel.domain.entity.Room;
import org.mephi.hotelbooking.hotel.domain.enums.RoomType;
import org.mephi.hotelbooking.hotel.domain.repository.HotelRepository;
import org.mephi.hotelbooking.hotel.domain.repository.ReservationRepository;
import org.mephi.hotelbooking.hotel.domain.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mephi.hotelbooking.hotel.TestSecurity.userJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvailabilityControllerTest extends BaseTest {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Room room;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        hotelRepository.deleteAll();

        Hotel hotel = hotelRepository.save(Hotel.builder()
                .name("Тестовый отель")
                .address("Адрес")
                .createdAt(Instant.now())
                .build());

        room = roomRepository.save(Room.builder()
                .hotelId(hotel.getId())
                .number("101")
                .roomType(RoomType.DOUBLE)
                .capacity(2)
                .pricePerNight(new BigDecimal("5000.00"))
                .available(true)
                .timesBooked(0L)
                .createdAt(Instant.now())
                .build());
    }

    @Test
    void shouldConfirmAvailabilityAndCreateHold() throws Exception {
        // given
        Map<String, Object> request = confirmRequest("req-1", "2027-05-01", "2027-05-05");

        // when & then
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HOLD"))
                .andExpect(jsonPath("$.replayed").value(false));

        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(1L);
    }

    @Test
    void shouldBeIdempotentForSameRequestId() throws Exception {
        // given
        Map<String, Object> request = confirmRequest("req-same", "2027-05-01", "2027-05-05");

        // when
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then повторная доставка не создаёт второй слот
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true));

        assertThat(reservationRepository.count()).isEqualTo(1);
        // timesBooked не должен увеличиться дважды
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(1L);
    }

    @Test
    void shouldReturnConflictForOverlappingDates() throws Exception {
        // given
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                confirmRequest("req-a", "2027-05-01", "2027-05-05"))))
                .andExpect(status().isOk());

        // when & then: частичное пересечение
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                confirmRequest("req-b", "2027-05-03", "2027-05-07"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(reservationRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldAllowBackToBackDatesWithoutOverlap() throws Exception {
        // given
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                confirmRequest("req-1", "2027-05-01", "2027-05-05"))))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                confirmRequest("req-2", "2027-05-05", "2027-05-09"))))
                .andExpect(status().isOk());

        assertThat(reservationRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldRejectInvalidDateRange() throws Exception {
        // given выезд раньше заезда
        Map<String, Object> request = confirmRequest("req-bad", "2027-05-05", "2027-05-01");

        // when & then
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectPastDates() throws Exception {
        // given
        Map<String, Object> request = confirmRequest("req-past", "2020-01-01", "2020-01-05");

        // when & then
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReleaseAndBeIdempotent() throws Exception {
        // given
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                confirmRequest("req-rel", "2027-05-01", "2027-05-05"))))
                .andExpect(status().isOk());

        Map<String, Object> release = Map.of("requestId", "req-rel", "bookingId", "book-1");

        // when & then первое снятие
        mockMvc.perform(post("/api/rooms/{id}/release", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(release)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"))
                .andExpect(jsonPath("$.replayed").value(false));

        // повторное снятие - идемпотентно
        mockMvc.perform(post("/api/rooms/{id}/release", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(release)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true));

        // после снятия даты снова свободны
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                confirmRequest("req-new", "2027-05-01", "2027-05-05"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundForUnknownRoom() throws Exception {
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability",
                         "00000000-0000-0000-0000-000000000000")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                confirmRequest("req-x", "2027-05-01", "2027-05-05"))))
                .andExpect(status().isNotFound());
    }

    private Map<String, Object> confirmRequest(String requestId, String start, String end) {
        return Map.of(
                "requestId", requestId,
                "bookingId", "book-" + requestId,
                "startDate", start,
                "endDate", end);
    }
}
