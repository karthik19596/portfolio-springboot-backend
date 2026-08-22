# Portfolio Spring Boot Backend

A production-ready **Task Management API** built with **Spring Boot 3**, designed to showcase real-world backend engineering skills.

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

- Secure JWT login & registration
- CRUD operations for tasks with pagination and sorting
- Role-based access (USER / ADMIN)
- MongoDB audit log for every task change
- Global exception handling with consistent API responses
- Input validation and Swagger documentation
- Unit tests for services and controllers
- Dockerized for easy deployment

## Prerequisites

- Java 17 or later
- Maven 3.8 or later
- Docker Desktop with the WSL 2 backend enabled (required for the Docker setup)

### Windows WSL 2 Setup

Open PowerShell as Administrator and run these commands if WSL is not already installed:

```powershell
wsl --install
```

Restart Windows when prompted. After restarting, update WSL and select version 2:

```powershell
wsl --update
wsl --set-default-version 2
wsl --status
```

Install and start Docker Desktop, then verify that Docker is running:

```powershell
docker info
```

In Docker Desktop, ensure **Use the WSL 2 based engine** is enabled under
**Settings > General**. The WSL 1 warning from `wsl --status` is harmless when
the default version is 2.

### Start MySQL and MongoDB on Windows

After restarting Windows, open PowerShell and run:

```powershell
wsl --status
wsl --update
wsl --set-default-version 2
docker info
docker compose -f D:\Projects\portfolio-springboot-backend\docker-compose.yml up -d mysql mongodb
```

Verify that MySQL is listening on port 3306:

```powershell
Test-NetConnection 127.0.0.1 -Port 3306
```

The expected result is:

```text
TcpTestSucceeded : True
```

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

## MySQL and MongoDB

```bash
# Start only the databases
docker compose up -d mysql mongodb

# Check container status
docker compose ps
```

The local `mongo` profile connects to:

- MySQL: `localhost:3306`, database `portfoliodb`
- MongoDB: `localhost:27017`, database `portfolio_audit`

Development MySQL credentials:

- Root: `root` / `rootpass`
- Application user: `portfolio` / `portfoliopass`

Run the application locally with MySQL and MongoDB:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mongo
```

On Windows PowerShell, quote the profile property if necessary:

```powershell
mvn -f D:\Projects\portfolio-springboot-backend\pom.xml spring-boot:run "-Dspring-boot.run.profiles=mongo"
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

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/signup | Register a new user |
| POST | /api/auth/login | Login and receive JWT |
| POST | /api/tasks | Create a task |
| GET | /api/tasks | List paginated tasks |
| GET | /api/tasks/{id} | Get a task by ID |
| PUT | /api/tasks/{id} | Update a task |
| DELETE | /api/tasks/{id} | Delete a task |
| GET | /api/admin/audit-logs | Admin-only audit logs |

## Sample Login

```json
{
  "username": "demo",
  "password": "password123"
}
```

Use the returned JWT in the `Authorization: Bearer <token>` header for protected endpoints.

## Testing

Run the automated tests with:

```bash
mvn test
```

## About

Built by **Prem Karthik** — Java Spring Boot Backend Developer with 5+ years of experience delivering enterprise-grade REST APIs, microservices, and secure backend systems.

Looking for a reliable backend developer? Message me on Fiverr before ordering.
