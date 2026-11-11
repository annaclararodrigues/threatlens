package com.backend.threatlens.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String LOGO_CID = "logo";
    private static final ClassPathResource LOGO = new ClassPathResource("static/email/logo.png");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendRegisterCode(String toEmail, String code) {
        sendTemplate(
                toEmail,
                "ThreatLens – Confirme seu e-mail",
                "email/register-code",
                Map.of("code", code),
                "Olá!\n\n" +
                "Use o código abaixo para ativar sua conta no ThreatLens:\n\n" +
                code + "\n\n" +
                "O código expira em 10 minutos.\n\n" +
                "Não foi você? Ignore este e-mail."
        );
    }

    public void sendResetPasswordCode(String toEmail, String code) {
        sendTemplate(
                toEmail,
                "ThreatLens – Redefinição de senha",
                "email/reset-password",
                Map.of("code", code),
                "Olá!\n\n" +
                "Recebemos um pedido para redefinir a senha da sua conta no ThreatLens. Use o código abaixo para continuar:\n\n" +
                code + "\n\n" +
                "O código expira em 10 minutos.\n\n" +
                "Não pediu a redefinição? Ignore este e-mail. Sua senha continua a mesma."
        );
    }

    private void sendTemplate(String toEmail, String subject, String template,
                              Map<String, Object> variables, String plainText) {
        String html = templateEngine.process(template, new Context(null, variables));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainText, html);
            helper.addInline(LOGO_CID, LOGO, "image/png");
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MailPreparationException("Falha ao montar e-mail para " + toEmail, e);
        }
    }
}
