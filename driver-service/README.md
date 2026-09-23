# RideLink - Driver & Vehicle Service (Member 2)

The **Driver & Vehicle Service** is the second core microservice for the RideLink platform (IT3130 - Application Development).

## 1. Responsibilities
- **Driver Operational Profile**: License details, experience years, ratings, total trips.
- **Vehicle Registration & Updates**: Make, model, year, license plate, vehicle type (CAR, VAN, BIKE, TUK), passenger capacity.
- **Availability Status Management**: `ONLINE`, `OFFLINE`, `ON_TRIP`.
- **Simulated Location Management**: Latitude, longitude, service area (e.g. Colombo, Kandy, Galle).
- **Eligible Driver Retrieval**: Filtered search by `serviceArea` and `vehicleType` for Ride Management Service.
- **Interservice Communication**: Synchronous REST call to Account Service `/api/users/{id}/validate` to verify driver identity and account status.

---

## 2. Tech Stack & Persistence Boundary
- **Framework**: Spring Boot 4.0.8 (Java 21)
- **Database**: MongoDB (`ridelink_driver_db`) — independent from Account Service
- **Security**: JWT validation (shared secret with Account Service cluster)
- **Interservice**: Spring `RestClient` → Account Service (`http://localhost:8081`)
- **Documentation**: OpenAPI 3 / Swagger UI

---

## 3. Configuration & Startup
### Prerequisites
- Java 21 JDK, MongoDB on `localhost:27017`
- Account Service running on port `8081`

```bash
cd driver-service
./mvnw spring-boot:run
```
Service starts on port `8082`.  
Swagger UI: [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)

---

## 4. API Endpoints

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/drivers` | Authenticated (DRIVER) | Register driver profile and vehicle |
| `GET` | `/api/drivers/{id}` | Public | Get driver by driver ID |
| `GET` | `/api/drivers/user/{userId}` | Public | Get driver by Account Service user ID |
| `PUT` | `/api/drivers/{id}/vehicle` | Authenticated | Update vehicle details |
| `PATCH` | `/api/drivers/{id}/availability` | Authenticated | Update availability status |
| `PATCH` | `/api/drivers/{id}/location` | Authenticated | Update location and service area |
| `PATCH` | `/api/drivers/{id}/trip-status` | Service | Mark driver ON_TRIP or ONLINE after trip |
| `GET` | `/api/drivers/available` | Public/Service | Get available ONLINE drivers (filtered) |

---

## 5. Interservice Communication Flow

```
Driver Service (POST /api/drivers)
        ↓
AccountServiceClient (RestClient)
        ↓
Account Service (GET /api/users/{userId}/validate)
        ↓
Returns: userId, role (DRIVER), status (ACTIVE)
        ↓
Driver profile is created if ACTIVE DRIVER ✓
```

---

## 6. Running Tests
```bash
./mvnw clean test
```
