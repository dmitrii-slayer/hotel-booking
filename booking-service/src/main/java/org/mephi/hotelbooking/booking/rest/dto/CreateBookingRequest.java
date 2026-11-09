package org.mephi.hotelbooking.booking.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на создание бронирования")
public class CreateBookingRequest {

    @Schema(description = "Идентификатор номера. Игнорируется при autoSelect = true",
            example = "d473a3ef-f765-499f-9463-f2c101d94898")
    private UUID roomId;

    @Schema(description = "Автоподбор номера по наименьшему times_booked", example = "false")
    private Boolean autoSelect;

    @Schema(description = "Дата заезда", example = "2027-02-01")
    @NotNull(message = "Дата заезда обязательна")
    private LocalDate startDate;

    @Schema(description = "Дата выезда", example = "2027-02-05")
    @NotNull(message = "Дата выезда обязательна")
    private LocalDate endDate;

    @Schema(description = "Корреляционный идентификатор запроса для идемпотентности. "
            + "Если не передан - генерируется на сервере",
            example = "83dd2e4c-d88e-4e8e-b164-ece66fef6264")
    private String requestId;

    public boolean isAutoSelectEnabled() {
        return Boolean.TRUE.equals(autoSelect);
    }

    public String ensureRequestId() {
        if (requestId == null || requestId.isBlank()) {
            requestId = java.util.UUID.randomUUID().toString();
        }

        return requestId;
    }

    @AssertTrue(message = "Укажите roomId или включите autoSelect")
    @Schema(hidden = true)
    public boolean isRoomIdOrAutoSelectPresent() {
        return Boolean.TRUE.equals(autoSelect) || roomId != null;
    }

    @AssertTrue(message = "Дата выезда должна быть позже даты заезда")
    @Schema(hidden = true)
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || startDate.isBefore(endDate);
    }
}
