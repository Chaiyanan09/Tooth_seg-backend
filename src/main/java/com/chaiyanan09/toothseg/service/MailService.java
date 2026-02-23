package com.chaiyanan09.toothseg.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class MailService {

    @Value("${resend.apiKey:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${app.mail.from:${APP_MAIL_FROM:}}")
    private String fromEmail;

    @Value("${app.mail.fromName:${APP_MAIL_FROMNAME:ToothAI}}")
    private String fromName;

    @Value("${app.reset.expiresMinutes:${APP_RESET_EXPIRES_MINUTES:15}}")
    private int expiresMinutes;

    private final HttpClient http = HttpClient.newHttpClient();

    public void sendResetLink(String toEmail, String resetUrl) {
        // ถ้าไม่ได้ตั้ง key -> ไม่ให้ระบบล่ม (กัน 500)
        if (resendApiKey == null || resendApiKey.isBlank()) {
            System.out.println("[RESET LINK] " + resetUrl);
            return;
        }

        String subject = "Reset your password";
        String text =
                "Reset link (valid for " + expiresMinutes + " minutes):\n" +
                        resetUrl + "\n\n" +
                        "If you did not request this, ignore this email.";

        // Resend expects JSON:
        // { "from": "Name <email>", "to": ["..."], "subject": "...", "text": "..." }
        String json = """
        {
          "from": "%s <%s>",
          "to": ["%s"],
          "subject": "%s",
          "text": %s
        }
        """.formatted(
                escape(fromName), escape(fromEmail), escape(toEmail), escape(subject), toJsonString(text)
        );

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) {
                throw new RuntimeException("Resend error: " + res.statusCode() + " " + res.body());
            }
        } catch (Exception e) {
            // กันระบบล่มเป็น 500 (สำคัญ)
            System.err.println("[MAIL ERROR] " + e.getMessage());
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String toJsonString(String s) {
        String esc = escape(s).replace("\n", "\\n");
        return "\"" + esc + "\"";
    }
}