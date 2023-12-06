package myapp;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;
import java.util.HashMap;


public class MultiTenantClientTests {

    private final String BASE_URL = "http://localhost:8080";
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
    }

    @Test
    void testSuccessfulLogin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "validUser");
        credentials.put("password", "validPassword");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/login", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Login successful for user"));
    }

    @Test
    void testFailedLogin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "invalidUser");
        credentials.put("password", "invalidPassword");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/login", request, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("Login failed"));
    }

    @Test
    void testEmptyUsernameAndPassword() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "");
        credentials.put("password", "");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/login", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testSuccessfulFileUpload() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> fileData = new HashMap<>();
        fileData.put("username", "validUser");
        fileData.put("file_name", "test.txt");
        fileData.put("file_content", "Sample content");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(fileData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/upload/tenantName", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("File 'test.txt' uploaded successfully"));
    }

    @Test
    void testUploadWithInvalidData() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> fileData = new HashMap<>();
        fileData.put("username", "validUser");
        fileData.put("file_name", "");
        fileData.put("file_content", "");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(fileData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/upload/tenantName", request, String.class);

        assertNotEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testUploadDuplicateFileName() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Assuming "test.txt" already exists in the system
        Map<String, String> fileData = new HashMap<>();
        fileData.put("username", "validUser");
        fileData.put("file_name", "test.txt");
        fileData.put("file_content", "New content");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(fileData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/upload/tenantName", request, String.class);

        assertNotEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testSuccessfullyListFiles() {
        String tenant = "tenantWithFiles";
        String username = "validUser";

        ResponseEntity<Map> response = restTemplate.getForEntity(
                BASE_URL + "/list/" + tenant + "?username=" + username, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("files"));
        assertFalse(((Map)response.getBody().get("files")).isEmpty());
    }

    @Test
    void testListFilesEmptyTenant() {
        String tenant = "emptyTenant";
        String username = "validUser";

        ResponseEntity<Map> response = restTemplate.getForEntity(
                BASE_URL + "/list/" + tenant + "?username=" + username, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("files"));
        assertTrue(((Map)response.getBody().get("files")).isEmpty());
    }
}

