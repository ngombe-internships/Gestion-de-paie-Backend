package com.hades.paie1;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "API Gestion Paie Hades",
                version = "1.0.0",
                description = "Documentation API pour la gestion de paie - Système Hades",
                contact = @Contact(
                        name = "Équipe Hades",
                        email = "support@hades.com"
                )
        )
)
public class Paie1Application {

    public static void main(String[] args) {
        SpringApplication.run(Paie1Application.class, args);
    }


}
