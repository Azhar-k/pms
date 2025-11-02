package com.klm.pms.service;

import com.klm.pms.dto.PageResponse;
import com.klm.pms.dto.RoomDTO;
import com.klm.pms.dto.RoomFilterRequest;
import com.klm.pms.mapper.RoomMapper;
import com.klm.pms.model.Reservation;
import com.klm.pms.model.Room;
import com.klm.pms.model.Room.RoomStatus;
import com.klm.pms.model.RoomType;
import com.klm.pms.repository.ReservationRepository;
import com.klm.pms.repository.RoomRepository;
import com.klm.pms.repository.RoomTypeRepository;
import com.klm.pms.repository.specification.RoomSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomService {

    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private ReservationRepository reservationRepository;

    public RoomDTO createRoom(RoomDTO roomDTO) {
        logger.info("Creating new room with number: {} and room type ID: {}", roomDTO.getRoomNumber(), roomDTO.getRoomTypeId());
        
        // Check if room number already exists
        if (roomRepository.findByRoomNumber(roomDTO.getRoomNumber()).isPresent()) {
            logger.warn("Failed to create room: Room number {} already exists", roomDTO.getRoomNumber());
            throw new RuntimeException("Room with number " + roomDTO.getRoomNumber() + " already exists");
        }
        
        // Fetch and validate room type
        RoomType roomType = roomTypeRepository.findById(roomDTO.getRoomTypeId())
                .orElseThrow(() -> {
                    logger.error("Room type not found with ID: {}", roomDTO.getRoomTypeId());
                    return new RuntimeException("Room type not found with id: " + roomDTO.getRoomTypeId());
                });
        
        logger.debug("Room type validation passed: {}", roomType.getName());
        
        Room room = roomMapper.toEntity(roomDTO);
        room.setRoomType(roomType);
        Room savedRoom = roomRepository.save(room);
        logger.info("Successfully created room with ID: {} and number: {}", savedRoom.getId(), savedRoom.getRoomNumber());
        return roomMapper.toDTO(savedRoom);
    }

    public RoomDTO updateRoom(Long id, RoomDTO roomDTO) {
        logger.info("Updating room with ID: {}", id);
        
        Room existingRoom = roomRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Room not found with ID: {}", id);
                    return new RuntimeException("Room not found with id: " + id);
                });
        
        // Check room number uniqueness if it's being changed
        if (!roomDTO.getRoomNumber().equals(existingRoom.getRoomNumber())) {
            if (roomRepository.findByRoomNumber(roomDTO.getRoomNumber()).isPresent()) {
                logger.warn("Failed to update room ID {}: Room number {} already exists", id, roomDTO.getRoomNumber());
                throw new RuntimeException("Room with number " + roomDTO.getRoomNumber() + " already exists");
            }
            logger.debug("Room number changed from {} to {}", existingRoom.getRoomNumber(), roomDTO.getRoomNumber());
        }
        
        // Fetch and validate room type if it's being changed
        if (roomDTO.getRoomTypeId() != null && 
            (existingRoom.getRoomType() == null || !roomDTO.getRoomTypeId().equals(existingRoom.getRoomType().getId()))) {
            RoomType roomType = roomTypeRepository.findById(roomDTO.getRoomTypeId())
                    .orElseThrow(() -> {
                        logger.error("Room type not found with ID: {}", roomDTO.getRoomTypeId());
                        return new RuntimeException("Room type not found with id: " + roomDTO.getRoomTypeId());
                    });
            existingRoom.setRoomType(roomType);
            logger.debug("Room type changed to: {}", roomType.getName());
        }
        
        existingRoom.setRoomNumber(roomDTO.getRoomNumber());
        if (roomDTO.getStatus() != null) {
            existingRoom.setStatus(roomDTO.getStatus());
            logger.debug("Room status changed to: {}", roomDTO.getStatus());
        }
        existingRoom.setMaxOccupancy(roomDTO.getMaxOccupancy());
        existingRoom.setAmenities(roomDTO.getAmenities());
        existingRoom.setDescription(roomDTO.getDescription());
        existingRoom.setFloor(roomDTO.getFloor());
        existingRoom.setHasBalcony(roomDTO.getHasBalcony());
        existingRoom.setHasView(roomDTO.getHasView());
        
        Room updatedRoom = roomRepository.save(existingRoom);
        logger.info("Successfully updated room with ID: {}", id);
        return roomMapper.toDTO(updatedRoom);
    }

    @Transactional(readOnly = true)
    public RoomDTO getRoomById(Long id) {
        logger.debug("Fetching room with ID: {}", id);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Room not found with ID: {}", id);
                    return new RuntimeException("Room not found with id: " + id);
                });
        logger.debug("Successfully retrieved room with ID: {}", id);
        return roomMapper.toDTO(room);
    }

    @Transactional(readOnly = true)
    public RoomDTO getRoomByNumber(String roomNumber) {
        logger.debug("Fetching room with number: {}", roomNumber);
        Room room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> {
                    logger.error("Room not found with number: {}", roomNumber);
                    return new RuntimeException("Room not found with number: " + roomNumber);
                });
        logger.debug("Successfully retrieved room with number: {}", roomNumber);
        return roomMapper.toDTO(room);
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getAllRooms() {
        logger.debug("Fetching all rooms");
        List<RoomDTO> rooms = roomRepository.findAll().stream()
                .map(roomMapper::toDTO)
                .collect(Collectors.toList());
        logger.info("Retrieved {} room(s)", rooms.size());
        return rooms;
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomDTO> getAllRoomsPaginated(RoomFilterRequest filter, int page, int size, String sortBy, String sortDir) {
        logger.debug("Fetching rooms with pagination - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
        
        // Default sorting
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = sortDir != null && sortDir.equalsIgnoreCase("desc") 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
            sort = Sort.by(direction, sortBy);
        }
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Build specification for filtering
        Specification<Room> spec = RoomSpecification.withFilters(filter);
        
        Page<Room> roomPage = roomRepository.findAll(spec, pageable);
        
        List<RoomDTO> roomDTOs = roomPage.getContent().stream()
                .map(roomMapper::toDTO)
                .collect(Collectors.toList());
        
        PageResponse<RoomDTO> response = new PageResponse<>(
            roomDTOs,
            roomPage.getNumber(),
            roomPage.getSize(),
            roomPage.getTotalElements()
        );
        
        logger.info("Retrieved {} room(s) out of {} total", roomDTOs.size(), roomPage.getTotalElements());
        return response;
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getAvailableRooms() {
        logger.debug("Fetching available rooms");
        // Default to today for check-in and check-out + 1 day
        LocalDate checkInDate = LocalDate.now();
        LocalDate checkOutDate = checkInDate.plusDays(1);
        return getAvailableRoomsForDateRange(checkInDate, checkOutDate);
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getAvailableRoomsForDateRange(LocalDate checkInDate, LocalDate checkOutDate) {
        logger.debug("Fetching available rooms for date range: {} to {}", checkInDate, checkOutDate);
        
        if (checkInDate.isAfter(checkOutDate)) {
            logger.warn("Invalid date range: check-in date {} is after check-out date {}", checkInDate, checkOutDate);
            throw new RuntimeException("Check-in date must be before check-out date");
        }
        
        // Get all rooms that are in READY or CLEANING status (not MAINTENANCE)
        List<Room> availableStatusRooms = roomRepository.findAll().stream()
                .filter(room -> room.getStatus() == RoomStatus.READY || room.getStatus() == RoomStatus.CLEANING)
                .collect(Collectors.toList());
        logger.debug("Found {} room(s) with READY or CLEANING status", availableStatusRooms.size());
        
        // Find all rooms that have conflicting reservations for the given date range
        Set<Long> occupiedRoomIds = availableStatusRooms.stream()
                .filter(room -> {
                    List<Reservation> conflictingReservations = reservationRepository.findConflictingReservations(
                            room.getId(), checkInDate, checkOutDate);
                    return !conflictingReservations.isEmpty();
                })
                .map(Room::getId)
                .collect(Collectors.toSet());
        
        logger.debug("Found {} room(s) with conflicting reservations", occupiedRoomIds.size());
        
        // Filter out rooms with conflicts
        List<RoomDTO> availableRooms = availableStatusRooms.stream()
                .filter(room -> !occupiedRoomIds.contains(room.getId()))
                .map(roomMapper::toDTO)
                .collect(Collectors.toList());
        
        logger.info("Retrieved {} available room(s) for date range {} to {}", 
                availableRooms.size(), checkInDate, checkOutDate);
        return availableRooms;
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getRoomsByType(Long roomTypeId) {
        logger.debug("Fetching rooms for room type ID: {}", roomTypeId);
        // Validate room type exists
        if (!roomTypeRepository.existsById(roomTypeId)) {
            logger.error("Room type not found with ID: {}", roomTypeId);
            throw new RuntimeException("Room type not found with id: " + roomTypeId);
        }
        List<RoomDTO> rooms = roomRepository.findByRoomTypeId(roomTypeId).stream()
                .map(roomMapper::toDTO)
                .collect(Collectors.toList());
        logger.info("Retrieved {} room(s) for room type ID: {}", rooms.size(), roomTypeId);
        return rooms;
    }

    public void deleteRoom(Long id) {
        logger.info("Deleting room with ID: {}", id);
        if (!roomRepository.existsById(id)) {
            logger.error("Failed to delete: Room not found with ID: {}", id);
            throw new RuntimeException("Room not found with id: " + id);
        }
        roomRepository.deleteById(id);
        logger.info("Successfully deleted room with ID: {}", id);
    }
}

