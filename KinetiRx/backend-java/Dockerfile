# syntax=docker/dockerfile:1

# ---- Build stage -------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

# Resolve dependencies first so source-only changes reuse this layer.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package \
    && mkdir -p /out \
    && cp target/kinetirx-backend-*.jar /out/app.jar

# ---- Runtime stage -----------------------------------------------------
# Ubuntu 24.04 ships postgresql-client-16, which matches the postgres:16 image in
# deploy/docker-compose.yml. pg_dump's custom format must not be newer than the server's major version.
FROM eclipse-temurin:21-jre-noble AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client-16 ca-certificates wget \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system kinetirx \
    && useradd --system --gid kinetirx --home /app kinetirx

WORKDIR /app
COPY --from=build /out/app.jar /app/kinetirx-backend.jar

ENV PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

USER kinetirx
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD wget --quiet --spider http://127.0.0.1:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/kinetirx-backend.jar"]
