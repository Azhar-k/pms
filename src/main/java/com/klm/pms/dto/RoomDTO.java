package com.klm.pms.dto;

import com.klm.pms.model.Room.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class RoomDTO {
    private Long id;
    
    @NotBlank(message = "Room number is required")
    private String roomNumber;
    
    @NotNull(message = "Room type ID is required")
    private Long roomTypeId;
    
    @NotNull(message = "Price per night is required")
    @Positive(message = "Price must be positive")
    private BigDecimal pricePerNight;
    
    private RoomStatus status;
    private Integer maxOccupancy;
    private String amenities;
    private String description;
    private Integer floor;
    private Boolean hasBalcony;
    private Boolean hasView;
    
    // Nested DTO for response
    private RoomTypeDTO roomType;

    // Constructors
    public RoomDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public RoomTypeDTO getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomTypeDTO roomType) {
        this.roomType = roomType;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public Integer getMaxOccupancy() {
        return maxOccupancy;
    }

    public void setMaxOccupancy(Integer maxOccupancy) {
        this.maxOccupancy = maxOccupancy;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public Boolean getHasBalcony() {
        return hasBalcony;
    }

    public void setHasBalcony(Boolean hasBalcony) {
        this.hasBalcony = hasBalcony;
    }

    public Boolean getHasView() {
        return hasView;
    }

    public void setHasView(Boolean hasView) {
        this.hasView = hasView;
    }
}

