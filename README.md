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

## Quick Start

```bash
# Clone / open the project
mvn clean install
mvn spring-boot:run
```

- API base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console`

The default profile uses an embedded H2 database and disables MongoDB, so the app runs instantly without any external services.

## Full-Stack with Docker

```bash
docker-compose up --build
```

This starts the app along with MySQL and MongoDB containers using the `mongo` profile.

To run locally with MongoDB instead of H2:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mongo
```

Make sure MySQL and MongoDB are running first (or use Docker Compose).

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

## About

Built by **Prem Karthik** — Java Spring Boot Backend Developer with 5+ years of experience delivering enterprise-grade REST APIs, microservices, and secure backend systems.

Looking for a reliable backend developer? Message me on Fiverr before ordering.
