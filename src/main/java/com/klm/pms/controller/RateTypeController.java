package com.klm.pms.controller;

import com.klm.pms.dto.RateTypeDTO;
import com.klm.pms.service.RateTypeService;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/rate-types")
@Tag(name = "Rate Type Management", description = "APIs for managing rate types and room type rates")
public class RateTypeController {

    @Autowired
    private RateTypeService rateTypeService;

    @PostMapping
    @Operation(summary = "Create a new rate type", description = "Creates a new rate type with room type rates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rate type created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or rate type name already exists")
    })
    public ResponseEntity<RateTypeDTO> createRateType(@Valid @RequestBody RateTypeDTO rateTypeDTO) {
        RateTypeDTO createdRateType = rateTypeService.createRateType(rateTypeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRateType);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get rate type by ID", description = "Retrieves a rate type with all its room type rates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rate type found"),
            @ApiResponse(responseCode = "404", description = "Rate type not found")
    })
    public ResponseEntity<RateTypeDTO> getRateTypeById(
            @Parameter(description = "Rate type ID", required = true) @PathVariable Long id) {
        RateTypeDTO rateType = rateTypeService.getRateTypeById(id);
        return ResponseEntity.ok(rateType);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get rate type by name", description = "Retrieves a rate type by its name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rate type found"),
            @ApiResponse(responseCode = "404", description = "Rate type not found")
    })
    public ResponseEntity<RateTypeDTO> getRateTypeByName(
            @Parameter(description = "Rate type name", required = true) @PathVariable String name) {
        RateTypeDTO rateType = rateTypeService.getRateTypeByName(name);
        return ResponseEntity.ok(rateType);
    }

    @GetMapping
    @Operation(summary = "Get all rate types", description = "Retrieves a list of all rate types in the system")
    @ApiResponse(responseCode = "200", description = "List of rate types retrieved successfully")
    public ResponseEntity<List<RateTypeDTO>> getAllRateTypes() {
        List<RateTypeDTO> rateTypes = rateTypeService.getAllRateTypes();
        return ResponseEntity.ok(rateTypes);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update rate type", description = "Updates an existing rate type's basic information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rate type updated successfully"),
            @ApiResponse(responseCode = "404", description = "Rate type not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or rate type name already exists")
    })
    public ResponseEntity<RateTypeDTO> updateRateType(
            @Parameter(description = "Rate type ID", required = true) @PathVariable Long id,
            @Valid @RequestBody RateTypeDTO rateTypeDTO) {
        RateTypeDTO updatedRateType = rateTypeService.updateRateType(id, rateTypeDTO);
        return ResponseEntity.ok(updatedRateType);
    }

    @PostMapping("/{rateTypeId}/room-type-rates")
    @Operation(summary = "Add room type rate", description = "Adds a rate for a specific room type to a rate type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room type rate added successfully"),
            @ApiResponse(responseCode = "404", description = "Rate type or room type not found"),
            @ApiResponse(responseCode = "400", description = "Rate already exists for this room type")
    })
    public ResponseEntity<RateTypeDTO> addRoomTypeRate(
            @Parameter(description = "Rate type ID", required = true) @PathVariable Long rateTypeId,
            @Valid @RequestBody RateTypeDTO.RoomTypeRateDTO roomTypeRateDTO) {
        RateTypeDTO rateType = rateTypeService.addRoomTypeRate(rateTypeId, roomTypeRateDTO);
        return ResponseEntity.ok(rateType);
    }

    @PutMapping("/{rateTypeId}/room-type-rates/{roomTypeId}")
    @Operation(summary = "Update room type rate", description = "Updates the rate for a specific room type in a rate type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room type rate updated successfully"),
            @ApiResponse(responseCode = "404", description = "Rate not found"),
            @ApiResponse(responseCode = "400", description = "Invalid rate value")
    })
    public ResponseEntity<RateTypeDTO> updateRoomTypeRate(
            @Parameter(description = "Rate type ID", required = true) @PathVariable Long rateTypeId,
            @Parameter(description = "Room type ID", required = true) @PathVariable Long roomTypeId,
            @Parameter(description = "New rate value", required = true) @RequestParam BigDecimal rate) {
        RateTypeDTO rateType = rateTypeService.updateRoomTypeRate(rateTypeId, roomTypeId, rate);
        return ResponseEntity.ok(rateType);
    }

    @DeleteMapping("/{rateTypeId}/room-type-rates/{roomTypeId}")
    @Operation(summary = "Remove room type rate", description = "Removes a rate for a specific room type from a rate type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Room type rate removed successfully"),
            @ApiResponse(responseCode = "404", description = "Rate not found")
    })
    public ResponseEntity<Void> removeRoomTypeRate(
            @Parameter(description = "Rate type ID", required = true) @PathVariable Long rateTypeId,
            @Parameter(description = "Room type ID", required = true) @PathVariable Long roomTypeId) {
        rateTypeService.removeRoomTypeRate(rateTypeId, roomTypeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{rateTypeId}/room-type-rates/{roomTypeId}")
    @Operation(summary = "Get rate for room type", description = "Retrieves the rate for a specific room type in a rate type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rate found"),
            @ApiResponse(responseCode = "404", description = "Rate not found")
    })
    public ResponseEntity<BigDecimal> getRateForRoomType(
            @Parameter(description = "Rate type ID", required = true) @PathVariable Long rateTypeId,
            @Parameter(description = "Room type ID", required = true) @PathVariable Long roomTypeId) {
        BigDecimal rate = rateTypeService.getRateForRoomType(rateTypeId, roomTypeId);
        return ResponseEntity.ok(rate);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete rate type", description = "Deletes a rate type from the system. Cannot delete if reservations are using this rate type.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rate type deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Rate type not found"),
            @ApiResponse(responseCode = "400", description = "Cannot delete rate type as it is in use by reservations")
    })
    public ResponseEntity<Void> deleteRateType(
            @Parameter(description = "Rate type ID", required = true) @PathVariable Long id) {
        rateTypeService.deleteRateType(id);
        return ResponseEntity.noContent().build();
    }
}

