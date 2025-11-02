package com.klm.pms.controller;

import com.klm.pms.dto.GuestDTO;
import com.klm.pms.dto.GuestFilterRequest;
import com.klm.pms.dto.PageResponse;
import com.klm.pms.service.GuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/guests")
@Tag(name = "Guest Management", description = "APIs for managing hotel guests")
public class GuestController {

    private static final Logger logger = LoggerFactory.getLogger(GuestController.class);

    @Autowired
    private GuestService guestService;

    @PostMapping
    @Operation(summary = "Create a new guest", description = "Creates a new guest record in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Guest created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<GuestDTO> createGuest(@Valid @RequestBody GuestDTO guestDTO) {
        logger.info("POST /api/guests - Creating new guest with email: {}", guestDTO.getEmail());
        GuestDTO createdGuest = guestService.createGuest(guestDTO);
        logger.info("POST /api/guests - Successfully created guest with ID: {}", createdGuest.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGuest);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get guest by ID", description = "Retrieves a guest by their unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Guest found"),
            @ApiResponse(responseCode = "404", description = "Guest not found")
    })
    public ResponseEntity<GuestDTO> getGuestById(
            @Parameter(description = "Guest ID", required = true) @PathVariable Long id) {
        logger.info("GET /api/guests/{} - Fetching guest by ID", id);
        GuestDTO guest = guestService.getGuestById(id);
        logger.info("GET /api/guests/{} - Successfully retrieved guest", id);
        return ResponseEntity.ok(guest);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get guest by email", description = "Retrieves a guest by their email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Guest found"),
            @ApiResponse(responseCode = "404", description = "Guest not found")
    })
    public ResponseEntity<GuestDTO> getGuestByEmail(
            @Parameter(description = "Guest email", required = true) @PathVariable String email) {
        logger.info("GET /api/guests/email/{} - Fetching guest by email", email);
        GuestDTO guest = guestService.getGuestByEmail(email);
        logger.info("GET /api/guests/email/{} - Successfully retrieved guest", email);
        return ResponseEntity.ok(guest);
    }

    @GetMapping
    @Operation(summary = "Get all guests", description = "Retrieves a list of all guests in the system with optional pagination, sorting, and filtering")
    @ApiResponse(responseCode = "200", description = "List of guests retrieved successfully")
    public ResponseEntity<?> getAllGuests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "10") Integer size,
            @Parameter(description = "Sort by field (e.g., lastName, firstName, email)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(required = false, defaultValue = "asc") String sortDir,
            @Parameter(description = "First name filter") @RequestParam(required = false) String firstName,
            @Parameter(description = "Last name filter") @RequestParam(required = false) String lastName,
            @Parameter(description = "Email filter") @RequestParam(required = false) String email,
            @Parameter(description = "Phone number filter") @RequestParam(required = false) String phoneNumber,
            @Parameter(description = "City filter") @RequestParam(required = false) String city,
            @Parameter(description = "State filter") @RequestParam(required = false) String state,
            @Parameter(description = "Country filter") @RequestParam(required = false) String country,
            @Parameter(description = "Identification type filter") @RequestParam(required = false) String identificationType,
            @Parameter(description = "Search term for name, email, phone, address") @RequestParam(required = false) String searchTerm) {
        
        // If pagination or filter parameters are provided, use paginated endpoint
        if (page != null || size != null || sortBy != null || sortDir != null || 
            firstName != null || lastName != null || email != null || 
            phoneNumber != null || city != null || state != null ||
            country != null || identificationType != null || searchTerm != null) {
            
            // Build filter request
            GuestFilterRequest filter = new GuestFilterRequest();
            filter.setFirstName(firstName);
            filter.setLastName(lastName);
            filter.setEmail(email);
            filter.setPhoneNumber(phoneNumber);
            filter.setCity(city);
            filter.setState(state);
            filter.setCountry(country);
            filter.setIdentificationType(identificationType);
            filter.setSearchTerm(searchTerm);
            
            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 10;
            
            logger.info("GET /api/guests - Fetching guests with pagination - page: {}, size: {}", pageNum, pageSize);
            PageResponse<GuestDTO> response = guestService.getAllGuestsPaginated(filter, pageNum, pageSize, sortBy, sortDir);
            logger.info("GET /api/guests - Retrieved {} guest(s) out of {} total", response.getContent().size(), response.getTotalElements());
            return ResponseEntity.ok(response);
        } else {
            // Use non-paginated endpoint for backward compatibility
            logger.info("GET /api/guests - Fetching all guests");
            List<GuestDTO> guests = guestService.getAllGuests();
            logger.info("GET /api/guests - Retrieved {} guest(s)", guests.size());
            return ResponseEntity.ok(guests);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update guest", description = "Updates an existing guest's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Guest updated successfully"),
            @ApiResponse(responseCode = "404", description = "Guest not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<GuestDTO> updateGuest(
            @Parameter(description = "Guest ID", required = true) @PathVariable Long id,
            @Valid @RequestBody GuestDTO guestDTO) {
        logger.info("PUT /api/guests/{} - Updating guest", id);
        GuestDTO updatedGuest = guestService.updateGuest(id, guestDTO);
        logger.info("PUT /api/guests/{} - Successfully updated guest", id);
        return ResponseEntity.ok(updatedGuest);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete guest", description = "Deletes a guest from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Guest deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Guest not found")
    })
    public ResponseEntity<Void> deleteGuest(
            @Parameter(description = "Guest ID", required = true) @PathVariable Long id) {
        logger.info("DELETE /api/guests/{} - Deleting guest", id);
        guestService.deleteGuest(id);
        logger.info("DELETE /api/guests/{} - Successfully deleted guest", id);
        return ResponseEntity.noContent().build();
    }
}

