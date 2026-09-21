package com.example.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ReservationRequest {

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;

    public ReservationRequest() {
    }

    public ReservationRequest(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        this.resourceId = resourceId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public @NotNull(message = "End time is required") LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(@NotNull(message = "End time is required") LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public @NotNull(message = "Resource ID is required") Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(@NotNull(message = "Resource ID is required") Long resourceId) {
        this.resourceId = resourceId;
    }

    public @NotNull(message = "Start time is required") LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(@NotNull(message = "Start time is required") LocalDateTime startTime) {
        this.startTime = startTime;
    }
}
