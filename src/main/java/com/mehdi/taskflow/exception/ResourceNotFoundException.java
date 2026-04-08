package com.mehdi.taskflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource cannot be found in the database.
 *
 * <p>Automatically produces an HTTP {@code 404 Not Found} response
 * via {@link GlobalExceptionHandler}.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * Project project = projectRepository.findById(id)
 *     .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
 * }</pre>
 *
 * @see GlobalExceptionHandler
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message description of the missing resource,
     *                included in the HTTP response body
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
