package com.backend.threatlens.config;

public class SecurityRoutes {

    private SecurityRoutes() {}

    public static final String[] PUBLIC = {
            "/auth/register",
            "/auth/verify",
            "/auth/resend-code",
            "/auth/resend-password",
            "/auth/login",
            "/auth/forgot-password",
            "/auth/refresh",
            "/auth/logout",
    };
}
