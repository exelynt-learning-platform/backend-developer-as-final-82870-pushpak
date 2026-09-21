package com.example.booking.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private Boolean available;

    @Column(
            name = "price_per_hour",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal pricePerHour;

    @OneToMany(mappedBy = "resource")
    private List<Reservation> reservations = new ArrayList<>();

    public Resource() {
    }

    public Resource(Boolean available, String description, Long id, String name, BigDecimal pricePerHour, List<Reservation> reservations, String type) {
        this.available = available;
        this.description = description;
        this.id = id;
        this.name = name;
        this.pricePerHour = pricePerHour;
        this.reservations = reservations;
        this.type = type;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
