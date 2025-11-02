package com.klm.pms.dto;

import com.klm.pms.model.Reservation.ReservationStatus;

import java.time.LocalDate;

public class ReservationFilterRequest {
    private String reservationNumber;
    private Long guestId;
    private Long roomId;
    private Long rateTypeId;
    private ReservationStatus status;
    private LocalDate checkInDateFrom;
    private LocalDate checkInDateTo;
    private LocalDate checkOutDateFrom;
    private LocalDate checkOutDateTo;
    private Integer minNumberOfGuests;
    private Integer maxNumberOfGuests;
    private String paymentStatus;
    private String searchTerm; // For searching in reservation number, special requests

    public ReservationFilterRequest() {
    }

    // Getters and Setters
    public String getReservationNumber() {
        return reservationNumber;
    }

    public void setReservationNumber(String reservationNumber) {
        this.reservationNumber = reservationNumber;
    }

    public Long getGuestId() {
        return guestId;
    }

    public void setGuestId(Long guestId) {
        this.guestId = guestId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getRateTypeId() {
        return rateTypeId;
    }

    public void setRateTypeId(Long rateTypeId) {
        this.rateTypeId = rateTypeId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDate getCheckInDateFrom() {
        return checkInDateFrom;
    }

    public void setCheckInDateFrom(LocalDate checkInDateFrom) {
        this.checkInDateFrom = checkInDateFrom;
    }

    public LocalDate getCheckInDateTo() {
        return checkInDateTo;
    }

    public void setCheckInDateTo(LocalDate checkInDateTo) {
        this.checkInDateTo = checkInDateTo;
    }

    public LocalDate getCheckOutDateFrom() {
        return checkOutDateFrom;
    }

    public void setCheckOutDateFrom(LocalDate checkOutDateFrom) {
        this.checkOutDateFrom = checkOutDateFrom;
    }

    public LocalDate getCheckOutDateTo() {
        return checkOutDateTo;
    }

    public void setCheckOutDateTo(LocalDate checkOutDateTo) {
        this.checkOutDateTo = checkOutDateTo;
    }

    public Integer getMinNumberOfGuests() {
        return minNumberOfGuests;
    }

    public void setMinNumberOfGuests(Integer minNumberOfGuests) {
        this.minNumberOfGuests = minNumberOfGuests;
    }

    public Integer getMaxNumberOfGuests() {
        return maxNumberOfGuests;
    }

    public void setMaxNumberOfGuests(Integer maxNumberOfGuests) {
        this.maxNumberOfGuests = maxNumberOfGuests;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
}

