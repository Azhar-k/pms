package com.klm.pms.controller;

import com.klm.pms.dto.RateTypeDTO;
import com.klm.pms.service.RateTypeService;
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

import java.math.BigDecimal;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/rate-types")
@Tag(name = "Rate Type Management", description = "APIs for managing rate types and room type rates")
public class RateTypeController {

    private static final Logger logger = LoggerFactory.getLogger(RateTypeController.class);

    @Autowired
    private RateTypeService rateTypeService;

    @PostMapping
    @Operation(summary = "Create a new rate type", description = "Creates a new rate type with room type rates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rate type created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or rate type name already exists")
    })
    public ResponseEntity<RateTypeDTO> createRateType(@Valid @RequestBody RateTypeDTO rateTypeDTO) {
        logger.info("POST /api/rate-types - Creating new rate type with name: {}", rateTypeDTO.getName());
        RateTypeDTO createdRateType = rateTypeService.createRateType(rateTypeDTO);
        logger.info("POST /api/rate-types - Successfully created rate type with ID: {}", createdRateType.getId());
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
        logger.info("GET /api/rate-types/{} - Fetching rate type by ID", id);
        RateTypeDTO rateType = rateTypeService.getRateTypeById(id);
        logger.info("GET /api/rate-types/{} - Successfully retrieved rate type", id);
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
        logger.info("GET /api/rate-types/name/{} - Fetching rate type by name", name);
        RateTypeDTO rateType = rateTypeService.getRateTypeByName(name);
        logger.info("GET /api/rate-types/name/{} - Successfully retrieved rate type", name);
        return ResponseEntity.ok(rateType);
    }

    @GetMapping
    @Operation(summary = "Get all rate types", description = "Retrieves a list of all rate types in the system")
    @ApiResponse(responseCode = "200", description = "List of rate types retrieved successfully")
    public ResponseEntity<List<RateTypeDTO>> getAllRateTypes() {
        logger.info("GET /api/rate-types - Fetching all rate types");
        List<RateTypeDTO> rateTypes = rateTypeService.getAllRateTypes();
        logger.info("GET /api/rate-types - Retrieved {} rate type(s)", rateTypes.size());
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
        logger.info("PUT /api/rate-types/{} - Updating rate type", id);
        RateTypeDTO updatedRateType = rateTypeService.updateRateType(id, rateTypeDTO);
        logger.info("PUT /api/rate-types/{} - Successfully updated rate type", id);
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
        logger.info("POST /api/rate-types/{}/room-type-rates - Adding room type rate", rateTypeId);
        RateTypeDTO rateType = rateTypeService.addRoomTypeRate(rateTypeId, roomTypeRateDTO);
        logger.info("POST /api/rate-types/{}/room-type-rates - Successfully added room type rate", rateTypeId);
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
        logger.info("PUT /api/rate-types/{}/room-type-rates/{} - Updating room type rate to {}", rateTypeId, roomTypeId, rate);
        RateTypeDTO rateType = rateTypeService.updateRoomTypeRate(rateTypeId, roomTypeId, rate);
        logger.info("PUT /api/rate-types/{}/room-type-rates/{} - Successfully updated room type rate", rateTypeId, roomTypeId);
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
        logger.info("DELETE /api/rate-types/{}/room-type-rates/{} - Removing room type rate", rateTypeId, roomTypeId);
        rateTypeService.removeRoomTypeRate(rateTypeId, roomTypeId);
        logger.info("DELETE /api/rate-types/{}/room-type-rates/{} - Successfully removed room type rate", rateTypeId, roomTypeId);
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
        logger.info("GET /api/rate-types/{}/room-type-rates/{} - Fetching rate for room type", rateTypeId, roomTypeId);
        BigDecimal rate = rateTypeService.getRateForRoomType(rateTypeId, roomTypeId);
        logger.info("GET /api/rate-types/{}/room-type-rates/{} - Retrieved rate: {}", rateTypeId, roomTypeId, rate);
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
        logger.info("DELETE /api/rate-types/{} - Deleting rate type", id);
        rateTypeService.deleteRateType(id);
        logger.info("DELETE /api/rate-types/{} - Successfully deleted rate type", id);
        return ResponseEntity.noContent().build();
    }
}

