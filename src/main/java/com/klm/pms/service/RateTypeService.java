package com.klm.pms.service;

import com.klm.pms.dto.RateTypeDTO;
import com.klm.pms.mapper.RateTypeMapper;
import com.klm.pms.model.RateType;
import com.klm.pms.model.RateTypeRoomTypeRate;
import com.klm.pms.model.RoomType;
import com.klm.pms.repository.RateTypeRepository;
import com.klm.pms.repository.RateTypeRoomTypeRateRepository;
import com.klm.pms.repository.RoomTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RateTypeService {

    private static final Logger logger = LoggerFactory.getLogger(RateTypeService.class);

    @Autowired
    private RateTypeRepository rateTypeRepository;

    @Autowired
    private RateTypeRoomTypeRateRepository rateTypeRoomTypeRateRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RateTypeMapper rateTypeMapper;

    public RateTypeDTO createRateType(RateTypeDTO rateTypeDTO) {
        logger.info("Creating new rate type with name: {} and {} room type rate(s)", 
                rateTypeDTO.getName(), rateTypeDTO.getRoomTypeRates() != null ? rateTypeDTO.getRoomTypeRates().size() : 0);
        
        // Check if rate type name already exists
        if (rateTypeRepository.existsByName(rateTypeDTO.getName())) {
            logger.warn("Failed to create rate type: Name '{}' already exists", rateTypeDTO.getName());
            throw new RuntimeException("Rate type with name '" + rateTypeDTO.getName() + "' already exists");
        }
        
        RateType rateType = rateTypeMapper.toEntity(rateTypeDTO);
        RateType savedRateType = rateTypeRepository.save(rateType);
        logger.debug("Rate type saved with ID: {}", savedRateType.getId());
        
        // Add room type rates
        if (rateTypeDTO.getRoomTypeRates() != null && !rateTypeDTO.getRoomTypeRates().isEmpty()) {
            logger.debug("Adding {} room type rate(s)", rateTypeDTO.getRoomTypeRates().size());
            for (RateTypeDTO.RoomTypeRateDTO roomTypeRateDTO : rateTypeDTO.getRoomTypeRates()) {
                RoomType roomType = roomTypeRepository.findById(roomTypeRateDTO.getRoomTypeId())
                        .orElseThrow(() -> {
                            logger.error("Room type not found with ID: {}", roomTypeRateDTO.getRoomTypeId());
                            return new RuntimeException("Room type not found with id: " + roomTypeRateDTO.getRoomTypeId());
                        });
                
                RateTypeRoomTypeRate rateTypeRoomTypeRate = new RateTypeRoomTypeRate();
                rateTypeRoomTypeRate.setRateType(savedRateType);
                rateTypeRoomTypeRate.setRoomType(roomType);
                rateTypeRoomTypeRate.setRate(roomTypeRateDTO.getRate());
                
                savedRateType.getRoomTypeRates().add(rateTypeRoomTypeRate);
                logger.debug("Added rate {} for room type: {}", roomTypeRateDTO.getRate(), roomType.getName());
            }
            savedRateType = rateTypeRepository.save(savedRateType);
        }
        
        logger.info("Successfully created rate type with ID: {} and name: {}", savedRateType.getId(), savedRateType.getName());
        return rateTypeMapper.toDTO(savedRateType);
    }

    public RateTypeDTO updateRateType(Long id, RateTypeDTO rateTypeDTO) {
        logger.info("Updating rate type with ID: {}", id);
        
        RateType existingRateType = rateTypeRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Rate type not found with ID: {}", id);
                    return new RuntimeException("Rate type not found with id: " + id);
                });
        
        // Check name uniqueness if it's being changed
        if (!rateTypeDTO.getName().equals(existingRateType.getName()) && 
            rateTypeRepository.existsByName(rateTypeDTO.getName())) {
            logger.warn("Failed to update rate type ID {}: Name '{}' already exists", id, rateTypeDTO.getName());
            throw new RuntimeException("Rate type with name '" + rateTypeDTO.getName() + "' already exists");
        }
        
        existingRateType.setName(rateTypeDTO.getName());
        existingRateType.setDescription(rateTypeDTO.getDescription());
        
        RateType updatedRateType = rateTypeRepository.save(existingRateType);
        logger.info("Successfully updated rate type with ID: {}", id);
        return rateTypeMapper.toDTO(updatedRateType);
    }

    public RateTypeDTO addRoomTypeRate(Long rateTypeId, RateTypeDTO.RoomTypeRateDTO roomTypeRateDTO) {
        logger.info("Adding room type rate for rate type ID: {} and room type ID: {} with rate: {}", 
                rateTypeId, roomTypeRateDTO.getRoomTypeId(), roomTypeRateDTO.getRate());
        
        RateType rateType = rateTypeRepository.findById(rateTypeId)
                .orElseThrow(() -> {
                    logger.error("Rate type not found with ID: {}", rateTypeId);
                    return new RuntimeException("Rate type not found with id: " + rateTypeId);
                });
        
        RoomType roomType = roomTypeRepository.findById(roomTypeRateDTO.getRoomTypeId())
                .orElseThrow(() -> {
                    logger.error("Room type not found with ID: {}", roomTypeRateDTO.getRoomTypeId());
                    return new RuntimeException("Room type not found with id: " + roomTypeRateDTO.getRoomTypeId());
                });
        
        // Check if rate already exists for this room type
        if (rateTypeRoomTypeRateRepository.findByRateTypeIdAndRoomTypeId(rateTypeId, roomTypeRateDTO.getRoomTypeId()).isPresent()) {
            logger.warn("Failed to add rate: Rate already exists for rate type ID {} and room type: {}", rateTypeId, roomType.getName());
            throw new RuntimeException("Rate already exists for room type: " + roomType.getName());
        }
        
        RateTypeRoomTypeRate rateTypeRoomTypeRate = new RateTypeRoomTypeRate();
        rateTypeRoomTypeRate.setRateType(rateType);
        rateTypeRoomTypeRate.setRoomType(roomType);
        rateTypeRoomTypeRate.setRate(roomTypeRateDTO.getRate());
        
        rateTypeRoomTypeRateRepository.save(rateTypeRoomTypeRate);
        rateType.getRoomTypeRates().add(rateTypeRoomTypeRate);
        logger.info("Successfully added rate {} for room type: {}", roomTypeRateDTO.getRate(), roomType.getName());
        
        return rateTypeMapper.toDTO(rateType);
    }

    public RateTypeDTO updateRoomTypeRate(Long rateTypeId, Long roomTypeId, BigDecimal newRate) {
        logger.info("Updating room type rate for rate type ID: {} and room type ID: {} to rate: {}", 
                rateTypeId, roomTypeId, newRate);
        
        RateTypeRoomTypeRate rateTypeRoomTypeRate = rateTypeRoomTypeRateRepository
                .findByRateTypeIdAndRoomTypeId(rateTypeId, roomTypeId)
                .orElseThrow(() -> {
                    logger.error("Rate not found for rate type ID: {} and room type ID: {}", rateTypeId, roomTypeId);
                    return new RuntimeException("Rate not found for rate type and room type combination");
                });
        
        BigDecimal oldRate = rateTypeRoomTypeRate.getRate();
        rateTypeRoomTypeRate.setRate(newRate);
        rateTypeRoomTypeRateRepository.save(rateTypeRoomTypeRate);
        logger.info("Rate updated from {} to {}", oldRate, newRate);
        
        RateType rateType = rateTypeRepository.findById(rateTypeId)
                .orElseThrow(() -> {
                    logger.error("Rate type not found with ID: {}", rateTypeId);
                    return new RuntimeException("Rate type not found with id: " + rateTypeId);
                });
        
        return rateTypeMapper.toDTO(rateType);
    }

    public void removeRoomTypeRate(Long rateTypeId, Long roomTypeId) {
        logger.info("Removing room type rate for rate type ID: {} and room type ID: {}", rateTypeId, roomTypeId);
        
        RateTypeRoomTypeRate rateTypeRoomTypeRate = rateTypeRoomTypeRateRepository
                .findByRateTypeIdAndRoomTypeId(rateTypeId, roomTypeId)
                .orElseThrow(() -> {
                    logger.error("Rate not found for rate type ID: {} and room type ID: {}", rateTypeId, roomTypeId);
                    return new RuntimeException("Rate not found for rate type and room type combination");
                });
        
        rateTypeRoomTypeRateRepository.delete(rateTypeRoomTypeRate);
        logger.info("Successfully removed room type rate");
    }

    @Transactional(readOnly = true)
    public RateTypeDTO getRateTypeById(Long id) {
        logger.debug("Fetching rate type with ID: {}", id);
        RateType rateType = rateTypeRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Rate type not found with ID: {}", id);
                    return new RuntimeException("Rate type not found with id: " + id);
                });
        logger.debug("Successfully retrieved rate type with ID: {}", id);
        return rateTypeMapper.toDTO(rateType);
    }

    @Transactional(readOnly = true)
    public RateTypeDTO getRateTypeByName(String name) {
        logger.debug("Fetching rate type with name: {}", name);
        RateType rateType = rateTypeRepository.findByName(name)
                .orElseThrow(() -> {
                    logger.error("Rate type not found with name: {}", name);
                    return new RuntimeException("Rate type not found with name: " + name);
                });
        logger.debug("Successfully retrieved rate type with name: {}", name);
        return rateTypeMapper.toDTO(rateType);
    }

    @Transactional(readOnly = true)
    public List<RateTypeDTO> getAllRateTypes() {
        logger.debug("Fetching all rate types");
        List<RateTypeDTO> rateTypes = rateTypeRepository.findAll().stream()
                .map(rateTypeMapper::toDTO)
                .collect(Collectors.toList());
        logger.info("Retrieved {} rate type(s)", rateTypes.size());
        return rateTypes;
    }

    @Transactional(readOnly = true)
    public BigDecimal getRateForRoomType(Long rateTypeId, Long roomTypeId) {
        logger.debug("Fetching rate for rate type ID: {} and room type ID: {}", rateTypeId, roomTypeId);
        
        RateTypeRoomTypeRate rateTypeRoomTypeRate = rateTypeRoomTypeRateRepository
                .findByRateTypeIdAndRoomTypeId(rateTypeId, roomTypeId)
                .orElseThrow(() -> {
                    logger.error("Rate not found for rate type ID: {} and room type ID: {}", rateTypeId, roomTypeId);
                    return new RuntimeException(
                            "Rate not found for rate type id: " + rateTypeId + " and room type id: " + roomTypeId);
                });
        
        logger.debug("Retrieved rate: {} for rate type ID: {} and room type ID: {}", 
                rateTypeRoomTypeRate.getRate(), rateTypeId, roomTypeId);
        return rateTypeRoomTypeRate.getRate();
    }

    public void deleteRateType(Long id) {
        logger.info("Deleting rate type with ID: {}", id);
        
        RateType rateType = rateTypeRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Rate type not found with ID: {}", id);
                    return new RuntimeException("Rate type not found with id: " + id);
                });
        
        // Check if any reservations are using this rate type
        if (!rateType.getReservations().isEmpty()) {
            logger.warn("Failed to delete rate type ID {}: {} reservation(s) are using this rate type", 
                    id, rateType.getReservations().size());
            throw new RuntimeException("Cannot delete rate type. There are " + 
                    rateType.getReservations().size() + " reservation(s) using this rate type");
        }
        
        rateTypeRepository.deleteById(id);
        logger.info("Successfully deleted rate type with ID: {}", id);
    }
}

