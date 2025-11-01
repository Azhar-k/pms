package com.klm.pms.controller;

import com.klm.pms.dto.ReservationDTO;
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
    @Operation(summary = "Get all reservations", description = "Retrieves a list of all reservations in the system")
    @ApiResponse(responseCode = "200", description = "List of reservations retrieved successfully")
    public ResponseEntity<List<ReservationDTO>> getAllReservations() {
        logger.info("GET /api/reservations - Fetching all reservations");
        List<ReservationDTO> reservations = reservationService.getAllReservations();
        logger.info("GET /api/reservations - Retrieved {} reservation(s)", reservations.size());
        return ResponseEntity.ok(reservations);
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

