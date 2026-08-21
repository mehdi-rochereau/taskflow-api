package com.mehdi.taskflow.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mehdi.taskflow.config.AuditService;
import com.mehdi.taskflow.config.MessageService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

    // The web slice tests already exercise this class indirectly, but they
    // assert the HTTP status the client receives. These tests assert the body
    // structure and the audit calls, which the slices never look at.
    @Mock private MessageService messageService;

    @Mock private AuditService auditService;

    @InjectMocks private GlobalExceptionHandler globalExceptionHandler;

    /** Asserts the three fields every standard error body carries. */
    private void assertStandardBody(
            ResponseEntity<Map<String, Object>> response, HttpStatus status, String message) {
        assertEquals(status, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(status.value(), body.get("status"));
        assertEquals(message, body.get("message"));
        // The timestamp is present on every error so that a log line can be
        // matched against a user's report of when it happened.
        assertNotNull(body.get("timestamp"));
        assertInstanceOf(LocalDateTime.class, body.get("timestamp"));
    }

    // ── 404 ──────────────────────────────────────────────────────────

    @Test
    void handleResourceNotFound_shouldReturn404_withTheExceptionMessage() {
        // GIVEN
        // The message comes from the exception, already resolved by the service
        // that threw it: the handler does not resolve it a second time.
        ResourceNotFoundException ex = new ResourceNotFoundException("Project not found");

        // WHEN
        ResponseEntity<Map<String, Object>> response =
                globalExceptionHandler.handleResourceNotFound(ex);

        // THEN
        assertStandardBody(response, HttpStatus.NOT_FOUND, "Project not found");
        verify(messageService, never()).get("error.access.denied");
    }

    // ── 403 ──────────────────────────────────────────────────────────

    @Test
    void handleAccessDenied_shouldReturn403_withAGenericMessage() {
        // GIVEN
        // The message is generic on purpose: telling the caller why access was
        // denied would confirm the resource exists.
        when(messageService.get("error.access.denied")).thenReturn("Access denied");

        // WHEN
        ResponseEntity<Map<String, Object>> response = globalExceptionHandler.handleAccessDenied();

        // THEN
        assertStandardBody(response, HttpStatus.FORBIDDEN, "Access denied");
    }

    // ── 401 ──────────────────────────────────────────────────────────

    @Test
    void handleBadCredentials_shouldReturn401_andAuditTheFailure() {
        // GIVEN
        when(messageService.get("error.bad.credentials"))
                .thenReturn("Invalid username or password");

        // WHEN
        ResponseEntity<Map<String, Object>> response =
                globalExceptionHandler.handleBadCredentials();

        // THEN
        assertStandardBody(response, HttpStatus.UNAUTHORIZED, "Invalid username or password");
        // The audit entry is the only trace a brute-force attempt leaves. A
        // handler that answered 401 without logging would make the attack
        // invisible.
        verify(auditService).logLoginFailure("credentials-invalid");
    }

    @Test
    void handleBadCredentials_shouldNotRevealWhichPartWasWrong() {
        // GIVEN
        // The message must not distinguish an unknown user from a wrong
        // password, otherwise it becomes a username enumeration oracle.
        when(messageService.get("error.bad.credentials"))
                .thenReturn("Invalid username or password");

        // WHEN
        ResponseEntity<Map<String, Object>> response =
                globalExceptionHandler.handleBadCredentials();

        // THEN
        assertNotNull(response.getBody());
        String message = (String) response.getBody().get("message");
        assertFalse(message.toLowerCase().contains("not found"));
        assertFalse(message.toLowerCase().contains("unknown"));
    }

    // ── 400, business ────────────────────────────────────────────────

    @Test
    void handleIllegalArgument_shouldReturn400_withTheExceptionMessage() {
        // GIVEN
        IllegalArgumentException ex =
                new IllegalArgumentException("This username is already taken");

        // WHEN
        ResponseEntity<Map<String, Object>> response =
                globalExceptionHandler.handleIllegalArgument(ex);

        // THEN
        assertStandardBody(response, HttpStatus.BAD_REQUEST, "This username is already taken");
    }

    // ── 400, validation ──────────────────────────────────────────────

    @Test
    void handleValidation_shouldGroupErrorsByField() {
        // GIVEN
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "username", "Username is required"));
        bindingResult.addError(new FieldError("request", "email", "Invalid email address"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        // WHEN
        ResponseEntity<Map<String, Object>> response = globalExceptionHandler.handleValidation(ex);

        // THEN
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.get("status"));
        assertNotNull(body.get("timestamp"));
        // The body carries "errors", never "message": the client needs to know
        // which field failed, not a single sentence.
        assertFalse(body.containsKey("message"));

        @SuppressWarnings("unchecked")
        Map<String, List<String>> errors = (Map<String, List<String>>) body.get("errors");
        assertEquals(2, errors.size());
        assertEquals(List.of("Username is required"), errors.get("username"));
        assertEquals(List.of("Invalid email address"), errors.get("email"));
    }

    @Test
    void handleValidation_shouldAccumulateSeveralMessagesUnderTheSameField() {
        // GIVEN
        // This is the only real logic in the class: computeIfAbsent accumulates
        // rather than overwrites. A blank username trips both @NotBlank and
        // @Size, and replacing instead of appending would silently drop one of
        // the two reasons.
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "username", "Username is required"));
        bindingResult.addError(
                new FieldError(
                        "request", "username", "Username must be between 3 and 50 characters"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        // WHEN
        ResponseEntity<Map<String, Object>> response = globalExceptionHandler.handleValidation(ex);

        // THEN
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, List<String>> errors =
                (Map<String, List<String>>) response.getBody().get("errors");
        assertEquals(1, errors.size());
        assertEquals(2, errors.get("username").size());
        assertTrue(errors.get("username").contains("Username is required"));
        assertTrue(errors.get("username").contains("Username must be between 3 and 50 characters"));
    }

    // ── 400, type mismatch ───────────────────────────────────────────

    @Test
    void handleTypeMismatch_shouldNameTheParameterAndItsExpectedType() {
        // GIVEN
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("projectId");
        when(ex.getRequiredType()).thenAnswer(invocation -> Long.class);
        when(messageService.get("error.parameter.type.mismatch", "projectId", "Long"))
                .thenReturn("Parameter 'projectId' must be of type Long");

        // WHEN
        ResponseEntity<Map<String, Object>> response =
                globalExceptionHandler.handleTypeMismatch(ex);

        // THEN
        assertStandardBody(
                response, HttpStatus.BAD_REQUEST, "Parameter 'projectId' must be of type Long");
    }

    @Test
    void handleTypeMismatch_shouldReportUnknown_whenRequiredTypeIsNull() {
        // GIVEN
        // getRequiredType() returns null when Spring cannot determine the target
        // type. The ternary exists for that case: without it the handler would
        // throw an NPE while handling another error, and the client would get a
        // 500 in place of the 400 it deserves.
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("status");
        when(ex.getRequiredType()).thenReturn(null);
        when(messageService.get("error.parameter.type.mismatch", "status", "unknown"))
                .thenReturn("Parameter 'status' must be of type unknown");

        // WHEN
        ResponseEntity<Map<String, Object>> response =
                globalExceptionHandler.handleTypeMismatch(ex);

        // THEN
        assertStandardBody(
                response, HttpStatus.BAD_REQUEST, "Parameter 'status' must be of type unknown");
    }

    // ── 500 ──────────────────────────────────────────────────────────

    @Test
    void handleGeneric_shouldReturn500_andAuditTheError() {
        // GIVEN
        IllegalStateException ex = new IllegalStateException("connection pool exhausted");
        when(messageService.get("error.unexpected")).thenReturn("An unexpected error occurred");

        // WHEN
        ResponseEntity<Map<String, Object>> response = globalExceptionHandler.handleGeneric(ex);

        // THEN
        assertStandardBody(
                response, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        verify(auditService).logUnexpectedError(ex);
    }

    @Test
    void handleGeneric_shouldNotLeakInternalDetailsToTheClient() {
        // GIVEN
        // The real cause goes to the audit log, never to the response: a stack
        // trace or a driver message handed to the client is reconnaissance.
        IllegalStateException ex =
                new IllegalStateException("jdbc:mysql://taskflow-db:3306 access denied for user");
        when(messageService.get("error.unexpected")).thenReturn("An unexpected error occurred");

        // WHEN
        ResponseEntity<Map<String, Object>> response = globalExceptionHandler.handleGeneric(ex);

        // THEN
        assertNotNull(response.getBody());
        String message = (String) response.getBody().get("message");
        assertFalse(message.contains("jdbc"));
        assertFalse(message.contains("taskflow-db"));
        verify(auditService).logUnexpectedError(ex);
    }
}
