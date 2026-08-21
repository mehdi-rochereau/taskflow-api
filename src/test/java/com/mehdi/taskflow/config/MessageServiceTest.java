package com.mehdi.taskflow.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {

    @Mock private MessageSource messageSource;

    @InjectMocks private MessageService messageService;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void get_shouldResolveMessage_forCurrentLocale() {
        // GIVEN
        when(messageSource.getMessage("error.user.not.found", null, Locale.ENGLISH))
                .thenReturn("User not found");

        // WHEN
        String result = messageService.get("error.user.not.found");

        // THEN
        assertEquals("User not found", result);
        verify(messageSource).getMessage("error.user.not.found", null, Locale.ENGLISH);
    }

    @Test
    void get_shouldFollowTheContextLocale_whenItChanges() {
        // GIVEN
        LocaleContextHolder.setLocale(Locale.FRENCH);
        when(messageSource.getMessage("error.user.not.found", null, Locale.FRENCH))
                .thenReturn("Utilisateur introuvable");

        // WHEN
        String result = messageService.get("error.user.not.found");

        // THEN
        assertEquals("Utilisateur introuvable", result);
        verify(messageSource).getMessage("error.user.not.found", null, Locale.FRENCH);
    }

    @Test
    void get_shouldPassArguments_whenProvided() {
        // GIVEN
        Object[] args = {"status", "TaskStatus"};
        when(messageSource.getMessage("error.parameter.type.mismatch", args, Locale.ENGLISH))
                .thenReturn("Parameter 'status' must be of type TaskStatus");

        // WHEN
        String result = messageService.get("error.parameter.type.mismatch", "status", "TaskStatus");

        // THEN
        assertEquals("Parameter 'status' must be of type TaskStatus", result);
        verify(messageSource).getMessage("error.parameter.type.mismatch", args, Locale.ENGLISH);
    }

    @Test
    void get_shouldPropagate_whenKeyIsMissing() {
        // GIVEN
        when(messageSource.getMessage("error.unknown.key", null, Locale.ENGLISH))
                // The two-argument constructor is used rather than the single-argument
                // one, which composes its message from the JVM default locale: the text
                // would then read fr_FR here and en_US on the runner, and the assertion
                // below would pass or fail depending on the machine.
                .thenThrow(new NoSuchMessageException("error.unknown.key", Locale.ENGLISH));

        // WHEN
        NoSuchMessageException ex =
                assertThrows(
                        NoSuchMessageException.class,
                        () -> messageService.get("error.unknown.key"));

        // THEN
        assertEquals(
                "No message found under code 'error.unknown.key' for locale 'en'.",
                ex.getMessage());
        verify(messageSource).getMessage("error.unknown.key", null, Locale.ENGLISH);
    }
}
