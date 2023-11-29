package com.example.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@RestController
public class MultiTenantInventoryServer {
    private static final Map<String, Map<String, String>> users = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> tenantFiles = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> tenants = new ConcurrentHashMap<>();

    private static void initializeUsers() {
        Map<String, String> sampleUser = new HashMap<>();
        sampleUser.put("password", hashPassword("password1"));
        sampleUser.put("role", "user");
        users.put("user", sampleUser);

        Map<String, String> adminUser = new HashMap<>();
        adminUser.put("password", hashPassword("password2"));
        adminUser.put("role", "admin");
        users.put("admin", adminUser); // Adding an admin user
    }

    // Define a set of admin usernames
    private static final Set<String> adminUsernames = new HashSet<>(Arrays.asList("admin", "admin1", "admin2"));

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
        newUser.put("password", hashPassword(password));
        newUser.put("role", role);

        users.put(username, newUser);
        return ResponseEntity.ok("User '" + username + "' created successfully.");
    }

    @PostMapping("/admin/createTenant")
    public ResponseEntity<String> createTenant(@RequestBody Map<String, String> tenantData) {
        String adminUsername = tenantData.get("adminUsername");
        if (!isAdmin(adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. Only admins can create tenants.");
        }

        String tenantName = tenantData.get("tenantName");
        if (tenantName == null || tenantName.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid tenant data. Please provide a valid tenant name.");
        }

        if (tenants.containsKey(tenantName)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tenant with the same name already exists.");
        }

        tenants.put(tenantName, new ConcurrentHashMap<>());
        return ResponseEntity.ok("Tenant '" + tenantName + "' created successfully.");
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
        return user != null && user.get("password").equals(hashPassword(password));
    }

    private boolean isAdmin(String username) {
        return adminUsernames.contains(username);
    }

    private boolean hasAccess(String username, String tenant) {
        Map<String, String> user = users.get(username);
        return user != null && (user.get("role").equals("admin") || username.equals(tenant));
    }

    // Method to hash passwords
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
