# NorthCare Hospital Core Service

HIPAA-compliant patient records microservice built with Java 21 + Spring Boot 3.2.

## Prerequisites

| Tool       | Version   |
|------------|-----------|
| Java       | 21+       |
| Maven      | 3.9+      |
| Docker     | 24+       |
| PostgreSQL | 15+ (or via Docker) |

---

## Local Development

### 1. Start PostgreSQL

```bash
docker run -d \
  --name northcare-db \
  -e POSTGRES_DB=northcare_hospital \
  -e POSTGRES_USER=northcare \
  -e POSTGRES_PASSWORD=changeme \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Set environment variables

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/northcare_hospital
export DB_USERNAME=northcare
export DB_PASSWORD=changeme
export ENCRYPTION_KEY=my-dev-only-key-min-32-chars-long!
```

### 3. Run the service

```bash
mvn spring-boot:run
```

The service starts on **http://localhost:8080**.

---

## API Documentation

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/api-docs | OpenAPI JSON spec |
| http://localhost:8080/health | Liveness probe |
| http://localhost:8080/ready | Readiness probe |
| http://localhost:8080/actuator/prometheus | Prometheus metrics |

---

## Running Tests (with Testcontainers)

Tests spin up a real PostgreSQL instance automatically via Testcontainers — no local DB setup required for tests.

```bash
mvn test
```

---

## Docker Build

```bash
docker build -t northcare/hospital-core:local .

docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/northcare_hospital \
  -e DB_USERNAME=northcare \
  -e DB_PASSWORD=changeme \
  -e ENCRYPTION_KEY=my-dev-only-key-min-32-chars-long! \
  northcare/hospital-core:local
```

---

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_URL` | Dev only | `jdbc:postgresql://localhost:5432/northcare_hospital` | JDBC connection URL |
| `DB_USERNAME` | Dev only | `northcare` | Database username |
| `DB_PASSWORD` | Dev only | `changeme` | Database password |
| `NORTHCARE_DB_SECRET_NAME` | Prod | _(empty)_ | AWS Secrets Manager secret name for DB creds |
| `ENCRYPTION_KEY` | Yes | `default-dev-key-change-in-prod` | AES-256-GCM key for PHI encryption |
| `AWS_REGION` | Prod | `us-east-1` | AWS region for Secrets Manager |

---

## HIPAA Notes

### PHI Fields

| Field | Entity | Protection |
|-------|--------|------------|
| `ssnLast4` | `Patient` | AES-256-GCM encrypted at rest; **never** returned in API responses |
| `dateOfBirth` | `Patient` | Stored in plaintext; restrict access via RBAC (Month 3) |
| `diagnosisCodes` | `Patient` | ICD-10 codes stored as JSONB |

### Soft Deletes

Patient records are **never physically deleted** (`DELETE` sets `is_active = false`). This preserves the audit trail required by HIPAA.

### Security Roadmap

| Month | Feature |
|-------|---------|
| Month 1 (now) | Basic Spring Security, stateless REST, PHI encryption |
| Month 2 | Role-based access control (RBAC), audit logging |
| Month 3 | JWT bearer tokens, refresh tokens, token revocation |

---

## Helm Deployment

```bash
helm upgrade --install hospital-core ./helm \
  --namespace northcare \
  --set image.tag=<git-sha>
```

---

## Project Structure

```
service-hospital-core/
├── src/
│   ├── main/java/com/northcare/hospitalcore/
│   │   ├── config/          # DatabaseConfig, SecurityConfig
│   │   ├── controller/      # PatientController, WardController, HealthController
│   │   ├── dto/             # Request/Response DTOs
│   │   ├── exception/       # GlobalExceptionHandler, ResourceNotFoundException
│   │   ├── mapper/          # MapStruct mappers (PatientMapper, WardMapper)
│   │   ├── model/           # JPA entities (Patient, Ward, LabResult)
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── service/         # Business logic (PatientService, WardService)
│   │   └── util/            # EncryptionUtil (AES-256-GCM)
│   ├── main/resources/
│   │   ├── application.yml
│   │   └── db/migration/    # Flyway SQL scripts
│   └── test/java/           # Integration tests (Testcontainers)
├── helm/values.yaml
├── Dockerfile
├── Jenkinsfile
└── pom.xml
```
