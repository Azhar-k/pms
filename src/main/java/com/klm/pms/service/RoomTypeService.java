package com.klm.pms.service;

import com.klm.pms.dto.RoomTypeDTO;
import com.klm.pms.mapper.RoomTypeMapper;
import com.klm.pms.model.RoomType;
import com.klm.pms.repository.RoomTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomTypeService {

    private static final Logger logger = LoggerFactory.getLogger(RoomTypeService.class);

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomTypeMapper roomTypeMapper;

    @Autowired
    private AuditService auditService;

    public RoomTypeDTO createRoomType(RoomTypeDTO roomTypeDTO) {
        logger.info("Creating new room type with name: {}", roomTypeDTO.getName());
        
        // Check if room type name already exists
        if (roomTypeRepository.existsByName(roomTypeDTO.getName())) {
            logger.warn("Failed to create room type: Name '{}' already exists", roomTypeDTO.getName());
            throw new RuntimeException("Room type with name '" + roomTypeDTO.getName() + "' already exists");
        }
        
        RoomType roomType = roomTypeMapper.toEntity(roomTypeDTO);
        RoomType savedRoomType = roomTypeRepository.save(roomType);
        logger.info("Successfully created room type with ID: {} and name: {}", savedRoomType.getId(), savedRoomType.getName());
        
        // Audit log
        auditService.logCreate("RoomType", savedRoomType.getId(), savedRoomType);
        
        return roomTypeMapper.toDTO(savedRoomType);
    }

    public RoomTypeDTO updateRoomType(Long id, RoomTypeDTO roomTypeDTO) {
        logger.info("Updating room type with ID: {}", id);
        
        RoomType existingRoomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Room type not found with ID: {}", id);
                    return new RuntimeException("Room type not found with id: " + id);
                });
        
        // Store old state for audit
        RoomType oldRoomType = new RoomType();
        oldRoomType.setId(existingRoomType.getId());
        oldRoomType.setName(existingRoomType.getName());
        oldRoomType.setDescription(existingRoomType.getDescription());
        oldRoomType.setBasePricePerNight(existingRoomType.getBasePricePerNight());
        oldRoomType.setMaxOccupancy(existingRoomType.getMaxOccupancy());
        oldRoomType.setAmenities(existingRoomType.getAmenities());
        oldRoomType.setDefaultRoomSize(existingRoomType.getDefaultRoomSize());
        oldRoomType.setHasBalcony(existingRoomType.getHasBalcony());
        oldRoomType.setHasView(existingRoomType.getHasView());
        oldRoomType.setHasMinibar(existingRoomType.getHasMinibar());
        oldRoomType.setHasSafe(existingRoomType.getHasSafe());
        oldRoomType.setHasAirConditioning(existingRoomType.getHasAirConditioning());
        oldRoomType.setBedType(existingRoomType.getBedType());
        
        // Check name uniqueness if it's being changed
        if (!roomTypeDTO.getName().equals(existingRoomType.getName()) && 
            roomTypeRepository.existsByName(roomTypeDTO.getName())) {
            logger.warn("Failed to update room type ID {}: Name '{}' already exists", id, roomTypeDTO.getName());
            throw new RuntimeException("Room type with name '" + roomTypeDTO.getName() + "' already exists");
        }
        
        existingRoomType.setName(roomTypeDTO.getName());
        existingRoomType.setDescription(roomTypeDTO.getDescription());
        existingRoomType.setBasePricePerNight(roomTypeDTO.getBasePricePerNight());
        existingRoomType.setMaxOccupancy(roomTypeDTO.getMaxOccupancy());
        existingRoomType.setAmenities(roomTypeDTO.getAmenities());
        existingRoomType.setDefaultRoomSize(roomTypeDTO.getDefaultRoomSize());
        existingRoomType.setHasBalcony(roomTypeDTO.getHasBalcony());
        existingRoomType.setHasView(roomTypeDTO.getHasView());
        existingRoomType.setHasMinibar(roomTypeDTO.getHasMinibar());
        existingRoomType.setHasSafe(roomTypeDTO.getHasSafe());
        existingRoomType.setHasAirConditioning(roomTypeDTO.getHasAirConditioning());
        existingRoomType.setBedType(roomTypeDTO.getBedType());
        
        RoomType updatedRoomType = roomTypeRepository.save(existingRoomType);
        logger.info("Successfully updated room type with ID: {}", id);
        
        // Audit log
        auditService.logUpdate("RoomType", id, oldRoomType, updatedRoomType);
        
        return roomTypeMapper.toDTO(updatedRoomType);
    }

    @Transactional(readOnly = true)
    public RoomTypeDTO getRoomTypeById(Long id) {
        logger.debug("Fetching room type with ID: {}", id);
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Room type not found with ID: {}", id);
                    return new RuntimeException("Room type not found with id: " + id);
                });
        logger.debug("Successfully retrieved room type with ID: {}", id);
        return roomTypeMapper.toDTO(roomType);
    }

    @Transactional(readOnly = true)
    public RoomTypeDTO getRoomTypeByName(String name) {
        logger.debug("Fetching room type with name: {}", name);
        RoomType roomType = roomTypeRepository.findByName(name)
                .orElseThrow(() -> {
                    logger.error("Room type not found with name: {}", name);
                    return new RuntimeException("Room type not found with name: " + name);
                });
        logger.debug("Successfully retrieved room type with name: {}", name);
        return roomTypeMapper.toDTO(roomType);
    }

    @Transactional(readOnly = true)
    public List<RoomTypeDTO> getAllRoomTypes() {
        logger.debug("Fetching all room types");
        List<RoomTypeDTO> roomTypes = roomTypeRepository.findAll().stream()
                .map(roomTypeMapper::toDTO)
                .collect(Collectors.toList());
        logger.info("Retrieved {} room type(s)", roomTypes.size());
        return roomTypes;
    }

    public void deleteRoomType(Long id) {
        logger.info("Deleting room type with ID: {}", id);
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Room type not found with ID: {}", id);
                    return new RuntimeException("Room type not found with id: " + id);
                });
        
        // Check if any rooms are using this room type
        if (!roomType.getRooms().isEmpty()) {
            logger.warn("Failed to delete room type ID {}: {} room(s) are using this room type", id, roomType.getRooms().size());
            throw new RuntimeException("Cannot delete room type. There are " + 
                    roomType.getRooms().size() + " room(s) using this room type");
        }
        
        // Audit log before deletion
        auditService.logDelete("RoomType", id, roomType);
        
        roomTypeRepository.deleteById(id);
        logger.info("Successfully deleted room type with ID: {}", id);
    }
}

