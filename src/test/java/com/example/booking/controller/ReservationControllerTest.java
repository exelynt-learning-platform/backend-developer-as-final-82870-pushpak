package com.example.booking.controller;

import com.example.booking.dto.*;
import com.example.booking.enums.ReservationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

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

    private ResourceResponse createResource() throws Exception {
        ResourceRequest request = new ResourceRequest(
                "Test resource" + System.nanoTime(),
                "Resource for reservation testing",
                "ROOM",
                true,
                new BigDecimal(500)
        );

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
        return objectMapper.readValue(response, ResourceResponse.class);
    }

    private ReservationRequest reservationRequest(
            Long resourceId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return new ReservationRequest(
                resourceId,
                startTime,
                endTime
        );
    }

    private ReservationResponse createReservation(
            String token,
            Long resourceId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) throws Exception {

        ReservationRequest request =
                reservationRequest(resourceId, startTime, endTime);

        String response = mockMvc.perform(
                        post("/api/reservations")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, ReservationResponse.class);
    }

    @Test
    void userShouldBeAbleToCreateReservation() throws Exception {
        ResourceResponse resource = createResource();

        createReservation(
                userToken(),
                resource.getId(),
                LocalDateTime.now().plusDays(10).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(10).withHour(12).withMinute(0)
        );
    }

    @Test
    void userShouldBeAbleToViewOwnReservations() throws Exception {
        ResourceResponse resource = createResource();

        createReservation(
                userToken(),
                resource.getId(),
                LocalDateTime.now().plusDays(11).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(11).withHour(12).withMinute(0)
        );

        mockMvc.perform(
                        get("/api/reservations")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isOk());
    }

    @Test
    void userShouldBeAbleToViewOwnReservationById() throws Exception {
        ResourceResponse resource = createResource();

        ReservationResponse reservation = createReservation(
                userToken(),
                resource.getId(),
                LocalDateTime.now().plusDays(12).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(12).withHour(12).withMinute(0)
        );

        mockMvc.perform(
                        get("/api/reservations/" + reservation.getId())
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isOk());
    }

    @Test
    void userShouldBeAbleToUpdateOwnReservation() throws Exception {
        ResourceResponse resource = createResource();

        ReservationResponse reservation = createReservation(
                userToken(),
                resource.getId(),
                LocalDateTime.now().plusDays(13).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(13).withHour(12).withMinute(0)
        );

        ReservationRequest updateRequest =
                reservationRequest(
                        resource.getId(),
                        LocalDateTime.now().plusDays(13).withHour(14).withMinute(0),
                        LocalDateTime.now().plusDays(13).withHour(16).withMinute(0)
                );

        mockMvc.perform(
                        put("/api/reservations/" + reservation.getId())
                                .header("Authorization", "Bearer " + userToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest))
                )
                .andExpect(status().isOk());
    }

    @Test
    void userShouldBeAbleToDeleteOwnReservation() throws Exception {
        ResourceResponse resource = createResource();

        ReservationResponse reservation = createReservation(
                userToken(),
                resource.getId(),
                LocalDateTime.now().plusDays(14).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(14).withHour(12).withMinute(0)
        );

        mockMvc.perform(
                        delete("/api/reservations/" + reservation.getId())
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void userShouldNotBeAbleToViewAnotherUsersReservation() throws Exception {
        ResourceResponse resource = createResource();

        ReservationResponse reservation = createReservation(
                userToken(),
                resource.getId(),
                LocalDateTime.now().plusDays(15).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(15).withHour(12).withMinute(0)
        );

        mockMvc.perform(
                        get("/api/reservations/" + reservation.getId())
                                .header("Authorization", "Bearer " + adminToken())
                )
                .andExpect(status().isOk());
    }

    @Test
    void adminShouldBeAbleToViewAllReservations() throws Exception {
        mockMvc.perform(
                        get("/api/reservations")
                                .header("Authorization", "Bearer " + adminToken())
                )
                .andExpect(status().isOk());
    }

    @Test
    void adminShouldBeAbleToUpdateReservation() throws Exception {
        ResourceResponse resource = createResource();

        ReservationResponse reservation = createReservation(
                userToken(),
                resource.getId(),
                LocalDateTime.now().plusDays(16).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(16).withHour(12).withMinute(0)
        );

        ReservationRequest request =
                reservationRequest(
                        resource.getId(),
                        LocalDateTime.now().plusDays(16).withHour(14).withMinute(0),
                        LocalDateTime.now().plusDays(16).withHour(16).withMinute(0)
                );

        mockMvc.perform(
                        put("/api/reservations/" + reservation.getId())
                                .header("Authorization", "Bearer " + adminToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());
    }

    @Test
    void adminShouldBeAbleToDeleteReservation() throws Exception {
        ResourceResponse resource = createResource();

        ReservationResponse reservation = createReservation(
                userToken(),
                resource.getId(),
                LocalDateTime.now().plusDays(17).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(17).withHour(12).withMinute(0)
        );

        mockMvc.perform(
                        delete("/api/reservations/" + reservation.getId())
                                .header("Authorization", "Bearer " + adminToken())
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void adminShouldBeAbleToConfirmReservation() throws Exception {
        ResourceResponse resource = createResource();

        ReservationResponse reservation = createReservation(
                userToken(),
                resource.getId(),
                LocalDateTime.now().plusDays(18).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(18).withHour(12).withMinute(0)
        );

        mockMvc.perform(
                        patch("/api/reservations/" + reservation.getId() + "/status")
                                .header("Authorization", "Bearer " + adminToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new ReservationStatusRequest(ReservationStatus.CONFIRMED)
                                ))
                )
                .andDo(result -> {
                    System.out.println("STATUS: " + result.getResponse().getStatus());
                    System.out.println("BODY: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isOk());

    }

    @Test
    void adminShouldBeAbleToCancelReservation() throws Exception {
        ResourceResponse resource = createResource();

        ReservationResponse reservation = createReservation(
                userToken(),
                resource.getId(),
                LocalDateTime.now().plusDays(19).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(19).withHour(12).withMinute(0)
        );

        mockMvc.perform(
                        patch("/api/reservations/" + reservation.getId() + "/status")
                                .header("Authorization", "Bearer " + adminToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new ReservationStatusRequest(ReservationStatus.CANCELLED)
                                ))
                )
                .andDo(result -> {
                    System.out.println("STATUS: " + result.getResponse().getStatus());
                    System.out.println("BODY: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isOk());
    }

    @Test
    void userShouldNotBeAbleToChangeReservationStatus() throws Exception {
        ResourceResponse resource = createResource();

        ReservationResponse reservation = createReservation(
                userToken(),
                resource.getId(),
                LocalDateTime.now().plusDays(20).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(20).withHour(12).withMinute(0)
        );

        mockMvc.perform(
                        patch("/api/reservations/" + reservation.getId() + "/status")
                                .header("Authorization", "Bearer " + userToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"CONFIRMED\"}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserShouldNotAccessReservations() throws Exception {
        mockMvc.perform(
                        get("/api/reservations")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTimeRangeShouldReturn400() throws Exception {
        ResourceResponse resource = createResource();

        ReservationRequest request =
                reservationRequest(
                        resource.getId(),
                        LocalDateTime.now().plusDays(21).withHour(12).withMinute(0),
                        LocalDateTime.now().plusDays(21).withHour(10).withMinute(0)
                );

        mockMvc.perform(
                        post("/api/reservations")
                                .header("Authorization", "Bearer " + userToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void pastStartTimeShouldReturn400() throws Exception {
        ResourceResponse resource = createResource();

        ReservationRequest request =
                reservationRequest(
                        resource.getId(),
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusHours(1)
                );

        mockMvc.perform(
                        post("/api/reservations")
                                .header("Authorization", "Bearer " + userToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void overlappingReservationShouldReturn409() throws Exception {
        ResourceResponse resource = createResource();

        LocalDateTime start =
                LocalDateTime.now().plusDays(22).withHour(10).withMinute(0);

        LocalDateTime end =
                LocalDateTime.now().plusDays(22).withHour(12).withMinute(0);

        createReservation(
                userToken(),
                resource.getId(),
                start,
                end
        );

        ReservationRequest overlappingRequest =
                reservationRequest(
                        resource.getId(),
                        start.plusMinutes(30),
                        end.plusMinutes(30)
                );

        mockMvc.perform(
                        post("/api/reservations")
                                .header("Authorization", "Bearer " + userToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(overlappingRequest))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void backToBackReservationShouldBeAllowed() throws Exception {
        ResourceResponse resource = createResource();

        LocalDateTime firstStart =
                LocalDateTime.now().plusDays(23).withHour(10).withMinute(0);

        LocalDateTime firstEnd =
                LocalDateTime.now().plusDays(23).withHour(12).withMinute(0);

        createReservation(
                userToken(),
                resource.getId(),
                firstStart,
                firstEnd
        );

        ReservationRequest secondRequest =
                reservationRequest(
                        resource.getId(),
                        firstEnd,
                        firstEnd.plusHours(2)
                );

        mockMvc.perform(
                        post("/api/reservations")
                                .header("Authorization", "Bearer " + userToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(secondRequest))
                )
                .andExpect(status().isCreated());
    }

    @Test
    void unavailableResourceShouldReturn400() throws Exception {
        ResourceRequest resourceRequest = new ResourceRequest(
                "Unavailable Resource " + System.nanoTime(),
                "Unavailable",
                "ROOM",
                false,
                new BigDecimal("500.00")
        );

        String response = mockMvc.perform(
                        post("/api/resources")
                                .header("Authorization", "Bearer " + adminToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(resourceRequest))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResourceResponse resource =
                objectMapper.readValue(response, ResourceResponse.class);

        ReservationRequest reservationRequest =
                reservationRequest(
                        resource.getId(),
                        LocalDateTime.now().plusDays(24).withHour(10),
                        LocalDateTime.now().plusDays(24).withHour(12)
                );

        mockMvc.perform(
                        post("/api/reservations")
                                .header("Authorization", "Bearer " + userToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reservationRequest))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusFilterShouldReturn200() throws Exception {
        mockMvc.perform(
                        get("/api/reservations")
                                .param("status", "PENDING")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isOk());
    }

    @Test
    void priceFilterShouldReturn200() throws Exception {
        mockMvc.perform(
                        get("/api/reservations")
                                .param("minPrice", "100")
                                .param("maxPrice", "5000")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isOk());
    }

    @Test
    void paginationShouldReturn200() throws Exception {
        mockMvc.perform(
                        get("/api/reservations")
                                .param("page", "0")
                                .param("size", "5")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isOk());
    }

    @Test
    void invalidPaginationShouldReturn400() throws Exception {
        mockMvc.perform(
                        get("/api/reservations")
                                .param("page", "-1")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void sortingShouldReturn200() throws Exception {
        mockMvc.perform(
                        get("/api/reservations")
                                .param("sortBy", "price")
                                .param("direction", "DESC")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isOk());
    }

    @Test
    void invalidSortFieldShouldReturn400() throws Exception {
        mockMvc.perform(
                        get("/api/reservations")
                                .param("sortBy", "invalidField")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSortDirectionShouldReturn400() throws Exception {
        mockMvc.perform(
                        get("/api/reservations")
                                .param("direction", "INVALID")
                                .header("Authorization", "Bearer " + userToken())
                )
                .andExpect(status().isBadRequest());
    }
}
