# ============================================================
# Stage 1 — Build
# Compiles the Spring Boot application with Gradle
# The builder stage contains JDK + Gradle + source code
# None of this ends up in the final image
# ============================================================
FROM gradle:8.13-jdk21-alpine AS builder

WORKDIR /app

# Copy dependency manifests first to leverage Docker layer caching.
# Docker builds images in layers — each instruction creates a layer.
# If these files haven't changed, Docker reuses the cached layer
# and skips re-downloading all dependencies (can save several minutes).
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/

# Download dependencies without building the application.
# This layer is cached as long as build.gradle doesn't change.
RUN gradle dependencies --no-daemon

# Copy source files — this layer changes on every code change.
# Placed after dependency download to maximize cache reuse.
COPY src/ src/

# Build the executable JAR.
# bootJar produces a fat JAR containing the app + all dependencies.
# -x test skips tests — already run in CI, not needed in Docker build.
# --no-daemon avoids starting a background Gradle daemon in containers.
RUN gradle bootJar --no-daemon -x test

# ============================================================
# Stage 2 — Runtime
# Runs the Spring Boot application with a minimal JRE image.
# Final image contains only JRE + JAR — no Gradle, no source code,
# no build tools. Significantly smaller and more secure.
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a dedicated non-root user and group for security.
# Running as root inside a container is a security risk —
# if the app is compromised, the attacker gets root on the host.
# -S creates a system user/group (no password, no home directory).
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow

# Copy only the built JAR from Stage 1.
# The *.jar glob matches the single JAR produced by bootJar.
# Everything else from Stage 1 (Gradle cache, source, etc.) is discarded.
COPY --from=builder /app/build/libs/*.jar app.jar

# Set correct ownership so the non-root user can read the JAR.
RUN chown taskflow:taskflow app.jar

# Switch to the non-root user for all subsequent instructions
# and at runtime.
USER taskflow

# Document which port the application listens on.
# EXPOSE is informational — it does not publish the port automatically.
# The actual port mapping is done in docker-compose.yml.
EXPOSE 8082

# JVM options optimized for container environments.
# -XX:+UseContainerSupport   — makes the JVM respect container CPU/memory
#                              limits instead of reading host machine specs.
# -XX:MaxRAMPercentage=75.0  — caps heap at 75% of container memory,
#                              leaving 25% for OS, threads and metaspace.
# -XX:+UseG1GC               — G1 garbage collector, well-suited for
#                              low-latency server workloads.
# -Djava.security.egd=...    — speeds up SecureRandom initialization on Linux
#                              by using /dev/urandom instead of /dev/random,
#                              which can block if entropy is low.
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

# Start the application.
# "exec" replaces the shell process with Java, making Java PID 1.
# This ensures Docker's SIGTERM signal reaches Java directly,
# enabling graceful shutdown (connection draining, clean thread stop).
# Without "exec", the shell becomes PID 1 and may not forward signals.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]