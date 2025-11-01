package com.klm.pms.service;

import com.klm.pms.dto.RoomTypeDTO;
import com.klm.pms.mapper.RoomTypeMapper;
import com.klm.pms.model.RoomType;
import com.klm.pms.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomTypeService {

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomTypeMapper roomTypeMapper;

    public RoomTypeDTO createRoomType(RoomTypeDTO roomTypeDTO) {
        // Check if room type name already exists
        if (roomTypeRepository.existsByName(roomTypeDTO.getName())) {
            throw new RuntimeException("Room type with name '" + roomTypeDTO.getName() + "' already exists");
        }
        
        RoomType roomType = roomTypeMapper.toEntity(roomTypeDTO);
        RoomType savedRoomType = roomTypeRepository.save(roomType);
        return roomTypeMapper.toDTO(savedRoomType);
    }

    public RoomTypeDTO updateRoomType(Long id, RoomTypeDTO roomTypeDTO) {
        RoomType existingRoomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room type not found with id: " + id));
        
        // Check name uniqueness if it's being changed
        if (!roomTypeDTO.getName().equals(existingRoomType.getName()) && 
            roomTypeRepository.existsByName(roomTypeDTO.getName())) {
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
        return roomTypeMapper.toDTO(updatedRoomType);
    }

    @Transactional(readOnly = true)
    public RoomTypeDTO getRoomTypeById(Long id) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room type not found with id: " + id));
        return roomTypeMapper.toDTO(roomType);
    }

    @Transactional(readOnly = true)
    public RoomTypeDTO getRoomTypeByName(String name) {
        RoomType roomType = roomTypeRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Room type not found with name: " + name));
        return roomTypeMapper.toDTO(roomType);
    }

    @Transactional(readOnly = true)
    public List<RoomTypeDTO> getAllRoomTypes() {
        return roomTypeRepository.findAll().stream()
                .map(roomTypeMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void deleteRoomType(Long id) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room type not found with id: " + id));
        
        // Check if any rooms are using this room type
        if (!roomType.getRooms().isEmpty()) {
            throw new RuntimeException("Cannot delete room type. There are " + 
                    roomType.getRooms().size() + " room(s) using this room type");
        }
        
        roomTypeRepository.deleteById(id);
    }
}

