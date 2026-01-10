package com.estevam.calculacoes.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI calculacoesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CalculAções API")
                        .description("API for calculating stock portfolio positions and tax declarations")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Estevam Galvão Albuquerque")
                                .email("contatoestevamalbuquerque@gmail.com")
                                .url("https://github.com/estevamgalvao"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.calculacoes.com")
                                .description("Production Server")
                ));
    }
}