# ==============================================================================
# PaperERP Enterprise Backend - Hardened Multi-Stage Containerfile
# Base Image: Eclipse Temurin 17 JRE Alpine (Minimal attack surface)
# ==============================================================================

FROM eclipse-temurin:17-jre-alpine

# Metadata
LABEL maintainer="PaperERP Enterprise Architecture Team" \
      service="paper-erp-api" \
      version="1.0.0-SNAPSHOT"

# 1. Create dedicated non-root application user and group
RUN addgroup -S appgroup -g 10001 && \
    adduser -S appuser -u 10001 -G appgroup -h /app -s /sbin/nologin

WORKDIR /app

# 2. Copy the pre-built, verified Spring Boot fat JAR into the image
# Note: Build JAR prior to docker build via ./mvnw.cmd clean package
COPY --chown=appuser:appgroup paper-erp-api/target/paper-erp-api-1.0.0-SNAPSHOT.jar /app/app.jar

# 3. Restrict file permissions
RUN chmod 500 /app/app.jar

# 4. Expose strictly the HTTP application port
EXPOSE 8080

# 5. Define non-root user execution
USER appuser:appgroup

# 6. Container healthcheck probing Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# 7. JVM Options tuned for container cgroups and memory limits
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport \
                       -XX:MaxRAMPercentage=75.0 \
                       -XX:InitialRAMPercentage=50.0 \
                       -XX:+ExitOnOutOfMemoryError \
                       -Djava.security.egd=file:/dev/./urandom"

# 8. Entrypoint execution
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
