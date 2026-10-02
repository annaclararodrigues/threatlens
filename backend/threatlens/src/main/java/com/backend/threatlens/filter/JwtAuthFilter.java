package com.backend.threatlens.filter;

import com.backend.threatlens.entity.UserEntity;
import com.backend.threatlens.enums.Role;
import com.backend.threatlens.security.UserPrincipal;
import com.backend.threatlens.utils.CookieUtil;
import com.backend.threatlens.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Autentica exclusivamente a partir das claims do JWT (email + role), sem consultar o banco a
 * cada requisição. A checagem de credenciais contra o banco só acontece no login, via
 * {@code UserDetailsServiceImpl} usado pelo {@code AuthenticationManager}.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && jwtUtil.isValidToken(token)) {
            String email = jwtUtil.extractEmail(token);
            Role extractedRole = jwtUtil.extractRole(token);
            UserEntity stub = UserEntity.builder()
                    .email(email)
                    .role(extractedRole != null ? extractedRole : Role.USER)
                    .build();
            UserPrincipal principal = new UserPrincipal(stub);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities()
                    );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> CookieUtil.ACCESS_TOKEN_COOKIE.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

}
