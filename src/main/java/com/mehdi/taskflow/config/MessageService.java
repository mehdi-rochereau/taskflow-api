package com.mehdi.taskflow.config;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility component for resolving i18n messages from the message source.
 *
 * <p>Resolves messages based on the current request locale,
 * determined by the {@code Accept-Language} HTTP header via
 * {@link LocaleContextHolder}.</p>
 *
 * @see MessageSource
 */
@Component
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Constructs a new {@code MessageService} with its required dependency.
     *
     * @param messageSource Spring message source backed by messages.properties files
     */
    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Resolves a message by key for the current request locale.
     *
     * @param key the message key defined in messages.properties
     * @return the resolved message string
     */
    public String get(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    /**
     * Resolves a message by key with arguments for the current request locale.
     *
     * @param key  the message key defined in messages.properties
     * @param args arguments to substitute in the message (e.g. {0}, {1})
     * @return the resolved message string with substituted arguments
     */
    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}