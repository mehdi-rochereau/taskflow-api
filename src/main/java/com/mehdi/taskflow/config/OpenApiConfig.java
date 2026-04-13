package com.mehdi.taskflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.1 documentation configuration for the TaskFlow API.
 *
 * <p>Configures the Swagger UI metadata via {@link OpenAPIDefinition}
 * and registers a global Bearer token security scheme via {@link SecurityScheme},
 * allowing JWT authentication directly from the Swagger UI interface.</p>
 *
 * <p>The Swagger UI is accessible at:
 * <a href="http://localhost:8082/swagger-ui/index.html">
 * http://localhost:8082/swagger-ui/index.html</a></p>
 *
 * <p>The raw OpenAPI specification is available at:
 * <a href="http://localhost:8082/v3/api-docs">
 * http://localhost:8082/v3/api-docs</a></p>
 *
 * @see <a href="https://swagger.io/specification/">OpenAPI Specification</a>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "TaskFlow API",
                version = "1.0",
                description = "Task management REST API — Spring Boot 3.5 / JWT / MySQL"
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}