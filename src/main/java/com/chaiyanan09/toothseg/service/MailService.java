package com.chaiyanan09.toothseg.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class MailService {

    @Value("${mailjet.apiKey:${MAILJET_API_KEY:}}")
    private String apiKey;

    @Value("${mailjet.apiSecret:${MAILJET_API_SECRET:}}")
    private String apiSecret;

    @Value("${app.mail.from:${APP_MAIL_FROM:}}")
    private String fromEmail;

    @Value("${app.mail.fromName:${APP_MAIL_FROMNAME:ToothAI}}")
    private String fromName;

    private final HttpClient http = HttpClient.newHttpClient();

    public void sendResetLink(String toEmail, String resetUrl) {
        if (apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
            System.out.println("[RESET LINK] " + resetUrl);
            return;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new RuntimeException("APP_MAIL_FROM is empty (must be a validated sender in Mailjet).");
        }

        String subject = "Reset your password";
        String text =
                "Reset link (valid for 15 minutes):\n" +
                        resetUrl + "\n\n" +
                        "If you did not request this, ignore this email.";

        String payload = """
        {
          "Messages":[
            {
              "From":{"Email":"%s","Name":"%s"},
              "To":[{"Email":"%s"}],
              "Subject":"%s",
              "TextPart":%s
            }
          ]
        }
        """.formatted(
                esc(fromEmail), esc(fromName), esc(toEmail), esc(subject), jsonString(text)
        );

        String basic = Base64.getEncoder().encodeToString((apiKey + ":" + apiSecret).getBytes(StandardCharsets.UTF_8));

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mailjet.com/v3.1/send"))
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) {
                throw new RuntimeException("Mailjet error: " + res.statusCode() + " " + res.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String jsonString(String s) {
        String esc = esc(s).replace("\n", "\\n");
        return "\"" + esc + "\"";
    }
}