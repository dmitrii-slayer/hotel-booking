package org.mephi.hotelbooking.booking.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на аутентификацию")
public class AuthRequest {

    @Schema(description = "Имя пользователя", example = "ivan_petrov")
    @NotBlank(message = "Имя пользователя не может быть пустым")
    private String username;

    @Schema(description = "Пароль", example = "secret123")
    @NotBlank(message = "Пароль не может быть пустым")
    private String password;
}
