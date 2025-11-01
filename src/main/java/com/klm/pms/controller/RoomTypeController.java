package com.klm.pms.controller;

import com.klm.pms.dto.RoomTypeDTO;
import com.klm.pms.service.RoomTypeService;
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

@RestController
@RequestMapping("/api/room-types")
@Tag(name = "Room Type Management", description = "APIs for managing hotel room types")
public class RoomTypeController {

    private static final Logger logger = LoggerFactory.getLogger(RoomTypeController.class);

    @Autowired
    private RoomTypeService roomTypeService;

    @PostMapping
    @Operation(summary = "Create a new room type", description = "Creates a new room type definition in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Room type created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or room type name already exists")
    })
    public ResponseEntity<RoomTypeDTO> createRoomType(@Valid @RequestBody RoomTypeDTO roomTypeDTO) {
        logger.info("POST /api/room-types - Creating new room type with name: {}", roomTypeDTO.getName());
        RoomTypeDTO createdRoomType = roomTypeService.createRoomType(roomTypeDTO);
        logger.info("POST /api/room-types - Successfully created room type with ID: {}", createdRoomType.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRoomType);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room type by ID", description = "Retrieves a room type by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room type found"),
            @ApiResponse(responseCode = "404", description = "Room type not found")
    })
    public ResponseEntity<RoomTypeDTO> getRoomTypeById(
            @Parameter(description = "Room type ID", required = true) @PathVariable Long id) {
        logger.info("GET /api/room-types/{} - Fetching room type by ID", id);
        RoomTypeDTO roomType = roomTypeService.getRoomTypeById(id);
        logger.info("GET /api/room-types/{} - Successfully retrieved room type", id);
        return ResponseEntity.ok(roomType);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get room type by name", description = "Retrieves a room type by its name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room type found"),
            @ApiResponse(responseCode = "404", description = "Room type not found")
    })
    public ResponseEntity<RoomTypeDTO> getRoomTypeByName(
            @Parameter(description = "Room type name", required = true) @PathVariable String name) {
        logger.info("GET /api/room-types/name/{} - Fetching room type by name", name);
        RoomTypeDTO roomType = roomTypeService.getRoomTypeByName(name);
        logger.info("GET /api/room-types/name/{} - Successfully retrieved room type", name);
        return ResponseEntity.ok(roomType);
    }

    @GetMapping
    @Operation(summary = "Get all room types", description = "Retrieves a list of all room types in the system")
    @ApiResponse(responseCode = "200", description = "List of room types retrieved successfully")
    public ResponseEntity<List<RoomTypeDTO>> getAllRoomTypes() {
        logger.info("GET /api/room-types - Fetching all room types");
        List<RoomTypeDTO> roomTypes = roomTypeService.getAllRoomTypes();
        logger.info("GET /api/room-types - Retrieved {} room type(s)", roomTypes.size());
        return ResponseEntity.ok(roomTypes);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update room type", description = "Updates an existing room type's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room type updated successfully"),
            @ApiResponse(responseCode = "404", description = "Room type not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or room type name already exists")
    })
    public ResponseEntity<RoomTypeDTO> updateRoomType(
            @Parameter(description = "Room type ID", required = true) @PathVariable Long id,
            @Valid @RequestBody RoomTypeDTO roomTypeDTO) {
        logger.info("PUT /api/room-types/{} - Updating room type", id);
        RoomTypeDTO updatedRoomType = roomTypeService.updateRoomType(id, roomTypeDTO);
        logger.info("PUT /api/room-types/{} - Successfully updated room type", id);
        return ResponseEntity.ok(updatedRoomType);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete room type", description = "Deletes a room type from the system. Cannot delete if rooms are using this type.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Room type deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Room type not found"),
            @ApiResponse(responseCode = "400", description = "Cannot delete room type as it is in use by rooms")
    })
    public ResponseEntity<Void> deleteRoomType(
            @Parameter(description = "Room type ID", required = true) @PathVariable Long id) {
        logger.info("DELETE /api/room-types/{} - Deleting room type", id);
        roomTypeService.deleteRoomType(id);
        logger.info("DELETE /api/room-types/{} - Successfully deleted room type", id);
        return ResponseEntity.noContent().build();
    }
}

