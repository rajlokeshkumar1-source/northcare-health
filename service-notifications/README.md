# NorthCare Notifications Service

Microservice responsible for **alerts, reminders, and emergency broadcasts** across NorthCare Health.

---

## Overview

| Detail        | Value                                    |
|---------------|------------------------------------------|
| Port          | `8084`                                   |
| Base path     | `/api/v1/notifications`                  |
| Database      | PostgreSQL – `northcare_notifications`   |
| Stack         | Java 21, Spring Boot 3.2.x, JPA, Flyway |
| Metrics       | Micrometer → Prometheus `/actuator/prometheus` |
| API Docs      | Swagger UI → `/swagger-ui.html`          |

---

## Channel Simulation

| Channel  | Current behaviour              | Production integration              |
|----------|-------------------------------|--------------------------------------|
| `EMAIL`  | Logs `📧 EMAIL to …`          | Inject `JavaMailSender` / AWS SES / SendGrid |
| `SMS`    | Logs `📱 SMS to … (≤160 chars)` | Inject Twilio `TwilioRestClient` / AWS SNS |
| `IN_APP` | Saves as `DELIVERED` in DB    | Front-end polls `GET /recipient/{id}` |
| `PUSH`   | Falls back to `FAILED`         | Inject FCM / APNs client             |

---

## Template System

Templates stored in `notification_templates` table with **`{{placeholder}}`** syntax.

### Seeded templates

| Code                      | Type                   | Channel  |
|---------------------------|------------------------|----------|
| `APPOINTMENT_REMINDER_24H`| `APPOINTMENT_REMINDER` | EMAIL    |
| `LAB_RESULT_READY`        | `LAB_RESULT_READY`     | EMAIL    |
| `PAYMENT_DUE_REMINDER`    | `BILLING_DUE`          | EMAIL    |
| `EMERGENCY_ALERT`         | `EMERGENCY_ALERT`      | IN_APP   |
| `PRESCRIPTION_READY`      | `PRESCRIPTION_READY`   | SMS      |

### Usage

```http
POST /api/v1/notifications/from-template
{
  "templateCode": "APPOINTMENT_REMINDER_24H",
  "recipientId":  "...",
  "recipientType": "PATIENT",
  "recipientEmail": "jane@example.com",
  "variables": {
    "patientName":      "Jane Doe",
    "appointmentDate":  "2025-01-15",
    "appointmentTime":  "10:30 AM",
    "doctorName":       "Dr. Smith",
    "location":         "Room 201"
  }
}
```

---

## Emergency Broadcast

Sends a **CRITICAL** priority notification to every recipient on every requested channel in a single call.

```http
POST /api/v1/notifications/emergency
{
  "message":      "Code Red – evacuate Building C",
  "recipientIds": ["uuid1", "uuid2", "uuid3"],
  "channels":     ["EMAIL", "IN_APP"]
}
```

Response:
```json
{ "count": 6, "notifications": [ ... ] }
```

---

## Scheduled Jobs

| Job                       | Default interval | Description                                       |
|---------------------------|-----------------|---------------------------------------------------|
| `retryFailedNotifications`| 5 min           | Re-sends `FAILED` notifications (up to 3 retries) |
| `processScheduled`        | 60 sec          | Sends `PENDING` notifications whose `scheduledAt` has passed |

Override via env vars:
```
NORTHCARE_NOTIFICATIONS_RETRY_DELAY_MS=300000
NORTHCARE_NOTIFICATIONS_SCHEDULED_POLL_MS=60000
```

---

## API Endpoints

### Notifications

| Method | Path                                     | Description                  |
|--------|------------------------------------------|------------------------------|
| POST   | `/api/v1/notifications`                  | Send a notification          |
| POST   | `/api/v1/notifications/emergency`        | Emergency broadcast          |
| POST   | `/api/v1/notifications/from-template`    | Send using a template        |
| GET    | `/api/v1/notifications/recipient/{id}`   | Paginated list for recipient |
| PUT    | `/api/v1/notifications/{id}/read`        | Mark notification as read    |
| GET    | `/api/v1/notifications/pending`          | Admin: view pending queue    |

### Templates

| Method | Path                        | Description             |
|--------|-----------------------------|-------------------------|
| GET    | `/api/v1/templates`         | List all templates      |
| POST   | `/api/v1/templates`         | Create template         |
| GET    | `/api/v1/templates/{id}`    | Get by ID               |
| PUT    | `/api/v1/templates/{id}`    | Update template         |
| DELETE | `/api/v1/templates/{id}`    | Soft-delete (isActive=false) |

### Health

| Method | Path      | Description             |
|--------|-----------|-------------------------|
| GET    | `/health` | Liveness check          |
| GET    | `/ready`  | Readiness check (DB)    |

---

## Environment Variables

| Variable            | Default                                              | Description             |
|---------------------|------------------------------------------------------|-------------------------|
| `DATABASE_URL`      | `jdbc:postgresql://localhost:5432/northcare_notifications` | JDBC URL           |
| `DB_USERNAME`       | `northcare`                                          | DB user                 |
| `DB_PASSWORD`       | `changeme`                                           | DB password (use Secret in k8s) |
| `MAIL_HOST`         | `localhost`                                          | SMTP host               |
| `MAIL_PORT`         | `1025`                                               | SMTP port               |

---

## Running Locally

```bash
# Start PostgreSQL
docker run -d --name pg-notifications \
  -e POSTGRES_DB=northcare_notifications \
  -e POSTGRES_USER=northcare \
  -e POSTGRES_PASSWORD=changeme \
  -p 5432:5432 postgres:16-alpine

# Run the service
./mvnw spring-boot:run
```

## Running Tests

```bash
./mvnw test
```

Tests use an H2 in-memory database with Flyway-compatible migrations — no PostgreSQL required.

---

## Adding a Real Email Provider (AWS SES)

1. Add dependency `spring-cloud-aws-starter-ses` to `pom.xml`
2. Inject `SesClient` or configure `JavaMailSender` with SES SMTP credentials
3. Replace the log statement in `EmailChannelService.send()` with the SES API call
4. Set `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_REGION` in your environment

## Adding a Real SMS Provider (Twilio)

1. Add dependency `com.twilio.sdk:twilio` to `pom.xml`
2. Inject credentials via `@Value` (`TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`)
3. Replace the log statement in `SmsChannelService.send()` with `Message.creator(...).create()`
