# Portfolio Spring Boot Backend

A production-ready **Task Management API** built with **Spring Boot 3**, designed to showcase real-world backend engineering skills.

Repository: `https://github.com/karthik19596/portfolio-springboot-backend`

Matching frontend: `https://github.com/karthik19596/portfolio-springboot-frontend`

## Tech Stack

- Java 17 + Spring Boot 3.3
- Spring Security + JWT Authentication
- Spring Data JPA + Hibernate (MySQL / H2)
- Spring Data MongoDB (Audit logs)
- OpenAPI 3 / Swagger UI
- JUnit 5 + Mockito
- Docker + Docker Compose
- Maven

## Features

- Secure JWT login & registration with username/email availability checks
- CRUD operations for tasks with pagination and sorting
- Role-based access with `USER`, `ADMIN`, and `SUPER_ADMIN`
- Protected admin management for users, roles, and tasks
- Refresh-token rotation with database persistence and revocation
- Password-reset request and confirmation endpoints
- MongoDB audit log for every task change
- Global exception handling with consistent API responses
- Input validation and Swagger documentation
- Unit tests for services and controllers
- Dockerized for easy deployment
- CORS configured for Angular frontend integration
- Public signup always creates a `USER`; elevated roles require admin authorization

## Prerequisites

- Java 17 or later
- Maven 3.8 or later
- Docker Desktop (required only for MySQL/MongoDB setup)

## Quick Start (H2)

The default profile uses an embedded, in-memory H2 database. Data is reset when the application restarts.

```bash
# From the project directory
mvn clean install
mvn spring-boot:run
```

- API base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console`
- Health check: `http://localhost:8080/actuator/health`

Expected health response:

```json
{"status":"UP"}
```

## MySQL and MongoDB (Local Development)

Start the databases:

```bash
docker compose up -d mysql mongodb
```

Check container status:

```bash
docker compose ps
```

The local `mongo` profile connects to:

- MySQL: `localhost:3306`, database `portfoliodb`
- MongoDB: `localhost:27017`, database `portfolio_audit`

Development MySQL credentials:

- Root: `root` / `rootpass`
- Application user: `portfolio` / `portfoliopass`

Run the application with MySQL and MongoDB:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mongo
```

On Windows PowerShell, quote the profile property:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=mongo"
```

## Run the Complete Stack with Docker

```bash
docker compose up --build
```

This starts the application, MySQL, and MongoDB containers. Inside Docker, the application uses the `mongo` profile and connects to the database services by their Compose names.

Stop the containers:

```bash
docker compose down
```

View MySQL logs:

```bash
docker compose logs mysql
```

## Angular Frontend

A matching Angular 22 frontend is available at:

`https://github.com/karthik19596/portfolio-springboot-frontend`

### Start the Frontend

From the frontend repository:

```bash
npm install
ng serve --open
```

The frontend runs at `http://localhost:4200` and proxies API calls to `http://localhost:8080`.

See the frontend README for more details.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signup` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT |
| POST | `/api/auth/logout` | Revoke the current access and refresh session |
| POST | `/api/auth/refresh` | Rotate a refresh token and receive a new JWT |
| GET | `/api/auth/check-username` | Check if username is available |
| GET | `/api/auth/check-email` | Check if email is available |
| POST | `/api/auth/password-reset/request` | Request a password-reset link by email |
| POST | `/api/auth/password-reset/confirm` | Set a new password with a reset token |
| POST | `/api/tasks` | Create a task |
| GET | `/api/tasks` | List paginated tasks |
| GET | `/api/tasks/{id}` | Get a task by ID |
| PUT | `/api/tasks/{id}` | Update a task |
| DELETE | `/api/tasks/{id}` | Delete a task |
| GET | `/api/admin/audit-logs` | Admin-only audit logs |
| GET | `/api/admin/users` | List users for administrators |
| POST | `/api/admin/users` | Create a user with an authorized role |
| PATCH | `/api/admin/users/{id}/role` | Change a user's role |
| PUT | `/api/admin/users/{id}` | Update a user's username, email, and role |
| DELETE | `/api/admin/users/{id}` | Delete a user and their tasks |
| GET | `/api/admin/tasks` | List all tasks for administrators |
| PUT | `/api/admin/tasks/{id}` | Update any task as an administrator |
| DELETE | `/api/admin/tasks/{id}` | Delete any task as an administrator |

Admin access rules:

- `ADMIN` users can manage `USER` accounts and tasks.
- `SUPER_ADMIN` users can view and manage `USER`, `ADMIN`, and `SUPER_ADMIN` accounts.
- Regular admins cannot see administrator accounts in `GET /api/admin/users`.
- Users cannot delete their own account.
- Only a `SUPER_ADMIN` can create, edit, delete, or assign the `SUPER_ADMIN` role.

## Sample Login

```json
{
  "username": "demo",
  "password": "password123"
}
```

Use the returned JWT in the `Authorization: Bearer <token>` header for protected endpoints.

Login and signup also return a refresh token. Refresh tokens are stored as
SHA-256 hashes in the database, rotated after use, and revoked on logout or
password reset. Access tokens expire according to `jwt.expiration-ms`.

Password-reset tokens are single-use and expire after 30 minutes. The reset
request endpoint intentionally returns the same response whether or not the
email exists. An email provider must be connected to deliver the token to
users; the confirm endpoint accepts the token from that email.

The Angular frontend provides `/forgot-password` and `/reset-password` pages.
The reset page accepts a token from an email link using the `token` query
parameter.

## Database Queries

### H2 Console (default profile)

URL: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:portfoliodb`
- Username: `root`
- Password: `root`

### MySQL Workbench (mongo profile)

- Hostname: `127.0.0.1`
- Port: `3306`
- Username: `root`
- Password: `rootpass`
- Default Schema: `portfoliodb`

Sample queries:

```sql
USE portfoliodb;

SELECT * FROM users;
SELECT * FROM tasks;

SELECT u.username, t.title, t.status, t.priority
FROM users u
JOIN tasks t ON u.id = t.user_id;
```

## Testing

Run the automated tests with:

```bash
mvn test
```

## Kubernetes and Observability

The Docker Desktop Kubernetes deployment, Prometheus metrics, Grafana
dashboards, Zipkin tracing, alert rules, Ingress, persistent volumes, and
deployment commands are documented here:

[`k8s/README.md`](k8s/README.md)

The complete platform command reference and implementation summary is here:

[`BackendKubernetes.md`](BackendKubernetes.md)

The backend also exposes:

- Health: `http://localhost:8080/actuator/health`
- Prometheus metrics: `http://localhost:8080/actuator/prometheus`

## About

Built by **Prem Karthik** — Java Spring Boot Backend Developer with 5+ years of experience delivering enterprise-grade REST APIs, microservices, and secure backend systems.
