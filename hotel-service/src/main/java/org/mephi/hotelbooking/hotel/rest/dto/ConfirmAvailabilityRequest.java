package org.mephi.hotelbooking.hotel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на подтверждение доступности номера (внутренний вызов Booking Service)")
public class ConfirmAvailabilityRequest {

    @Schema(description = "Корреляционный идентификатор запроса для идемпотентности",
            example = "34395b16-b139-422c-a2f3-7838188de422")
    @NotBlank(message = "requestId обязателен")
    @Size(max = 100, message = "requestId не должен превышать 100 символов")
    private String requestId;

    @Schema(description = "Идентификатор бронирования (сквозная корреляция)",
            example = "34395b16-b139-422c-a2f3-7838188de422")
    @NotBlank(message = "bookingId обязателен")
    @Size(max = 100, message = "bookingId не должен превышать 100 символов")
    private String bookingId;

    @Schema(description = "Дата заезда", example = "2026-02-01")
    @NotNull(message = "Дата заезда обязательна")
    private LocalDate startDate;

    @Schema(description = "Дата выезда", example = "2026-02-05")
    @NotNull(message = "Дата выезда обязательна")
    private LocalDate endDate;
}
