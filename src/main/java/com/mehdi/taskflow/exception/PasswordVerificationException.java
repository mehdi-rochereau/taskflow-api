package com.mehdi.taskflow.exception;

/**
 * Exception raised when a submitted password fails verification against the stored BCrypt hash.
 *
 * <p>Reserved for that single case. A password that is present, well formed, and simply wrong is
 * neither a malformed request nor a business rule violation: the caller is asked to prove their
 * identity and fails to. Every other rule bearing on a submitted value stays on {@link
 * IllegalArgumentException} and its {@code 400}, including the rule forbidding a new password
 * identical to the current one, which constrains the value itself rather than verifying an
 * identity.
 *
 * <p>Mapped to {@code 422 Unprocessable Entity} by {@link GlobalExceptionHandler}. The {@code 401}
 * family was considered and rejected: the frontend treats a {@code 401} on a non-auth endpoint as
 * an expired session and silently refreshes, then replays the original request carrying the same
 * wrong password.
 *
 * @see GlobalExceptionHandler
 */
public class PasswordVerificationException extends RuntimeException {

    /**
     * Constructs a new {@code PasswordVerificationException} with the given message.
     *
     * @param message the error message, already resolved for the current request locale by {@code
     *     MessageService}
     */
    public PasswordVerificationException(String message) {
        super(message);
    }
}
