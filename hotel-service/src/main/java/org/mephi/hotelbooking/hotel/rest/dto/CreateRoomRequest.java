package org.mephi.hotelbooking.hotel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mephi.hotelbooking.hotel.domain.enums.RoomType;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на создание номера в отеле")
public class CreateRoomRequest {

    @Schema(description = "Идентификатор отеля", example = "accb3212-53ea-46c6-9a87-923e70ae156f")
    @NotNull(message = "Идентификатор отеля обязателен")
    private UUID hotelId;

    @Schema(description = "Номер комнаты", example = "101")
    @NotBlank(message = "Номер комнаты не может быть пустым")
    @Size(max = 20, message = "Номер комнаты не должен превышать 20 символов")
    private String number;

    @Schema(description = "Тип номера", example = "DOUBLE")
    @NotNull(message = "Тип номера обязателен")
    private RoomType roomType;

    @Schema(description = "Вместимость, человек", example = "2")
    @NotNull(message = "Вместимость обязательна")
    @Min(value = 1, message = "Вместимость должна быть не менее 1")
    private Integer capacity;

    @Schema(description = "Цена за ночь", example = "7500.00")
    @NotNull(message = "Цена за ночь обязательна")
    @DecimalMin(value = "0.01", message = "Цена за ночь должна быть положительной")
    private BigDecimal pricePerNight;

    @Schema(description = "Операционная доступность номера", example = "true")
    private Boolean available;
}
