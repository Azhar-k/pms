package com.klm.pms.controller;

import com.klm.pms.dto.InvoiceDTO;
import com.klm.pms.dto.InvoiceFilterRequest;
import com.klm.pms.dto.PageResponse;
import com.klm.pms.model.Invoice.InvoiceStatus;
import com.klm.pms.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoice Management", description = "APIs for managing invoices and billing")
public class InvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

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
        logger.info("POST /api/invoices/generate/{} - Generating invoice for reservation", reservationId);
        InvoiceDTO invoice = invoiceService.generateInvoice(reservationId);
        logger.info("POST /api/invoices/generate/{} - Successfully generated invoice with ID: {}", reservationId, invoice.getId());
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
        logger.info("GET /api/invoices/{} - Fetching invoice by ID", id);
        InvoiceDTO invoice = invoiceService.getInvoiceById(id);
        logger.info("GET /api/invoices/{} - Successfully retrieved invoice", id);
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
        logger.info("GET /api/invoices/number/{} - Fetching invoice by number", invoiceNumber);
        InvoiceDTO invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
        logger.info("GET /api/invoices/number/{} - Successfully retrieved invoice", invoiceNumber);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping
    @Operation(summary = "Get all invoices", description = "Retrieves a list of all invoices in the system with optional pagination, sorting, and filtering")
    @ApiResponse(responseCode = "200", description = "List of invoices retrieved successfully")
    public ResponseEntity<?> getAllInvoices(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "10") Integer size,
            @Parameter(description = "Sort by field (e.g., issuedDate, totalAmount, status)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @Parameter(description = "Invoice number filter") @RequestParam(required = false) String invoiceNumber,
            @Parameter(description = "Reservation ID filter") @RequestParam(required = false) Long reservationId,
            @Parameter(description = "Status filter") @RequestParam(required = false) InvoiceStatus status,
            @Parameter(description = "Issued date from (yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime issuedDateFrom,
            @Parameter(description = "Issued date to (yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime issuedDateTo,
            @Parameter(description = "Paid date from (yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime paidDateFrom,
            @Parameter(description = "Paid date to (yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime paidDateTo,
            @Parameter(description = "Due date from (yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDateFrom,
            @Parameter(description = "Due date to (yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDateTo,
            @Parameter(description = "Payment method filter") @RequestParam(required = false) String paymentMethod,
            @Parameter(description = "Search term for invoice number, notes") @RequestParam(required = false) String searchTerm) {
        
        // If pagination or filter parameters are provided, use paginated endpoint
        if (page != null || size != null || sortBy != null || sortDir != null || 
            invoiceNumber != null || reservationId != null || status != null ||
            issuedDateFrom != null || issuedDateTo != null || paidDateFrom != null ||
            paidDateTo != null || dueDateFrom != null || dueDateTo != null ||
            paymentMethod != null || searchTerm != null) {
            
            // Build filter request
            InvoiceFilterRequest filter = new InvoiceFilterRequest();
            filter.setInvoiceNumber(invoiceNumber);
            filter.setReservationId(reservationId);
            filter.setStatus(status);
            filter.setIssuedDateFrom(issuedDateFrom);
            filter.setIssuedDateTo(issuedDateTo);
            filter.setPaidDateFrom(paidDateFrom);
            filter.setPaidDateTo(paidDateTo);
            filter.setDueDateFrom(dueDateFrom);
            filter.setDueDateTo(dueDateTo);
            filter.setPaymentMethod(paymentMethod);
            filter.setSearchTerm(searchTerm);
            
            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 10;
            
            logger.info("GET /api/invoices - Fetching invoices with pagination - page: {}, size: {}", pageNum, pageSize);
            PageResponse<InvoiceDTO> response = invoiceService.getAllInvoicesPaginated(filter, pageNum, pageSize, sortBy, sortDir);
            logger.info("GET /api/invoices - Retrieved {} invoice(s) out of {} total", response.getContent().size(), response.getTotalElements());
            return ResponseEntity.ok(response);
        } else {
            // Use non-paginated endpoint for backward compatibility
            logger.info("GET /api/invoices - Fetching all invoices");
            List<InvoiceDTO> invoices = invoiceService.getAllInvoices();
            logger.info("GET /api/invoices - Retrieved {} invoice(s)", invoices.size());
            return ResponseEntity.ok(invoices);
        }
    }

    @GetMapping("/reservation/{reservationId}")
    @Operation(summary = "Get invoices by reservation", description = "Retrieves all invoices for a specific reservation")
    @ApiResponse(responseCode = "200", description = "List of invoices retrieved successfully")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByReservation(
            @Parameter(description = "Reservation ID", required = true) @PathVariable Long reservationId) {
        logger.info("GET /api/invoices/reservation/{} - Fetching invoices by reservation", reservationId);
        List<InvoiceDTO> invoices = invoiceService.getInvoicesByReservation(reservationId);
        logger.info("GET /api/invoices/reservation/{} - Retrieved {} invoice(s)", reservationId, invoices.size());
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get invoices by status", description = "Retrieves all invoices with a specific status")
    @ApiResponse(responseCode = "200", description = "List of invoices retrieved successfully")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByStatus(
            @Parameter(description = "Invoice status", required = true) @PathVariable InvoiceStatus status) {
        logger.info("GET /api/invoices/status/{} - Fetching invoices by status", status);
        List<InvoiceDTO> invoices = invoiceService.getInvoicesByStatus(status);
        logger.info("GET /api/invoices/status/{} - Retrieved {} invoice(s)", status, invoices.size());
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
        logger.info("POST /api/invoices/{}/items - Adding item to invoice", invoiceId);
        InvoiceDTO invoice = invoiceService.addInvoiceItem(invoiceId, itemDTO);
        logger.info("POST /api/invoices/{}/items - Successfully added item to invoice", invoiceId);
        return ResponseEntity.ok(invoice);
    }

    @DeleteMapping("/{invoiceId}/items/{itemId}")
    @Operation(summary = "Remove item from invoice", description = "Removes an item from an existing invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item removed successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice or invoice item not found"),
            @ApiResponse(responseCode = "400", description = "Cannot remove items from a paid invoice")
    })
    public ResponseEntity<InvoiceDTO> removeInvoiceItem(
            @Parameter(description = "Invoice ID", required = true) @PathVariable Long invoiceId,
            @Parameter(description = "Invoice Item ID", required = true) @PathVariable Long itemId) {
        logger.info("DELETE /api/invoices/{}/items/{} - Removing item from invoice", invoiceId, itemId);
        InvoiceDTO invoice = invoiceService.removeInvoiceItem(invoiceId, itemId);
        logger.info("DELETE /api/invoices/{}/items/{} - Successfully removed item from invoice", invoiceId, itemId);
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
        logger.info("POST /api/invoices/{}/pay - Marking invoice as paid with method: {}", invoiceId, paymentMethod);
        InvoiceDTO invoice = invoiceService.markInvoiceAsPaid(invoiceId, paymentMethod);
        logger.info("POST /api/invoices/{}/pay - Successfully marked invoice as paid", invoiceId);
        return ResponseEntity.ok(invoice);
    }
}

