package org.mephi.hotelbooking.hotel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.hotel.domain.entity.Reservation;
import org.mephi.hotelbooking.hotel.domain.entity.Room;
import org.mephi.hotelbooking.hotel.domain.enums.ReservationStatus;
import org.mephi.hotelbooking.hotel.domain.repository.ReservationRepository;
import org.mephi.hotelbooking.hotel.domain.repository.RoomRepository;
import org.mephi.hotelbooking.hotel.exception.BookingConflictException;
import org.mephi.hotelbooking.hotel.exception.EntityNotFoundException;
import org.mephi.hotelbooking.hotel.exception.InvalidDateRangeException;
import org.mephi.hotelbooking.hotel.exception.RoomNotAvailableException;
import org.mephi.hotelbooking.hotel.rest.dto.AvailabilityResponse;
import org.mephi.hotelbooking.hotel.rest.dto.ConfirmAvailabilityRequest;
import org.mephi.hotelbooking.hotel.rest.dto.ReleaseRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public AvailabilityResponse confirmAvailability(UUID roomId, ConfirmAvailabilityRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());

        Room room = lockRoom(roomId);

        if (!Boolean.TRUE.equals(room.getAvailable())) {
            throw new RoomNotAvailableException(
                    "Номер выведен из эксплуатации: " + room.getNumber());
        }

        Reservation existing = reservationRepository
                .findByRoomIdAndRequestId(roomId, request.getRequestId())
                .orElse(null);

        if (existing != null) {
            if (!samePeriod(existing, request)) {
                throw new InvalidDateRangeException(
                        "requestId уже использован для другого периода: " + existing.getStartDate()
                                + " - " + existing.getEndDate());
            }

            log.info("Повторное подтверждение проигнорировано (идемпотентность): roomId={}, requestId={}, status={}",
                    roomId, request.getRequestId(), existing.getStatus());

            return toResponse(existing, true);
        }

        List<Reservation> conflicts = reservationRepository.findConflicting(
                roomId, request.getStartDate(), request.getEndDate());

        if (!conflicts.isEmpty()) {
            throw new BookingConflictException(
                    "Номер " + room.getNumber() + " уже занят на запрошенные даты: "
                            + request.getStartDate() + " - " + request.getEndDate());
        }

        Reservation reservation = Reservation.builder()
                .roomId(roomId)
                .requestId(request.getRequestId())
                .bookingId(request.getBookingId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(ReservationStatus.HOLD)
                .createdAt(Instant.now())
                .build();

        Reservation saved = reservationRepository.save(reservation);

        room.incrementTimesBooked();
        roomRepository.save(room);

        log.info("Слот заблокирован: bookingId={}, roomId={}, requestId={}, {} - {}",
                request.getBookingId(), roomId, request.getRequestId(),
                request.getStartDate(), request.getEndDate());

        return toResponse(saved, false);
    }

    @Transactional
    public AvailabilityResponse release(UUID roomId, ReleaseRequest request) {
        Room room = lockRoom(roomId);

        Reservation reservation = reservationRepository
                .findByRoomIdAndRequestId(roomId, request.getRequestId())
                .orElse(null);

        if (reservation == null) {
            log.warn("Компенсация: слот не найден, повторное снятие не требуется. roomId={}, requestId={}",
                    roomId, request.getRequestId());

            return AvailabilityResponse.builder()
                    .roomId(roomId)
                    .requestId(request.getRequestId())
                    .bookingId(request.getBookingId())
                    .replayed(true)
                    .build();
        }

        if (reservation.getStatus() != ReservationStatus.RELEASED) {
            reservation.setStatus(ReservationStatus.RELEASED);
            reservationRepository.save(reservation);

            if (room.getTimesBooked() > 0) {
                room.setTimesBooked(room.getTimesBooked() - 1);
                roomRepository.save(room);
            }

            log.info("Компенсация выполнена: слот снят. bookingId={}, roomId={}, requestId={}",
                    request.getBookingId(), roomId, request.getRequestId());

            return toResponse(reservation, false);
        }

        log.info("Компенсация уже выполнялась (идемпотентность): roomId={}, requestId={}",
                roomId, request.getRequestId());

        return toResponse(reservation, true);
    }

    @Transactional
    public void confirmBooking(String bookingId) {
        List<Reservation> reservations = reservationRepository.findByBookingId(bookingId);

        for (Reservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.HOLD) {
                reservation.setStatus(ReservationStatus.CONFIRMED);
                reservationRepository.save(reservation);
            }
        }

        log.info("Слоты бронирования переведены в CONFIRMED: bookingId={}, количество={}",
                bookingId, reservations.size());
    }

    private Room lockRoom(UUID roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Номер не найден: " + roomId));
    }

    private boolean samePeriod(Reservation reservation, ConfirmAvailabilityRequest request) {
        return reservation.getStartDate().equals(request.getStartDate())
                && reservation.getEndDate().equals(request.getEndDate());
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (!startDate.isBefore(endDate)) {
            throw new InvalidDateRangeException(
                    "Дата выезда должна быть позже даты заезда: " + startDate + " - " + endDate);
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new InvalidDateRangeException("Дата заезда не может быть в прошлом: " + startDate);
        }
    }

    private AvailabilityResponse toResponse(Reservation reservation, boolean replayed) {
        return AvailabilityResponse.builder()
                .reservationId(reservation.getId())
                .roomId(reservation.getRoomId())
                .requestId(reservation.getRequestId())
                .bookingId(reservation.getBookingId())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .status(reservation.getStatus())
                .replayed(replayed)
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
