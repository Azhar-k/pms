package com.klm.pms.dto;

import com.klm.pms.model.Room.RoomStatus;

public class RoomFilterRequest {
    private String roomNumber;
    private Long roomTypeId;
    private RoomStatus status;
    private Integer minMaxOccupancy;
    private Integer maxMaxOccupancy;
    private Integer floor;
    private Boolean hasBalcony;
    private Boolean hasView;
    private String searchTerm; // For searching in room number, description, amenities

    public RoomFilterRequest() {
    }

    // Getters and Setters
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

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public Integer getMinMaxOccupancy() {
        return minMaxOccupancy;
    }

    public void setMinMaxOccupancy(Integer minMaxOccupancy) {
        this.minMaxOccupancy = minMaxOccupancy;
    }

    public Integer getMaxMaxOccupancy() {
        return maxMaxOccupancy;
    }

    public void setMaxMaxOccupancy(Integer maxMaxOccupancy) {
        this.maxMaxOccupancy = maxMaxOccupancy;
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

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
}

