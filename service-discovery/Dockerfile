# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN addgroup --system northcare && adduser --system --group northcare
COPY --from=builder /app/target/*.jar app.jar
USER northcare
EXPOSE 8761
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
