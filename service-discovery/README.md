# NorthCare Service Discovery (Eureka)

## What is this?
Eureka Server provides a service registry where every microservice pod registers on startup.
Unlike Kubernetes DNS (which resolves to Service IP), **Eureka shows individual pods** —
so if hospital-core has 3 replicas, you'll see 3 instances in the dashboard.

## Eureka Dashboard
After deploying to EKS, access the dashboard via port-forward:
```bash
kubectl port-forward svc/service-discovery 8761:8761 -n discovery
# Open: http://localhost:8761
# Login: eureka-admin / (from K8s secret)
```

## Registered Services
All 5 NorthCare services register automatically:
| Service | App Name | Namespace |
|---------|----------|-----------|
| hospital-core | HOSPITAL-CORE | hospital |
| telehealth | TELEHEALTH | telehealth |
| billing | BILLING | billing |
| insurance | INSURANCE | insurance |
| notifications | NOTIFICATIONS | notifs |

## How Pod Count Visibility Works
- Each pod = 1 Eureka instance registration
- Scale hospital-core: `kubectl scale deployment hospital-core --replicas=3 -n hospital`
- Refresh Eureka dashboard → see 3 instances of HOSPITAL-CORE
- Scale down to 1 → instances deregister within ~30s

## Local Development
```bash
mvn spring-boot:run
# Dashboard: http://localhost:8761
# Login: eureka-admin / northcare-eureka-2025
```

## Environment Variables
| Variable | Default | Description |
|----------|---------|-------------|
| EUREKA_ADMIN_USERNAME | eureka-admin | Dashboard login username |
| EUREKA_ADMIN_PASSWORD | northcare-eureka-2025 | Dashboard login password |
| EUREKA_HOSTNAME | localhost | Hostname for self-registration |
| EUREKA_SELF_PRESERVATION | false | Enable self-preservation mode |
