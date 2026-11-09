package org.mephi.hotelbooking.booking.security;

import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.booking.domain.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final long expirationMs;

    public JwtTokenProvider(JwtEncoder jwtEncoder,
                            @Value("${app.jwt.expiration:3600000}") long expirationMs) {
        this.jwtEncoder = jwtEncoder;
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(expirationMs);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("userId", user.getId().toString())
                .claim("roles", List.of(user.getRole().name()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        log.debug("Токен выдан пользователю: {}, роль: {}, истекает: {}",
                user.getUsername(), user.getRole(), expiresAt);

        return token;
    }

    public Instant getExpirationInstant() {
        return Instant.now().plusMillis(expirationMs);
    }
}
