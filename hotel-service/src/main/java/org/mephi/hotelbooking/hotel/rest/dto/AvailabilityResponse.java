package org.mephi.hotelbooking.hotel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mephi.hotelbooking.hotel.domain.enums.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Результат подтверждения доступности номера")
public class AvailabilityResponse {

    @Schema(description = "Идентификатор слота", example = "c8b660f5-b57f-42df-801c-e586e328607f")
    private UUID reservationId;

    @Schema(description = "Идентификатор номера", example = "31998ef7-35c8-4fd4-998f-cc438848f03b")
    private UUID roomId;

    @Schema(description = "Корреляционный идентификатор запроса",
            example = "14cc5b19-9f2a-40ae-88ce-d8b7f2e8289d")
    private String requestId;

    @Schema(description = "Идентификатор бронирования", example = "df21a35a-e3ad-4890-bef1-18a3b9e45d1c")
    private String bookingId;

    @Schema(description = "Дата заезда", example = "2026-02-01")
    private LocalDate startDate;

    @Schema(description = "Дата выезда", example = "2026-02-05")
    private LocalDate endDate;

    @Schema(description = "Статус слота", example = "HOLD")
    private ReservationStatus status;

    @Schema(description = "Признак повторной обработки запроса (идемпотентность)", example = "false")
    private Boolean replayed;

    @Schema(description = "Дата создания", example = "2026-01-15T10:30:00Z")
    private Instant createdAt;
}
