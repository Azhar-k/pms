package com.klm.pms.mapper;

import com.klm.pms.dto.RoomTypeDTO;
import com.klm.pms.model.RoomType;
import org.springframework.stereotype.Component;

@Component
public class RoomTypeMapper {
    public RoomTypeDTO toDTO(RoomType roomType) {
        if (roomType == null) return null;
        
        RoomTypeDTO dto = new RoomTypeDTO();
        dto.setId(roomType.getId());
        dto.setName(roomType.getName());
        dto.setDescription(roomType.getDescription());
        dto.setBasePricePerNight(roomType.getBasePricePerNight());
        dto.setMaxOccupancy(roomType.getMaxOccupancy());
        dto.setAmenities(roomType.getAmenities());
        dto.setDefaultRoomSize(roomType.getDefaultRoomSize());
        dto.setHasBalcony(roomType.getHasBalcony());
        dto.setHasView(roomType.getHasView());
        dto.setHasMinibar(roomType.getHasMinibar());
        dto.setHasSafe(roomType.getHasSafe());
        dto.setHasAirConditioning(roomType.getHasAirConditioning());
        dto.setBedType(roomType.getBedType());
        return dto;
    }

    public RoomType toEntity(RoomTypeDTO dto) {
        if (dto == null) return null;
        
        RoomType roomType = new RoomType();
        roomType.setId(dto.getId());
        roomType.setName(dto.getName());
        roomType.setDescription(dto.getDescription());
        roomType.setBasePricePerNight(dto.getBasePricePerNight());
        roomType.setMaxOccupancy(dto.getMaxOccupancy());
        roomType.setAmenities(dto.getAmenities());
        roomType.setDefaultRoomSize(dto.getDefaultRoomSize());
        roomType.setHasBalcony(dto.getHasBalcony());
        roomType.setHasView(dto.getHasView());
        roomType.setHasMinibar(dto.getHasMinibar());
        roomType.setHasSafe(dto.getHasSafe());
        roomType.setHasAirConditioning(dto.getHasAirConditioning());
        roomType.setBedType(dto.getBedType());
        return roomType;
    }
}

