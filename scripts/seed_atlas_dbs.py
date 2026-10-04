from pymongo import MongoClient
import datetime
from bson import ObjectId

URI = "mongodb+srv://admin:1234@cluster0.feaigzn.mongodb.net/?retryWrites=true&w=majority"

def run():
    client = MongoClient(URI, serverSelectionTimeoutMS=10000)
    print("Connected to MongoDB Atlas!")
    
    # List current databases
    dbs = client.list_database_names()
    print("Current Databases in Atlas:", dbs)

    # 1. Check/Get passenger from ridelink_account_db
    account_db = client["ridelink_account_db"]
    passenger = account_db.users.find_one({"role": "PASSENGER"})
    if not passenger:
        passenger_id = ObjectId()
        account_db.users.insert_one({
            "_id": passenger_id,
            "fullName": "Kasun Perera",
            "email": "kasun@example.com",
            "phoneNumber": "+94771234567",
            "role": "PASSENGER",
            "status": "ACTIVE",
            "createdAt": datetime.datetime.now(datetime.timezone.utc),
            "updatedAt": datetime.datetime.now(datetime.timezone.utc),
            "_class": "com.ridelink.account_service.entity.User"
        })
        passenger_str_id = str(passenger_id)
        print("Created sample passenger:", passenger_str_id)
    else:
        passenger_str_id = str(passenger["_id"])
        print("Existing passenger:", passenger_str_id)

    # 2. Check/Get driver from ridelink_driver_db
    driver_db = client["ridelink_driver_db"]
    driver = driver_db.drivers.find_one()
    if not driver:
        driver_id = ObjectId()
        driver_db.drivers.insert_one({
            "_id": driver_id,
            "userId": "sample-driver-user-id",
            "licenseNumber": "B1234567",
            "serviceArea": "Colombo",
            "vehicleType": "CAR",
            "vehicleModel": "Toyota Prius",
            "vehiclePlateNumber": "WP-CAB-1234",
            "vehicleCapacity": 4,
            "availabilityStatus": "ONLINE",
            "currentLatitude": 6.9271,
            "currentLongitude": 79.8612,
            "rating": 4.9,
            "totalTrips": 25,
            "createdAt": datetime.datetime.now(datetime.timezone.utc),
            "_class": "com.ridelink.driver_service.entity.Driver"
        })
        driver_str_id = str(driver_id)
        print("Created sample driver:", driver_str_id)
    else:
        driver_str_id = str(driver["_id"])
        # Ensure availability is ONLINE
        driver_db.drivers.update_one({"_id": driver["_id"]}, {"$set": {"availabilityStatus": "ONLINE"}})
        print("Existing driver set to ONLINE:", driver_str_id)

    # 3. Create Ride in ridelink_ride_db
    ride_db = client["ridelink_ride_db"]
    existing_ride = ride_db.rides.find_one()
    if not existing_ride:
        ride_id = ObjectId()
        ride_doc = {
            "_id": ride_id,
            "passengerId": passenger_str_id,
            "passengerName": "Kasun Perera",
            "passengerPhone": "+94771234567",
            "driverId": driver_str_id,
            "driverUserId": "sample-driver-user-id",
            "driverName": "Nimal Bandara",
            "driverPhone": "+94779876543",
            "vehiclePlate": "WP-CAB-1234",
            "vehicleModel": "Toyota Prius",
            "pickupLocation": {
                "address": "Colombo Fort Railway Station",
                "latitude": 6.9344,
                "longitude": 79.8428
            },
            "dropoffLocation": {
                "address": "Bambalapitiya Junction",
                "latitude": 6.8943,
                "longitude": 79.8558
            },
            "serviceArea": "Colombo",
            "vehicleType": "CAR",
            "status": "COMPLETED",
            "estimatedDistanceKm": 5.8,
            "fareAmount": 750.0,
            "requestedAt": datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(minutes=30),
            "assignedAt": datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(minutes=28),
            "acceptedAt": datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(minutes=25),
            "startedAt": datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(minutes=20),
            "completedAt": datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(minutes=2),
            "createdAt": datetime.datetime.now(datetime.timezone.utc),
            "updatedAt": datetime.datetime.now(datetime.timezone.utc),
            "_class": "com.ridelink.ride_service.entity.Ride"
        }
        ride_db.rides.insert_one(ride_doc)
        ride_str_id = str(ride_id)
        print("Created COMPLETED ride in ridelink_ride_db! ID:", ride_str_id)
    else:
        ride_str_id = str(existing_ride["_id"])
        # Ensure it is COMPLETED for payment test
        ride_db.rides.update_one({"_id": existing_ride["_id"]}, {"$set": {"status": "COMPLETED", "fareAmount": 750.0}})
        print("Existing ride updated to COMPLETED! ID:", ride_str_id)

    # 4. Create Payment & Receipt in ridelink_payment_db
    payment_db = client["ridelink_payment_db"]
    existing_payment = payment_db.payments.find_one()
    if not existing_payment:
        pay_id = ObjectId()
        receipt_id = "REC-" + datetime.datetime.now().strftime("%Y%m%d") + "-0001"
        payment_doc = {
            "_id": pay_id,
            "transactionId": "TXN-" + datetime.datetime.now().strftime("%Y%m%d%H%M%S") + "-999",
            "rideId": ride_str_id,
            "passengerId": passenger_str_id,
            "amount": 750.0,
            "paymentMethod": "CARD",
            "status": "COMPLETED",
            "paidAt": datetime.datetime.now(datetime.timezone.utc),
            "createdAt": datetime.datetime.now(datetime.timezone.utc),
            "_class": "com.ridelink.fare_service.entity.Payment"
        }
        receipt_doc = {
            "_id": ObjectId(),
            "receiptNumber": receipt_id,
            "paymentId": str(pay_id),
            "rideId": ride_str_id,
            "passengerId": passenger_str_id,
            "totalFare": 750.0,
            "baseFare": 200.0,
            "distanceFare": 450.0,
            "timeFare": 100.0,
            "surgeMultiplier": 1.0,
            "paymentMethod": "CARD",
            "issuedAt": datetime.datetime.now(datetime.timezone.utc),
            "_class": "com.ridelink.fare_service.entity.Receipt"
        }
        payment_db.payments.insert_one(payment_doc)
        payment_db.receipts.insert_one(receipt_doc)
        print("Created COMPLETED Payment and Receipt in ridelink_payment_db!")
    else:
        print("Existing payment in ridelink_payment_db:", str(existing_payment["_id"]))

    dbs_after = client.list_database_names()
    print("\nSUCCESS! Databases in MongoDB Atlas now:")
    for d in dbs_after:
        if "ridelink" in d:
            print(" ->", d)

    print("\n--- TEST IDs FOR POSTMAN ---")
    print(f"passengerId: {passenger_str_id}")
    print(f"driverId:    {driver_str_id}")
    print(f"rideId:      {ride_str_id}")

if __name__ == "__main__":
    run()
