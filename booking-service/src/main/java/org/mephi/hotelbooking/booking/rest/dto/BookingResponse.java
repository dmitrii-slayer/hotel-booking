package org.mephi.hotelbooking.booking.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mephi.hotelbooking.booking.domain.enums.BookingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о бронировании")
public class BookingResponse {

    @Schema(description = "Идентификатор бронирования",
            example = "f2d2c99d-88a6-433a-a30b-c001ac953d4f")
    private UUID id;

    @Schema(description = "Идентификатор пользователя",
            example = "2c940f49-61e0-419d-b68d-8ca6291798bf")
    private UUID userId;

    @Schema(description = "Имя пользователя", example = "ivan_petrov")
    private String username;

    @Schema(description = "Идентификатор номера",
            example = "d473a3ef-f765-499f-9463-f2c101d94898")
    private UUID roomId;

    @Schema(description = "Номер комнаты", example = "103")
    private String roomNumber;

    @Schema(description = "Идентификатор отеля",
            example = "40d95c36-c832-4581-851e-8c0cf6419754")
    private UUID hotelId;

    @Schema(description = "Дата заезда", example = "2027-02-01")
    private LocalDate startDate;

    @Schema(description = "Дата выезда", example = "2027-02-05")
    private LocalDate endDate;

    @Schema(description = "Статус бронирования", example = "CONFIRMED")
    private BookingStatus status;

    @Schema(description = "Причина отмены (для CANCELLED)", example = "Номер занят на запрошенные даты")
    private String failureReason;

    @Schema(description = "Корреляционный идентификатор запроса",
            example = "83dd2e4c-d88e-4e8e-b164-ece66fef6264")
    private String requestId;

    @Schema(description = "Дата создания", example = "2026-01-15T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "Дата последнего изменения", example = "2026-01-15T10:30:05Z")
    private Instant updatedAt;
}
