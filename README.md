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
- Role-based access (USER / ADMIN)
- MongoDB audit log for every task change
- Global exception handling with consistent API responses
- Input validation and Swagger documentation
- Unit tests for services and controllers
- Dockerized for easy deployment
- CORS configured for Angular frontend integration

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
| GET | `/api/auth/check-username` | Check if username is available |
| GET | `/api/auth/check-email` | Check if email is available |
| POST | `/api/tasks` | Create a task |
| GET | `/api/tasks` | List paginated tasks |
| GET | `/api/tasks/{id}` | Get a task by ID |
| PUT | `/api/tasks/{id}` | Update a task |
| DELETE | `/api/tasks/{id}` | Delete a task |
| GET | `/api/admin/audit-logs` | Admin-only audit logs |

## Sample Login

```json
{
  "username": "demo",
  "password": "password123"
}
```

Use the returned JWT in the `Authorization: Bearer <token>` header for protected endpoints.

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

## About

Built by **Prem Karthik** — Java Spring Boot Backend Developer with 5+ years of experience delivering enterprise-grade REST APIs, microservices, and secure backend systems.
