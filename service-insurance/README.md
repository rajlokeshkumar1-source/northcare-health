# NorthCare Insurance Service

Microservice responsible for managing insurance policies and processing claims within the NorthCare Health platform.

## Overview

| Property | Value |
|---|---|
| Port | `8083` |
| Base Path | `/api/v1` |
| Database | PostgreSQL (`northcare_insurance`) |
| Namespace | `insurance` |

## Tech Stack

- Java 21 · Spring Boot 3.2.x · Maven
- Spring Data JPA · PostgreSQL · Flyway
- Lombok · Micrometer + Prometheus · springdoc-openapi

---

## API Endpoints

### Policies — `/api/v1/policies`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/` | Create a new policy |
| `GET` | `/{id}` | Get policy by ID |
| `GET` | `/` | List all policies (paginated) |
| `GET` | `/patient/{patientId}` | All policies for a patient |
| `PUT` | `/{id}` | Update a policy |
| `DELETE` | `/{id}` | Cancel a policy |
| `PUT` | `/{id}/suspend` | Suspend a policy |
| `PUT` | `/{id}/reactivate` | Reactivate a suspended policy |
| `POST` | `/coverage-check` | Check if a patient is covered for an amount |

### Claims — `/api/v1/claims`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/` | Submit a new claim |
| `GET` | `/{id}` | Get claim by ID |
| `PUT` | `/{id}/review` | Move claim to UNDER_REVIEW |
| `PUT` | `/{id}/approve` | Approve with `approvedAmount` |
| `PUT` | `/{id}/deny` | Deny with `denialReason` |
| `PUT` | `/{id}/appeal` | Appeal a denied/partial claim |
| `PUT` | `/{id}/pay` | Mark approved claim as PAID |
| `GET` | `/patient/{patientId}` | Claims for a patient (paginated) |
| `GET` | `/policy/{policyId}` | Claims for a policy (paginated) |

---

## Claim Lifecycle State Machine

```
                    ┌─────────────────────────────────┐
                    │           SUBMITTED              │
                    └──────────────┬──────────────────┘
                                   │ review()
                                   ▼
                    ┌─────────────────────────────────┐
                    │          UNDER_REVIEW            │
                    └────┬────────────────┬───────────┘
                         │ approve()      │ deny()
                         ▼               ▼
               ┌──────────────┐   ┌──────────────┐
               │   APPROVED   │   │    DENIED    │
               │     or       │   └──────┬───────┘
               │  PARTIALLY   │          │ appeal()
               │   APPROVED   │          ▼
               └──────┬───────┘   ┌──────────────┐
                      │ pay()     │   APPEALED   │
                      ▼           └──────┬───────┘
               ┌──────────────┐         │ approve() / deny()
               │     PAID     │◄────────┘
               └──────────────┘
```

---

## Policy Types

| Type | Description |
|------|-------------|
| `BASIC` | Minimal coverage for essential services |
| `EXTENDED` | Broader coverage including specialists |
| `PREMIUM` | Full coverage with dental, vision, etc. |
| `GOVERNMENT` | Government-subsidized plan (OHIP, etc.) |

## Policy Statuses

| Status | Description |
|--------|-------------|
| `ACTIVE` | Currently valid and accepting claims |
| `EXPIRED` | Past coverage end date |
| `SUSPENDED` | Temporarily inactive (non-payment, etc.) |
| `CANCELLED` | Permanently terminated |

---

## Running Locally

```bash
# Start PostgreSQL
docker run -d \
  --name postgres-insurance \
  -e POSTGRES_DB=northcare_insurance \
  -e POSTGRES_USER=northcare \
  -e POSTGRES_PASSWORD=northcare \
  -p 5432:5432 \
  postgres:15

# Run the service
./mvnw spring-boot:run
```

## API Docs

- Swagger UI: http://localhost:8083/swagger-ui.html
- OpenAPI JSON: http://localhost:8083/api-docs
- Actuator: http://localhost:8083/actuator
- Prometheus: http://localhost:8083/actuator/prometheus

---

## Database Migrations

| Migration | Description |
|-----------|-------------|
| `V1__create_policies_table.sql` | `insurance_policies` table with indexes |
| `V2__create_claims_table.sql` | `claims` table with FK, indexes, and GIN indexes for JSONB |

---

## Docker

```bash
docker build -t northcare/insurance-service:latest .
docker run -p 8083:8080 \
  -e DB_USERNAME=northcare \
  -e DB_PASSWORD=northcare \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/northcare_insurance \
  northcare/insurance-service:latest
```

---

## Helm Deployment

```bash
helm upgrade --install insurance ./helm \
  --namespace insurance \
  --create-namespace \
  --set image.tag=<VERSION>
```
