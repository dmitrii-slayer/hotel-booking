package org.mephi.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.booking.domain.entity.Booking;
import org.mephi.hotelbooking.booking.domain.enums.BookingStatus;
import org.mephi.hotelbooking.booking.domain.repository.BookingRepository;
import org.mephi.hotelbooking.booking.exception.EntityNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingStateService {

    private final BookingRepository bookingRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Booking markConfirmed(UUID bookingId) {
        Booking booking = find(bookingId);

        if (booking.isConfirmed()) {
            log.info("[saga][{}] Повторное подтверждение проигнорировано (идемпотентность)", bookingId);
            return booking;
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(Instant.now());

        return bookingRepository.save(booking);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Booking markCancelled(UUID bookingId, String reason) {
        Booking booking = find(bookingId);

        if (booking.isCancelled()) {
            log.info("[saga][{}] Повторная отмена проигнорирована (идемпотентность)", bookingId);
            return booking;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setFailureReason(truncate(reason));
        booking.setUpdatedAt(Instant.now());

        return bookingRepository.save(booking);
    }

    private Booking find(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Бронирование не найдено: " + bookingId));
    }

    private String truncate(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
