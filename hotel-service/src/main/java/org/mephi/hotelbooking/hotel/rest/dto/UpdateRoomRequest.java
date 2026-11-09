package org.mephi.hotelbooking.hotel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mephi.hotelbooking.hotel.domain.enums.RoomType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на обновление номера (частичное обновление: null-поля игнорируются)")
public class UpdateRoomRequest {

    @Schema(description = "Номер комнаты", example = "101")
    @Size(max = 20, message = "Номер комнаты не должен превышать 20 символов")
    private String number;

    @Schema(description = "Тип номера", example = "SUITE")
    private RoomType roomType;

    @Schema(description = "Вместимость, человек", example = "3")
    @Min(value = 1, message = "Вместимость должна быть не менее 1")
    private Integer capacity;

    @Schema(description = "Цена за ночь", example = "9000.00")
    @DecimalMin(value = "0.01", message = "Цена за ночь должна быть положительной")
    private BigDecimal pricePerNight;

    @Schema(description = "Операционная доступность номера (ремонт/вывод из эксплуатации)", example = "false")
    private Boolean available;
}
