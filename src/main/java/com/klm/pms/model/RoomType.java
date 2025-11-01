package com.klm.pms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "room_types")
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Room type name is required")
    @Column(nullable = false, unique = true)
    private String name; // e.g., "SINGLE", "DOUBLE", "SUITE", "DELUXE"

    private String description;

    @NotNull(message = "Base price per night is required")
    @Positive(message = "Base price must be positive")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePricePerNight;

    @NotNull(message = "Max occupancy is required")
    @Positive(message = "Max occupancy must be positive")
    @Column(nullable = false)
    private Integer maxOccupancy;

    private String amenities; // Comma-separated or JSON string

    private Integer defaultRoomSize; // in square meters/feet

    private Boolean hasBalcony;

    private Boolean hasView;

    private Boolean hasMinibar;

    private Boolean hasSafe;

    private Boolean hasAirConditioning;

    private String bedType; // SINGLE, DOUBLE, QUEEN, KING, etc.

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Room> rooms = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public RoomType() {
    }

    public RoomType(String name, BigDecimal basePricePerNight, Integer maxOccupancy) {
        this.name = name;
        this.basePricePerNight = basePricePerNight;
        this.maxOccupancy = maxOccupancy;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }
}

