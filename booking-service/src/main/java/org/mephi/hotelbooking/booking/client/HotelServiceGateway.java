package org.mephi.hotelbooking.booking.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.booking.client.dto.AvailabilityDto;
import org.mephi.hotelbooking.booking.client.dto.ConfirmAvailabilityDto;
import org.mephi.hotelbooking.booking.client.dto.ReleaseDto;
import org.mephi.hotelbooking.booking.client.dto.RoomDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotelServiceGateway {

    private static final String INSTANCE = "hotelService";

    private final HotelClient hotelClient;

    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    public List<RoomDto> getRecommendedRooms(LocalDate startDate, LocalDate endDate,
                                               String traceId, String bearerToken) {
        return hotelClient.getRecommendedRooms(startDate, endDate, traceId, bearerToken);
    }

    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    public RoomDto getRoom(UUID roomId, String traceId, String bearerToken) {
        return hotelClient.getRoom(roomId, traceId, bearerToken);
    }

    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    public AvailabilityDto confirmAvailability(UUID roomId, ConfirmAvailabilityDto request,
                                               String traceId, String bearerToken) {
        log.debug("Подтверждение доступности через Hotel Service: roomId={}, requestId={}",
                roomId, request.getRequestId());

        return hotelClient.confirmAvailability(roomId, request, traceId, bearerToken);
    }

    @CircuitBreaker(name = INSTANCE)
    public AvailabilityDto release(UUID roomId, ReleaseDto request, String traceId, String bearerToken) {
        log.warn("Компенсирующее действие: снятие блокировки roomId={}, requestId={}",
                roomId, request.getRequestId());

        return hotelClient.release(roomId, request, traceId, bearerToken);
    }

    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    public void confirmBookingSlots(String bookingId, String traceId, String bearerToken) {
        hotelClient.confirmBookingSlots(bookingId, traceId, bearerToken);
    }
}
