// Import necessary libraries and packages

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.web.server.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

// Define class as Spring Boot application and RESTful controller
@SpringBootApplication
@RestController

public class MultiTenantInventoryServer {
    // Define two concurrent hashmaps to store user data and tenant files
    private static final Map<String, Map<String, String>> users = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> tenantFiles = new ConcurrentHashMap<>();

    // Main method to start the spring boot application
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MultiTenantInventoryServer.class);

        // Set default props
        app.setDefaultProperties(Map.of(
                "server.port", "8080"
        ));

        // Run the app
        app.run(args);
    }

    // POST endpoint for user login
    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> credentials) {
        // Extract username and password from the request body
        String username = credentials.get("username");
        String password = credentials.get("password");
        // Call the authenticate method to check if the provided credentials are valid
        if (authenticate(username, password)) {
            return "Login successful";
        } else {
            return "Login failed";
        }
    }

    @PostMapping("/upload/{tenant}")
    public String uploadFile(@PathVariable String tenant, @RequestBody Map<String, String> fileData) {
        // Extract the username from the request body
        String username = fileData.get("username");

        // Validate if user has access to upload a file for the specified tenant
        if (!hasAccess(username, tenant)) {
            return "Access denied";
        }

        // Extract file name and content from the request body
        String fileName = fileData.get("file_name");
        String fileContent = fileData.get("file_content");

        // Store the file content in the tenantFiles map under the specified tenant
        tenantFiles.computeIfAbsent(tenant, k -> new ConcurrentHashMap<>())
                .put(fileName, fileContent);
        return "File uploaded successfully";
    }

    // Define a GET endpoint for listing files for a specific tenant
    @GetMapping("/list/{tenant}")
    public Map<String, List<Map<String, String>>> listFiles(@PathVariable String tenant, @RequestParam String username) {
        // Check if the user has access to list files for the specified tenant
        if (!hasAccess(username, tenant)) {
            return Collections.singletonMap("message", Collections.singletonList(Collections.singletonMap("error", "Access denied")));
        }

        // Create a response map containing a list of files for the specified tenant
        Map<String, List<Map<String, String>>> response = new HashMap<>();
        Map<String, String> tenantData = tenantFiles.getOrDefault(tenant, new HashMap<>());

        response.put("files", Collections.singletonList(tenantData));
        return response;
    }


    // Private method to authenticate user based on username and password
    private boolean authenticate(String username, String password) {
        // Retrieve user data from user map to check if the password matches
        Map<String, String> user = users.get(username);
        return user != null && user.get("password").equals(password);
    }

    // Private method to check if a user has access to a specific tenants resource
    private boolean hasAccess(String username, String tenant) {
        // Retrieve the users data from the users map and check if they are an account owner (admin) or the tenant owner
        Map<String, String> user = users.get(username);
        return user != null && (user.get("role").equals("admin")) || username.equals(tenant);
    }
}