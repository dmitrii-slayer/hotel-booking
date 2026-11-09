package org.mephi.hotelbooking.hotel;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class TestSecurity {

    private TestSecurity() {
    }

    public static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor userJwt() {
        return jwtWithRoles("USER");
    }

    public static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return jwtWithRoles("ADMIN");
    }

    public static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithRoles(String... roles) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("test_user")
                .claim("userId", "11111111-1111-1111-1111-111111111111")
                .claim("roles", List.of(roles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));

        List<GrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        return jwt().jwt(builder.build()).authorities(authorities);
    }
}
