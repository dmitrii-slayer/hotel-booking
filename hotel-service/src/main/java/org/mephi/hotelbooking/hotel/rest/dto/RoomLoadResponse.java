package org.mephi.hotelbooking.hotel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Статистика загруженности по одному номеру")
public class RoomLoadResponse {

    @Schema(description = "Идентификатор номера", example = "258d8747-8582-4075-8490-89a17617208e")
    private UUID roomId;

    @Schema(description = "Номер комнаты", example = "101")
    private String number;

    @Schema(description = "Название отеля", example = "Гранд Отель Москва")
    private String hotelName;

    @Schema(description = "Сколько раз номер бронировали", example = "12")
    private Long timesBooked;

    @Schema(description = "Количество активных слотов", example = "3")
    private Long activeReservations;

    @Schema(description = "Количество ночей в активных слотах", example = "14")
    private Long bookedNights;

    @Schema(description = "Доля занятости на горизонте, %", example = "37.5")
    private Double loadPercent;

    @Schema(description = "Начало горизонта расчёта", example = "2026-01-01")
    private LocalDate horizonStart;

    @Schema(description = "Конец горизонта расчёта", example = "2026-03-31")
    private LocalDate horizonEnd;

    @Schema(description = "Момент расчёта", example = "2026-01-15T10:30:00Z")
    private Instant calculatedAt;
}
