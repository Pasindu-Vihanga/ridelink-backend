# RideLink - Fare & Payment Service (Member 4)

The **Fare & Payment Service** is the financial and transaction processing microservice for the RideLink platform (IT3130 - Application Development, Member 4).

## 1. Responsibilities
- **Fare Estimation**: Computes upfront estimates for passengers prior to booking based on vehicle type, distance, and duration.
- **Documented Pricing Rule Engine**:
  $$\text{Fare} = \max\left(\text{MinFare}, \text{round}\left((\text{BaseFare} + (\text{DistanceKm} \times \text{PerKmRate}) + (\text{DurationMinutes} \times \text{PerMinuteRate})) \times \text{SurgeMultiplier}, 2\right)\right)$$
  - **CAR**: Base Rs. 200, Per Km Rs. 120, Per Min Rs. 10, Minimum Rs. 250
  - **VAN**: Base Rs. 350, Per Km Rs. 160, Per Min Rs. 15, Minimum Rs. 400
  - **TUK**: Base Rs. 100, Per Km Rs. 80, Per Min Rs. 8, Minimum Rs. 150
  - **BIKE**: Base Rs. 80, Per Km Rs. 50, Per Min Rs. 5, Minimum Rs. 100
- **Final Fare Invoicing & Payment**: Processes simulated payments (`CARD`, `CASH`, `WALLET`) for rides.
- **Interservice Validation**: Synchronously verifies ride validity, passenger ownership, and ride status via Ride Management Service (`GET /api/rides/{id}`).
- **Idempotency & Duplicate Prevention**: Prevents double charging on already paid rides.
- **Negative Scenario Simulation**: Supports testing payment declines and authorization failures (`simulateFailure: true`).
- **Digital Receipts**: Issues itemized digital receipts with breakdown of base fare, distance charge, time charge, surge, and unique reference (`REC-XXXXX`).

---

## 2. Tech Stack & Persistence Boundary
- **Framework**: Spring Boot 4.0.8 (Java 21)
- **Database**: MongoDB (`ridelink_payment_db`) — independent from Account, Driver, and Ride services
- **Security**: JWT validation (shared secret across cluster)
- **Interservice Client**: Spring `RestClient` connecting to Ride Service (`http://localhost:8083`)
- **Documentation**: OpenAPI 3 / Swagger UI

---

## 3. Configuration & Startup
### Prerequisites
- Java 21 JDK, MongoDB on `localhost:27017`
- Ride Management Service running on port `8083`

```bash
cd fare-service
./mvnw spring-boot:run
```
Service starts on port `8084`.  
Swagger UI: [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)

---

## 4. API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/fares/estimate` | Calculate upfront fare estimate for vehicle types |
| `POST` | `/api/payments/process` | Process simulated payment for a completed ride |
| `GET` | `/api/payments/{id}` | Get payment transaction details by ID |
| `GET` | `/api/payments/ride/{rideId}` | Get payment record for a ride |
| `GET` | `/api/payments/passenger/{passengerId}` | Get payment history for a passenger |
| `GET` | `/api/payments/{id}/receipt` | Get itemized digital receipt by payment ID |
| `GET` | `/api/payments/ride/{rideId}/receipt` | Get itemized digital receipt by ride ID |

---

## 5. Interservice Sequence Diagram

```
Passenger               Payment Service               Ride Service
    |                          |                           |
    |---- POST /api/payments ->|                           |
    |                          |--- GET /api/rides/{id} -->|
    |                          |<-- Status: COMPLETED -----|
    |                          |                           |
    |                          | [Check Duplicate Payment] |
    |                          | [Calculate Final Fare]    |
    |                          | [Record TXN & Receipt]    |
    |                          |                           |
    |<--- 201 Created ---------|                           |
```

---

## 6. Running Tests
```bash
./mvnw clean test
```
