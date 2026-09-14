FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests


FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        s3270 \
        openssh-client \
        ca-certificates \
        curl \
    && command -v s3270 \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system monibank \
    && useradd \
        --system \
        --gid monibank \
        --home-dir /app \
        --create-home \
        monibank

WORKDIR /app

COPY --from=build /build/target/*.jar /app/monibank-backend.jar

RUN mkdir -p /app/logs/operations \
    && chown -R monibank:monibank /app

USER monibank

EXPOSE 8080

HEALTHCHECK \
    --interval=30s \
    --timeout=5s \
    --start-period=90s \
    --retries=3 \
    CMD curl --fail --silent \
        http://localhost:8080/api/mainframe/status \
        > /dev/null || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/monibank-backend.jar"]