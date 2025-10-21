package org.example.security.config;

import lombok.RequiredArgsConstructor;
import org.example.security.cors.CorsOriginLoader;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final CorsOriginLoader corsOriginLoader;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsOriginLoader.loadOrigins().toArray(new String[0]))
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
        registry.addMapping("/v1/api/**")
                .allowedOrigins(corsOriginLoader.loadOrigins().toArray(new String[0]))
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
