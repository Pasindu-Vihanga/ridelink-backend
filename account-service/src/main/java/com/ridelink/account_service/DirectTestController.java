package com.ridelink.account_service;

import com.ridelink.account_service.entity.AccountStatus;
import com.ridelink.account_service.entity.Role;
import com.ridelink.account_service.entity.User;
import com.ridelink.account_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class DirectTestController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping("/direct-test/save-user")
    public String directSaveUser() {
        try {
            // Clear any existing test data
            userRepository.deleteAll();
            
            // Create test user
            User testUser = User.builder()
                    .fullName("Direct Test User " + System.currentTimeMillis())
                    .email("directtest@ridelink.com")
                    .password("testpassword")
                    .phoneNumber("9876543210")
                    .role(Role.PASSENGER)
                    .status(AccountStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            System.out.println("🚀 DIRECT TEST: About to save user to MongoDB");
            
            // Method 1: Repository save
            User savedUser = userRepository.save(testUser);
            System.out.println("✅ REPOSITORY SAVE: User ID = " + savedUser.getId());
            
            // Method 2: MongoTemplate save (alternative)
            mongoTemplate.save(testUser, "users");
            System.out.println("✅ TEMPLATE SAVE: Saved to collection 'users'");
            
            // Verify data exists
            long count = userRepository.count();
            System.out.println("📊 VERIFICATION: Total users = " + count);
            
            // Check database name
            String dbName = mongoTemplate.getDb().getName();
            System.out.println("🏷️  DATABASE: " + dbName);
            
            return String.format("""
                ✅ DIRECT TEST SUCCESSFUL!
                
                🆔 Saved User ID: %s
                👤 Full Name: %s
                📧 Email: %s
                📊 Total Users: %d
                🏷️  Database: %s
                📝 Collection: users
                
                🔍 Check MongoDB Atlas:
                Database → Browse Collections → %s → users
                Should show %d document(s)
                """, 
                savedUser.getId(), savedUser.getFullName(), savedUser.getEmail(), 
                count, dbName, dbName, count);
                
        } catch (Exception e) {
            System.err.println("❌ DIRECT TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            return "❌ DIRECT TEST FAILED: " + e.getMessage();
        }
    }
    
    @GetMapping("/direct-test/check-data")
    public String checkData() {
        try {
            long count = userRepository.count();
            String dbName = mongoTemplate.getDb().getName();
            
            if (count > 0) {
                return String.format("""
                    ✅ DATA EXISTS IN MONGODB!
                    
                    📊 Total Users: %d
                    🏷️  Database: %s
                    📝 Collection: users
                    
                    ✨ Data should be visible in Atlas now!
                    """, count, dbName);
            } else {
                return String.format("""
                    ❌ NO DATA FOUND
                    
                    🏷️  Database: %s
                    📝 Collection: users
                    📊 Count: 0
                    
                    Try POST /direct-test/save-user first
                    """, dbName);
            }
            
        } catch (Exception e) {
            return "❌ CHECK FAILED: " + e.getMessage();
        }
    }
}