package com.backend.threatlens.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window rate limiter for sensitive auth endpoints, backed by an in-memory map.
 * Valid for a single-instance deployment only: counters are not shared across processes,
 * so running multiple instances behind a load balancer would multiply the effective limit
 * by the instance count. A shared store (Redis) or a dedicated gateway (Bucket4j,
 * Spring Cloud Gateway) would be needed if the app is ever scaled horizontally.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Limit(int maxAttempts, Duration window) {}

    private static final Map<String, Limit> LIMITS = Map.of(
            "/auth/login", new Limit(5, Duration.ofMinutes(15)),
            "/auth/register", new Limit(5, Duration.ofMinutes(15)),
            "/auth/verify", new Limit(10, Duration.ofMinutes(10))
    );

    private static class Bucket {
        int count;
        long windowStartMillis;
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {

        Limit limit = "POST".equalsIgnoreCase(request.getMethod()) ? LIMITS.get(request.getRequestURI()) : null;

        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());

        boolean allowed;
        synchronized (bucket) {
            long now = System.currentTimeMillis();
            if (now - bucket.windowStartMillis > limit.window().toMillis()) {
                bucket.windowStartMillis = now;
                bucket.count = 0;
            }
            bucket.count++;
            allowed = bucket.count <= limit.maxAttempts();
        }

        if (!allowed) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Muitas tentativas. Tente novamente mais tarde.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}