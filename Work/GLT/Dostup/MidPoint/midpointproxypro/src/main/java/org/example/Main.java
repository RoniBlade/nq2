package org.example;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@OpenAPIDefinition(
        info = @Info(
                title = "proxy midpoint",
                version = "1.0"
        )
)
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
