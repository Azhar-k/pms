package com.klm.pms.mapper;

import com.klm.pms.dto.RateTypeDTO;
import com.klm.pms.model.RateType;
import com.klm.pms.model.RateTypeRoomTypeRate;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class RateTypeMapper {
    
    public RateTypeDTO toDTO(RateType rateType) {
        if (rateType == null) return null;
        
        RateTypeDTO dto = new RateTypeDTO();
        dto.setId(rateType.getId());
        dto.setName(rateType.getName());
        dto.setDescription(rateType.getDescription());
        
        // Map room type rates
        if (rateType.getRoomTypeRates() != null) {
            dto.setRoomTypeRates(rateType.getRoomTypeRates().stream()
                    .map(this::toRoomTypeRateDTO)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }

    public RateType toEntity(RateTypeDTO dto) {
        if (dto == null) return null;
        
        RateType rateType = new RateType();
        rateType.setId(dto.getId());
        rateType.setName(dto.getName());
        rateType.setDescription(dto.getDescription());
        // Note: roomTypeRates will be set by service layer after validation
        return rateType;
    }
    
    private RateTypeDTO.RoomTypeRateDTO toRoomTypeRateDTO(RateTypeRoomTypeRate rateTypeRoomTypeRate) {
        if (rateTypeRoomTypeRate == null) return null;
        
        RateTypeDTO.RoomTypeRateDTO dto = new RateTypeDTO.RoomTypeRateDTO();
        dto.setId(rateTypeRoomTypeRate.getId());
        dto.setRoomTypeId(rateTypeRoomTypeRate.getRoomType() != null ? 
                rateTypeRoomTypeRate.getRoomType().getId() : null);
        dto.setRoomTypeName(rateTypeRoomTypeRate.getRoomType() != null ? 
                rateTypeRoomTypeRate.getRoomType().getName() : null);
        dto.setRate(rateTypeRoomTypeRate.getRate());
        return dto;
    }
}

