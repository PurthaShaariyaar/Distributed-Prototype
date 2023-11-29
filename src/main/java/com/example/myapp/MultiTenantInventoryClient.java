package com.example.myapp;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.util.*;

public class MultiTenantInventoryClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String serverUrl = "http://localhost:8080";
        String userRole = null;

        while (true) {
            displayMenu(userRole);

            System.out.print("Enter the number of your choice: ");
            int choice = readInt(scanner);

            switch (choice) {
                case 1:
                    userRole = login(scanner, serverUrl);
                    break;
                case 2:
                    uploadFile(scanner, serverUrl);
                    break;
                case 3:
                    listFiles(scanner, serverUrl);
                    break;
                case 4:
                    System.out.println("Exiting the client.");
                    return;
                case 5:
                    if ("admin".equals(userRole)) {
                        createUser(scanner, serverUrl);
                    } else {
                        System.out.println("Unauthorized action!");
                    }
                    break;
                case 6:
                    if ("admin".equals(userRole)) {
                        createTenant(scanner, serverUrl);
                    } else {
                        System.out.println("Unauthorized action!");
                    }
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a valid option.");
            }
        }
    }

    private static void displayMenu(String userRole) {
        System.out.println("Choose an action:");
        System.out.println("1. Login");
        System.out.println("2. Upload File");
        System.out.println("3. List Files");
        System.out.println("4. Exit");

        if ("admin".equals(userRole)) {
            System.out.println("5. Create User");
            System.out.println("6. Create Tenant");
        }
    }

    private static int readInt(Scanner scanner) {
        while (!scanner.hasNextInt()) {
            System.out.println("Invalid input. Please enter a number.");
            scanner.next(); // consume the invalid input
        }
        int number = scanner.nextInt();
        scanner.nextLine(); // consume the newline character
        return number;
    }

    private static String login(Scanner scanner, String serverUrl) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        Map<String, String> credentials = Map.of("username", username, "password", password);
        String loginResult = sendHttpPostRequest(serverUrl + "/login", credentials);
        System.out.println("Login result: " + loginResult);

        if (loginResult != null && loginResult.startsWith("Login successful for user:")) {
            String userRole = loginResult.substring(loginResult.lastIndexOf(":") + 1).trim();
            System.out.println("Logged in as: " + userRole);
            return userRole;
        } else {
            System.out.println("Login failed. Please try again.");
            return null;
        }
    }


    private static void createTenant(Scanner scanner, String serverUrl) {
        System.out.print("Enter admin username: ");
        String adminUsername = scanner.nextLine();

        System.out.print("Enter tenant name: ");
        String tenantName = scanner.nextLine();

        Map<String, String> tenantData = Map.of(
                "adminUsername", adminUsername,
                "tenantName", tenantName
        );

        String createTenantResult = sendHttpPostRequest(serverUrl + "/admin/createTenant", tenantData);
        System.out.println("Create tenant result: " + createTenantResult);
    }

    private static void uploadFile(Scanner scanner, String serverUrl) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter tenant: ");
        String tenant = scanner.nextLine();
        System.out.print("Enter file name: ");
        String fileName = scanner.nextLine();
        System.out.print("Enter file content: ");
        String fileContent = scanner.nextLine();

        Map<String, String> fileData = Map.of(
                "username", username,
                "file_name", fileName,
                "file_content", fileContent
        );

        String uploadResult = sendHttpPostRequest(serverUrl + "/upload/" + tenant, fileData);
        System.out.println("Upload result: " + uploadResult);
    }

    private static void listFiles(Scanner scanner, String serverUrl) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter tenant: ");
        String tenant = scanner.nextLine();

        Map<String, String> queryParams = Map.of("username", username);
        Map<String, List<Map<String, String>>> fileList = sendHttpGetRequest(serverUrl + "/list/" + tenant, queryParams);
        System.out.println("File list: " + fileList.get("files"));
    }

    private static void createUser(Scanner scanner, String serverUrl) {
        System.out.print("Enter admin username: ");
        String adminUsername = scanner.nextLine();

        System.out.print("Enter new username: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        System.out.print("Enter role (user or admin): ");
        String role = scanner.nextLine();

        Map<String, String> userData = Map.of(
                "adminUsername", adminUsername,
                "username", username,
                "password", password,
                "role", role
        );

        String createUserResult = sendHttpPostRequest(serverUrl + "/admin/createUser", userData);
        System.out.println("Create user result: " + createUserResult);
    }

    private static String sendHttpPostRequest(String url, Map<String, String> data) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String jsonInputString = new ObjectMapper().writeValueAsString(data);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Request failed";
        }
    }

    private static Map<String, List<Map<String, String>>> sendHttpGetRequest(String url, Map<String, String> queryParams) {
        try {
            StringBuilder urlBuilder = new StringBuilder(url);
            urlBuilder.append("?");
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                urlBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            String finalUrl = urlBuilder.toString();

            HttpURLConnection connection = (HttpURLConnection) new URL(finalUrl).openConnection();
            connection.setRequestMethod("GET");

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(response.toString(), Map.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
}
