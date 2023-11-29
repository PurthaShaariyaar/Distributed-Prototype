package com.example.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
public class MultiTenantInventoryServer {
    private static final Map<String, Map<String, String>> users = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> tenantFiles = new ConcurrentHashMap<>();

    private static void initializeUsers() {
        Map<String, String> sampleUser = new HashMap<>();
        sampleUser.put("password", "password1");
        sampleUser.put("role", "user");
        users.put("user1", sampleUser);

        Map<String, String> adminUser = new HashMap<>();
        adminUser.put("password", "password2");
        adminUser.put("role", "admin");
        users.put("admin1", adminUser); // Adding an admin user
    }

    // Define a set of admin usernames
    private static final Set<String> adminUsernames = new HashSet<>(Arrays.asList("admin1", "admin2"));

    public static void main(String[] args) {
        initializeUsers();
        SpringApplication app = new SpringApplication(MultiTenantInventoryServer.class);
        app.setDefaultProperties(Map.of(
                "server.port", "8080"
        ));
        app.run(args);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (authenticate(username, password)) {
            return ResponseEntity.ok("Login successful for user: " + username);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login failed. Invalid credentials.");
        }
    }

    @PostMapping("/admin/createUser")
    public ResponseEntity<String> createUser(@RequestBody Map<String, String> userData) {
        String adminUsername = userData.get("adminUsername");
        if (!isAdmin(adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. Only admins can create users.");
        }

        String username = userData.get("username");
        String password = userData.get("password");
        String role = userData.get("role");

        if (username == null || password == null || role == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user data. Please provide username, password, and role.");
        }

        if (users.containsKey(username)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User with the same username already exists.");
        }

        Map<String, String> newUser = new HashMap<>();
        newUser.put("password", password);
        newUser.put("role", role);

        users.put(username, newUser);
        return ResponseEntity.ok("User '" + username + "' created successfully.");
    }

    @PostMapping("/upload/{tenant}")
    public ResponseEntity<String> uploadFile(@PathVariable String tenant, @RequestBody Map<String, String> fileData) {
        String username = fileData.get("username");

        if (!hasAccess(username, tenant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. You do not have permission.");
        }

        String fileName = fileData.get("file_name");
        String fileContent = fileData.get("file_content");

        tenantFiles.computeIfAbsent(tenant, k -> new ConcurrentHashMap<>()).put(fileName, fileContent);
        return ResponseEntity.ok("File '" + fileName + "' uploaded successfully for tenant: " + tenant);
    }

    @GetMapping("/list/{tenant}")
    public ResponseEntity<Object> listFiles(@PathVariable String tenant, @RequestParam String username) {
        if (hasAccess(username, tenant)) {
            Map<String, String> tenantData = tenantFiles.getOrDefault(tenant, new HashMap<>());
            return ResponseEntity.ok(Collections.singletonMap("files", Collections.singletonList(tenantData)));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("error", "Access denied. You do not have permission."));
        }
    }

    private boolean authenticate(String username, String password) {
        Map<String, String> user = users.get(username);
        return user != null && user.get("password").equals(password);
    }

    private boolean isAdmin(String username) {
        return adminUsernames.contains(username);
    }

    private boolean hasAccess(String username, String tenant) {
        Map<String, String> user = users.get(username);
        return user != null && (user.get("role").equals("admin") || username.equals(tenant));
    }
}
