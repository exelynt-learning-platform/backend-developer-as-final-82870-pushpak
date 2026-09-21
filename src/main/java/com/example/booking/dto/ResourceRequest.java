package com.example.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ResourceRequest {

    @NotBlank(message = "Resource name is required")
    @Size(max = 100, message = "Resource name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Resource type is required")
    @Size(max = 50, message = "Resource type must not exceed 50 characters")
    private String type;

    @NotNull(message = "Availability is required")
    private Boolean available;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.01", message = "Price per hour must be greater than 0")
    private BigDecimal pricePerHour;

    public ResourceRequest() {
    }

    public ResourceRequest(String name, String description, String type, Boolean available, BigDecimal pricePerHour) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.available = available;
        this.pricePerHour = pricePerHour;
    }

    public @NotNull(message = "Availability is required") Boolean getAvailable() {
        return available;
    }

    public void setAvailable(@NotNull(message = "Availability is required") Boolean available) {
        this.available = available;
    }

    public @Size(max = 500, message = "Description must not exceed 500 characters") String getDescription() {
        return description;
    }

    public void setDescription(@Size(max = 500, message = "Description must not exceed 500 characters") String description) {
        this.description = description;
    }

    public @NotBlank(message = "Resource name is required") @Size(max = 100, message = "Resource name must not exceed 100 characters") String getName() {
        return name;
    }

    public void setName(@NotBlank(message = "Resource name is required") @Size(max = 100, message = "Resource name must not exceed 100 characters") String name) {
        this.name = name;
    }

    public @NotNull(message = "Price per hour is required") @DecimalMin(value = "0.01", message = "Price per hour must be greater than 0") BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(@NotNull(message = "Price per hour is required") @DecimalMin(value = "0.01", message = "Price per hour must be greater than 0") BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public @NotBlank(message = "Resource type is required") @Size(max = 50, message = "Resource type must not exceed 50 characters") String getType() {
        return type;
    }

    public void setType(@NotBlank(message = "Resource type is required") @Size(max = 50, message = "Resource type must not exceed 50 characters") String type) {
        this.type = type;
    }
}
