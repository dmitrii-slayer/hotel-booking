package org.mephi.hotelbooking.booking.rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mephi.hotelbooking.booking.BaseTest;
import org.mephi.hotelbooking.booking.client.HotelServiceGateway;
import org.mephi.hotelbooking.booking.domain.entity.Booking;
import org.mephi.hotelbooking.booking.domain.enums.BookingStatus;
import org.mephi.hotelbooking.booking.domain.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mephi.hotelbooking.booking.TestSecurity.OTHER_USER_ID;
import static org.mephi.hotelbooking.booking.TestSecurity.USER_ID;
import static org.mephi.hotelbooking.booking.TestSecurity.adminJwt;
import static org.mephi.hotelbooking.booking.TestSecurity.otherUserJwt;
import static org.mephi.hotelbooking.booking.TestSecurity.userJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingAccessTest extends BaseTest {

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoBean
    private HotelServiceGateway hotelGateway;

    private Booking ownBooking;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        ownBooking = bookingRepository.save(booking(UUID.fromString(USER_ID)));
    }

    @Test
    void shouldReturnOwnBooking() throws Exception {
        mockMvc.perform(get("/booking/{id}", ownBooking.getId()).with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownBooking.getId().toString()));
    }

    @Test
    void shouldForbidReadingOtherUsersBooking() throws Exception {
        mockMvc.perform(get("/booking/{id}", ownBooking.getId()).with(otherUserJwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldAllowAdminToReadAnyBooking() throws Exception {
        mockMvc.perform(get("/booking/{id}", ownBooking.getId()).with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnOnlyOwnBookingsInHistory() throws Exception {
        // given есть бронь другого пользователя
        bookingRepository.save(booking(UUID.fromString(OTHER_USER_ID)));

        // when & then пользователь видит только свою
        mockMvc.perform(get("/bookings").with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ownBooking.getId().toString()));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/bookings"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/booking/{id}", ownBooking.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyUserAccessToAdminEndpoints() throws Exception {
        mockMvc.perform(get("/users").with(userJwt()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/user/{id}", UUID.randomUUID()).with(userJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminAccessToUsers() throws Exception {
        mockMvc.perform(get("/users").with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldCancelOwnBookingAndReleaseSlot() throws Exception {
        // given
        when(hotelGateway.release(any(), any(), any(), anyString())).thenReturn(null);

        // when & then
        mockMvc.perform(delete("/booking/{id}", ownBooking.getId()).with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Booking cancelled = bookingRepository.findById(ownBooking.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(hotelGateway, times(1)).release(eq(ownBooking.getRoomId()), any(), any(), anyString());
    }

    @Test
    void shouldForbidCancellingOtherUsersBooking() throws Exception {
        mockMvc.perform(delete("/booking/{id}", ownBooking.getId()).with(otherUserJwt()))
                .andExpect(status().isForbidden());

        Booking untouched = bookingRepository.findById(ownBooking.getId()).orElseThrow();
        assertThat(untouched.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void shouldReturnNotFoundForUnknownBooking() throws Exception {
        mockMvc.perform(get("/booking/{id}", UUID.randomUUID()).with(userJwt()))
                .andExpect(status().isNotFound());
    }

    private Booking booking(UUID userId) {
        return Booking.builder()
                .userId(userId)
                .roomId(UUID.fromString("d473a3ef-f765-499f-9463-f2c101d94898"))
                .startDate(LocalDate.of(2027, 6, 1))
                .endDate(LocalDate.of(2027, 6, 5))
                .status(BookingStatus.CONFIRMED)
                .requestId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
