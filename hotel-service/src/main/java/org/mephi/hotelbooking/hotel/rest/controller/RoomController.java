package org.mephi.hotelbooking.hotel.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.hotel.rest.api.RoomApi;
import org.mephi.hotelbooking.hotel.rest.dto.AvailabilityResponse;
import org.mephi.hotelbooking.hotel.rest.dto.ConfirmAvailabilityRequest;
import org.mephi.hotelbooking.hotel.rest.dto.CreateRoomRequest;
import org.mephi.hotelbooking.hotel.rest.dto.ReleaseRequest;
import org.mephi.hotelbooking.hotel.rest.dto.RoomLoadResponse;
import org.mephi.hotelbooking.hotel.rest.dto.RoomResponse;
import org.mephi.hotelbooking.hotel.rest.dto.UpdateRoomRequest;
import org.mephi.hotelbooking.hotel.service.AvailabilityService;
import org.mephi.hotelbooking.hotel.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RoomController implements RoomApi {

    private final RoomService roomService;
    private final AvailabilityService availabilityService;

    @Override
    public ResponseEntity<RoomResponse> createRoom(CreateRoomRequest request) {
        log.info("Создание номера {} в отеле {}", request.getNumber(), request.getHotelId());
        return ResponseEntity.ok(roomService.createRoom(request));
    }

    @Override
    public ResponseEntity<List<RoomResponse>> getAvailableRooms(LocalDate startDate, LocalDate endDate) {
        log.info("Получение свободных номеров, период: {} - {}", startDate, endDate);
        return ResponseEntity.ok(roomService.getAvailableRooms(startDate, endDate));
    }

    @Override
    public ResponseEntity<List<RoomResponse>> getRecommendedRooms(LocalDate startDate, LocalDate endDate) {
        log.info("Получение рекомендованных номеров, период: {} - {}", startDate, endDate);
        return ResponseEntity.ok(roomService.getRecommendedRooms(startDate, endDate));
    }

    @Override
    public ResponseEntity<List<RoomLoadResponse>> getRoomStatistics(LocalDate horizonStart, LocalDate horizonEnd) {
        log.info("Расчёт статистики загруженности, горизонт: {} - {}", horizonStart, horizonEnd);
        return ResponseEntity.ok(roomService.getStatistics(horizonStart, horizonEnd));
    }

    @Override
    public ResponseEntity<List<RoomResponse>> getRoomsByHotel(UUID hotelId) {
        log.info("Получение номеров отеля: {}", hotelId);
        return ResponseEntity.ok(roomService.getRoomsByHotel(hotelId));
    }

    @Override
    public ResponseEntity<RoomResponse> getRoomById(UUID roomId) {
        log.info("Получение номера: {}", roomId);
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    @Override
    public ResponseEntity<RoomResponse> updateRoom(UUID roomId, UpdateRoomRequest request) {
        log.info("Обновление номера: {}", roomId);
        return ResponseEntity.ok(roomService.updateRoom(roomId, request));
    }

    @Override
    public ResponseEntity<Void> deleteRoom(UUID roomId) {
        log.info("Удаление номера: {}", roomId);
        roomService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AvailabilityResponse> confirmAvailability(UUID roomId,
                                                                   ConfirmAvailabilityRequest request) {
        log.info("Подтверждение доступности: roomId={}, requestId={}, bookingId={}",
                roomId, request.getRequestId(), request.getBookingId());

        return ResponseEntity.ok(availabilityService.confirmAvailability(roomId, request));
    }

    @Override
    public ResponseEntity<AvailabilityResponse> releaseAvailability(UUID roomId, ReleaseRequest request) {
        log.warn("Компенсация: снятие блокировки roomId={}, requestId={}, bookingId={}",
                roomId, request.getRequestId(), request.getBookingId());

        return ResponseEntity.ok(availabilityService.release(roomId, request));
    }

    @Override
    public ResponseEntity<Void> confirmBookingSlots(String bookingId) {
        log.info("Фиксация слотов бронирования: {}", bookingId);
        availabilityService.confirmBooking(bookingId);
        return ResponseEntity.ok().build();
    }
}
