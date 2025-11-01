package com.klm.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RateTypeDTO {
    private Long id;
    
    @NotBlank(message = "Rate type name is required")
    private String name;
    
    private String description;
    
    private List<RoomTypeRateDTO> roomTypeRates = new ArrayList<>();

    // Constructors
    public RateTypeDTO() {
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

    public List<RoomTypeRateDTO> getRoomTypeRates() {
        return roomTypeRates;
    }

    public void setRoomTypeRates(List<RoomTypeRateDTO> roomTypeRates) {
        this.roomTypeRates = roomTypeRates;
    }

    public static class RoomTypeRateDTO {
        private Long id;
        
        @NotNull(message = "Room type ID is required")
        private Long roomTypeId;
        
        private String roomTypeName; // For response
        
        @NotNull(message = "Rate is required")
        @Positive(message = "Rate must be positive")
        private BigDecimal rate;

        // Constructors
        public RoomTypeRateDTO() {
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getRoomTypeId() {
            return roomTypeId;
        }

        public void setRoomTypeId(Long roomTypeId) {
            this.roomTypeId = roomTypeId;
        }

        public String getRoomTypeName() {
            return roomTypeName;
        }

        public void setRoomTypeName(String roomTypeName) {
            this.roomTypeName = roomTypeName;
        }

        public BigDecimal getRate() {
            return rate;
        }

        public void setRate(BigDecimal rate) {
            this.rate = rate;
        }
    }
}

