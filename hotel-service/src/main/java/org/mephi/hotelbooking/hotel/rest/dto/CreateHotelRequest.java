package org.mephi.hotelbooking.hotel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на создание отеля")
public class CreateHotelRequest {

    @Schema(description = "Название отеля", example = "Гранд Отель Москва")
    @NotBlank(message = "Название отеля не может быть пустым")
    @Size(max = 150, message = "Название отеля не должно превышать 150 символов")
    private String name;

    @Schema(description = "Адрес отеля", example = "г. Москва, ул. Тверская, д. 1")
    @NotBlank(message = "Адрес не может быть пустым")
    @Size(max = 255, message = "Адрес не должен превышать 255 символов")
    private String address;

    @Schema(description = "Описание отеля", example = "Пятизвёздочный отель в центре города")
    @Size(max = 500, message = "Описание не должно превышать 500 символов")
    private String description;
}
