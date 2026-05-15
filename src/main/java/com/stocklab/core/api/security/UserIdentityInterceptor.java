package com.stocklab.core.api.security;

import com.stocklab.core.api.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserIdentityInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "authenticatedUserId";
    public static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String headerValue = request.getHeader(USER_ID_HEADER);
        if (headerValue == null || headerValue.isBlank()) {
            throw new UnauthorizedException("Missing " + USER_ID_HEADER + " header");
        }

        try {
            request.setAttribute(USER_ID_ATTRIBUTE, Long.parseLong(headerValue.trim()));
            return true;
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException("Invalid " + USER_ID_HEADER + " header");
        }
    }
}
