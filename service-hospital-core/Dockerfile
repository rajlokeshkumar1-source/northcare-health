# ──────────────────────────────────────────────────────────────────────────────
# Stage 1 – Build
# ──────────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Cache dependency layer separately for faster rebuilds
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ──────────────────────────────────────────────────────────────────────────────
# Stage 2 – Runtime (minimal JRE image)
# ──────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user for security hardening
RUN addgroup --system northcare && adduser --system --group northcare

COPY --from=builder /app/target/*.jar app.jar

USER northcare

EXPOSE 8080

# JVM tuning: container-aware heap sizing + GC logging
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
