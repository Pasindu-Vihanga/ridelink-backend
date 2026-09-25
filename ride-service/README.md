# RideLink - Ride Management Service (Member 3)

The **Ride Management Service** is the central coordination microservice for the RideLink platform (IT3130 - Application Development).

## 1. Responsibilities
- **Ride Request Creation**: Captures passenger ID, pickup location, dropoff location, vehicle type, and estimated distance.
- **Interservice Passenger Validation**: Synchronously calls Account Service `GET /api/users/{id}/validate` to verify passenger identity, `ACTIVE` status, and `PASSENGER` role.
- **Driver Dispatch & Assignment**: Queries Driver & Vehicle Service `GET /api/drivers/available?serviceArea=...&vehicleType=...` and selects the best driver using a documented rating/experience ranking algorithm.
- **Ride Lifecycle State Machine**: Strictly enforces transitions:
  - `REQUESTED` ➔ `ASSIGNED` ➔ `ACCEPTED` ➔ `IN_PROGRESS` ➔ `COMPLETED`
  - Active states ➔ `CANCELLED` (disallows invalid transitions such as cancelling a completed ride).
- **Interservice Driver Status Updates**: Calls Driver Service `PATCH /api/drivers/{id}/trip-status` to set driver `ON_TRIP` upon assignment, and restores driver to `ONLINE` upon ride completion or cancellation.
- **Ride Queries**: Retrieve ride by ID, passenger history, driver history, and status filtering.

---

## 2. Tech Stack & Persistence Boundary
- **Framework**: Spring Boot 4.0.8 (Java 21)
- **Database**: MongoDB (`ridelink_ride_db`) — independent from Account & Driver services
- **Security**: JWT validation (shared secret across microservices cluster)
- **Interservice Clients**: Spring `RestClient` connecting to:
  - Account Service: `http://localhost:8081`
  - Driver & Vehicle Service: `http://localhost:8082`
- **Documentation**: OpenAPI 3 / Swagger UI

---

## 3. Configuration & Startup
### Prerequisites
- Java 21 JDK, MongoDB on `localhost:27017`
- Account Service running on port `8081`
- Driver Service running on port `8082`

```bash
cd ride-service
./mvnw spring-boot:run
```
Service starts on port `8083`.  
Swagger UI: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)

---

## 4. API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/rides` | Create ride request and auto-assign driver |
| `GET` | `/api/rides/{id}` | Get ride details by ID |
| `GET` | `/api/rides/passenger/{passengerId}` | Get ride history for a passenger |
| `GET` | `/api/rides/driver/{driverId}` | Get ride history for a driver |
| `PATCH` | `/api/rides/{id}/accept` | Driver accepts assigned ride (`ASSIGNED` ➔ `ACCEPTED`) |
| `PATCH` | `/api/rides/{id}/start` | Driver starts trip (`ACCEPTED` ➔ `IN_PROGRESS`) |
| `PATCH` | `/api/rides/{id}/complete` | Driver completes trip (`IN_PROGRESS` ➔ `COMPLETED`) |
| `PATCH` | `/api/rides/{id}/cancel` | Cancel ride with reason |
| `GET` | `/api/rides` | List all rides (with optional status filter) |

---

## 5. Interservice Sequence Diagram

```
Passenger                Ride Service               Account Service          Driver Service
    |                          |                           |                       |
    |---- POST /api/rides ---->|                           |                       |
    |                          |--- GET /api/users/{id} -->|                       |
    |                          |<-- Status: ACTIVE --------|                       |
    |                          |                                                   |
    |                          |--- GET /api/drivers/available ------------------->|
    |                          |<-- List of available drivers ---------------------|
    |                          |                                                   |
    |                          |--- PATCH /api/drivers/{id}/trip-status (true) --->|
    |                          |<-- Status: ON_TRIP -------------------------------|
    |                          |                                                   |
    |<--- 201 Created (Ride) --|                                                   |
```

---

## 6. Running Tests
```bash
./mvnw clean test
```
