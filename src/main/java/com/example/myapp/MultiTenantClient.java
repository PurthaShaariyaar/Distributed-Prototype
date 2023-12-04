package com.example.myapp;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.util.*;

public class MultiTenantClient {
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
                    readFile(scanner, serverUrl);
                    break;
                case 5:
                    updateFile(scanner, serverUrl);
                    break;
                case 6:
                    deleteFile(scanner, serverUrl);
                    break;
                case 7:
                    downloadFile(scanner, serverUrl);
                    break;
                case 8:
                    System.out.println("Exiting the client.");
                    return;
                case 9:
                    if ("admin".equals(userRole)) {
                        createUser(scanner, serverUrl);
                    } else {
                        System.out.println("Unauthorized action!");
                    }
                    break;
                case 10:
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
        System.out.println("4. Read File");
        System.out.println("5. Update File");
        System.out.println("6. Delete File");
        System.out.println("7. Download File");
        System.out.println("8. Exit");

        if ("admin".equals(userRole)) {
            System.out.println("9. Create User");
            System.out.println("10. Create Tenant");
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
        System.out.print("Enter password: "); // Add password prompt
        String password = scanner.nextLine();  // Read password
        System.out.print("Enter tenant: ");
        String tenant = scanner.nextLine();
        System.out.print("Enter file name: ");
        String fileName = scanner.nextLine();
        System.out.print("Enter file content: ");
        String fileContent = scanner.nextLine();

        Map<String, String> fileData = new HashMap<>();
        fileData.put("username", username);
        fileData.put("password", password); // Include password in the request
        fileData.put("file_name", fileName);
        fileData.put("file_content", fileContent);

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

        System.out.print("Enter accessible tenants (comma-separated, leave empty if none): ");
        String accessibleTenants = scanner.nextLine();

        Map<String, String> userData = Map.of(
                "adminUsername", adminUsername,
                "username", username,
                "password", password,
                "role", role,
                "accessibleTenants", accessibleTenants
        );

        String createUserResult = sendHttpPostRequest(serverUrl + "/admin/createUser", userData);
        System.out.println("Create user result: " + createUserResult);
    }

    // Method to read the file
    private static void readFile(Scanner scanner, String serverUrl) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter tenant: ");
        String tenant = scanner.nextLine();
        System.out.print("Enter file name to read: ");
        String fileName = scanner.nextLine().trim();

        // Construct the request data with username as a query parameter
        Map<String, String> queryParams = Map.of("username", username);

        Map<String, List<Map<String, String>>> response = sendHttpGetRequest(serverUrl + "/file/" + tenant + "/" + fileName, queryParams);

        if (response != null) {
            List<Map<String, String>> fileContentList = response.get("fileContent");
            if (fileContentList != null && !fileContentList.isEmpty()) {
                String fileContent = fileContentList.get(0).get("content");
                System.out.println("File content: " + fileContent);
            } else {
                System.out.println("File content not found.");
            }
        } else {
            System.out.println("Failed to read file.");
        }
    }

    // Method to update file
    private static void updateFile(Scanner scanner, String serverUrl) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter tenant: ");
        String tenant = scanner.nextLine().trim();
        System.out.print("Enter the filename to update: ");
        String fileName = scanner.nextLine();
        System.out.print("Enter new file content: ");
        String fileContent = scanner.nextLine();

        // Construct the request data with updated file content
        Map<String, String> fileData = new HashMap<>();
        fileData.put("username", username);
        fileData.put("file_content", fileContent); // Updated file content

        String updateResult = sendHttpPutRequest(serverUrl + "/file/" + tenant + "/" + fileName, fileData);
        System.out.println("Update result: " + updateResult);
    }


    // Method to delete file
    private static void deleteFile(Scanner scanner, String serverUrl) {

        // ask for username then tenant and file name to delete
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter tenant: ");
        String tenant = scanner.nextLine();
        System.out.print("Enter file name to delete: ");
        String fileName = scanner.nextLine();

        String deleteResult = sendHttpDeleteRequest(serverUrl + "/file/" + tenant + "/" + fileName, username);
        System.out.println("Delete result: " + deleteResult);
    }

    // Method to download file
    private static void downloadFile(Scanner scanner, String serverUrl) {

        // ask for username then the tenant and file name
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter tenant: ");
        String tenant = scanner.nextLine();
        System.out.print("Enter the file name to download: ");
        String fileName = scanner.nextLine();

        // assign file data to get each byte of the file
        byte[] fileData = sendHttpGetRequestForFile(serverUrl + "/download/" + tenant + "/" + fileName, username);
        // ensure file is not empty
        if (fileData != null) {
            // read from file until null
            try (FileOutputStream fileOutputStream = new FileOutputStream(fileName + ".txt")) {
                fileOutputStream.write(fileData);
                System.out.println("File downloaded successfully: " + fileName + ".txt");
            } catch (IOException e) {
                System.out.println("Error while writing file: " + e.getMessage());
            }
        }
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

    private static String sendHttpPutRequest(String url, Map<String, String> data) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("PUT");
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
            return "Request failed: " + e.getMessage();
        }
    }

    private static String sendHttpDeleteRequest(String url, String username) {
        try {
            // Construct the full URL with the username as a query parameter
            String finalUrl = url + "?username=" + URLEncoder.encode(username, "UTF-8");

            HttpURLConnection connection = (HttpURLConnection) new URL(finalUrl).openConnection();
            connection.setRequestMethod("DELETE");

            // Check the response code to determine if the request was successful
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response from the server
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                // Handle non-OK response
                return "Server responded with status code: " + responseCode;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Request failed: " + e.getMessage();
        }
    }

    private static byte[] sendHttpGetRequestForFile(String url, String username) {
        try {
            // Append username as a query parameter
            String finalUrl = url + "?username=" + URLEncoder.encode(username, "UTF-8");

            HttpURLConnection connection = (HttpURLConnection) new URL(finalUrl).openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response as a byte array
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try (InputStream inputStream = connection.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                    return byteArrayOutputStream.toByteArray();
                }
            } else {
                // Handle non-OK response
                System.out.println("Server responded with status code: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
