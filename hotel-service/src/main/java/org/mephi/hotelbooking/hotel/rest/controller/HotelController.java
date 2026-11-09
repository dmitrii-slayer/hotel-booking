package org.mephi.hotelbooking.hotel.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.hotel.rest.api.HotelApi;
import org.mephi.hotelbooking.hotel.rest.dto.CreateHotelRequest;
import org.mephi.hotelbooking.hotel.rest.dto.HotelResponse;
import org.mephi.hotelbooking.hotel.service.HotelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HotelController implements HotelApi {

    private final HotelService hotelService;

    @Override
    public ResponseEntity<HotelResponse> createHotel(CreateHotelRequest request) {
        log.info("Создание отеля: {}", request.getName());
        return ResponseEntity.ok(hotelService.createHotel(request));
    }

    @Override
    public ResponseEntity<List<HotelResponse>> getHotels(String name) {
        log.info("Получение отелей, фильтр по названию: {}", name);
        return ResponseEntity.ok(hotelService.getHotels(name));
    }

    @Override
    public ResponseEntity<HotelResponse> getHotelById(UUID hotelId) {
        log.info("Получение отеля: {}", hotelId);
        return ResponseEntity.ok(hotelService.getHotelById(hotelId));
    }

    @Override
    public ResponseEntity<HotelResponse> updateHotel(UUID hotelId, CreateHotelRequest request) {
        log.info("Обновление отеля: {}", hotelId);
        return ResponseEntity.ok(hotelService.updateHotel(hotelId, request));
    }

    @Override
    public ResponseEntity<Void> deleteHotel(UUID hotelId) {
        log.info("Удаление отеля: {}", hotelId);
        hotelService.deleteHotel(hotelId);
        return ResponseEntity.noContent().build();
    }
}
