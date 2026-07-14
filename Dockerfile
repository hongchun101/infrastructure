# syntax=docker/dockerfile:1.7

# ---------- build stage ----------
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

# cache gradle wrapper + dependencies first
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --version --no-daemon

COPY core/build.gradle.kts security/build.gradle.kts observability/build.gradle.kts alert/build.gradle.kts app/build.gradle.kts \
    core/ security/ observability/ alert/ app/
RUN ./gradlew :app:bootJar -x test --no-daemon

# ---------- runtime stage ----------
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app

# a non-root user owns the jar and the JVM scratch dirs
COPY --from=build --chown=app:app /workspace/app/build/libs/app.jar /app/app.jar

USER app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom" \
    SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]