package com.example.back_end.service;

import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final String DEFAULT_FROM = "noreply@yourlms.local";

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                       MailProperties mailProperties,
                       @Value("${app.mail.from:noreply@yourlms.local}") String fromAddress) {
        JavaMailSender available = mailSenderProvider.getIfAvailable();
        if (available == null && mailProperties.getHost() != null) {
            available = buildFallbackSender(mailProperties);
        }
        this.mailSender = available;
        this.fromAddress = fromAddress;
    }

    public void sendOtpEmail(String to, String subject, String code, int expireMinutes) {
        String body = "Ma OTP cua ban la: " + code + "\nMa se het han sau " + expireMinutes + " phut.";
        sendTextMail(to, subject, body);
    }

    private void sendTextMail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Skip sending email because recipient is empty. Subject: {}", subject);
            return;
        }
        if (mailSender == null) {
            log.warn("Mail sender is not configured. OTP email to {} skipped. Body: {}", to, body);
            return;
        }
        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setFrom(parseAddress(fromAddress));
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            log.warn("Khong the gui email toi {}: {}", to, ex.getMessage());
        }
    }

    private InternetAddress parseAddress(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            raw = DEFAULT_FROM;
        }
        if (raw.contains("<") && raw.contains(">")) {
            String name = raw.substring(0, raw.indexOf("<")).trim();
            String email = raw.substring(raw.indexOf("<") + 1, raw.indexOf(">")).trim();
            return new InternetAddress(email, name, StandardCharsets.UTF_8.displayName());
        }
        return new InternetAddress(raw.trim());
    }

    private JavaMailSender buildFallbackSender(MailProperties props) {
        if (props.getHost() == null) return null;
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(props.getHost());
        if (props.getPort() != null) impl.setPort(props.getPort());
        impl.setUsername(props.getUsername());
        impl.setPassword(props.getPassword());
        impl.setProtocol(props.getProtocol());
        Properties javaMailProps = impl.getJavaMailProperties();
        Map<String, String> custom = props.getProperties();
        custom.forEach(javaMailProps::put);
        javaMailProps.putIfAbsent("mail.smtp.auth", "true");
        javaMailProps.putIfAbsent("mail.smtp.starttls.enable", "true");
        javaMailProps.putIfAbsent("mail.smtp.starttls.required", "true");
        log.info("Initialized fallback JavaMailSender for host {}:{}", props.getHost(), props.getPort());
        return impl;
    }
}
