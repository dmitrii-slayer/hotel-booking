package org.mephi.hotelbooking.hotel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.hotel.domain.entity.Hotel;
import org.mephi.hotelbooking.hotel.domain.entity.Reservation;
import org.mephi.hotelbooking.hotel.domain.entity.Room;
import org.mephi.hotelbooking.hotel.domain.repository.HotelRepository;
import org.mephi.hotelbooking.hotel.domain.repository.ReservationRepository;
import org.mephi.hotelbooking.hotel.domain.repository.RoomRepository;
import org.mephi.hotelbooking.hotel.exception.EntityNotFoundException;
import org.mephi.hotelbooking.hotel.exception.InvalidDateRangeException;
import org.mephi.hotelbooking.hotel.rest.dto.CreateRoomRequest;
import org.mephi.hotelbooking.hotel.rest.dto.RoomLoadResponse;
import org.mephi.hotelbooking.hotel.rest.dto.RoomResponse;
import org.mephi.hotelbooking.hotel.rest.dto.UpdateRoomRequest;
import org.mephi.hotelbooking.hotel.rest.mapper.RoomMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final ReservationRepository reservationRepository;
    private final RoomMapper roomMapper;

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new EntityNotFoundException("Отель не найден: " + request.getHotelId()));

        if (roomRepository.existsByHotelIdAndNumber(hotel.getId(), request.getNumber())) {
            throw new IllegalArgumentException(
                    "Номер " + request.getNumber() + " уже существует в отеле " + hotel.getName());
        }

        Room room = roomMapper.toEntity(request, hotel.getId());
        Room saved = roomRepository.save(room);

        log.info("Номер создан: {} в отеле {} (ID: {})", saved.getNumber(), hotel.getName(), saved.getId());

        return roomMapper.toResponse(saved, hotel.getName());
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByHotel(UUID hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new EntityNotFoundException("Отель не найден: " + hotelId));

        return roomRepository.findByHotelId(hotelId).stream()
                .map(room -> roomMapper.toResponse(room, hotel.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(UUID roomId) {
        Room room = findRoom(roomId);
        return roomMapper.toResponse(room, findHotelName(room.getHotelId()));
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRecommendedRooms(LocalDate startDate, LocalDate endDate) {
        return findFreeRooms(startDate, endDate).stream()
                .sorted(Comparator.comparing(Room::getTimesBooked)
                        .thenComparing(Room::getId))
                .map(room -> roomMapper.toResponse(room, findHotelName(room.getHotelId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(LocalDate startDate, LocalDate endDate) {
        return findFreeRooms(startDate, endDate).stream()
                .map(room -> roomMapper.toResponse(room, findHotelName(room.getHotelId())))
                .toList();
    }

    private List<Room> findFreeRooms(LocalDate startDate, LocalDate endDate) {
        List<Room> rooms = roomRepository.findAllByAvailableTrue();

        if (startDate == null && endDate == null) {
            return rooms;
        }

        validateHorizon(startDate, endDate);

        return rooms.stream()
                .filter(room -> isFree(room.getId(), startDate, endDate))
                .toList();
    }

    private void validateHorizon(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidDateRangeException("Укажите обе даты или ни одной");
        }

        if (!startDate.isBefore(endDate)) {
            throw new InvalidDateRangeException(
                    "Дата выезда должна быть позже даты заезда: " + startDate + " - " + endDate);
        }
    }

    @Transactional
    public RoomResponse updateRoom(UUID roomId, UpdateRoomRequest request) {
        Room room = findRoom(roomId);

        if (request.getNumber() != null
                && !request.getNumber().equals(room.getNumber())
                && roomRepository.existsByHotelIdAndNumber(room.getHotelId(), request.getNumber())) {
            throw new IllegalArgumentException(
                    "Номер " + request.getNumber() + " уже существует в этом отеле");
        }

        roomMapper.updateEntity(request, room);
        Room saved = roomRepository.save(room);

        log.info("Номер обновлён: {} (ID: {})", saved.getNumber(), saved.getId());

        return roomMapper.toResponse(saved, findHotelName(saved.getHotelId()));
    }

    @Transactional
    public void deleteRoom(UUID roomId) {
        Room room = findRoom(roomId);

        reservationRepository.deleteAll(reservationRepository.findByRoomId(roomId));
        roomRepository.delete(room);

        log.info("Номер удалён: {} (ID: {}), слоты очищены", room.getNumber(), roomId);
    }

    @Transactional(readOnly = true)
    public List<RoomLoadResponse> getStatistics(LocalDate horizonStart, LocalDate horizonEnd) {
        LocalDate start = horizonStart != null ? horizonStart : LocalDate.now();
        LocalDate end = horizonEnd != null ? horizonEnd : start.plusMonths(1);

        if (!start.isBefore(end)) {
            throw new InvalidDateRangeException(
                    "Начало горизонта должно быть раньше конца: " + start + " - " + end);
        }

        long totalDays = ChronoUnit.DAYS.between(start, end);

        return roomRepository.findAll().stream()
                .map(room -> buildLoad(room, start, end, totalDays))
                .sorted(Comparator.comparing(RoomLoadResponse::getTimesBooked).reversed())
                .toList();
    }

    private RoomLoadResponse buildLoad(Room room, LocalDate start, LocalDate end, long totalDays) {
        List<Reservation> reservations = reservationRepository.findConflicting(room.getId(), start, end);

        long bookedNights = reservations.stream()
                .mapToLong(reservation -> ChronoUnit.DAYS.between(
                        max(reservation.getStartDate(), start),
                        min(reservation.getEndDate(), end)))
                .sum();

        double loadPercent = Math.round((bookedNights * 100.0 / totalDays) * 100.0) / 100.0;

        return RoomLoadResponse.builder()
                .roomId(room.getId())
                .number(room.getNumber())
                .hotelName(findHotelName(room.getHotelId()))
                .timesBooked(room.getTimesBooked())
                .activeReservations((long) reservations.size())
                .bookedNights(bookedNights)
                .loadPercent(loadPercent)
                .horizonStart(start)
                .horizonEnd(end)
                .calculatedAt(Instant.now())
                .build();
    }

    private boolean isFree(UUID roomId, LocalDate startDate, LocalDate endDate) {
        return reservationRepository.findConflicting(roomId, startDate, endDate).isEmpty();
    }

    private LocalDate max(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private String findHotelName(UUID hotelId) {
        return hotelRepository.findById(hotelId)
                .map(Hotel::getName)
                .orElse("Неизвестный отель");
    }

    private Room findRoom(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Номер не найден: " + roomId));
    }
}
