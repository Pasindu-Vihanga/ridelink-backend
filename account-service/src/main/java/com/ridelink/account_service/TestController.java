package com.ridelink.account_service;

import com.ridelink.account_service.entity.AccountStatus;
import com.ridelink.account_service.entity.Role;
import com.ridelink.account_service.entity.User;
import com.ridelink.account_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class TestController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @GetMapping("/test/db")
    public String testDatabase() {
        try {
            long count = userRepository.count();
            return "✅ Database connection successful! User count: " + count;
        } catch (Exception e) {
            return "❌ Database connection failed: " + e.getMessage();
        }
    }

    @GetMapping("/test/connection-details")
    public String getConnectionDetails() {
        try {
            String dbName = mongoTemplate.getDb().getName();
            long userCount = userRepository.count();
            
            // Extract cluster info from URI
            String clusterInfo = mongoUri.contains("@") ? 
                mongoUri.substring(mongoUri.indexOf("@") + 1, mongoUri.indexOf("/", mongoUri.indexOf("@"))) : 
                "Unknown";
                
            return String.format("""
                ✅ MongoDB Connection Details:
                🏷️  Database Name: %s
                🌐 Cluster: %s
                📊 User Count: %d
                🔗 URI: %s...
                
                ⚠️  If database not visible in Atlas:
                1. Check if you're in correct Atlas project
                2. Verify cluster name matches
                3. Refresh Atlas browser tab
                4. Check if data actually saved
                """, 
                dbName, clusterInfo, userCount, mongoUri.substring(0, 30));
                
        } catch (Exception e) {
            return "❌ Connection details failed: " + e.getMessage();
        }
    }

    @GetMapping("/test/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/test/create-sample-user")
    public String createSampleUser() {
        try {
            String dbName = mongoTemplate.getDb().getName();
            
            // Check if sample user already exists
            if (userRepository.existsByEmail("sample@ridelink.com")) {
                long totalCount = userRepository.count();
                return String.format("""
                    ✅ Sample user already exists!
                    📊 Total users: %d
                    🏷️  Database: %s
                    📝 Collection: users
                    
                    ❓ Not visible in Atlas? Check:
                    1. Atlas Project/Organization
                    2. Cluster name: %s
                    3. Browser refresh (F5)
                    """, 
                    totalCount, dbName, mongoUri.contains("@") ? 
                        mongoUri.substring(mongoUri.indexOf("@") + 1, mongoUri.indexOf("/", mongoUri.indexOf("@"))) : 
                        "cluster info not found");
            }

            User sampleUser = User.builder()
                    .fullName("Sample Test User - " + LocalDateTime.now())
                    .email("sample@ridelink.com")
                    .password("$2a$10$dummyHashedPassword")
                    .phoneNumber("1234567890")
                    .role(Role.PASSENGER)
                    .status(AccountStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            User saved = userRepository.save(sampleUser);
            long totalCount = userRepository.count();
            
            return String.format("""
                ✅ Sample user created successfully!
                🆔 User ID: %s
                👤 Name: %s
                📊 Total users in DB: %d
                🏷️  Database: %s
                📝 Collection: users
                
                🔍 To see in MongoDB Atlas:
                1. Go to Database → Browse Collections
                2. Look for database: %s
                3. Collection: users
                4. Should have %d documents
                """, 
                saved.getId(), saved.getFullName(), totalCount, dbName, dbName, totalCount);
                
        } catch (Exception e) {
            return "❌ Failed to create user: " + e.getMessage();
        }
    }

    @PostMapping("/test/force-create-multiple")
    public String createMultipleUsers() {
        try {
            String dbName = mongoTemplate.getDb().getName();
            
            // Create 3 test users to make database more visible
            for (int i = 1; i <= 3; i++) {
                String email = "test" + i + "@ridelink.com";
                if (!userRepository.existsByEmail(email)) {
                    User user = User.builder()
                            .fullName("Test User " + i)
                            .email(email)
                            .password("$2a$10$dummyHashedPassword")
                            .phoneNumber("123456789" + i)
                            .role(Role.PASSENGER)
                            .status(AccountStatus.ACTIVE)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    userRepository.save(user);
                }
            }
            
            long totalCount = userRepository.count();
            return String.format("""
                ✅ Multiple users created!
                📊 Total users: %d
                🏷️  Database: %s
                
                🎯 This should definitely be visible in Atlas now!
                Go to: Database → Browse Collections → %s → users
                """, totalCount, dbName, dbName);
                
        } catch (Exception e) {
            return "❌ Failed to create multiple users: " + e.getMessage();
        }
    }
}