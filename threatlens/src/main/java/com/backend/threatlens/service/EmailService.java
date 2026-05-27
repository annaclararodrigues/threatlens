package com.backend.threatlens.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("ThreatLens – Código de verificação");
        message.setText(
                "Olá!\n\n" +
                "Seu código de verificação é: " + code + "\n\n" +
                "O código expira em 10 minutos.\n\n"
        );
        mailSender.send(message);
    }
}
