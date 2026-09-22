# Resource Booking System

A RESTful Resource Booking System built with **Spring Boot, Spring Security, JWT, JPA/Hibernate, and MySQL**.

## Tech Stack

- Java 21
- Spring Boot 4.1.1
- Spring Security + JWT
- Spring Data JPA / Hibernate
- MySQL
- Maven
- Swagger / OpenAPI
- JUnit / MockMvc

## Features

- JWT authentication
- `ADMIN` and `USER` role-based access
- Resource CRUD
- Resource pagination and sorting
- Reservation management
- Reservation ownership
- Reservation status management
- Filtering by status and price
- Validation and centralized error handling
- Login rate limiting
- Swagger/OpenAPI documentation

## Setup

### 1. Create Database

```sql
CREATE DATABASE resource_booking_db;
```

### 2. Configure Environment Variables

```env
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
SEED_DATA_ENABLED=
SEED_ADMIN_USERNAME=
SEED_ADMIN_PASSWORD=
SEED_USER1_USERNAME=
SEED_USER1_PASSWORD=
SEED_USER2_USERNAME=
SEED_USER2_PASSWORD=
SWAGGER_ENABLED=
```

### 3. Run Application

```bash
.\mvnw.cmd spring-boot:run
```

Application runs at:
`http://localhost:8080`

## API Endpoints

### Authentication
- `POST /auth/login`

### Resources
- `GET /api/resources`
- `GET /api/resources/{id}`
- `POST /api/resources`
- `PUT /api/resources/{id}`
- `DELETE /api/resources/{id}`

### Reservations
- `POST /api/reservations`
- `GET /api/reservations`
- `GET /api/reservations/{id}`
- `PUT /api/reservations/{id}`
- `PATCH /api/reservations/{id}/status`
- `DELETE /api/reservations/{id}`

## Authorization Matrix

| Operation | USER | ADMIN |
| :--- | :---: | :---: |
| View resources | ✓ | ✓ |
| Manage resources | ✗ | ✓ |
| Create reservation | ✓ | ✓ |
| View reservations | Own | All |
| Update/Delete reservation | ✗ | ✓ |
| Change reservation status | ✗ | ✓ |

## Pagination & Sorting

Resources and reservations support:
- `page`
- `size`
- `sortBy`
- `direction`

Maximum page size: `100`.

Reservations also support:
- `status`
- `minPrice`
- `maxPrice`

## Swagger

Enable Swagger using:
```env
SWAGGER_ENABLED=true
```

Then open:
`http://localhost:8080/swagger-ui.html`

## Testing

Run all tests:
```bash
.\mvnw.cmd clean test
```

## Security

- Passwords are hashed using BCrypt.
- JWT is used for stateless authentication.
- Sensitive credentials are provided through environment variables.
- Users can only access their own reservations.
- Unexpected server errors are logged without exposing internal details.
