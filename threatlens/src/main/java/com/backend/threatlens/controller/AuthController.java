package com.backend.threatlens.controller;

import com.backend.threatlens.dto.request.LoginRequestDTO;
import com.backend.threatlens.dto.request.RegisterRequestDTO;
import com.backend.threatlens.dto.request.VerifyCodeRequestDTO;
import com.backend.threatlens.dto.response.AuthResponseDTO;
import com.backend.threatlens.dto.response.MessageResponseDTO;
import com.backend.threatlens.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<MessageResponseDTO> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        MessageResponseDTO response = authService.register(registerRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponseDTO> verify(@Valid @RequestBody VerifyCodeRequestDTO verifyCodeRequestDTO) {
        AuthResponseDTO response = authService.verifyCode(verifyCodeRequestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-code")
    public ResponseEntity<MessageResponseDTO> resendCode(@RequestParam String email) {
        MessageResponseDTO response = authService.resendCode(email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        AuthResponseDTO response = authService.login(loginRequestDTO);
        return ResponseEntity.ok(response);
    }

}
