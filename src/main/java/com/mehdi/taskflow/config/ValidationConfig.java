package com.mehdi.taskflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Configuration for Bean Validation (JSR-380) with i18n support.
 *
 * <h2>Problem solved</h2>
 * <p>By default, Hibernate Validator resolves validation messages
 * ({@code @NotBlank}, {@code @Size}, etc.) using the JVM default locale,
 * ignoring the {@code Accept-Language} HTTP header sent by the client.</p>
 *
 * <p>This configuration bridges Spring's {@link ResourceBundleMessageSource}
 * with Hibernate Validator's message interpolator, so that validation messages
 * are resolved from the correct locale based on the {@code Accept-Language} header
 * populated in {@link org.springframework.context.i18n.LocaleContextHolder}
 * by Spring MVC.</p>
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>The client sends {@code Accept-Language: fr} in the HTTP request header</li>
 *   <li>Spring MVC stores the locale in {@link org.springframework.context.i18n.LocaleContextHolder}</li>
 *   <li>When validation fails, Hibernate Validator asks the {@link LocalValidatorFactoryBean}
 *       to resolve the message key (e.g. {@code {validation.username.required}})</li>
 *   <li>The {@link ResourceBundleMessageSource} looks up the key in
 *       {@code ValidationMessages_fr.properties} and returns the French message</li>
 * </ol>
 *
 * <h2>Message files loaded</h2>
 * <ul>
 *   <li>{@code messages.properties} / {@code messages_fr.properties} — application error messages</li>
 *   <li>{@code ValidationMessages.properties} / {@code ValidationMessages_fr.properties} — Bean Validation messages</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>
 * // Request with Accept-Language: fr
 * POST /api/auth/register
 * Accept-Language: fr
 * { "username": "" }
 *
 * // Response
 * {
 *   "errors": {
 *     "username": ["Le nom d'utilisateur est obligatoire"]
 *   }
 * }
 *
 * // Request with Accept-Language: en
 * // Response
 * {
 *   "errors": {
 *     "username": ["Username is required"]
 *   }
 * }
 * </pre>
 *
 * @see org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
 * @see org.springframework.context.support.ResourceBundleMessageSource
 * @see org.springframework.context.i18n.LocaleContextHolder
 */
@Configuration
public class ValidationConfig {

    /**
     * Configures the Bean Validation factory to resolve constraint messages
     * using Spring's {@link ResourceBundleMessageSource} instead of the
     * default Hibernate Validator message interpolator.
     *
     * <p>Both {@code messages} and {@code ValidationMessages} basenames are loaded
     * so that all i18n keys are available during validation.</p>
     *
     * <p>{@code setFallbackToSystemLocale(false)} ensures that if a locale is not
     * supported (e.g. Spanish), the default English messages are returned instead
     * of the JVM system locale messages.</p>
     *
     * @return a fully configured {@link LocalValidatorFactoryBean}
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages", "ValidationMessages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);

        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}