package com.klm.pms.mapper;

import com.klm.pms.dto.GuestDTO;
import com.klm.pms.model.Guest;
import org.springframework.stereotype.Component;

@Component
public class GuestMapper {
    public GuestDTO toDTO(Guest guest) {
        if (guest == null) return null;
        
        GuestDTO dto = new GuestDTO();
        dto.setId(guest.getId());
        dto.setFirstName(guest.getFirstName());
        dto.setLastName(guest.getLastName());
        dto.setEmail(guest.getEmail());
        dto.setPhoneNumber(guest.getPhoneNumber());
        dto.setAddress(guest.getAddress());
        dto.setCity(guest.getCity());
        dto.setState(guest.getState());
        dto.setCountry(guest.getCountry());
        dto.setPostalCode(guest.getPostalCode());
        dto.setIdentificationType(guest.getIdentificationType());
        dto.setIdentificationNumber(guest.getIdentificationNumber());
        return dto;
    }

    public Guest toEntity(GuestDTO dto) {
        if (dto == null) return null;
        
        Guest guest = new Guest();
        guest.setId(dto.getId());
        guest.setFirstName(dto.getFirstName());
        guest.setLastName(dto.getLastName());
        guest.setEmail(dto.getEmail());
        guest.setPhoneNumber(dto.getPhoneNumber());
        guest.setAddress(dto.getAddress());
        guest.setCity(dto.getCity());
        guest.setState(dto.getState());
        guest.setCountry(dto.getCountry());
        guest.setPostalCode(dto.getPostalCode());
        guest.setIdentificationType(dto.getIdentificationType());
        guest.setIdentificationNumber(dto.getIdentificationNumber());
        return guest;
    }
}

