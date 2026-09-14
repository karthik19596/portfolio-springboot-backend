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

Private GHCR images also require an `imagePullSecret`.

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
