package com.example.booking.controller;

import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.dto.ReservationStatusRequest;
import com.example.booking.entity.Reservation;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request, Authentication authentication)
    {
        ReservationResponse response = reservationService.createReservation(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getReservations(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        boolean isAdmin = authentication.getAuthorities()
                .stream().anyMatch(authority -> Objects.equals(authority.getAuthority(), "ROLE_ADMIN"));

        return ResponseEntity.ok(
                reservationService.getAllReservations(
                        authentication.getName(),
                        isAdmin,
                        page,
                        size,
                        status,
                        minPrice,
                        maxPrice,
                        sortBy,
                        direction
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), "ROLE_ADMIN"));

        return ResponseEntity.ok(reservationService.getReservationById(id, authentication.getName(), isAdmin));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id, @Valid @RequestBody ReservationRequest request, Authentication authentication
    ) {
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), "ROLE_ADMIN"));

        return ResponseEntity.ok(reservationService.updateReservation(id, request, authentication.getName(), isAdmin));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReservationResponse> updateReservationStatus(
            @PathVariable Long id, @Valid @RequestBody ReservationStatusRequest request
            ) {
        return ResponseEntity.ok(reservationService.updateReservationStatus(id, request.getReservationStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), "ROLE_ADMIN"));

        reservationService.deleteReservation(id, authentication.getName(), isAdmin);
        return ResponseEntity.noContent().build();
    }

}
