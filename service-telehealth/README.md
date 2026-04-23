# NorthCare Telehealth Service

Spring Boot 3.2.x microservice for **video consultations** and **remote patient monitoring** in the NorthCare health platform.

---

## Purpose

| Domain | Responsibility |
|--------|---------------|
| Video Consultations | Schedule, start, complete, and cancel telehealth appointments |
| Remote Monitoring   | Ingest device readings (heart rate, glucose, BP, SpO₂, temperature) and flag out-of-range alerts |

---

## API Endpoints

### Consultations – `POST /api/v1/consultations`

| Method | Path | Description |
|--------|------|-------------|
| `POST`  | `/api/v1/consultations`                     | Schedule a consultation |
| `GET`   | `/api/v1/consultations/{id}`                | Get by ID (**includes PHI notes**) |
| `GET`   | `/api/v1/consultations/patient/{patientId}` | Paginated list for patient (notes excluded) |
| `GET`   | `/api/v1/consultations/today`               | All active consultations for today |
| `GET`   | `/api/v1/consultations/status/{status}`     | Paginated list by status |
| `PATCH` | `/api/v1/consultations/{id}/start`          | Transition SCHEDULED → IN_PROGRESS |
| `PATCH` | `/api/v1/consultations/{id}/complete`       | Transition IN_PROGRESS → COMPLETED + notes |
| `PATCH` | `/api/v1/consultations/{id}/cancel`         | Cancel (any non-terminal state) |

### Remote Monitoring – `POST /api/v1/monitoring`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/monitoring/readings`                     | Ingest a device reading |
| `GET`  | `/api/v1/monitoring/patients/{patientId}/readings` | Paginated history (newest first) |
| `GET`  | `/api/v1/monitoring/patients/{patientId}/alerts`   | All alert readings for a patient |

### System

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health`                  | Liveness probe |
| `GET` | `/ready`                   | Readiness probe |
| `GET` | `/actuator/prometheus`     | Prometheus metrics scrape |
| `GET` | `/swagger-ui.html`         | Swagger UI |
| `GET` | `/api-docs`                | OpenAPI JSON spec |

---

## Local Setup

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL 15+

### Database

```sql
CREATE DATABASE northcare_telehealth;
```

### Run

```bash
cd service-telehealth

# Set credentials (or export them)
export DB_USERNAME=postgres
export DB_PASSWORD=your_password

mvn spring-boot:run
```

Service starts on **http://localhost:8081**

Swagger UI: http://localhost:8081/swagger-ui.html

### Run Tests

```bash
mvn test
```

Tests use `@WebMvcTest` with Mockito – no database required.

---

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8081` | Local HTTP port |
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `spring.datasource.url` | `localhost:5432/northcare_telehealth` | JDBC URL |

---

## Docker

```bash
# Build image
docker build -t northcare/telehealth:latest .

# Run container
docker run -p 8081:8081 \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=secret \
  northcare/telehealth:latest
```

---

## Helm (Kubernetes)

```bash
helm upgrade --install telehealth-service ./helm \
  --namespace telehealth \
  --create-namespace \
  --set image.tag=<version>
```

The chart configures:
- 2 replicas with HPA (max 6)
- Liveness + readiness probes on `/health` and `/ready`
- Prometheus scraping via pod annotations
- Non-root container security context

---

## HIPAA Notes – Doctor Notes Field (PHI)

> ⚠️ The `notes` field on the `Consultation` entity contains **Protected Health Information (PHI)** as defined by HIPAA.

| Control | Implementation |
|---------|---------------|
| **API masking** | `notes` is **excluded** (`null`, omitted via `@JsonInclude(NON_NULL)`) from all **list/page** responses |
| **Single record** | `notes` is included only in `GET /api/v1/consultations/{id}` – limit access to authorised clinicians |
| **Database** | `notes` column stored in `consultations.notes TEXT`; encrypt at rest via PostgreSQL TDE or cloud KMS |
| **Logging** | Doctor notes are **never** logged; `log.info/warn` statements use only non-PHI identifiers |
| **Audit** | Add an audit-log interceptor before production deployment to record every read of a PHI field |

---

## Service State Machine – Consultation

```
SCHEDULED ──► IN_PROGRESS ──► COMPLETED
    │                │
    └──► CANCELLED   └──► CANCELLED
         NO_SHOW (set externally by scheduler job)
```
