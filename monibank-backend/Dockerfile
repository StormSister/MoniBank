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
    && rm -rf /var/lib/apt/lists/*

# J3270 passes "-scriptport localhost:PORT", while Linux s3270
# expects "-scriptport PORT". Keep the original binary and place
# a compatibility wrapper earlier in PATH.
RUN mv /usr/bin/s3270 /usr/bin/s3270.real

COPY docker/s3270-wrapper.sh /usr/local/bin/s3270

RUN sed -i 's/\r$//' /usr/local/bin/s3270 \
    && chmod 755 /usr/local/bin/s3270 \
    && command -v s3270

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