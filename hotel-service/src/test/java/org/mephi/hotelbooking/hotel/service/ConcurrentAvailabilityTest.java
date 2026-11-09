package org.mephi.hotelbooking.hotel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mephi.hotelbooking.hotel.BaseTest;
import org.mephi.hotelbooking.hotel.domain.entity.Hotel;
import org.mephi.hotelbooking.hotel.domain.entity.Room;
import org.mephi.hotelbooking.hotel.domain.enums.RoomType;
import org.mephi.hotelbooking.hotel.domain.repository.HotelRepository;
import org.mephi.hotelbooking.hotel.domain.repository.ReservationRepository;
import org.mephi.hotelbooking.hotel.domain.repository.RoomRepository;
import org.mephi.hotelbooking.hotel.exception.BookingConflictException;
import org.mephi.hotelbooking.hotel.rest.dto.ConfirmAvailabilityRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentAvailabilityTest extends BaseTest {

    private static final int THREADS = 8;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Room room;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        hotelRepository.deleteAll();

        Hotel hotel = hotelRepository.save(Hotel.builder()
                .name("Отель для конкурентных тестов")
                .address("Адрес")
                .createdAt(Instant.now())
                .build());

        room = roomRepository.save(Room.builder()
                .hotelId(hotel.getId())
                .number("777")
                .roomType(RoomType.DOUBLE)
                .capacity(2)
                .pricePerNight(new BigDecimal("1000.00"))
                .available(true)
                .timesBooked(0L)
                .createdAt(Instant.now())
                .build());
    }

    @Test
    void shouldAllowOnlyOneBookingForOverlappingDatesUnderConcurrency() throws Exception {
        // given
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger conflicts = new AtomicInteger();
        AtomicInteger successes = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            final String requestId = "concurrent-" + i;

            tasks.add(() -> {
                start.await();

                ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                        .requestId(requestId)
                        .bookingId("book-" + requestId)
                        .startDate(LocalDate.of(2027, 9, 1))
                        .endDate(LocalDate.of(2027, 9, 5))
                        .build();

                try {
                    availabilityService.confirmAvailability(room.getId(), request);
                    successes.incrementAndGet();
                } catch (BookingConflictException ex) {
                    conflicts.incrementAndGet();
                }

                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(executor.submit(task));
        }

        start.countDown();

        for (Future<Void> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // then ровно один успех, остальные получили конфликт
        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(THREADS - 1);
        assertThat(reservationRepository.count()).isEqualTo(1);

        Room updated = roomRepository.findById(room.getId()).orElseThrow();
        assertThat(updated.getTimesBooked()).isEqualTo(1L);
    }

    @Test
    void shouldCountEachDistinctBookingSeparately() {
        // given два непересекающихся периода
        ConfirmAvailabilityRequest first = ConfirmAvailabilityRequest.builder()
                .requestId("distinct-1")
                .bookingId("book-1")
                .startDate(LocalDate.of(2027, 10, 1))
                .endDate(LocalDate.of(2027, 10, 3))
                .build();

        ConfirmAvailabilityRequest second = ConfirmAvailabilityRequest.builder()
                .requestId("distinct-2")
                .bookingId("book-2")
                .startDate(LocalDate.of(2027, 10, 10))
                .endDate(LocalDate.of(2027, 10, 12))
                .build();

        // when
        availabilityService.confirmAvailability(room.getId(), first);
        availabilityService.confirmAvailability(room.getId(), second);

        // then
        assertThat(reservationRepository.count()).isEqualTo(2);
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(2L);
    }

    @Test
    void shouldNotDuplicateUnderRepeatedDelivery() {
        // given один и тот же requestId пришел несколько раз
        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .requestId("retry-token")
                .bookingId("book-retry")
                .startDate(LocalDate.of(2027, 11, 1))
                .endDate(LocalDate.of(2027, 11, 5))
                .build();

        // when
        for (int i = 0; i < 5; i++) {
            availabilityService.confirmAvailability(room.getId(), request);
        }

        // then
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(1L);
    }

    @Test
    void shouldFreeDatesAfterCompensation() {
        // given
        UUID roomId = room.getId();
        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .requestId("compensate-me")
                .bookingId("book-compensate")
                .startDate(LocalDate.of(2027, 12, 1))
                .endDate(LocalDate.of(2027, 12, 5))
                .build();

        availabilityService.confirmAvailability(roomId, request);

        // when
        availabilityService.release(roomId, org.mephi.hotelbooking.hotel.rest.dto.ReleaseRequest.builder()
                .requestId("compensate-me")
                .bookingId("book-compensate")
                .build());

        // then
        assertThat(reservationRepository.findConflicting(roomId,
                LocalDate.of(2027, 12, 1), LocalDate.of(2027, 12, 5))).isEmpty();

        // даты снова можно занять
        ConfirmAvailabilityRequest again = ConfirmAvailabilityRequest.builder()
                .requestId("after-compensation")
                .bookingId("book-new")
                .startDate(LocalDate.of(2027, 12, 1))
                .endDate(LocalDate.of(2027, 12, 5))
                .build();

        availabilityService.confirmAvailability(roomId, again);
        assertThat(reservationRepository.count()).isEqualTo(2);
    }
}
