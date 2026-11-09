package org.mephi.hotelbooking.booking.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.booking.domain.enums.Role;
import org.mephi.hotelbooking.booking.rest.api.UserAdminApi;
import org.mephi.hotelbooking.booking.rest.dto.UserResponse;
import org.mephi.hotelbooking.booking.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserAdminController implements UserAdminApi {

    private final UserService userService;

    @Override
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Получение списка пользователей");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Override
    public ResponseEntity<UserResponse> createUser(String username, String password, Role role) {
        log.info("Создание пользователя администратором: {}", username);
        return ResponseEntity.ok(userService.createUser(username, password, role));
    }

    @Override
    public ResponseEntity<UserResponse> updateUser(UUID userId, String username, String password, Role role) {
        log.info("Обновление пользователя администратором: {}", userId);
        return ResponseEntity.ok(userService.updateUser(userId, username, password, role));
    }

    @Override
    public ResponseEntity<Void> deleteUser(UUID userId) {
        log.info("Удаление пользователя: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
