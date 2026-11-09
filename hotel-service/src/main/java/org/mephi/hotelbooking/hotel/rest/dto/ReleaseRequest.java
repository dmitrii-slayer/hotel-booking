package org.mephi.hotelbooking.hotel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на снятие временной блокировки слота (компенсация)")
public class ReleaseRequest {

    @Schema(description = "Корреляционный идентификатор запроса для идемпотентности",
            example = "ff1bac05-f9b6-4b2f-9106-1584e4ebdce5")
    @NotBlank(message = "requestId обязателен")
    @Size(max = 100, message = "requestId не должен превышать 100 символов")
    private String requestId;

    @Schema(description = "Идентификатор бронирования (сквозная корреляция)",
            example = "ff1bac05-f9b6-4b2f-9106-1584e4ebdce5")
    @NotBlank(message = "bookingId обязателен")
    @Size(max = 100, message = "bookingId не должен превышать 100 символов")
    private String bookingId;
}
