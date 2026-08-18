package com.mehdi.taskflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
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
 * <p>Configures the Swagger UI metadata via {@link OpenAPIDefinition}.
 * No security scheme is declared: authentication travels in the {@code jwt}
 * HttpOnly cookie, which the browser attaches on its own, so there is nothing
 * for the reader to paste into an {@code Authorize} dialog.</p>
 *
 * <p>The Swagger UI is accessible at {@code /swagger-ui/index.html}, served from
 * {@code http://localhost:8082} locally and from
 * {@code https://api.taskflow.mehdi-rochereau.dev} in production.</p>
 *
 * <p>The API documentation (Redoc) is served by the Angular frontend at
 * {@code /api-docs}: {@code http://localhost:4200} locally,
 * {@code https://taskflow.mehdi-rochereau.dev} in production.</p>
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
                        
                        Authentication travels in HttpOnly cookies, set by the API and sent back automatically by any client that keeps a cookie jar: browsers, Postman, curl with `-c` and `-b`. There is nothing to copy and no header to set.
                        
                        1. Register via `POST /api/auth/register`, or login via `POST /api/auth/login`
                        2. The response sets the `jwt` and `refreshToken` cookies
                        3. Every subsequent call is authenticated, including `Try it out` below
                        
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
                        
                        The `jwt` cookie is valid for **15 minutes**, scoped to `/api`. After expiration, the client calls `POST /api/auth/refresh`, which reads the `refreshToken` cookie, scoped to `/api/auth` and valid for 7 days, and issues a new pair.
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
                // Production first: Swagger UI selects the first entry by default.
                // With localhost first, the public documentation targeted the
                // visitor's own machine over plain HTTP from an HTTPS page.
                @Server(url = "https://api.taskflow.mehdi-rochereau.dev", description = "Production server"),
                @Server(url = "http://localhost:8082", description = "Local development server")
        }
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
