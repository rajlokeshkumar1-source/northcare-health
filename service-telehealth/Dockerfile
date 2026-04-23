# ─────────────────────────────────────────────
# Stage 1 – Build
# ─────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

# Cache dependency layer separately from source
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ─────────────────────────────────────────────
# Stage 2 – Runtime (minimal JRE image)
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for least-privilege execution
RUN addgroup -S northcare && adduser -S telehealth -G northcare

COPY --from=builder /build/target/telehealth-service-*.jar app.jar

RUN chown northcare:northcare app.jar

USER telehealth

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
