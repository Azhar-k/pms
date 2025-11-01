package com.klm.pms.service;

import com.klm.pms.dto.RateTypeDTO;
import com.klm.pms.mapper.RateTypeMapper;
import com.klm.pms.model.RateType;
import com.klm.pms.model.RateTypeRoomTypeRate;
import com.klm.pms.model.RoomType;
import com.klm.pms.repository.RateTypeRepository;
import com.klm.pms.repository.RateTypeRoomTypeRateRepository;
import com.klm.pms.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RateTypeService {

    @Autowired
    private RateTypeRepository rateTypeRepository;

    @Autowired
    private RateTypeRoomTypeRateRepository rateTypeRoomTypeRateRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RateTypeMapper rateTypeMapper;

    public RateTypeDTO createRateType(RateTypeDTO rateTypeDTO) {
        // Check if rate type name already exists
        if (rateTypeRepository.existsByName(rateTypeDTO.getName())) {
            throw new RuntimeException("Rate type with name '" + rateTypeDTO.getName() + "' already exists");
        }
        
        RateType rateType = rateTypeMapper.toEntity(rateTypeDTO);
        RateType savedRateType = rateTypeRepository.save(rateType);
        
        // Add room type rates
        if (rateTypeDTO.getRoomTypeRates() != null && !rateTypeDTO.getRoomTypeRates().isEmpty()) {
            for (RateTypeDTO.RoomTypeRateDTO roomTypeRateDTO : rateTypeDTO.getRoomTypeRates()) {
                RoomType roomType = roomTypeRepository.findById(roomTypeRateDTO.getRoomTypeId())
                        .orElseThrow(() -> new RuntimeException("Room type not found with id: " + roomTypeRateDTO.getRoomTypeId()));
                
                RateTypeRoomTypeRate rateTypeRoomTypeRate = new RateTypeRoomTypeRate();
                rateTypeRoomTypeRate.setRateType(savedRateType);
                rateTypeRoomTypeRate.setRoomType(roomType);
                rateTypeRoomTypeRate.setRate(roomTypeRateDTO.getRate());
                
                savedRateType.getRoomTypeRates().add(rateTypeRoomTypeRate);
            }
            savedRateType = rateTypeRepository.save(savedRateType);
        }
        
        return rateTypeMapper.toDTO(savedRateType);
    }

    public RateTypeDTO updateRateType(Long id, RateTypeDTO rateTypeDTO) {
        RateType existingRateType = rateTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rate type not found with id: " + id));
        
        // Check name uniqueness if it's being changed
        if (!rateTypeDTO.getName().equals(existingRateType.getName()) && 
            rateTypeRepository.existsByName(rateTypeDTO.getName())) {
            throw new RuntimeException("Rate type with name '" + rateTypeDTO.getName() + "' already exists");
        }
        
        existingRateType.setName(rateTypeDTO.getName());
        existingRateType.setDescription(rateTypeDTO.getDescription());
        
        RateType updatedRateType = rateTypeRepository.save(existingRateType);
        return rateTypeMapper.toDTO(updatedRateType);
    }

    public RateTypeDTO addRoomTypeRate(Long rateTypeId, RateTypeDTO.RoomTypeRateDTO roomTypeRateDTO) {
        RateType rateType = rateTypeRepository.findById(rateTypeId)
                .orElseThrow(() -> new RuntimeException("Rate type not found with id: " + rateTypeId));
        
        RoomType roomType = roomTypeRepository.findById(roomTypeRateDTO.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Room type not found with id: " + roomTypeRateDTO.getRoomTypeId()));
        
        // Check if rate already exists for this room type
        if (rateTypeRoomTypeRateRepository.findByRateTypeIdAndRoomTypeId(rateTypeId, roomTypeRateDTO.getRoomTypeId()).isPresent()) {
            throw new RuntimeException("Rate already exists for room type: " + roomType.getName());
        }
        
        RateTypeRoomTypeRate rateTypeRoomTypeRate = new RateTypeRoomTypeRate();
        rateTypeRoomTypeRate.setRateType(rateType);
        rateTypeRoomTypeRate.setRoomType(roomType);
        rateTypeRoomTypeRate.setRate(roomTypeRateDTO.getRate());
        
        rateTypeRoomTypeRateRepository.save(rateTypeRoomTypeRate);
        rateType.getRoomTypeRates().add(rateTypeRoomTypeRate);
        
        return rateTypeMapper.toDTO(rateType);
    }

    public RateTypeDTO updateRoomTypeRate(Long rateTypeId, Long roomTypeId, BigDecimal newRate) {
        RateTypeRoomTypeRate rateTypeRoomTypeRate = rateTypeRoomTypeRateRepository
                .findByRateTypeIdAndRoomTypeId(rateTypeId, roomTypeId)
                .orElseThrow(() -> new RuntimeException("Rate not found for rate type and room type combination"));
        
        rateTypeRoomTypeRate.setRate(newRate);
        rateTypeRoomTypeRateRepository.save(rateTypeRoomTypeRate);
        
        RateType rateType = rateTypeRepository.findById(rateTypeId)
                .orElseThrow(() -> new RuntimeException("Rate type not found with id: " + rateTypeId));
        
        return rateTypeMapper.toDTO(rateType);
    }

    public void removeRoomTypeRate(Long rateTypeId, Long roomTypeId) {
        RateTypeRoomTypeRate rateTypeRoomTypeRate = rateTypeRoomTypeRateRepository
                .findByRateTypeIdAndRoomTypeId(rateTypeId, roomTypeId)
                .orElseThrow(() -> new RuntimeException("Rate not found for rate type and room type combination"));
        
        rateTypeRoomTypeRateRepository.delete(rateTypeRoomTypeRate);
    }

    @Transactional(readOnly = true)
    public RateTypeDTO getRateTypeById(Long id) {
        RateType rateType = rateTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rate type not found with id: " + id));
        return rateTypeMapper.toDTO(rateType);
    }

    @Transactional(readOnly = true)
    public RateTypeDTO getRateTypeByName(String name) {
        RateType rateType = rateTypeRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Rate type not found with name: " + name));
        return rateTypeMapper.toDTO(rateType);
    }

    @Transactional(readOnly = true)
    public List<RateTypeDTO> getAllRateTypes() {
        return rateTypeRepository.findAll().stream()
                .map(rateTypeMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getRateForRoomType(Long rateTypeId, Long roomTypeId) {
        RateTypeRoomTypeRate rateTypeRoomTypeRate = rateTypeRoomTypeRateRepository
                .findByRateTypeIdAndRoomTypeId(rateTypeId, roomTypeId)
                .orElseThrow(() -> new RuntimeException(
                        "Rate not found for rate type id: " + rateTypeId + " and room type id: " + roomTypeId));
        
        return rateTypeRoomTypeRate.getRate();
    }

    public void deleteRateType(Long id) {
        RateType rateType = rateTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rate type not found with id: " + id));
        
        // Check if any reservations are using this rate type
        if (!rateType.getReservations().isEmpty()) {
            throw new RuntimeException("Cannot delete rate type. There are " + 
                    rateType.getReservations().size() + " reservation(s) using this rate type");
        }
        
        rateTypeRepository.deleteById(id);
    }
}

