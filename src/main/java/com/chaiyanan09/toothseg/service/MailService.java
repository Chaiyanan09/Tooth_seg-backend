package com.chaiyanan09.toothseg.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender sender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.fromName:ToothAI}")
    private String fromName;

    public MailService(JavaMailSender sender) {
        this.sender = sender;
    }

    public void sendResetLink(String to, String link) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail, fromName);  // ✅ ชื่อ ToothAI
            helper.setTo(to);
            helper.setSubject("Reset your password");
            helper.setText(
                    "Reset link (valid for 15 minutes):\n" + link + "\n\n" +
                            "If you did not request this, ignore this email.",
                    false
            );

            sender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}