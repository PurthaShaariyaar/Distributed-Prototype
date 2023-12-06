package myapp;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.hamcrest.Matchers.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
public class MultiTenantServerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testFailedLogin() throws Exception {
        String json = "{\"username\":\"wrongUser\",\"password\":\"wrongPassword\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Login failed")));
    }

    @Test
    public void testAdminAccessControl() throws Exception {
        // Example for admin-only createUser endpoint
        String json = "{\"adminUsername\":\"user\",\"username\":\"newUser\",\"password\":\"pass123\",\"role\":\"user\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/createUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Access denied")));
    }

    @Test
    public void testCreateUserWithValidData() throws Exception {
        String json = "{\"adminUsername\":\"admin\",\"username\":\"newUser\",\"password\":\"pass123\",\"role\":\"user\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/createUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User 'newUser' created successfully.")));
    }

    @Test
    public void testCreateUserWithInvalidData() throws Exception {
        String json = "{\"adminUsername\":\"admin\",\"username\":\"\",\"password\":\"\",\"role\":\"user\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/createUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNonAdminUserCreation() throws Exception {
        String json = "{\"adminUsername\":\"user\",\"username\":\"newUser\",\"password\":\"pass123\",\"role\":\"user\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/createUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }
}
