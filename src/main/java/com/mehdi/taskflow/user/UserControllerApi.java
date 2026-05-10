package com.mehdi.taskflow.user;

import com.mehdi.taskflow.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/**
 * OpenAPI documentation interface for {@link UserController}.
 *
 * <p>Declares all Swagger/OpenAPI annotations for user profile endpoints,
 * keeping {@link UserController} clean and focused on business logic.
 * This separation follows the Interface Segregation Principle — documentation
 * concerns are isolated from routing and delegation logic.</p>
 *
 * <p>All endpoints defined here require a valid JWT token passed as a Bearer token
 * in the {@code Authorization} header or via the {@code jwt} HttpOnly cookie.</p>
 *
 * @see UserController
 * @see UserService
 */
@Tag(
        name = "User",
        description = "Endpoints for managing the authenticated user's profile. All endpoints require a valid JWT token."
)
@SecurityRequirement(name = "bearerAuth")
public interface UserControllerApi {

    @Operation(
            summary = "Get authenticated user profile",
            description = """
                    Returns the public profile of the currently authenticated user.
                    
                    The response never includes the password or internal fields.
                    """,
            parameters = {
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Profile retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponse.class),
                                    examples = @ExampleObject(
                                            name = "Success",
                                            value = """
                                                    {
                                                      "id": 1,
                                                      "username": "mehdi",
                                                      "email": "mehdi@example.com",
                                                      "role": "ROLE_USER",
                                                      "createdAt": "2026-04-01T10:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Missing or invalid JWT token",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "status": 401,
                                                      "message": "Authentication required"
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
    ResponseEntity<UserResponse> getProfile();
}
