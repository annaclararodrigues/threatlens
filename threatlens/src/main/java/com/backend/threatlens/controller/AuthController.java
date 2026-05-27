package com.backend.threatlens.controller;

import com.backend.threatlens.dto.request.*;
import com.backend.threatlens.dto.response.AuthResponseDTO;
import com.backend.threatlens.dto.response.AuthTokens;
import com.backend.threatlens.dto.response.MessageResponseDTO;
import com.backend.threatlens.security.UserPrincipal;
import com.backend.threatlens.service.AuthService;
import com.backend.threatlens.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<MessageResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(dto));
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponseDTO> verify(@Valid @RequestBody VerifyCodeRequestDTO dto) {
        return buildAuthResponse(authService.verifyCode(dto));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<MessageResponseDTO> resendCode(@RequestParam String email) {
        return ResponseEntity.ok(authService.resendCode(email));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return buildAuthResponse(authService.login(dto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request)
                .orElseThrow(() -> new RuntimeException("Refresh token não encontrado."));
        return buildAuthResponse(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponseDTO> logout(HttpServletRequest request) {
        extractRefreshTokenFromCookie(request).ifPresent(authService::logout);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, CookieUtil.clearAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, CookieUtil.clearRefreshTokenCookie().toString())
                .body(new MessageResponseDTO("Logout realizado com sucesso."));
    }

    @PostMapping("/resend-password")
    public ResponseEntity<MessageResponseDTO> resendPasswordCode(@RequestParam String email) {
        return ResponseEntity.ok(authService.resendPasswordCode(email));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDTO> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        return ResponseEntity.ok(authService.forgotPassword(dto));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDTO> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.resetPassword(dto, principal.getEmail()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponseDTO> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.changePassword(dto, principal.getEmail()));
    }

    // --- helpers ---

    private ResponseEntity<AuthResponseDTO> buildAuthResponse(AuthTokens tokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, CookieUtil.buildAccessTokenCookie(tokens.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, CookieUtil.buildRefreshTokenCookie(tokens.refreshToken()).toString())
                .body(new AuthResponseDTO(tokens.username(), tokens.email()));
    }

    private Optional<String> extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> CookieUtil.REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
