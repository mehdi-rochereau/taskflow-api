package com.mehdi.taskflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point.
 *
 * <p>{@code @SpringBootApplication} combines three annotations: component scanning rooted at this
 * package, auto-configuration from the classpath, and the configuration class marker. Every
 * component of the application therefore lives under {@code com.mehdi.taskflow}, and a class placed
 * outside it would never be discovered.
 *
 * <p>{@code @EnableScheduling} activates the {@code @Scheduled} annotations, which here drive the
 * nightly purge of expired refresh tokens. Without it the annotation is silently ignored and the
 * purge never runs.
 */
@SpringBootApplication
@EnableScheduling
public class TaskflowApiApplication {

    /**
     * Boots the Spring context and starts the embedded Tomcat server.
     *
     * @param args command line arguments, forwarded to Spring Boot as configuration properties
     */
    public static void main(String[] args) {
        SpringApplication.run(TaskflowApiApplication.class, args);
    }
}
