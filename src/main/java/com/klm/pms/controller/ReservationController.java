package com.klm.pms.controller;

import com.klm.pms.dto.PageResponse;
import com.klm.pms.dto.ReservationDTO;
import com.klm.pms.dto.ReservationFilterRequest;
import com.klm.pms.model.Reservation.ReservationStatus;
import com.klm.pms.service.ReservationService;
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

import java.time.LocalDate;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservation Management", description = "APIs for managing hotel reservations, check-in, and check-out")
public class ReservationController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Create a new reservation", description = "Creates a new reservation for a guest")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reservation created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or room not available")
    })
    public ResponseEntity<ReservationDTO> createReservation(@Valid @RequestBody ReservationDTO reservationDTO) {
        logger.info("POST /api/reservations - Creating new reservation for guest ID: {}, room ID: {}", 
                reservationDTO.getGuestId(), reservationDTO.getRoomId());
        ReservationDTO createdReservation = reservationService.createReservation(reservationDTO);
        logger.info("POST /api/reservations - Successfully created reservation with ID: {}", createdReservation.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReservation);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID", description = "Retrieves a reservation by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation found"),
            @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<ReservationDTO> getReservationById(
            @Parameter(description = "Reservation ID", required = true) @PathVariable Long id) {
        logger.info("GET /api/reservations/{} - Fetching reservation by ID", id);
        ReservationDTO reservation = reservationService.getReservationById(id);
        logger.info("GET /api/reservations/{} - Successfully retrieved reservation", id);
        return ResponseEntity.ok(reservation);
    }

    @GetMapping("/number/{reservationNumber}")
    @Operation(summary = "Get reservation by reservation number", description = "Retrieves a reservation by its reservation number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation found"),
            @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<ReservationDTO> getReservationByNumber(
            @Parameter(description = "Reservation number", required = true) @PathVariable String reservationNumber) {
        logger.info("GET /api/reservations/number/{} - Fetching reservation by number", reservationNumber);
        ReservationDTO reservation = reservationService.getReservationByNumber(reservationNumber);
        logger.info("GET /api/reservations/number/{} - Successfully retrieved reservation", reservationNumber);
        return ResponseEntity.ok(reservation);
    }

    @GetMapping
    @Operation(summary = "Get all reservations", description = "Retrieves a list of all reservations in the system with optional pagination, sorting, and filtering")
    @ApiResponse(responseCode = "200", description = "List of reservations retrieved successfully")
    public ResponseEntity<?> getAllReservations(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "10") Integer size,
            @Parameter(description = "Sort by field (e.g., checkInDate, createdAt, status)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @Parameter(description = "Reservation number filter") @RequestParam(required = false) String reservationNumber,
            @Parameter(description = "Guest ID filter") @RequestParam(required = false) Long guestId,
            @Parameter(description = "Room ID filter") @RequestParam(required = false) Long roomId,
            @Parameter(description = "Rate type ID filter") @RequestParam(required = false) Long rateTypeId,
            @Parameter(description = "Status filter") @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Check-in date from (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDateFrom,
            @Parameter(description = "Check-in date to (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDateTo,
            @Parameter(description = "Check-out date from (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDateFrom,
            @Parameter(description = "Check-out date to (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDateTo,
            @Parameter(description = "Minimum number of guests") @RequestParam(required = false) Integer minNumberOfGuests,
            @Parameter(description = "Maximum number of guests") @RequestParam(required = false) Integer maxNumberOfGuests,
            @Parameter(description = "Payment status filter") @RequestParam(required = false) String paymentStatus,
            @Parameter(description = "Search term for reservation number, special requests") @RequestParam(required = false) String searchTerm) {
        
        // If pagination or filter parameters are provided, use paginated endpoint
        if (page != null || size != null || sortBy != null || sortDir != null || 
            reservationNumber != null || guestId != null || roomId != null || 
            rateTypeId != null || status != null || checkInDateFrom != null ||
            checkInDateTo != null || checkOutDateFrom != null || checkOutDateTo != null ||
            minNumberOfGuests != null || maxNumberOfGuests != null || paymentStatus != null ||
            searchTerm != null) {
            
            // Build filter request
            ReservationFilterRequest filter = new ReservationFilterRequest();
            filter.setReservationNumber(reservationNumber);
            filter.setGuestId(guestId);
            filter.setRoomId(roomId);
            filter.setRateTypeId(rateTypeId);
            filter.setStatus(status);
            filter.setCheckInDateFrom(checkInDateFrom);
            filter.setCheckInDateTo(checkInDateTo);
            filter.setCheckOutDateFrom(checkOutDateFrom);
            filter.setCheckOutDateTo(checkOutDateTo);
            filter.setMinNumberOfGuests(minNumberOfGuests);
            filter.setMaxNumberOfGuests(maxNumberOfGuests);
            filter.setPaymentStatus(paymentStatus);
            filter.setSearchTerm(searchTerm);
            
            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 10;
            
            logger.info("GET /api/reservations - Fetching reservations with pagination - page: {}, size: {}", pageNum, pageSize);
            PageResponse<ReservationDTO> response = reservationService.getAllReservationsPaginated(filter, pageNum, pageSize, sortBy, sortDir);
            logger.info("GET /api/reservations - Retrieved {} reservation(s) out of {} total", response.getContent().size(), response.getTotalElements());
            return ResponseEntity.ok(response);
        } else {
            // Use non-paginated endpoint for backward compatibility
            logger.info("GET /api/reservations - Fetching all reservations");
            List<ReservationDTO> reservations = reservationService.getAllReservations();
            logger.info("GET /api/reservations - Retrieved {} reservation(s)", reservations.size());
            return ResponseEntity.ok(reservations);
        }
    }

    @GetMapping("/guest/{guestId}")
    @Operation(summary = "Get reservations by guest", description = "Retrieves all reservations for a specific guest")
    @ApiResponse(responseCode = "200", description = "List of reservations retrieved successfully")
    public ResponseEntity<List<ReservationDTO>> getReservationsByGuest(
            @Parameter(description = "Guest ID", required = true) @PathVariable Long guestId) {
        logger.info("GET /api/reservations/guest/{} - Fetching reservations by guest", guestId);
        List<ReservationDTO> reservations = reservationService.getReservationsByGuest(guestId);
        logger.info("GET /api/reservations/guest/{} - Retrieved {} reservation(s)", guestId, reservations.size());
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get reservations by status", description = "Retrieves all reservations with a specific status")
    @ApiResponse(responseCode = "200", description = "List of reservations retrieved successfully")
    public ResponseEntity<List<ReservationDTO>> getReservationsByStatus(
            @Parameter(description = "Reservation status", required = true) @PathVariable ReservationStatus status) {
        logger.info("GET /api/reservations/status/{} - Fetching reservations by status", status);
        List<ReservationDTO> reservations = reservationService.getReservationsByStatus(status);
        logger.info("GET /api/reservations/status/{} - Retrieved {} reservation(s)", status, reservations.size());
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get reservations by date range", description = "Retrieves all reservations within a date range")
    @ApiResponse(responseCode = "200", description = "List of reservations retrieved successfully")
    public ResponseEntity<List<ReservationDTO>> getReservationsByDateRange(
            @Parameter(description = "Start date (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        logger.info("GET /api/reservations/date-range - Fetching reservations from {} to {}", startDate, endDate);
        List<ReservationDTO> reservations = reservationService.getReservationsByDateRange(startDate, endDate);
        logger.info("GET /api/reservations/date-range - Retrieved {} reservation(s)", reservations.size());
        return ResponseEntity.ok(reservations);
    }

    @PostMapping("/{id}/check-in")
    @Operation(summary = "Check in a guest", description = "Performs check-in for a reservation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check-in successful"),
            @ApiResponse(responseCode = "404", description = "Reservation not found"),
            @ApiResponse(responseCode = "400", description = "Invalid reservation status for check-in")
    })
    public ResponseEntity<ReservationDTO> checkIn(
            @Parameter(description = "Reservation ID", required = true) @PathVariable Long id) {
        logger.info("POST /api/reservations/{}/check-in - Processing check-in", id);
        ReservationDTO reservation = reservationService.checkIn(id);
        logger.info("POST /api/reservations/{}/check-in - Successfully checked in", id);
        return ResponseEntity.ok(reservation);
    }

    @PostMapping("/{id}/check-out")
    @Operation(summary = "Check out a guest", description = "Performs check-out for a reservation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check-out successful"),
            @ApiResponse(responseCode = "404", description = "Reservation not found"),
            @ApiResponse(responseCode = "400", description = "Reservation must be checked in before checkout")
    })
    public ResponseEntity<ReservationDTO> checkOut(
            @Parameter(description = "Reservation ID", required = true) @PathVariable Long id) {
        logger.info("POST /api/reservations/{}/check-out - Processing check-out", id);
        ReservationDTO reservation = reservationService.checkOut(id);
        logger.info("POST /api/reservations/{}/check-out - Successfully checked out", id);
        return ResponseEntity.ok(reservation);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a reservation", description = "Updates an existing reservation. Allows changing room, dates, guest, rate type, and other details. Cannot update checked-out reservations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation updated successfully"),
            @ApiResponse(responseCode = "404", description = "Reservation not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data, room not available, or cannot update checked-out reservation")
    })
    public ResponseEntity<ReservationDTO> updateReservation(
            @Parameter(description = "Reservation ID", required = true) @PathVariable Long id,
            @Valid @RequestBody ReservationDTO reservationDTO) {
        logger.info("PUT /api/reservations/{} - Updating reservation", id);
        ReservationDTO updatedReservation = reservationService.updateReservation(id, reservationDTO);
        logger.info("PUT /api/reservations/{} - Successfully updated reservation", id);
        return ResponseEntity.ok(updatedReservation);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a reservation", description = "Cancels an existing reservation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Reservation not found"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel a checked-out reservation")
    })
    public ResponseEntity<ReservationDTO> cancelReservation(
            @Parameter(description = "Reservation ID", required = true) @PathVariable Long id) {
        logger.info("POST /api/reservations/{}/cancel - Cancelling reservation", id);
        ReservationDTO reservation = reservationService.cancelReservation(id);
        logger.info("POST /api/reservations/{}/cancel - Successfully cancelled reservation", id);
        return ResponseEntity.ok(reservation);
    }
}

