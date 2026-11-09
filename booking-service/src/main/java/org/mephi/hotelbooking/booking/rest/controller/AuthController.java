package org.mephi.hotelbooking.booking.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.booking.rest.api.AuthApi;
import org.mephi.hotelbooking.booking.rest.dto.AuthRequest;
import org.mephi.hotelbooking.booking.rest.dto.AuthResponse;
import org.mephi.hotelbooking.booking.rest.dto.RegisterRequest;
import org.mephi.hotelbooking.booking.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<AuthResponse> register(RegisterRequest request) {
        log.info("Регистрация пользователя: {}", request.getUsername());
        return ResponseEntity.ok(authService.register(request));
    }

    @Override
    public ResponseEntity<AuthResponse> authenticate(AuthRequest request) {
        log.info("Аутентификация пользователя: {}", request.getUsername());
        return ResponseEntity.ok(authService.authenticate(request));
    }
}
