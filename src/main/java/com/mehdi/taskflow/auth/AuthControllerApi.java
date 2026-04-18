package com.mehdi.taskflow.auth;

import com.mehdi.taskflow.user.dto.AuthResponse;
import com.mehdi.taskflow.user.dto.LoginRequest;
import com.mehdi.taskflow.user.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenAPI documentation interface for {@link AuthController}.
 *
 * <p>Declares all Swagger/OpenAPI annotations for authentication endpoints,
 * keeping {@link AuthController} clean and focused on business logic.</p>
 *
 * <p>All endpoints defined here are public — no JWT token required.</p>
 *
 * @see AuthController
 */
@Tag(
        name = "Authentication",
        description = "Public endpoints for account registration and login. No JWT token required."
)
public interface AuthControllerApi {

    @Operation(
            summary = "Register a new user account",
            description = """
                    Creates a new user account with a BCrypt-encoded password.
                    
                    Returns a signed JWT token valid for **24 hours** upon successful registration.
                    
                    **Constraints:**
                    - `username` must be between 3 and 50 characters and unique
                    - `email` must be a valid email address and unique
                    - `password` must be at least 8 characters
                    """,
            parameters = {
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Account successfully created",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponse.class),
                                    examples = @ExampleObject(
                                            name = "Success",
                                            value = """
                                                    {
                                                      "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtZWhkaSIsImlhdCI6MTc...",
                                                      "username": "mehdi",
                                                      "email": "mehdi@example.com"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation failed or username/email already taken",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "Validation error",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "errors": {
                                                                "username": ["Username is required"],
                                                                "email": ["Invalid email address"],
                                                                "password": ["Password must be at least 8 characters"]
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "Username already taken",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "message": "This username is already taken"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "Email already taken",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "message": "This email is already in use"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 500,
                                                      "message": "An unexpected error occurred"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request);

    @Operation(
            summary = "Login",
            description = """
                    Authenticates a user and returns a signed JWT token valid for **24 hours**.
                    
                    The `identifier` field accepts either a **username** or an **email address**.
                    
                    Use the returned `token` as a Bearer token in the `Authorization` header for all protected endpoints:
                    ```
                    Authorization: Bearer <token>
                    ```
                    """,
            parameters = {
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Login successful",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponse.class),
                                    examples = @ExampleObject(
                                            name = "Success",
                                            value = """
                                                    {
                                                      "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtZWhkaSIsImlhdCI6MTc...",
                                                      "username": "mehdi",
                                                      "email": "mehdi@example.com"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Missing or blank fields",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 400,
                                                      "errors": {
                                                        "identifier": ["Username or email is required"],
                                                        "password": ["Password is required"]
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Invalid credentials",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 401,
                                                      "message": "Bad credentials"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 500,
                                                      "message": "An unexpected error occurred"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request);
}
