package org.mephi.hotelbooking.booking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.booking.domain.entity.User;
import org.mephi.hotelbooking.booking.domain.enums.Role;
import org.mephi.hotelbooking.booking.domain.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Предзаполнение пользователей пропущено");
            return;
        }

        userRepository.save(user("admin", "admin123", Role.ADMIN));
        userRepository.save(user("ivan", "ivan123", Role.USER));
        userRepository.save(user("maria", "maria123", Role.USER));

        log.info("Предзаполнение пользователей выполнено: admin/admin123, ivan/ivan123, maria/maria123");
    }

    private User user(String username, String password, Role role) {
        return User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .createdAt(Instant.now())
                .build();
    }
}
