package com.klm.pms.controller;

import com.klm.pms.dto.RoomDTO;
import com.klm.pms.service.RoomService;
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

import java.time.LocalDate;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Room Management", description = "APIs for managing hotel rooms")
public class RoomController {

    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);

    @Autowired
    private RoomService roomService;

    @PostMapping
    @Operation(summary = "Create a new room", description = "Creates a new room record in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Room created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody RoomDTO roomDTO) {
        logger.info("POST /api/rooms - Creating new room with number: {}", roomDTO.getRoomNumber());
        RoomDTO createdRoom = roomService.createRoom(roomDTO);
        logger.info("POST /api/rooms - Successfully created room with ID: {}", createdRoom.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRoom);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID", description = "Retrieves a room by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room found"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ResponseEntity<RoomDTO> getRoomById(
            @Parameter(description = "Room ID", required = true) @PathVariable Long id) {
        logger.info("GET /api/rooms/{} - Fetching room by ID", id);
        RoomDTO room = roomService.getRoomById(id);
        logger.info("GET /api/rooms/{} - Successfully retrieved room", id);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/number/{roomNumber}")
    @Operation(summary = "Get room by room number", description = "Retrieves a room by its room number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room found"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ResponseEntity<RoomDTO> getRoomByNumber(
            @Parameter(description = "Room number", required = true) @PathVariable String roomNumber) {
        logger.info("GET /api/rooms/number/{} - Fetching room by number", roomNumber);
        RoomDTO room = roomService.getRoomByNumber(roomNumber);
        logger.info("GET /api/rooms/number/{} - Successfully retrieved room", roomNumber);
        return ResponseEntity.ok(room);
    }

    @GetMapping
    @Operation(summary = "Get all rooms", description = "Retrieves a list of all rooms in the system")
    @ApiResponse(responseCode = "200", description = "List of rooms retrieved successfully")
    public ResponseEntity<List<RoomDTO>> getAllRooms() {
        logger.info("GET /api/rooms - Fetching all rooms");
        List<RoomDTO> rooms = roomService.getAllRooms();
        logger.info("GET /api/rooms - Retrieved {} room(s)", rooms.size());
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/available")
    @Operation(summary = "Get available rooms", description = "Retrieves a list of available rooms for today (defaults to today and tomorrow). Room availability is determined by date range and reservation conflicts, not just room status.")
    @ApiResponse(responseCode = "200", description = "List of available rooms retrieved successfully")
    public ResponseEntity<List<RoomDTO>> getAvailableRooms() {
        logger.info("GET /api/rooms/available - Fetching available rooms");
        List<RoomDTO> rooms = roomService.getAvailableRooms();
        logger.info("GET /api/rooms/available - Retrieved {} available room(s)", rooms.size());
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/available/range")
    @Operation(summary = "Get available rooms for date range", description = "Retrieves a list of available rooms for a specific date range. Room availability is determined by checking room status (must be READY) and reservation conflicts for the given date range.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of available rooms retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date range")
    })
    public ResponseEntity<List<RoomDTO>> getAvailableRoomsForDateRange(
            @Parameter(description = "Check-in date (format: yyyy-MM-dd)", required = true) 
            @RequestParam LocalDate checkInDate,
            @Parameter(description = "Check-out date (format: yyyy-MM-dd)", required = true) 
            @RequestParam LocalDate checkOutDate) {
        logger.info("GET /api/rooms/available/range - Fetching available rooms for date range: {} to {}", checkInDate, checkOutDate);
        List<RoomDTO> rooms = roomService.getAvailableRoomsForDateRange(checkInDate, checkOutDate);
        logger.info("GET /api/rooms/available/range - Retrieved {} available room(s) for date range {} to {}", 
                rooms.size(), checkInDate, checkOutDate);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/type/{roomTypeId}")
    @Operation(summary = "Get rooms by room type", description = "Retrieves all rooms of a specific room type")
    @ApiResponse(responseCode = "200", description = "List of rooms retrieved successfully")
    public ResponseEntity<List<RoomDTO>> getRoomsByType(
            @Parameter(description = "Room type ID", required = true) @PathVariable Long roomTypeId) {
        logger.info("GET /api/rooms/type/{} - Fetching rooms by room type", roomTypeId);
        List<RoomDTO> rooms = roomService.getRoomsByType(roomTypeId);
        logger.info("GET /api/rooms/type/{} - Retrieved {} room(s)", roomTypeId, rooms.size());
        return ResponseEntity.ok(rooms);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update room", description = "Updates an existing room's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room updated successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<RoomDTO> updateRoom(
            @Parameter(description = "Room ID", required = true) @PathVariable Long id,
            @Valid @RequestBody RoomDTO roomDTO) {
        logger.info("PUT /api/rooms/{} - Updating room", id);
        RoomDTO updatedRoom = roomService.updateRoom(id, roomDTO);
        logger.info("PUT /api/rooms/{} - Successfully updated room", id);
        return ResponseEntity.ok(updatedRoom);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete room", description = "Deletes a room from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Room deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ResponseEntity<Void> deleteRoom(
            @Parameter(description = "Room ID", required = true) @PathVariable Long id) {
        logger.info("DELETE /api/rooms/{} - Deleting room", id);
        roomService.deleteRoom(id);
        logger.info("DELETE /api/rooms/{} - Successfully deleted room", id);
        return ResponseEntity.noContent().build();
    }
}

