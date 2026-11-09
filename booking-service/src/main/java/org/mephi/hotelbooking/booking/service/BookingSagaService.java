package org.mephi.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.booking.client.HotelServiceGateway;
import org.mephi.hotelbooking.booking.client.dto.AvailabilityDto;
import org.mephi.hotelbooking.booking.client.dto.ConfirmAvailabilityDto;
import org.mephi.hotelbooking.booking.client.dto.ReleaseDto;
import org.mephi.hotelbooking.booking.client.dto.RoomDto;
import org.mephi.hotelbooking.booking.client.exception.HotelServiceException;
import org.mephi.hotelbooking.booking.client.exception.RoomUnavailableException;
import org.mephi.hotelbooking.booking.domain.entity.Booking;
import org.mephi.hotelbooking.booking.domain.enums.BookingStatus;
import org.mephi.hotelbooking.booking.domain.repository.BookingRepository;
import org.mephi.hotelbooking.booking.exception.SagaFailedException;
import org.mephi.hotelbooking.booking.rest.dto.CreateBookingRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingSagaService {

    private final BookingRepository bookingRepository;
    private final BookingStateService stateService;
    private final HotelServiceGateway hotelGateway;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Booking createPending(UUID userId, UUID roomId, UUID hotelId,
                                 CreateBookingRequest request, String traceId) {
        Booking booking = Booking.builder()
                .userId(userId)
                .roomId(roomId)
                .hotelId(hotelId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(BookingStatus.PENDING)
                .requestId(request.getRequestId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Booking saved = bookingRepository.saveAndFlush(booking);

        log.info("[saga][{}] Шаг 1: бронирование создано в PENDING: roomId={}, requestId={}, traceId={}",
                saved.getId(), roomId, request.getRequestId(), traceId);

        return saved;
    }

    public Booking confirmOrCompensate(Booking booking, String traceId, String bearerToken) {
        UUID bookingId = booking.getId();
        UUID roomId = booking.getRoomId();

        try {
            AvailabilityDto availability = requestAvailability(booking, roomId, traceId, bearerToken);

            log.info("[saga][{}] Шаг 2: Hotel Service подтвердил доступность: status={}, replayed={}",
                    bookingId, availability.getStatus(), availability.getReplayed());

            if (!isHolding(availability)) {
                log.warn("[saga][{}] Шаг 2: слот в статусе {} - компенсация",
                        bookingId, availability.getStatus());
                return compensate(booking, "Слот недоступен: " + availability.getStatus(), traceId, bearerToken);
            }

            Booking confirmed = stateService.markConfirmed(bookingId);
            confirmSlots(bookingId, traceId, bearerToken);

            log.info("[saga][{}] Шаг 3: бронирование CONFIRMED", bookingId);

            return confirmed;

        } catch (RoomUnavailableException ex) {
            log.warn("[saga][{}] Шаг 2: номер занят - компенсация. Причина: {}", bookingId, ex.getMessage());
            return compensate(booking, ex.getMessage(), traceId, bearerToken);

        } catch (HotelServiceException ex) {
            log.error("[saga][{}] Шаг 2: сбой Hotel Service - компенсация. Причина: {}",
                    bookingId, ex.getMessage());
            return compensate(booking, ex.getMessage(), traceId, bearerToken);

        } catch (Exception ex) {
            log.error("[saga][{}] Шаг 2: неожиданная ошибка - компенсация", bookingId, ex);
            return compensate(booking, "Внутренняя ошибка: " + ex.getMessage(), traceId, bearerToken);
        }
    }

    private boolean isHolding(AvailabilityDto availability) {
        return "HOLD".equals(availability.getStatus()) || "CONFIRMED".equals(availability.getStatus());
    }

    private Booking compensate(Booking booking, String reason, String traceId, String bearerToken) {
        Booking cancelled = stateService.markCancelled(booking.getId(), reason);

        log.warn("[saga][{}] Компенсация: бронирование CANCELLED. Причина: {}", booking.getId(), reason);

        releaseQuietly(booking, traceId, bearerToken);

        return cancelled;
    }

    private AvailabilityDto requestAvailability(Booking booking, UUID roomId,
                                                String traceId, String bearerToken) {
        ConfirmAvailabilityDto request = ConfirmAvailabilityDto.builder()
                .requestId(booking.getRequestId())
                .bookingId(booking.getId().toString())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .build();

        return hotelGateway.confirmAvailability(roomId, request, traceId, bearerToken);
    }

    private void confirmSlots(UUID bookingId, String traceId, String bearerToken) {
        hotelGateway.confirmBookingSlots(bookingId.toString(), traceId, bearerToken);
    }

    private void releaseQuietly(Booking booking, String traceId, String bearerToken) {
        ReleaseDto request = ReleaseDto.builder()
                .requestId(booking.getRequestId())
                .bookingId(booking.getId().toString())
                .build();

        try {
            hotelGateway.release(booking.getRoomId(), request, traceId, bearerToken);
            log.info("[saga][{}] Компенсация: блокировка слота снята", booking.getId());
        } catch (Exception ex) {
            log.error("[saga][{}] Компенсация не удалась, требуется ручной разбор: {}",
                    booking.getId(), ex.getMessage());
        }
    }

    public RoomDto selectRoom(LocalDate startDate, LocalDate endDate, String traceId, String bearerToken) {
        List<RoomDto> rooms = hotelGateway.getRecommendedRooms(startDate, endDate, traceId, bearerToken);

        return rooms.stream()
                .filter(room -> Boolean.TRUE.equals(room.getAvailable()))
                .min(Comparator.comparing(RoomDto::getTimesBooked, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RoomDto::getId))
                .orElseThrow(() -> new SagaFailedException("Нет доступных номеров для бронирования"));
    }
}
