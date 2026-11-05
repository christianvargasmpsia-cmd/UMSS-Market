# syntax=docker/dockerfile:1.7

# Build stage: compile the Spring Boot application from source.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml ./pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

COPY src/main ./src/main
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests package \
    && test -f target/umss-market-api-0.0.1-SNAPSHOT.jar

# Runtime stage: only the packaged JAR and a JRE are shipped.
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S -g 10001 appgroup \
    && adduser -S -u 10001 -G appgroup -h /app appuser

WORKDIR /app

COPY --from=build --chown=appuser:appgroup \
    /workspace/target/umss-market-api-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 8080

USER 10001

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD ["wget", "-q", "-O", "/dev/null", "http://127.0.0.1:8080/actuator/health"]

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
