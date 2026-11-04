package com.backend.threatlens.service;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        emailService = new EmailService(mailSender, templateEngine);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@threatlens.com");

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    // --- sendRegisterCode ---

    @Test
    void sendRegisterCode_sendsHtmlWithCodeAndInlineLogo() throws Exception {
        emailService.sendRegisterCode("user@test.com", "4827");

        MimeMessage message = captureSentMessage();
        assertThat(message.getSubject()).isEqualTo("ThreatLens – Confirme seu e-mail");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("user@test.com");

        List<Part> parts = leafParts(message);
        assertThat(contentOf(parts, "text/html"))
                .contains("Confirme seu e-mail", "cid:logo", "ThreatLens 2026")
                .containsSubsequence(">4<", ">8<", ">2<", ">7<");
        assertThat(contentOf(parts, "text/plain")).contains("4827");
        assertThat(hasInlineLogo(parts)).isTrue();
    }

    // --- sendResetPasswordCode ---

    @Test
    void sendResetPasswordCode_sendsHtmlWithCodeAndInlineLogo() throws Exception {
        emailService.sendResetPasswordCode("user@test.com", "0193");

        MimeMessage message = captureSentMessage();
        assertThat(message.getSubject()).isEqualTo("ThreatLens – Redefinição de senha");

        List<Part> parts = leafParts(message);
        assertThat(contentOf(parts, "text/html"))
                .contains("Redefinição de senha", "cid:logo", "Sua senha continua a mesma")
                .containsSubsequence(">0<", ">1<", ">9<", ">3<");
        assertThat(contentOf(parts, "text/plain")).contains("0193");
        assertThat(hasInlineLogo(parts)).isTrue();
    }

    private MimeMessage captureSentMessage() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();
        message.saveChanges();
        return message;
    }

    private static List<Part> leafParts(Part part) throws Exception {
        List<Part> parts = new ArrayList<>();
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart child = multipart.getBodyPart(i);
                parts.addAll(leafParts(child));
            }
        } else {
            parts.add(part);
        }
        return parts;
    }

    private static String contentOf(List<Part> parts, String mimeType) throws Exception {
        for (Part part : parts) {
            if (part.isMimeType(mimeType)) {
                return (String) part.getContent();
            }
        }
        throw new AssertionError("Parte " + mimeType + " não encontrada");
    }

    private static boolean hasInlineLogo(List<Part> parts) throws Exception {
        for (Part part : parts) {
            String[] contentId = part.getHeader("Content-ID");
            if (part.isMimeType("image/png") && contentId != null && contentId[0].equals("<logo>")) {
                return true;
            }
        }
        return false;
    }
}
