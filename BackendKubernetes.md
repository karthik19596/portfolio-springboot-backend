# Backend Kubernetes and Platform Work

This document records the Kubernetes commands and platform changes completed
for the Portfolio Task Management application.

## Repositories

- Backend: `portfolio-springboot-backend`
- Frontend: `portfolio-springboot-frontend`
- Kubernetes manifests: `portfolio-springboot-backend/k8s`

## Kubernetes Cluster

The application was deployed to Docker Desktop Kubernetes.

```powershell
kubectl get nodes
kubectl get pods -n portfolio
kubectl get svc -n portfolio
kubectl get pvc -n portfolio
```

The cluster contains:

- Spring Boot backend
- Angular frontend
- MySQL
- MongoDB
- Prometheus
- Grafana
- Zipkin
- NGINX Ingress

## Container Images

The application images were built locally:

```powershell
docker build -t portfolio-backend:observability D:\Projects\Portfolio\portfolio-springboot-backend
docker build -t portfolio-frontend:local D:\Projects\Portfolio\portfolio-springboot-frontend
```

The backend and frontend Dockerfiles support production container builds.
The frontend image uses Node.js 24 and NGINX.

## Kubernetes Secrets

Secrets are created locally and are not committed to Git:

```powershell
kubectl apply -f k8s\namespace.yaml

kubectl create secret generic portfolio-secrets `
  --namespace portfolio `
  --from-literal=mysql-root-password=<MYSQL_ROOT_PASSWORD> `
  --from-literal=mysql-password=<MYSQL_PASSWORD> `
  --from-literal=jwt-secret=<JWT_SECRET> `
  --dry-run=client -o yaml | kubectl apply -f -
```

Verify the Secret keys without displaying values:

```powershell
kubectl describe secret portfolio-secrets -n portfolio
```

`k8s/secret.yaml` is ignored by Git and should not be applied or committed.

## Application Deployment

Apply resources in dependency order:

```powershell
$k8s = "D:\Projects\Portfolio\portfolio-springboot-backend\k8s"

kubectl apply -f "$k8s\mysql.yaml"
kubectl apply -f "$k8s\mongodb.yaml"
kubectl apply -f "$k8s\backend.yaml"
kubectl apply -f "$k8s\frontend.yaml"
kubectl apply -f "$k8s\prometheus.yaml"
kubectl apply -f "$k8s\grafana.yaml"
kubectl apply -f "$k8s\zipkin.yaml"
kubectl apply -f "$k8s\ingress.yaml"
```

Check rollout status:

```powershell
kubectl rollout status deployment/backend -n portfolio
kubectl rollout status deployment/frontend -n portfolio
kubectl rollout status deployment/mysql -n portfolio
kubectl rollout status deployment/mongodb -n portfolio
kubectl rollout status deployment/prometheus -n portfolio
kubectl rollout status deployment/grafana -n portfolio
kubectl rollout status deployment/zipkin -n portfolio
```

The database Deployments use the `Recreate` strategy because their
`ReadWriteOnce` volumes must not be mounted by two database pods at the same
time.

All Deployments have CPU and memory requests and limits.

## Application Access

The frontend can be accessed through the NGINX Ingress controller:

```powershell
kubectl port-forward -n ingress-nginx service/ingress-nginx-controller 8082:80
```

Open:

```text
http://localhost:8082
```

The frontend NGINX configuration forwards `/api` requests to the internal
backend Service.

Other useful port-forwards:

```powershell
kubectl port-forward -n portfolio service/frontend 8081:80
kubectl port-forward -n portfolio service/backend 8080:8080
kubectl port-forward -n portfolio service/prometheus 9090:9090
kubectl port-forward -n portfolio service/grafana 3000:3000
kubectl port-forward -n portfolio service/zipkin 9411:9411
```

## Monitoring

### Prometheus

Prometheus scrapes the backend endpoint:

```text
/actuator/prometheus
```

Check the Prometheus target:

```powershell
kubectl exec -n portfolio deployment/prometheus -- wget -qO- http://localhost:9090/api/v1/targets
```

The backend target should report:

```text
health: up
```

Prometheus rules are stored in `k8s/prometheus.yaml`:

- `BackendDown`: fires after the backend is unavailable for one minute
- `BackendHttp5xxErrors`: fires after HTTP 5xx errors continue for two minutes

Inspect rule status:

```powershell
kubectl exec -n portfolio deployment/prometheus -- wget -qO- http://localhost:9090/api/v1/rules
```

### Grafana

Grafana uses Prometheus as its default data source.
The Grafana data is stored in the `grafana-data-v2` PersistentVolumeClaim.

```powershell
kubectl get pvc grafana-data-v2 -n portfolio
kubectl get pods -l app=grafana -n portfolio
```

The Grafana dashboard contains:

- Backend availability
- Backend request rate
- Backend HTTP error rate
- JVM memory usage

### Zipkin and OpenTelemetry

The backend uses Micrometer Tracing with the OpenTelemetry bridge and exports
traces to Zipkin.

Open Zipkin:

```powershell
kubectl port-forward -n portfolio service/zipkin 9411:9411
```

Browse to:

```text
http://localhost:9411
```

## Ingress

Install the NGINX Ingress controller once per cluster:

```powershell
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=180s
kubectl get ingressclass
```

The application Ingress is defined in:

```text
k8s/ingress.yaml
```

Check it:

```powershell
kubectl get ingress -n portfolio
kubectl describe ingress portfolio -n portfolio
```

## Scaling and Troubleshooting

Scale the backend for an alert test:

```powershell
kubectl scale deployment backend --replicas=0 -n portfolio
kubectl get deployment backend -n portfolio
```

Restore it:

```powershell
kubectl scale deployment backend --replicas=1 -n portfolio
kubectl rollout status deployment/backend -n portfolio
```

Useful troubleshooting commands:

```powershell
kubectl logs deployment/backend -n portfolio
kubectl describe pod -n portfolio <POD_NAME>
kubectl get events -n portfolio --sort-by=.lastTimestamp
kubectl get endpoints backend prometheus grafana -n portfolio
kubectl get hpa -n portfolio
```

Stop workloads while keeping persistent data:

```powershell
kubectl scale deployment --all --replicas=0 -n portfolio
```

Delete the namespace and its local persistent data:

```powershell
kubectl delete namespace portfolio
```

Use the delete command only when a full local reset is intended.

## CI/CD

Both repositories contain a GitHub Actions workflow:

```text
.github/workflows/ci-cd.yml
```

The workflows:

- Run backend tests or frontend production builds
- Build Docker images
- Publish images to GitHub Container Registry on pushes to `main`
- Run validation on pull requests

The backend repository also contains:

```text
.github/workflows/qodana_code_quality.yml
qodana.yaml
```

Qodana performs Java code quality analysis for Java 17.
The `QODANA_TOKEN` is configured through GitHub repository secrets when
required.

## Helm Chart

The Helm chart packages the complete platform:

```text
k8s/helm/portfolio
```

It includes:

- Backend and frontend Deployments
- MySQL and MongoDB
- Prometheus, Grafana, and Zipkin
- PersistentVolumeClaims
- Ingress
- Prometheus alert rules
- Configurable Kubernetes Secrets
- Resource requests and limits

The chart is configured through:

```text
k8s/helm/portfolio/values.yaml
k8s/helm/portfolio/values-local.example.yaml
```

Create the ignored local values file:

```powershell
Copy-Item `
  k8s\helm\portfolio\values-local.example.yaml `
  k8s\helm\portfolio\values-local.yaml
```

Replace the placeholders before installing. The local values file is ignored
by Git because it contains credentials.

Validate the chart:

```powershell
helm lint k8s\helm\portfolio `
  --values k8s\helm\portfolio\values-local.yaml

helm template portfolio-helm k8s\helm\portfolio `
  --namespace portfolio-helm `
  --set namespace=portfolio-helm `
  --set ingress.host=portfolio-helm.local `
  --values k8s\helm\portfolio\values-local.yaml
```

Install or upgrade the Helm release in a separate test namespace:

```powershell
helm upgrade --install portfolio-helm k8s\helm\portfolio `
  --namespace portfolio-helm `
  --create-namespace `
  --set namespace=portfolio-helm `
  --set ingress.host=portfolio-helm.local `
  --values k8s\helm\portfolio\values-local.yaml
```

Check the release:

```powershell
helm status portfolio-helm -n portfolio-helm
kubectl get pods -n portfolio-helm
kubectl get pvc -n portfolio-helm
kubectl get ingress -n portfolio-helm
```

The Helm deployment is tested in `portfolio-helm` so it does not conflict
with the original raw-manifest deployment in `portfolio`.

## Documentation and Security Notes

- `README.md` contains backend setup and API documentation.
- `k8s/README.md` contains the Kubernetes deployment guide.
- `BackendKubernetes.md` contains this command and platform reference.
- Never commit passwords, JWT secrets, or `k8s/secret.yaml`.
- GHCR images should use immutable commit or release tags for cloud deployment
  instead of relying only on `latest`.
