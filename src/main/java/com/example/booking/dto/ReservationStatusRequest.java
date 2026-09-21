package com.example.booking.dto;

import com.example.booking.enums.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public class ReservationStatusRequest {

    @NotNull(message = "Reservation status is required")
    private ReservationStatus reservationStatus;

    public ReservationStatusRequest() {}

    public ReservationStatusRequest(ReservationStatus reservationStatus) {
        this.reservationStatus = reservationStatus;
    }

    public @NotNull(message = "Reservation status is required") ReservationStatus getReservationStatus() {
        return reservationStatus;
    }

    public void setReservationStatus(@NotNull(message = "Reservation status is required") ReservationStatus reservationStatus) {
        this.reservationStatus = reservationStatus;
    }
}
