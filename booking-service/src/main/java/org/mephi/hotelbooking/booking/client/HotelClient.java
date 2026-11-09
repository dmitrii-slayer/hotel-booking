package org.mephi.hotelbooking.booking.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.booking.client.dto.AvailabilityDto;
import org.mephi.hotelbooking.booking.client.dto.ConfirmAvailabilityDto;
import org.mephi.hotelbooking.booking.client.dto.ReleaseDto;
import org.mephi.hotelbooking.booking.client.dto.RoomDto;
import org.mephi.hotelbooking.booking.client.exception.HotelServiceException;
import org.mephi.hotelbooking.booking.client.exception.RetryableHotelServiceException;
import org.mephi.hotelbooking.booking.client.exception.RoomUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class HotelClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HotelClient(@Value("${app.hotel.url}") String baseUrl,
                       @Value("${app.hotel.connect-timeout-ms:2000}") long connectTimeoutMs,
                       @Value("${app.hotel.read-timeout-ms:3000}") long readTimeoutMs,
                       ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public List<RoomDto> getRecommendedRooms(LocalDate startDate, LocalDate endDate,
                                             String traceId, String bearerToken) {
        try {
            List<RoomDto> rooms = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/rooms/recommend")
                            .queryParamIfPresent("startDate", Optional.ofNullable(startDate))
                            .queryParamIfPresent("endDate", Optional.ofNullable(endDate))
                            .build())
                    .header("X-Trace-Id", traceId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RoomDto>>() {
                    });

            return rooms != null ? rooms : List.of();
        } catch (RestClientResponseException ex) {
            throw translate(ex, "Не удалось получить рекомендованные номера");
        } catch (ResourceAccessException ex) {
            throw new RetryableHotelServiceException("Hotel Service недоступен при получении номеров", ex);
        }
    }

    public RoomDto getRoom(UUID roomId, String traceId, String bearerToken) {
        try {
            return restClient.get()
                    .uri("/api/rooms/{roomId}", roomId)
                    .header("X-Trace-Id", traceId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(RoomDto.class);
        } catch (RestClientResponseException ex) {
            throw translate(ex, "Не удалось получить номер " + roomId);
        } catch (ResourceAccessException ex) {
            throw new RetryableHotelServiceException("Hotel Service недоступен при получении номера " + roomId, ex);
        }
    }

    public AvailabilityDto confirmAvailability(UUID roomId, ConfirmAvailabilityDto request,
                                               String traceId, String bearerToken) {
        try {
            return restClient.post()
                    .uri("/api/rooms/{roomId}/confirm-availability", roomId)
                    .header("X-Trace-Id", traceId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .body(request)
                    .retrieve()
                    .body(AvailabilityDto.class);
        } catch (RestClientResponseException ex) {
            throw translate(ex, "Не удалось подтвердить доступность номера " + roomId);
        } catch (ResourceAccessException ex) {
            throw new RetryableHotelServiceException("Hotel Service недоступен при подтверждении доступности", ex);
        }
    }

    public AvailabilityDto release(UUID roomId, ReleaseDto request, String traceId, String bearerToken) {
        try {
            return restClient.post()
                    .uri("/api/rooms/{roomId}/release", roomId)
                    .header("X-Trace-Id", traceId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .body(request)
                    .retrieve()
                    .body(AvailabilityDto.class);
        } catch (RestClientResponseException ex) {
            throw translate(ex, "Не удалось снять блокировку номера " + roomId);
        } catch (ResourceAccessException ex) {
            throw new RetryableHotelServiceException("Hotel Service недоступен при компенсации", ex);
        }
    }

    public void confirmBookingSlots(String bookingId, String traceId, String bearerToken) {
        try {
            restClient.post()
                    .uri("/api/rooms/booking/{bookingId}/confirm", bookingId)
                    .header("X-Trace-Id", traceId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw translate(ex, "Не удалось зафиксировать слоты бронирования " + bookingId);
        } catch (ResourceAccessException ex) {
            throw new RetryableHotelServiceException("Hotel Service недоступен при фиксации слотов", ex);
        }
    }

    private RuntimeException translate(RestClientResponseException ex, String context) {
        HttpStatusCode status = ex.getStatusCode();

        if (status.value() == 409) {
            log.warn("{}: конфликт (409) - {}", context, ex.getResponseBodyAsString());
            return new RoomUnavailableException(extractMessage(ex, "Номер занят на запрошенные даты"));
        }

        if (status.value() == 404) {
            log.warn("{}: не найдено (404)", context);
            return new HotelServiceException(context + ": ресурс не найден");
        }

        if (status.value() == 400) {
            log.warn("{}: неверный запрос (400) - {}", context, ex.getResponseBodyAsString());
            return new HotelServiceException(extractMessage(ex, "Неверный запрос к Hotel Service"));
        }

        log.error("{}: ошибка Hotel Service, статус {}", context, status.value());
        return new RetryableHotelServiceException(context + ": статус " + status.value());
    }

    private String extractMessage(RestClientResponseException ex, String fallback) {
        String body = ex.getResponseBodyAsString();

        if (!StringUtils.hasText(body)) {
            return fallback;
        }

        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode message = node.get("message");

            if (message != null && message.isTextual() && !message.asText().isBlank()) {
                return message.asText();
            }
        } catch (JsonProcessingException ignored) {
            log.debug("Тело ошибки Hotel Service не является JSON: {}", body);
        }

        return fallback;
    }
}
