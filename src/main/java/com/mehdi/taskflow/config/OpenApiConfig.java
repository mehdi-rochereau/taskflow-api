package com.mehdi.taskflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
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
                version = "1.0.0",
                description = """
                        ## TaskFlow REST API
                        
                        A task management API built with **Spring Boot 3.5**, **JWT authentication** and **MySQL**.
                        
                        > **Documentation:** [Redoc](http://localhost:8082/redoc.html) — [Swagger UI](http://localhost:8082/swagger-ui/index.html)
                        
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
                        
                        JWT tokens are valid for **24 hours**. After expiration, re-authenticate to obtain a new token.
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
                @Server(url = "http://localhost:8082", description = "Local development server")
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
}
