package example.data.service;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        // Pulls your API gateway URL dynamically, defaulting to port 8080
        @Value("${app.gateway.url:http://localhost:8080}")
        private String gatewayUrl;

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Data Service & Budget Tracker API")
                                                .version("v1.0")
                                                .description("Manages user expenses, budgeting math, and administrative properties. "
                                                                +
                                                                "All secured endpoints must be routed through the API Gateway with a valid JWT.")
                                                .contact(new Contact()
                                                                .name("Backend Team")
                                                                .email("developer@example.com")))
                                .servers(List.of(
                                                new Server()
                                                                .url(gatewayUrl)
                                                                .description("API Gateway")))
                                // Applies the global authorization lock to all endpoints by default
                                .addSecurityItem(new SecurityRequirement()
                                                .addList("Bearer Authentication"))
                                .components(new Components()
                                                .addSecuritySchemes("Bearer Authentication",
                                                                new SecurityScheme()
                                                                                .name("Bearer Authentication")
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("Paste the JWT token received from your Auth Server login.")));
        }
}