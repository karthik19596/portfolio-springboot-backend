# Local Kubernetes deployment

This setup is intended for Docker Desktop Kubernetes on Windows.

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
docker build -t portfolio-backend:local D:\Projects\Portfolio\portfolio-springboot-backend
docker build -t portfolio-frontend:local D:\Projects\Portfolio\portfolio-springboot-frontend
```

## 3. Deploy the stack

```powershell
$k8s = "D:\Projects\Portfolio\portfolio-springboot-backend\k8s"
kubectl apply -f "$k8s\namespace.yaml"
kubectl apply -f "$k8s\secret.yaml"
kubectl apply -f "$k8s\mysql.yaml"
kubectl apply -f "$k8s\mongodb.yaml"
kubectl apply -f "$k8s\backend.yaml"
kubectl apply -f "$k8s\frontend.yaml"
kubectl get pods -n portfolio -w
```

Press `Ctrl+C` after the pods are ready. Use port-forwarding to access the frontend:

```powershell
kubectl port-forward -n portfolio service/frontend 8081:80
```

Keep that terminal open and browse to:

`http://localhost:8081`

The Service is also exposed as NodePort `30080`, so `http://localhost:30080`
may work depending on Docker Desktop networking.

Check the backend:

```powershell
kubectl get svc -n portfolio
kubectl logs deployment/backend -n portfolio
```

## 4. Stop or remove it

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

Browse to `http://localhost:3000` and sign in with:

- Username: `admin`
- Password: `admin`

Grafana already has Prometheus configured as its default data source. Change
the default password before using this outside a local learning cluster.

Grafana data is stored in the `grafana-data-v2` PersistentVolumeClaim so
dashboards and alert settings survive pod restarts.

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
