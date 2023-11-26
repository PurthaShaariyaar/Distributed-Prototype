// Import necessary libraries and packages

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.util.*;

public class MultiTenantInventoryClient {
    public static void main(String[] args) {
        // Specify server url and port number
        String serverUrl = "http://localhost:8080";

        // Set initial user credentials and file data
        Map<String, String> credentials = Map.of("username", "user1", "password", "password1");
        Map<String, String> fileData = Map.of("username", "user1", "file_name", "sample.txt", "file_content", "This is a sample file content.");

        // Perform login operation
        String loginResult = sendHttpPostRequest(serverUrl + "/login", credentials);
        System.out.println("Login result: " + loginResult);

        // Upload a file
        String uploadResult = sendHttpPostRequest(serverUrl, "/upload/user1", fileData);
        System.out.println("Upload result: " + uploadResult);

        // List files
        Map<String, String> queryParams = Map.of("username", "user1");
        Map<String, List<Map<String, String>>> fileList = sendHttpGetRequest(serverUrl + "/list/user1", queryParams);
        System.out.println("File list: " + fileList.get("files"));
    }

    // Function to send an HTTP POST request
    private static String sendHttpPostRequest(String url, Map<String, String> data) {
        try {
            // Create an HTTP connection to the specified URL
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // Convert the data (in this case, a Map) to JSON format
            String jsonInputString = new ObjectMapper().writeValueAsString(data);

            // Send the JSON data in the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read and collect the response from the server
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

    // Function to send an HTTP GET request with query parameters
    private static Map<String, List<Map<String, String>>> sendHttpGetRequest(String url, Map<String, String> queryParams) {
        try {
            // Construct the URL with query parameters
            StringBuilder urlBuilder = new StringBuilder(url);
            urlBuilder.append("?");
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                urlBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            String finalUrl = urlBuilder.toString();

            // Create an HTTP connection to the final URL
            HttpURLConnection connection = (HttpURLConnection) new URL(finalUrl).openConnection();
            connection.setRequestMethod("GET");

            // Read and collect the response from the server
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                // Parse the JSON response into a Map
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(response.toString(), Map.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
}