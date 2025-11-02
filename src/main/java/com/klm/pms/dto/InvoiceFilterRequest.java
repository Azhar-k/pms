package com.klm.pms.dto;

import com.klm.pms.model.Invoice.InvoiceStatus;

import java.time.LocalDateTime;

public class InvoiceFilterRequest {
    private String invoiceNumber;
    private Long reservationId;
    private InvoiceStatus status;
    private LocalDateTime issuedDateFrom;
    private LocalDateTime issuedDateTo;
    private LocalDateTime paidDateFrom;
    private LocalDateTime paidDateTo;
    private LocalDateTime dueDateFrom;
    private LocalDateTime dueDateTo;
    private String paymentMethod;
    private String searchTerm; // For searching in invoice number, notes

    public InvoiceFilterRequest() {
    }

    // Getters and Setters
    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public LocalDateTime getIssuedDateFrom() {
        return issuedDateFrom;
    }

    public void setIssuedDateFrom(LocalDateTime issuedDateFrom) {
        this.issuedDateFrom = issuedDateFrom;
    }

    public LocalDateTime getIssuedDateTo() {
        return issuedDateTo;
    }

    public void setIssuedDateTo(LocalDateTime issuedDateTo) {
        this.issuedDateTo = issuedDateTo;
    }

    public LocalDateTime getPaidDateFrom() {
        return paidDateFrom;
    }

    public void setPaidDateFrom(LocalDateTime paidDateFrom) {
        this.paidDateFrom = paidDateFrom;
    }

    public LocalDateTime getPaidDateTo() {
        return paidDateTo;
    }

    public void setPaidDateTo(LocalDateTime paidDateTo) {
        this.paidDateTo = paidDateTo;
    }

    public LocalDateTime getDueDateFrom() {
        return dueDateFrom;
    }

    public void setDueDateFrom(LocalDateTime dueDateFrom) {
        this.dueDateFrom = dueDateFrom;
    }

    public LocalDateTime getDueDateTo() {
        return dueDateTo;
    }

    public void setDueDateTo(LocalDateTime dueDateTo) {
        this.dueDateTo = dueDateTo;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
}

