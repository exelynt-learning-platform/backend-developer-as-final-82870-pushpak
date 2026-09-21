package com.example.booking.controller;

import com.example.booking.dto.LoginRequest;
import com.example.booking.dto.LoginResponse;
import com.example.booking.dto.ResourceRequest;
import com.example.booking.dto.ResourceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String login(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest(username, password);
        String response = mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        return loginResponse.getToken();
    }

    private String adminToken() throws Exception {
        return login("admin", "Admin@123");
    }

    private String userToken() throws Exception {
        return login("user", "User@123");
    }

    private ResourceRequest validResourceRequest() {
        return new ResourceRequest(
                "Test Resource",
                "Resource created during automated testing",
                "ROOM",
                true,
                new BigDecimal("500.00")
        );
    }

    @Test
    void userShouldBeAbleToGetAllResources() throws Exception {
        mockMvc.perform(
                get("/api/resources")
                        .header("Authorization", "Bearer " + userToken())
        )
                .andExpect(status().isOk());
    }

    @Test
    void adminShouldBeAbleToGetAllResources() throws Exception {
        mockMvc.perform(
                        get("/api/resources")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isOk());
    }

    @Test
    void userShouldBeAbleToGetAllResourceById() throws Exception {
        String token = userToken();
        String response = mockMvc.perform(
                get("/api/resources")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + token)
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long resourceId = objectMapper
                .readTree(response)
                .get("content")
                .get(0)
                .get("id")
                .asLong();
        mockMvc.perform(
                get("/api/resources/" + resourceId)
                        .header("Authorization", "Bearer " + token)
        )
                .andExpect(status().isOk());
    }


    @Test
    void adminShouldBeAbleToCreateResource() throws Exception {
        ResourceRequest request = validResourceRequest();

        mockMvc.perform(
                        post("/api/resources")
                                .header("Authorization", "Bearer " + adminToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated());
    }

    @Test
    void userShouldNotBeAbleToCreateResource() throws Exception {
        ResourceRequest request = validResourceRequest();

        mockMvc.perform(
                        post("/api/resources")
                                .header("Authorization", "Bearer " + userToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldBeAbleToUpdateResource() throws Exception {
        ResourceRequest createRequest = validResourceRequest();

        String response = mockMvc.perform(
                        post("/api/resources")
                                .header("Authorization", "Bearer " + adminToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRequest))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResourceResponse createdResource =
                objectMapper.readValue(response, ResourceResponse.class);

        ResourceRequest updateRequest = new ResourceRequest(
                "Updated Resource",
                "Updated description",
                "LAB",
                true,
                new BigDecimal("750.00")
        );

        mockMvc.perform(
                        put("/api/resources/" + createdResource.getId())
                                .header("Authorization", "Bearer " + adminToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest))
                )
                .andExpect(status().isOk());
    }

    @Test
    void userShouldNotBeAbleToUpdateResource() throws Exception {
        ResourceRequest request = validResourceRequest();

        mockMvc.perform(
                        put("/api/resources/1")
                                .header("Authorization", "Bearer " + userToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void userShouldNotBeAbleToDeleteResource() throws Exception {
        mockMvc.perform(
                        delete("/api/resources/1")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldBeAbleToDeleteResourceWithoutReservations() throws Exception {
        ResourceRequest request = validResourceRequest();

        String response = mockMvc.perform(
                        post("/api/resources")
                                .header("Authorization", "Bearer " + adminToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResourceResponse createdResource =
                objectMapper.readValue(response, ResourceResponse.class);

        mockMvc.perform(
                        delete("/api/resources/" + createdResource.getId())
                                .header("Authorization", "Bearer " + adminToken())
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void unauthenticatedUserShouldNotAccessResources() throws Exception {
        mockMvc.perform(
                        get("/api/resources")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getNonExistingResourceShouldReturn404() throws Exception {
        mockMvc.perform(
                        get("/api/resources/999999999")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void createResourceWithInvalidDataShouldReturn400() throws Exception {
        ResourceRequest request = new ResourceRequest(
                "",
                "Invalid resource",
                "",
                null,
                new BigDecimal("-100.00")
        );

        mockMvc.perform(
                        post("/api/resources")
                                .header("Authorization", "Bearer " + adminToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getResourcesWithInvalidPaginationShouldReturn400() throws Exception {
        mockMvc.perform(
                        get("/api/resources")
                                .param("page", "-1")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getResourcesWithInvalidSortFieldShouldReturn400() throws Exception {
        mockMvc.perform(
                        get("/api/resources")
                                .param("sortBy", "invalidField")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getResourcesWithInvalidSortDirectionShouldReturn400() throws Exception {
        mockMvc.perform(
                        get("/api/resources")
                                .param("direction", "INVALID")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isBadRequest());
    }
}
