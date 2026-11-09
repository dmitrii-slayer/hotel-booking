package org.mephi.hotelbooking.hotel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация об отеле")
public class HotelResponse {

    @Schema(description = "Идентификатор отеля", example = "39716fa2-ebe6-49b1-bc34-2179faa867bc")
    private UUID id;

    @Schema(description = "Название отеля", example = "Гранд Отель Москва")
    private String name;

    @Schema(description = "Адрес отеля", example = "г. Москва, ул. Тверская, д. 1")
    private String address;

    @Schema(description = "Описание отеля", example = "Пятизвёздочный отель в центре города")
    private String description;

    @Schema(description = "Количество номеров в отеле", example = "42")
    private Long roomsCount;

    @Schema(description = "Дата создания", example = "2026-01-15T10:30:00Z")
    private Instant createdAt;
}
