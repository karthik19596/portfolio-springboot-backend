# Portfolio Helm chart

This chart packages the backend, frontend, MySQL, MongoDB, Prometheus, Grafana,
Zipkin, persistent volumes, Ingress, and Kubernetes Secret.

The application deployed by this chart supports `USER`, `ADMIN`, and
`SUPER_ADMIN` roles, refresh-token rotation, password reset, and the
role-protected admin dashboard. New public registrations always start as
`USER`; bootstrap the first `SUPER_ADMIN` through MySQL after installation.

## Prerequisites

- Helm 3
- A Kubernetes cluster
- Local images loaded into the cluster, or images available in a registry
- An NGINX Ingress controller if `ingress.enabled` is true

Check Helm:

```powershell
helm version
```

## Local installation

Copy the example values and replace every placeholder:

```powershell
Copy-Item values-local.example.yaml values-local.yaml
```

Install or upgrade:

```powershell
helm upgrade --install portfolio . `
  --namespace portfolio `
  --create-namespace `
  --values values-local.yaml
```

Check the release:

```powershell
helm status portfolio -n portfolio
kubectl get pods -n portfolio
```

The chart creates the `portfolio-secrets` Secret from the values file. Do not
commit `values-local.yaml`.

After bootstrapping a `SUPER_ADMIN`, regular admins can manage normal users and
tasks. Only `SUPER_ADMIN` users can see or manage administrator accounts.

## Use an existing Secret

For an existing Secret, set:

```yaml
secrets:
  create: false

existingSecret:
  enabled: true
  name: portfolio-secrets
```

The existing Secret must contain:

- `mysql-root-password`
- `mysql-password`
- `jwt-secret`
- `grafana-admin-password`

## Registry images

For GHCR images, override the image values:

```powershell
helm upgrade --install portfolio . `
  --namespace portfolio `
  --create-namespace `
  --set secrets.create=false `
  --set existingSecret.enabled=true `
  --set existingSecret.name=portfolio-secrets `
  --set images.backend.repository=ghcr.io/karthik19596/portfolio-springboot-backend `
  --set images.backend.tag=<BACKEND_TAG> `
  --set images.frontend.repository=ghcr.io/karthik19596/portfolio-springboot-frontend `
  --set images.frontend.tag=<FRONTEND_TAG>
```

Private GHCR images also require an `imagePullSecret`. Create one and reference
it through `imagePullSecrets`:

```powershell
kubectl create secret docker-registry ghcr-pull-secret `
  --namespace portfolio `
  --docker-server=ghcr.io `
  --docker-username=karthik19596 `
  --docker-password=<GITHUB_PACKAGES_TOKEN>
```

```yaml
imagePullSecrets:
  - name: ghcr-pull-secret
```

## GKE Autopilot installation

Copy the cloud example values and replace every placeholder, including the
public host name:

```powershell
Copy-Item values-gke.example.yaml values-gke.yaml
```

The cloud values differ from the local ones in five ways:

- `images.*` point at the GHCR packages and use `pullPolicy: Always`.
- `imagePullSecrets` stays empty while both GHCR packages are public, and takes
  the pull Secret name if either becomes private.
- `resources.*` match the Autopilot minimum of 250m CPU and 512Mi memory per
  Pod, with limits equal to requests, because Autopilot rewrites anything lower.
- `storage.className` is `standard-rwo`, the Autopilot balanced Persistent Disk class.
- The Ingress uses the `gce` class with a Google-managed certificate instead of
  NGINX, and the frontend Service carries the `cloud.google.com/neg` annotation
  so the load balancer routes to Pods directly.

Reserve a global static IP and point the domain at it before installing:

```powershell
gcloud compute addresses create portfolio-ip --global
gcloud compute addresses describe portfolio-ip --global --format="value(address)"
```

Create an `A` record for the host in `ingress.host` that points to that address.
The managed certificate stays in `Provisioning` until DNS resolves, which can
take up to 60 minutes.

Install the chart:

```powershell
helm upgrade --install portfolio . `
  --namespace portfolio `
  --create-namespace `
  --values values-gke.yaml
```

Watch the certificate and load balancer become ready:

```powershell
kubectl get managedcertificate -n portfolio -w
kubectl get ingress portfolio -n portfolio
```

Prometheus, Grafana, and Zipkin stay internal. Reach them with
`kubectl port-forward` rather than exposing them through the Ingress.

`values-gke.yaml` is ignored by Git because it contains credentials.

## Render and validate

Render manifests without installing:

```powershell
helm template portfolio . --values values-local.yaml
```

Validate the rendered resources with:

```powershell
helm lint . --values values-local.yaml
```

## Upgrade and remove

```powershell
helm upgrade portfolio . -n portfolio --values values-local.yaml
helm history portfolio -n portfolio
helm uninstall portfolio -n portfolio
```

Uninstalling does not necessarily remove PVCs. Delete PVCs only when a full
local data reset is intended.
