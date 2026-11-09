package org.mephi.hotelbooking.booking.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.mephi.hotelbooking.booking.domain.enums.Role;
import org.mephi.hotelbooking.booking.rest.dto.ErrorDto;
import org.mephi.hotelbooking.booking.rest.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Пользователи", description = "Администрирование пользователей (только ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public interface UserAdminApi {

    @GetMapping("/users")
    @Operation(
            operationId = "getAllUsers",
            summary = "Получить список пользователей",
            responses = @ApiResponse(responseCode = "200", description = "Список получен",
                    content = @Content(schema = @Schema(implementation = UserResponse[].class))))
    ResponseEntity<List<UserResponse>> getAllUsers();

    @PostMapping("/user")
    @Operation(
            operationId = "createUserByAdmin",
            summary = "Создать пользователя",
            description = "Создаёт пользователя с заданной ролью. Доступно администратору.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь создан",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Имя занято или ошибка валидации",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<UserResponse> createUser(
            @Parameter(description = "Имя пользователя", required = true) @RequestParam String username,
            @Parameter(description = "Пароль", required = true) @RequestParam String password,
            @Parameter(description = "Роль пользователя") @RequestParam(required = false) Role role);

    @PatchMapping("/user/{userId}")
    @Operation(
            operationId = "updateUserByAdmin",
            summary = "Обновить данные пользователя",
            description = "Обновляет переданные поля; null-параметры игнорируются.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь обновлён",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "Идентификатор пользователя") @PathVariable UUID userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) Role role);

    @DeleteMapping("/user/{userId}")
    @Operation(
            operationId = "deleteUserByAdmin",
            summary = "Удалить пользователя",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Пользователь удалён"),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<Void> deleteUser(
            @Parameter(description = "Идентификатор пользователя") @PathVariable UUID userId);
}
