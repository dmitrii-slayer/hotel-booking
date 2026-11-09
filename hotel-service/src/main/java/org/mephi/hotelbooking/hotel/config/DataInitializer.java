package org.mephi.hotelbooking.hotel.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.hotel.domain.entity.Hotel;
import org.mephi.hotelbooking.hotel.domain.entity.Room;
import org.mephi.hotelbooking.hotel.domain.enums.RoomType;
import org.mephi.hotelbooking.hotel.domain.repository.HotelRepository;
import org.mephi.hotelbooking.hotel.domain.repository.RoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Предзаполнение отелей и номеров для локального тестирования.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        if (hotelRepository.count() > 0) {
            log.info("Предзаполнение пропущено: отели уже есть");
            return;
        }

        Hotel grand = hotelRepository.save(Hotel.builder()
                .name("Гранд Отель Москва")
                .address("г. Москва, ул. Тверская, д. 1")
                .description("Пятизвёздочный отель в центре города")
                .createdAt(Instant.now())
                .build());

        Hotel resort = hotelRepository.save(Hotel.builder()
                .name("Приморский курорт")
                .address("г. Сочи, ул. Набережная, д. 12")
                .description("Курортный отель на берегу моря")
                .createdAt(Instant.now())
                .build());

        Hotel business = hotelRepository.save(Hotel.builder()
                .name("Бизнес-Отель Сити")
                .address("г. Санкт-Петербург, Невский пр., д. 88")
                .description("Отель для деловых поездок рядом с вокзалом")
                .createdAt(Instant.now())
                .build());

        roomRepository.saveAll(List.of(
                room(grand, "101", RoomType.SINGLE, 1, "4500.00", 7L),
                room(grand, "102", RoomType.DOUBLE, 2, "7500.00", 3L),
                room(grand, "103", RoomType.DOUBLE, 2, "7500.00", 0L),
                room(grand, "201", RoomType.SUITE, 4, "15000.00", 2L),

                room(resort, "1", RoomType.FAMILY, 5, "9800.00", 5L),
                room(resort, "2", RoomType.DOUBLE, 2, "6200.00", 1L),
                room(resort, "3", RoomType.TWIN, 2, "5900.00", 0L),

                room(business, "A1", RoomType.SINGLE, 1, "3900.00", 4L),
                room(business, "A2", RoomType.TWIN, 2, "5400.00", 0L),
                room(business, "A3", RoomType.SUITE, 3, "11000.00", 1L)
        ));

        log.info("Предзаполнение выполнено: отелей={}, номеров={}",
                hotelRepository.count(), roomRepository.count());
    }

    private Room room(Hotel hotel, String number, RoomType type, int capacity,
                      String price, Long timesBooked) {
        return Room.builder()
                .hotelId(hotel.getId())
                .number(number)
                .roomType(type)
                .capacity(capacity)
                .pricePerNight(new BigDecimal(price))
                .available(true)
                .timesBooked(timesBooked)
                .createdAt(Instant.now())
                .build();
    }
}
