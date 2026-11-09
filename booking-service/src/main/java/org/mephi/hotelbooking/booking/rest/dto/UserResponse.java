package org.mephi.hotelbooking.booking.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о пользователе")
public class UserResponse {

    @Schema(description = "Идентификатор пользователя", example = "c8a61139-ad09-40e9-ad49-cca3ef85e956")
    private UUID id;

    @Schema(description = "Имя пользователя", example = "ivan_petrov")
    private String username;

    @Schema(description = "Роль пользователя", example = "USER")
    private String role;

    @Schema(description = "Дата регистрации", example = "2026-01-15T10:30:00Z")
    private Instant createdAt;
}
