package org.mephi.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.booking.domain.entity.User;
import org.mephi.hotelbooking.booking.domain.enums.Role;
import org.mephi.hotelbooking.booking.domain.repository.UserRepository;
import org.mephi.hotelbooking.booking.exception.EntityNotFoundException;
import org.mephi.hotelbooking.booking.rest.dto.UserResponse;
import org.mephi.hotelbooking.booking.rest.mapper.BookingMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingMapper bookingMapper;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(bookingMapper::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(String username, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует: " + username);
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role != null ? role : Role.USER)
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(user);
        log.info("Пользователь создан администратором: {} (роль: {})", saved.getUsername(), saved.getRole());

        return bookingMapper.toUserResponse(saved);
    }

    @Transactional
    public UserResponse updateUser(UUID userId, String username, String password, Role role) {
        User user = findUser(userId);

        if (username != null && !username.equals(user.getUsername())) {
            if (userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Пользователь с таким именем уже существует: " + username);
            }
            user.setUsername(username);
        }

        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        if (role != null) {
            user.setRole(role);
        }

        User saved = userRepository.save(user);
        log.info("Пользователь обновлён администратором: {} (ID: {})", saved.getUsername(), userId);

        return bookingMapper.toUserResponse(saved);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = findUser(userId);
        userRepository.delete(user);

        log.info("Пользователь удалён: {} (ID: {})", user.getUsername(), userId);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));
    }
}
