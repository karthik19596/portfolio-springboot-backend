# Local Kubernetes deployment

This setup is intended for Docker Desktop Kubernetes on Windows.

The deployed application includes JWT refresh-token rotation, password reset,
role-based administration, and the Angular admin dashboard. Kubernetes
controls deployment infrastructure; application roles are managed through the
application after the first `SUPER_ADMIN` is bootstrapped.

## 1. Enable the local cluster

1. Start Docker Desktop.
2. Open **Settings > Kubernetes**.
3. Enable Kubernetes and wait until the cluster shows `Running`.
4. Verify it:

```powershell
kubectl get nodes
```

## 2. Build the application images

Docker Desktop must be running:

```powershell
docker build -t portfolio-backend:observability D:\Projects\Portfolio\portfolio-springboot-backend
docker build -t portfolio-frontend:local D:\Projects\Portfolio\portfolio-springboot-frontend
```

## 3. Create local secrets

Do not apply `secret.yaml` to the cluster or commit it to Git. Create the
Secret locally:

```powershell
$k8s = "D:\Projects\Portfolio\portfolio-springboot-backend\k8s"
kubectl apply -f "$k8s\namespace.yaml"
kubectl create secret generic portfolio-secrets `
  --namespace portfolio `
  --from-literal=mysql-root-password=rootpass `
  --from-literal=mysql-password=portfoliopass `
  --from-literal=jwt-secret='replace-with-a-long-random-secret' `
  --dry-run=client -o yaml | kubectl apply -f -
```

## 4. Deploy the stack

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
kubectl get pods -n portfolio -w
```

The MySQL and MongoDB Deployments use the `Recreate` strategy because their
data volumes support one writer at a time. All Deployments include CPU and
memory requests and limits.

Press `Ctrl+C` after the pods are ready. Use the Ingress controller to access
the frontend:

```powershell
kubectl port-forward -n ingress-nginx service/ingress-nginx-controller 8082:80
```

Keep that terminal open and browse to:

`http://localhost:8082`

The frontend Nginx configuration forwards `/api` requests to the internal
backend Service.

After the first deployment, create a user through the frontend and promote the
first administrator using MySQL:

```powershell
kubectl exec -it deployment/mysql -n portfolio -- `
  mysql -u root -p"$env:MYSQL_ROOT_PASSWORD" -e `
  "USE portfoliodb; UPDATE users SET role='SUPER_ADMIN' WHERE email='your-email@example.com';"
```

Log out and log in again after changing a role so the new role is included in
the JWT.

Check the backend:

```powershell
kubectl get svc -n portfolio
kubectl logs deployment/backend -n portfolio
```

## 5. Stop or remove it

Stop the workloads while keeping database data:

```powershell
kubectl scale deployment --all --replicas=0 -n portfolio
```

Remove the application and its persistent data:

```powershell
kubectl delete namespace portfolio
```

The final command deletes the local database volumes too. Use it only when you want a clean reset.

## Observability

Prometheus scrapes the backend at `/actuator/prometheus`.

Open Prometheus:

```powershell
kubectl port-forward -n portfolio service/prometheus 9090:9090
```

Browse to `http://localhost:9090/targets`. The `portfolio-backend` target
should show `UP`.

Open Grafana in another terminal:

```powershell
kubectl port-forward -n portfolio service/grafana 3000:3000
```

Browse to `http://localhost:3000` and sign in with the Grafana admin password
you configured. If port `3000` is already in use, use `3001:3000` in the
port-forward command and browse to `http://localhost:3001`.

Grafana already has Prometheus configured as its default data source. Grafana
data is stored in the `grafana-data-v2` PersistentVolumeClaim, so dashboards
and alert settings survive pod restarts.

Open Zipkin to inspect distributed traces:

```powershell
kubectl port-forward -n portfolio service/zipkin 9411:9411
```

Browse to `http://localhost:9411`, click **Run Query**, and call an API
endpoint from the frontend. Spring Boot automatically creates OpenTelemetry
spans for the request and exports them to Zipkin.

## Ingress

The application Ingress routes `/` to the Angular frontend. The frontend Nginx
configuration routes `/api` to the internal backend Service.

On Docker Desktop, access it locally with:

```powershell
kubectl port-forward -n ingress-nginx service/ingress-nginx-controller 8082:80
```

Browse to `http://localhost:8082`.

Prometheus alert rules are stored in `prometheus.yaml` and include:

- `BackendDown`: fires after the backend target is unavailable for one minute.
- `BackendHttp5xxErrors`: fires after server errors continue for two minutes.

To inspect their state:

```powershell
kubectl exec -n portfolio deployment/prometheus -- wget -qO- http://localhost:9090/api/v1/rules
```

## Ingress controller installation

Install the NGINX Ingress controller once per local cluster:

```powershell
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=180s
kubectl get ingressclass
```

## CI/CD

The backend and frontend repositories each contain a GitHub Actions workflow:

```text
.github/workflows/ci-cd.yml
```

Pull requests run tests and builds. Pushes to `main` also publish images to
GitHub Container Registry. The local Kubernetes cluster uses locally built
images; cloud Kubernetes deployments should use the published GHCR image tags.

## Cloud environment setup (GKE Autopilot)

Install the Google Cloud CLI and Helm once:

```powershell
winget install --id Google.CloudSDK --silent
winget install --id Helm.Helm --silent
```

Open a new terminal afterwards so the updated `PATH` is picked up.

Authenticate, create the project, and link a billing account:

```powershell
gcloud auth login
gcloud projects create portfolio-springboot --name="Portfolio Spring Boot"
gcloud config set project portfolio-springboot
gcloud billing accounts list
gcloud billing projects link portfolio-springboot --billing-account=<BILLING_ACCOUNT_ID>
```

`gcloud billing projects describe portfolio-springboot` must report
`billingEnabled: true` before anything else works. A newly created free-trial
billing account can report `open: false` for a while; API enablement fails with
`UREQ_PROJECT_BILLING_NOT_OPEN` until Google finishes verifying the payment
method.

Enable the APIs and create the cluster:

```powershell
gcloud services enable container.googleapis.com --project portfolio-springboot
gcloud container clusters create-auto portfolio --region us-central1
```

Point kubectl and Helm at the cluster. Both read the same kubeconfig, so one
command covers them:

```powershell
gcloud container clusters get-credentials portfolio --region us-central1
kubectl get nodes
kubectl config get-contexts
```

Autopilot reports no nodes until the first workload is scheduled, so an empty
`kubectl get nodes` right after creation is normal.

## Helm

The Helm chart is located at:

```text
k8s/helm/portfolio
```

It packages the backend, frontend, databases, Prometheus, Grafana, Zipkin,
Ingress, persistent volumes, and Secrets.

Check that Helm is installed:

```powershell
helm version
```

Create a local values file and replace every placeholder:

```powershell
Copy-Item k8s\helm\portfolio\values-local.example.yaml k8s\helm\portfolio\values-local.yaml
```

Install or upgrade the chart:

```powershell
helm upgrade --install portfolio k8s\helm\portfolio `
  --namespace portfolio `
  --create-namespace `
  --values k8s\helm\portfolio\values-local.yaml
```

Render or lint the chart before installing:

```powershell
helm template portfolio k8s\helm\portfolio `
  --values k8s\helm\portfolio\values-local.yaml

helm lint k8s\helm\portfolio `
  --values k8s\helm\portfolio\values-local.yaml
```

`values-local.yaml` is ignored by Git because it contains local credentials.

For a cloud installation on GKE Autopilot, use `values-gke.example.yaml` as the
starting point instead. The chart README at `k8s/helm/portfolio/README.md`
covers the GHCR pull Secret, the static IP, the DNS record, and the
Google-managed certificate.
