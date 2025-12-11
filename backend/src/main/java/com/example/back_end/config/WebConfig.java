package com.example.back_end.config;

import com.example.back_end.util.UploadStorage;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        List<String> locations = new ArrayList<>();
        for (Path candidate : UploadStorage.candidateRoots()) {
            Path abs = candidate.toAbsolutePath();
            try {
                Files.createDirectories(abs);
                String uri = abs.toUri().toString();
                if (!uri.endsWith("/")) uri = uri + "/";
                locations.add(uri);
            } catch (Exception ignored) {
            }
        }
        if (locations.isEmpty()) {
            Path fallback = Path.of("uploads").toAbsolutePath();
            String uri = fallback.toUri().toString();
            if (!uri.endsWith("/")) uri = uri + "/";
            locations.add(uri);
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(locations.toArray(new String[0]));
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}

