package org.mephi.hotelbooking.booking.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mephi.hotelbooking.booking.domain.enums.Role;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ с токеном доступа")
public class AuthResponse {

    @Schema(description = "JWT-токен доступа (срок действия - 1 час)",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJpdmFuIn0.signature")
    private String token;

    @Schema(description = "Тип токена", example = "Bearer")
    private String tokenType;

    @Schema(description = "Идентификатор пользователя", example = "c8a61139-ad09-40e9-ad49-cca3ef85e956")
    private UUID userId;

    @Schema(description = "Имя пользователя", example = "ivan_petrov")
    private String username;

    @Schema(description = "Роль пользователя", example = "USER")
    private Role role;

    @Schema(description = "Момент истечения токена", example = "2026-01-15T11:30:00Z")
    private Instant expiresAt;
}
