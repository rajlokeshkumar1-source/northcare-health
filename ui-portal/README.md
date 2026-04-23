# NorthCare UI Portal

**Tech Stack:** React 18 + TypeScript + Material UI v5 + Vite

## Pages
- 📊 Dashboard — Quick stats & overview
- 👥 Patients — Patient management
- 📹 Telehealth — Consultations
- 🧾 Billing — Invoices & payments
- 🛡️ Insurance — Policies & claims
- 🔔 Notifications — Alerts & templates
- 🩺 Service Health — Live Eureka pod registry

## Local Setup
```bash
npm install
npm run dev
# Open http://localhost:3000
```

## K8s Deployment
```bash
# Build
npm run build

# Docker
docker build -t northcare/ui-portal:1.0.0 .

# Deploy via ArgoCD
kubectl apply -f gitops-argocd/apps/ui-portal.yaml
```

## Access
```bash
kubectl port-forward svc/ui-portal 8080:80 -n ui
# http://localhost:8080
```
