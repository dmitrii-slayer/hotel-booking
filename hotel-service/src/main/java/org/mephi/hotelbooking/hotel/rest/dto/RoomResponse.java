package org.mephi.hotelbooking.hotel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mephi.hotelbooking.hotel.domain.enums.RoomType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о номере")
public class RoomResponse {

    @Schema(description = "Идентификатор номера", example = "220568c8-bd26-45b5-878d-8595194febc3")
    private UUID id;

    @Schema(description = "Идентификатор отеля", example = "7f7b8cf8-c0b7-4cfc-b38f-4d05411d2ce9")
    private UUID hotelId;

    @Schema(description = "Название отеля", example = "Гранд Отель Москва")
    private String hotelName;

    @Schema(description = "Номер комнаты", example = "101")
    private String number;

    @Schema(description = "Тип номера", example = "DOUBLE")
    private RoomType roomType;

    @Schema(description = "Вместимость, человек", example = "2")
    private Integer capacity;

    @Schema(description = "Цена за ночь", example = "7500.00")
    private BigDecimal pricePerNight;

    @Schema(description = "Операционная доступность", example = "true")
    private Boolean available;

    @Schema(description = "Сколько раз номер бронировали", example = "12")
    private Long timesBooked;

    @Schema(description = "Дата создания", example = "2026-01-15T10:30:00Z")
    private Instant createdAt;
}
