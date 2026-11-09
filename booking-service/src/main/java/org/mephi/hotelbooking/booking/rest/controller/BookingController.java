package org.mephi.hotelbooking.booking.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.booking.config.TraceIdFilter;
import org.mephi.hotelbooking.booking.domain.enums.BookingStatus;
import org.mephi.hotelbooking.booking.rest.api.BookingApi;
import org.mephi.hotelbooking.booking.rest.dto.BookingResponse;
import org.mephi.hotelbooking.booking.rest.dto.CreateBookingRequest;
import org.mephi.hotelbooking.booking.rest.dto.PageResponse;
import org.mephi.hotelbooking.booking.service.BookingService;
import org.mephi.hotelbooking.booking.service.CurrentUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BookingController implements BookingApi {

    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    @Override
    public ResponseEntity<BookingResponse> createBooking(CreateBookingRequest request) {
        UUID userId = currentUserService.getCurrentUserId();
        String username = currentUserService.getCurrentUsername();
        String traceId = TraceIdFilter.currentTraceId();
        String bearerToken = currentUserService.getBearerToken();

        log.info("[saga] Запрос на создание бронирования: userId={}, autoSelect={}, roomId={}, период {} - {}, traceId={}",
                userId, request.getAutoSelect(), request.getRoomId(),
                request.getStartDate(), request.getEndDate(), traceId);

        BookingResponse response = bookingService.createBooking(userId, username, request, traceId, bearerToken);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PageResponse<BookingResponse>> getUserBookings(BookingStatus status, Pageable pageable) {
        UUID userId = currentUserService.getCurrentUserId();
        String username = currentUserService.getCurrentUsername();

        log.info("История бронирований пользователя: {}, статус: {}", username, status);

        return ResponseEntity.ok(bookingService.getUserBookings(userId, username, status, pageable));
    }

    @Override
    public ResponseEntity<BookingResponse> getBookingById(UUID bookingId) {
        UUID userId = currentUserService.getCurrentUserId();
        boolean admin = currentUserService.isAdmin();
        String traceId = TraceIdFilter.currentTraceId();
        String bearerToken = currentUserService.getBearerToken();

        log.info("Получение бронирования: {}, запросил: {}", bookingId, userId);

        return ResponseEntity.ok(bookingService.getBooking(bookingId, userId, admin, traceId, bearerToken));
    }

    @Override
    public ResponseEntity<BookingResponse> cancelBooking(UUID bookingId) {
        UUID userId = currentUserService.getCurrentUserId();
        boolean admin = currentUserService.isAdmin();
        String traceId = TraceIdFilter.currentTraceId();
        String bearerToken = currentUserService.getBearerToken();

        log.info("[saga] Отмена бронирования: {}, запросил: {}, traceId={}", bookingId, userId, traceId);

        return ResponseEntity.ok(bookingService.cancelBooking(bookingId, userId, admin, traceId, bearerToken));
    }
}
