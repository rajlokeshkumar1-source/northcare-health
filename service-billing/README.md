# NorthCare Billing Service

Manages **invoicing**, **payment processing**, and **daily reconciliation** for the NorthCare Health platform.

---

## Purpose

| Capability | Detail |
|---|---|
| Invoice lifecycle | DRAFT → ISSUED → PAID / PARTIALLY_PAID / OVERDUE / CANCELLED / WRITTEN_OFF |
| Payment methods | Credit Card, Bank Transfer, Insurance, Cash, Cheque |
| Tax | 13 % Ontario HST applied automatically to every invoice |
| Reconciliation | Daily report grouping completed payments by method |
| Invoice numbering | Auto-sequenced: `INV-<YEAR>-<SEQ5>` (e.g. `INV-2025-00001`) |

---

## Tech Stack

- **Java 21** + **Spring Boot 3.2.x**
- **Spring Data JPA** / Hibernate 6
- **PostgreSQL 15** (production), **H2** (tests)
- **Flyway** — versioned schema migrations
- **Micrometer + Prometheus** — metrics at `/actuator/prometheus`
- **springdoc-openapi** — Swagger UI at `/swagger-ui.html`
- **Lombok** — boilerplate reduction

---

## Running Locally

```bash
# Start PostgreSQL
docker run -d \
  -e POSTGRES_DB=northcare_billing \
  -e POSTGRES_USER=northcare \
  -e POSTGRES_PASSWORD=northcare \
  -p 5432:5432 \
  postgres:15

# Run the service
./mvnw spring-boot:run
```

Service starts on **port 8082**.

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET`  | `/api/v1/invoices` | List all invoices (paginated) |
| `POST` | `/api/v1/invoices` | Create a new invoice (DRAFT) |
| `GET`  | `/api/v1/invoices/{id}` | Get invoice by ID |
| `PUT`  | `/api/v1/invoices/{id}/issue` | Issue a draft invoice |
| `POST` | `/api/v1/invoices/{id}/payments` | Record a payment |
| `GET`  | `/api/v1/invoices/overdue` | List overdue invoices |
| `GET`  | `/api/v1/invoices/reconciliation?date=2025-01-15` | Daily reconciliation report |
| `GET`  | `/health` | Liveness probe |
| `GET`  | `/ready` | Readiness probe |

---

## Invoice Lifecycle

```
           issue()           processPayment() — full
DRAFT ──────────► ISSUED ──────────────────────────► PAID
                    │
                    │  processPayment() — partial
                    └──────────────────────────────► PARTIALLY_PAID ──► PAID
                    │
                    │  dueDate passed, not paid
                    └──────────────────────────────► OVERDUE
                    │
                    └──────────────────────────────► CANCELLED
                    └──────────────────────────────► WRITTEN_OFF
```

---

## Create Invoice — Example Request

```json
POST /api/v1/invoices
{
  "patientId": "550e8400-e29b-41d4-a716-446655440000",
  "patientName": "Jane Smith",
  "serviceDate": "2025-01-15",
  "dueDate": "2025-02-14",
  "lineItems": [
    {
      "serviceCode": "99213",
      "description": "Office Visit – Level 3",
      "quantity": 1,
      "unitPrice": 150.00
    }
  ],
  "notes": "Follow-up consultation"
}
```

---

## Reconciliation Report — Example Response

```json
GET /api/v1/invoices/reconciliation?date=2025-01-15
{
  "date": "2025-01-15",
  "totalTransactions": 5,
  "totalAmount": 1305.75,
  "amountByMethod": {
    "CREDIT_CARD": 855.75,
    "INSURANCE":   450.00
  },
  "countByMethod": {
    "CREDIT_CARD": 3,
    "INSURANCE":   2
  }
}
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `northcare` | PostgreSQL username |
| `DB_PASSWORD` | `northcare` | PostgreSQL password |
| `DB_URL` | `localhost:5432/northcare_billing` | Override in `application.yml` |

---

## Ports

| Purpose | Port |
|---|---|
| Application | `8082` |
| Prometheus metrics | `/actuator/prometheus` |
| Swagger UI | `/swagger-ui.html` |

---

## Running Tests

```bash
./mvnw test
```

Tests use an **in-memory H2** database in PostgreSQL-compatibility mode. All migrations run automatically.

---

## Kubernetes / Helm

```bash
helm upgrade --install billing-service ./helm \
  --namespace billing \
  --create-namespace \
  --set image.tag=<BUILD_NUMBER>
```
