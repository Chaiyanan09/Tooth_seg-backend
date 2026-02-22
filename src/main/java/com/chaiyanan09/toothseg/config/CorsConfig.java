package com.chaiyanan09.toothseg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // เราใช้ Bearer token ใน header ไม่ใช้ cookie -> ไม่ต้อง allowCredentials
        cfg.setAllowCredentials(false);

        // ✅ อนุญาต Vercel + local dev
        cfg.setAllowedOriginPatterns(List.of(
                "https://*.vercel.app",
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type"));
        cfg.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}