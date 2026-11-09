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
import static org.mephi.hotelbooking.hotel.TestSecurity.adminJwt;
import static org.mephi.hotelbooking.hotel.TestSecurity.userJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HotelControllerTest extends BaseTest {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void clearDatabase() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        hotelRepository.deleteAll();
    }

    @Test
    void shouldCreateHotelAsAdmin() throws Exception {
        // given
        Map<String, Object> request = Map.of(
                "name", "Гранд Отель",
                "address", "г. Москва, ул. Тверская, д. 1",
                "description", "Пятизвёздочный отель");

        // when & then
        mockMvc.perform(post("/api/hotels")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Гранд Отель"))
                .andExpect(jsonPath("$.id").isNotEmpty());

        assertThat(hotelRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectHotelCreationForUser() throws Exception {
        // given
        Map<String, Object> request = Map.of("name", "Отель", "address", "Адрес");

        // when & then
        mockMvc.perform(post("/api/hotels")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(hotelRepository.count()).isZero();
    }

    @Test
    void shouldRejectHotelCreationWithoutToken() throws Exception {
        // given
        Map<String, Object> request = Map.of("name", "Отель", "address", "Адрес");

        // when & then
        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnValidationErrorWithDetails() throws Exception {
        // given
        Map<String, Object> request = Map.of("name", "", "address", "");

        // when & then
        mockMvc.perform(post("/api/hotels")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.name").isNotEmpty());
    }

    @Test
    void shouldReturnNotFoundForUnknownHotel() throws Exception {
        // when & then
        mockMvc.perform(get("/api/hotels/00000000-0000-0000-0000-000000000000")
                        .with(userJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnRoomsSortedByTimesBooked() throws Exception {
        // given
        Hotel hotel = hotelRepository.save(hotel());
        roomRepository.save(room(hotel, "101", 7L));
        roomRepository.save(room(hotel, "102", 0L));
        roomRepository.save(room(hotel, "103", 3L));

        // when & then
        mockMvc.perform(get("/api/rooms/recommend").with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("102"))
                .andExpect(jsonPath("$[0].timesBooked").value(0))
                .andExpect(jsonPath("$[1].number").value("103"))
                .andExpect(jsonPath("$[2].number").value("101"));
    }

    @Test
    void shouldUpdateRoomPartially() throws Exception {
        // given
        Hotel hotel = hotelRepository.save(hotel());
        Room room = roomRepository.save(room(hotel, "101", 2L));

        // when & then
        mockMvc.perform(patch("/api/rooms/{id}", room.getId())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pricePerNight\": 9999.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricePerNight").value(9999.00))
                .andExpect(jsonPath("$.number").value("101"));
    }

    @Test
    void shouldReturnStatisticsForAdmin() throws Exception {
        // given
        Hotel hotel = hotelRepository.save(hotel());
        roomRepository.save(room(hotel, "101", 5L));

        // when & then
        mockMvc.perform(get("/api/rooms/statistics").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("101"))
                .andExpect(jsonPath("$[0].timesBooked").value(5));
    }

    @Test
    void shouldDenyStatisticsForUser() throws Exception {
        mockMvc.perform(get("/api/rooms/statistics").with(userJwt()))
                .andExpect(status().isForbidden());
    }

    private Hotel hotel() {
        return Hotel.builder()
                .name("Тестовый отель")
                .address("г. Тест, ул. Тестовая, д. 1")
                .description("Для тестов")
                .createdAt(Instant.now())
                .build();
    }

    private Room room(Hotel hotel, String number, long timesBooked) {
        return Room.builder()
                .hotelId(hotel.getId())
                .number(number)
                .roomType(RoomType.DOUBLE)
                .capacity(2)
                .pricePerNight(new BigDecimal("5000.00"))
                .available(true)
                .timesBooked(timesBooked)
                .createdAt(Instant.now())
                .build();
    }
}
