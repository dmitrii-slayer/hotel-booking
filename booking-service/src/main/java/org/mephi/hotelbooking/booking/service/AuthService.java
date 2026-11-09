package org.mephi.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.booking.domain.entity.User;
import org.mephi.hotelbooking.booking.domain.enums.Role;
import org.mephi.hotelbooking.booking.domain.repository.UserRepository;
import org.mephi.hotelbooking.booking.rest.dto.AuthRequest;
import org.mephi.hotelbooking.booking.rest.dto.AuthResponse;
import org.mephi.hotelbooking.booking.rest.dto.RegisterRequest;
import org.mephi.hotelbooking.booking.security.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        userRepository.findByUsername(request.getUsername()).ifPresent(existing -> {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует: " + request.getUsername());
        });

        if (request.getRole() != null && request.getRole() != Role.USER) {
            log.warn("Саморегистрация всегда создаёт USER, запрошенная роль {} проигнорирована: {}",
                    request.getRole(), request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(user);
        log.info("Пользователь зарегистрирован: {} (роль: {})", saved.getUsername(), saved.getRole());

        return buildResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Неверное имя пользователя или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Неверное имя пользователя или пароль");
        }

        log.info("Пользователь аутентифицирован: {}", user.getUsername());

        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .expiresAt(jwtTokenProvider.getExpirationInstant())
                .build();
    }
}
