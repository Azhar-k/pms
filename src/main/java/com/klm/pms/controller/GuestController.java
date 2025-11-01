package com.klm.pms.controller;

import com.klm.pms.dto.GuestDTO;
import com.klm.pms.service.GuestService;
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
@RequestMapping("/api/guests")
@Tag(name = "Guest Management", description = "APIs for managing hotel guests")
public class GuestController {

    @Autowired
    private GuestService guestService;

    @PostMapping
    @Operation(summary = "Create a new guest", description = "Creates a new guest record in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Guest created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<GuestDTO> createGuest(@Valid @RequestBody GuestDTO guestDTO) {
        GuestDTO createdGuest = guestService.createGuest(guestDTO);
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
        GuestDTO guest = guestService.getGuestById(id);
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
        GuestDTO guest = guestService.getGuestByEmail(email);
        return ResponseEntity.ok(guest);
    }

    @GetMapping
    @Operation(summary = "Get all guests", description = "Retrieves a list of all guests in the system")
    @ApiResponse(responseCode = "200", description = "List of guests retrieved successfully")
    public ResponseEntity<List<GuestDTO>> getAllGuests() {
        List<GuestDTO> guests = guestService.getAllGuests();
        return ResponseEntity.ok(guests);
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
        GuestDTO updatedGuest = guestService.updateGuest(id, guestDTO);
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
        guestService.deleteGuest(id);
        return ResponseEntity.noContent().build();
    }
}

