package org.mephi.hotelbooking.booking.service;

import org.mephi.hotelbooking.booking.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    public UUID getCurrentUserId() {
        Jwt jwt = getJwt();

        String userId = jwt.getClaimAsString("userId");
        if (userId == null) {
            throw new UnauthorizedException("В токене отсутствует claim userId");
        }

        return UUID.fromString(userId);
    }

    public String getCurrentUsername() {
        return getJwt().getSubject();
    }

    public boolean isAdmin() {
        return getJwt().getClaimAsStringList("roles") != null
                && getJwt().getClaimAsStringList("roles").contains("ADMIN");
    }

    public String getBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getCredentials() instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }

        throw new UnauthorizedException("Не удалось получить токен доступа");
    }

    private Jwt getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UnauthorizedException("Пользователь не авторизован");
        }

        return jwt;
    }
}
