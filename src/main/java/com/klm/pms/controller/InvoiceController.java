package com.klm.pms.controller;

import com.klm.pms.dto.InvoiceDTO;
import com.klm.pms.model.Invoice.InvoiceStatus;
import com.klm.pms.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoice Management", description = "APIs for managing invoices and billing")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @PostMapping("/generate/{reservationId}")
    @Operation(summary = "Generate invoice", description = "Generates an invoice for a reservation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invoice generated successfully"),
            @ApiResponse(responseCode = "404", description = "Reservation not found"),
            @ApiResponse(responseCode = "400", description = "Invoice already exists for this reservation")
    })
    public ResponseEntity<InvoiceDTO> generateInvoice(
            @Parameter(description = "Reservation ID", required = true) @PathVariable Long reservationId) {
        InvoiceDTO invoice = invoiceService.generateInvoice(reservationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID", description = "Retrieves an invoice by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice found"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceDTO> getInvoiceById(
            @Parameter(description = "Invoice ID", required = true) @PathVariable Long id) {
        InvoiceDTO invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/number/{invoiceNumber}")
    @Operation(summary = "Get invoice by invoice number", description = "Retrieves an invoice by its invoice number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice found"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceDTO> getInvoiceByNumber(
            @Parameter(description = "Invoice number", required = true) @PathVariable String invoiceNumber) {
        InvoiceDTO invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping
    @Operation(summary = "Get all invoices", description = "Retrieves a list of all invoices in the system")
    @ApiResponse(responseCode = "200", description = "List of invoices retrieved successfully")
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
        List<InvoiceDTO> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/reservation/{reservationId}")
    @Operation(summary = "Get invoices by reservation", description = "Retrieves all invoices for a specific reservation")
    @ApiResponse(responseCode = "200", description = "List of invoices retrieved successfully")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByReservation(
            @Parameter(description = "Reservation ID", required = true) @PathVariable Long reservationId) {
        List<InvoiceDTO> invoices = invoiceService.getInvoicesByReservation(reservationId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get invoices by status", description = "Retrieves all invoices with a specific status")
    @ApiResponse(responseCode = "200", description = "List of invoices retrieved successfully")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByStatus(
            @Parameter(description = "Invoice status", required = true) @PathVariable InvoiceStatus status) {
        List<InvoiceDTO> invoices = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity.ok(invoices);
    }

    @PostMapping("/{invoiceId}/items")
    @Operation(summary = "Add item to invoice", description = "Adds a new item to an existing invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item added successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found"),
            @ApiResponse(responseCode = "400", description = "Cannot add items to a paid invoice")
    })
    public ResponseEntity<InvoiceDTO> addInvoiceItem(
            @Parameter(description = "Invoice ID", required = true) @PathVariable Long invoiceId,
            @Valid @RequestBody InvoiceDTO.InvoiceItemDTO itemDTO) {
        InvoiceDTO invoice = invoiceService.addInvoiceItem(invoiceId, itemDTO);
        return ResponseEntity.ok(invoice);
    }

    @PostMapping("/{invoiceId}/pay")
    @Operation(summary = "Mark invoice as paid", description = "Marks an invoice as paid and records payment method")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice marked as paid successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found"),
            @ApiResponse(responseCode = "400", description = "Invoice is already paid")
    })
    public ResponseEntity<InvoiceDTO> markInvoiceAsPaid(
            @Parameter(description = "Invoice ID", required = true) @PathVariable Long invoiceId,
            @Parameter(description = "Payment method", required = true) @RequestParam String paymentMethod) {
        InvoiceDTO invoice = invoiceService.markInvoiceAsPaid(invoiceId, paymentMethod);
        return ResponseEntity.ok(invoice);
    }
}

