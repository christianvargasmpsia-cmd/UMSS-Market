FROM eclipse-temurin:21-jre-noble

WORKDIR /app

RUN groupadd --system appgroup && \
    useradd --system --gid appgroup appuser

COPY target/umss-market-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]