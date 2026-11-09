package org.mephi.hotelbooking.booking;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class TestSecurity {

    public static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    public static final String OTHER_USER_ID = "22222222-2222-2222-2222-222222222222";

    private TestSecurity() {
    }

    public static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor userJwt() {
        return jwtWith(USER_ID, "ivan", "USER");
    }

    public static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor otherUserJwt() {
        return jwtWith(OTHER_USER_ID, "maria", "USER");
    }

    public static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return jwtWith(USER_ID, "admin", "ADMIN");
    }

    public static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWith(
            String userId, String username, String... roles) {

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(username)
                .claim("userId", userId)
                .claim("roles", List.of(roles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        List<GrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        return jwt().jwt(jwt).authorities(authorities);
    }
}
