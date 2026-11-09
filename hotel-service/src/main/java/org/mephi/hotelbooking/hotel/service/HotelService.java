package org.mephi.hotelbooking.hotel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.hotel.domain.entity.Hotel;
import org.mephi.hotelbooking.hotel.domain.entity.Room;
import org.mephi.hotelbooking.hotel.domain.repository.HotelRepository;
import org.mephi.hotelbooking.hotel.domain.repository.ReservationRepository;
import org.mephi.hotelbooking.hotel.domain.repository.RoomRepository;
import org.mephi.hotelbooking.hotel.exception.EntityNotFoundException;
import org.mephi.hotelbooking.hotel.rest.dto.CreateHotelRequest;
import org.mephi.hotelbooking.hotel.rest.dto.HotelResponse;
import org.mephi.hotelbooking.hotel.rest.mapper.HotelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final HotelMapper hotelMapper;

    @Transactional
    public HotelResponse createHotel(CreateHotelRequest request) {
        if (hotelRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Отель с названием уже существует: " + request.getName());
        }

        Hotel hotel = hotelMapper.toEntity(request);
        Hotel saved = hotelRepository.save(hotel);

        log.info("Отель создан: {} (ID: {})", saved.getName(), saved.getId());

        return hotelMapper.toResponse(saved, 0L);
    }

    @Transactional(readOnly = true)
    public List<HotelResponse> getHotels(String name) {
        List<Hotel> hotels = (name == null || name.isBlank())
                ? hotelRepository.findAll()
                : hotelRepository.findByNameContainingIgnoreCase(name);

        return hotels.stream()
                .map(hotel -> hotelMapper.toResponse(hotel, (long) roomRepository.findByHotelId(hotel.getId()).size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotelById(UUID hotelId) {
        Hotel hotel = findHotel(hotelId);
        long roomsCount = roomRepository.findByHotelId(hotelId).size();

        return hotelMapper.toResponse(hotel, roomsCount);
    }

    @Transactional
    public HotelResponse updateHotel(UUID hotelId, CreateHotelRequest request) {
        Hotel hotel = findHotel(hotelId);

        if (!request.getName().equals(hotel.getName())
                && hotelRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Отель с названием уже существует: " + request.getName());
        }

        hotel.setName(request.getName());
        hotel.setAddress(request.getAddress());
        hotel.setDescription(request.getDescription());

        Hotel saved = hotelRepository.save(hotel);
        log.info("Отель обновлён: {} (ID: {})", saved.getName(), saved.getId());

        return hotelMapper.toResponse(saved, (long) roomRepository.findByHotelId(hotelId).size());
    }

    @Transactional
    public void deleteHotel(UUID hotelId) {
        Hotel hotel = findHotel(hotelId);

        List<Room> rooms = roomRepository.findByHotelId(hotelId);

        for (Room room : rooms) {
            reservationRepository.deleteAll(reservationRepository.findByRoomId(room.getId()));
        }

        roomRepository.deleteAll(rooms);
        hotelRepository.delete(hotel);

        log.info("Отель удалён: {} (ID: {}), номеров: {}, слоты очищены",
                hotel.getName(), hotelId, rooms.size());
    }

    private Hotel findHotel(UUID hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new EntityNotFoundException("Отель не найден: " + hotelId));
    }
}
