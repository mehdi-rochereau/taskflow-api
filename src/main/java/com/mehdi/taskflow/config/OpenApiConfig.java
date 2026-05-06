package com.mehdi.taskflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springframework.context.annotation.Bean;
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
 * <p>The API documentation (Redoc) is accessible at:
 * <a href="http://localhost:4200/api-docs">
 * http://localhost:4200/api-docs</a> (requires Angular frontend)</p>
 *
 * @see <a href="https://swagger.io/specification/">OpenAPI Specification</a>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "TaskFlow API",
                version = "1.0.0",
                description = """
                        ## TaskFlow REST API
                        
                        A task management API built with **Spring Boot 3.5**, **JWT authentication** and **MySQL**.
                        
                        > **Documentation:** [Redoc](https://taskflow.mehdi-rochereau.dev/api-docs) — [Swagger UI](https://api.taskflow.mehdi-rochereau.dev/swagger-ui/index.html)
                        
                        ### Authentication
                        
                        All protected endpoints require a valid JWT token passed as a Bearer token in the `Authorization` header:
                        ```
                        Authorization: Bearer <your-token>
                        ```
                        
                        To obtain a token:
                        1. Register a new account via `POST /api/auth/register`
                        2. Or login via `POST /api/auth/login`
                        3. Copy the `token` from the response
                        4. Click the **Authorize** button above and paste the token
                        
                        ### Internationalization (i18n)
                        
                        All error messages support English and French. Use the `Accept-Language` header to select the language:
                        ```
                        Accept-Language: en
                        Accept-Language: fr
                        ```
                        
                        ### Error responses
                        
                        All error responses follow a consistent structure:
                        ```json
                        {
                          "timestamp": "2026-04-18T10:00:00",
                          "status": 404,
                          "message": "Project not found"
                        }
                        ```
                        
                        Validation errors return a map of field-level messages:
                        ```json
                        {
                          "timestamp": "2026-04-18T10:00:00",
                          "status": 400,
                          "errors": {
                            "name": ["Project name is required"]
                          }
                        }
                        ```
                        
                        ### Token expiration
                        
                        JWT tokens are valid for **15 minutes**. After expiration, the client automatically refreshes the token using the `refreshToken` HttpOnly cookie via `POST /api/auth/refresh`.
                        """,
                contact = @Contact(
                        name = "Mehdi Rochereau",
                        url = "https://github.com/mehdi-rochereau/taskflow-api"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(url = "https://api.taskflow.mehdi-rochereau.dev", description = "Production server")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Bearer token obtained from POST /api/auth/login or POST /api/auth/register"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addParameters("Accept-Language", new Parameter()
                                .in("header")
                                .name("Accept-Language")
                                .description("Language for error messages. Supported values: `en` (default), `fr`")
                                .required(false)
                                .schema(new StringSchema()
                                        .addEnumItem("en")
                                        .addEnumItem("fr")
                                        ._default("en"))
                        )
                );
    }
}
