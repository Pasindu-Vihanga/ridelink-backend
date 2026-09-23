# RideLink - Account Service (Member 1)

The **Account Service** is one of the four core microservices for the RideLink ride-sharing backend platform (IT3130 - Application Development).

## 1. Responsibilities
- **Passenger & Driver Registration**: Creates passenger and driver accounts with password encryption (BCrypt) and input validation.
- **Login & Token Issuance**: Authenticates users and issues signed JWT bearer tokens with embedded claims (`userId`, `role`, `status`).
- **Role-Based Access Control**: Supports `PASSENGER`, `DRIVER`, and `ADMIN` roles with method and endpoint-level security.
- **Profile Management**: Viewing and updating user profiles, updating passwords, and self-deactivation.
- **Account Status Management**: Lifecycle management for user accounts (`ACTIVE`, `SUSPENDED`, `DEACTIVATED`).
- **Interservice Verification**: Synchronous validation endpoint (`/api/users/{id}/validate`) for other microservices (Driver Service, Ride Service) to verify account status and identity.

---

## 2. Technology Stack & Persistence Boundary
- **Framework**: Spring Boot 4.0.8 (Java 21)
- **Database**: MongoDB (`mongodb://localhost:27017/ridelink_account_db`)
- **Security**: Spring Security 6+ & JJWT (0.12.6)
- **API Documentation**: OpenAPI 3 / Swagger UI (`springdoc-openapi-starter-webmvc-ui`)
- **Testing**: JUnit 5, Mockito, Spring MockMvc

---

## 3. Configuration & Startup
### Prerequisites
- Java 21 JDK
- MongoDB running on `localhost:27017`

### Running the Service
```bash
# Navigate to account-service directory
cd account-service

# Run with Maven Wrapper
./mvnw spring-boot:run
```
The service will start on port `8081`.

### API Documentation (Swagger UI)
Once started, visit:
- **Swagger UI**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **OpenAPI JSON Spec**: [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)

---

## 4. API Endpoints

### Authentication (`/api/auth`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a new Passenger or Driver |
| `POST` | `/api/auth/login` | Public | Authenticate credentials and receive JWT |

### User & Profile Management (`/api/users`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/users/me` | Authenticated | Get current authenticated user profile |
| `GET` | `/api/users/{id}` | Authenticated | Get user profile by ID |
| `PUT` | `/api/users/profile` | Authenticated | Update user's name & phone number |
| `POST` | `/api/users/change-password` | Authenticated | Change user password |
| `POST` | `/api/users/deactivate` | Authenticated | Deactivate user account |
| `GET` | `/api/users/{id}/validate` | Public/Service | Interservice verification of active user |
| `PATCH` | `/api/users/{id}/status` | Admin | Update account status (`ACTIVE`, `SUSPENDED`, `DEACTIVATED`) |
| `PATCH` | `/api/users/{id}/role` | Admin | Update user role (`PASSENGER`, `DRIVER`, `ADMIN`) |
| `GET` | `/api/users` | Admin | List all users (filter by `role` or `status`) |

---

## 5. Running Tests
```bash
./mvnw clean test
```
The test suite includes 30 unit and slice tests covering normal flows, input validation failures, security restrictions, and boundary/negative scenarios.
