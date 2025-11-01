package com.klm.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class RoomTypeDTO {
    private Long id;
    
    @NotBlank(message = "Room type name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Base price per night is required")
    @Positive(message = "Base price must be positive")
    private BigDecimal basePricePerNight;
    
    @NotNull(message = "Max occupancy is required")
    @Positive(message = "Max occupancy must be positive")
    private Integer maxOccupancy;
    
    private String amenities;
    private Integer defaultRoomSize;
    private Boolean hasBalcony;
    private Boolean hasView;
    private Boolean hasMinibar;
    private Boolean hasSafe;
    private Boolean hasAirConditioning;
    private String bedType;

    // Constructors
    public RoomTypeDTO() {
    }

    // Getters and Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePricePerNight() {
        return basePricePerNight;
    }

    public void setBasePricePerNight(BigDecimal basePricePerNight) {
        this.basePricePerNight = basePricePerNight;
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

    public Integer getDefaultRoomSize() {
        return defaultRoomSize;
    }

    public void setDefaultRoomSize(Integer defaultRoomSize) {
        this.defaultRoomSize = defaultRoomSize;
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

    public Boolean getHasMinibar() {
        return hasMinibar;
    }

    public void setHasMinibar(Boolean hasMinibar) {
        this.hasMinibar = hasMinibar;
    }

    public Boolean getHasSafe() {
        return hasSafe;
    }

    public void setHasSafe(Boolean hasSafe) {
        this.hasSafe = hasSafe;
    }

    public Boolean getHasAirConditioning() {
        return hasAirConditioning;
    }

    public void setHasAirConditioning(Boolean hasAirConditioning) {
        this.hasAirConditioning = hasAirConditioning;
    }

    public String getBedType() {
        return bedType;
    }

    public void setBedType(String bedType) {
        this.bedType = bedType;
    }
}

