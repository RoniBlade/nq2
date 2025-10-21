package org.example.security.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Midpoint Proxy Kamille",
                description = "Proxy для IDM системы MidPoint",
                version = "1.0.0",
                contact = @Contact(
                        name = "Adam Latyrov and Maksim Kukushkin"
                )
        )
)
public class OpenApiConfig {
}
