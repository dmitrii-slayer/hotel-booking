package org.mephi.hotelbooking.booking.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.mephi.hotelbooking.booking.rest.dto.AuthRequest;
import org.mephi.hotelbooking.booking.rest.dto.AuthResponse;
import org.mephi.hotelbooking.booking.rest.dto.ErrorDto;
import org.mephi.hotelbooking.booking.rest.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/user")
@Tag(name = "Аутентификация", description = "Регистрация и вход пользователей")
public interface AuthApi {

    @PostMapping("/register")
    @Operation(
            operationId = "registerUser",
            summary = "Зарегистрировать пользователя",
            description = "Создаёт пользователя и сразу выдаёт JWT-токен (срок действия - 1 час).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь зарегистрирован",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации или имя занято",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request);

    @PostMapping("/auth")
    @Operation(
            operationId = "authenticateUser",
            summary = "Авторизовать пользователя",
            description = "Проверяет учётные данные и выдаёт JWT-токен.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Токен выдан",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неверные учётные данные",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request);
}
