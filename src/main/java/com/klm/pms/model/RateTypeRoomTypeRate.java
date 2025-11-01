package com.klm.pms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Entity
@Table(name = "rate_type_room_type_rates", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"rate_type_id", "room_type_id"}))
public class RateTypeRoomTypeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_type_id", nullable = false)
    private RateType rateType;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @NotNull(message = "Rate is required")
    @Positive(message = "Rate must be positive")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rate;

    // Constructors
    public RateTypeRoomTypeRate() {
    }

    public RateTypeRoomTypeRate(RateType rateType, RoomType roomType, BigDecimal rate) {
        this.rateType = rateType;
        this.roomType = roomType;
        this.rate = rate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RateType getRateType() {
        return rateType;
    }

    public void setRateType(RateType rateType) {
        this.rateType = rateType;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}

