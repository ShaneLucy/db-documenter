# Dockerfile for db-documenter CLI tool
# Requires the fat JAR to be built first: mvn clean package -DskipTests
FROM eclipse-temurin:25-jre-alpine

LABEL org.opencontainers.image.source="https://github.com/ShaneLucy/db-documenter"
LABEL org.opencontainers.image.description="Generates PlantUML ER diagrams from live database schemas"

RUN addgroup --system appuser && \
    adduser --system --ingroup appuser --no-create-home appuser

WORKDIR /app

COPY --chown=appuser:appuser target/*-jar-with-dependencies.jar app.jar

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--help"]
