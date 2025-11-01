package com.klm.pms.mapper;

import com.klm.pms.dto.RoomDTO;
import com.klm.pms.model.Room;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {
    public RoomDTO toDTO(Room room) {
        if (room == null) return null;
        
        RoomDTO dto = new RoomDTO();
        dto.setId(room.getId());
        dto.setRoomNumber(room.getRoomNumber());
        dto.setRoomType(room.getRoomType());
        dto.setPricePerNight(room.getPricePerNight());
        dto.setStatus(room.getStatus());
        dto.setMaxOccupancy(room.getMaxOccupancy());
        dto.setAmenities(room.getAmenities());
        dto.setDescription(room.getDescription());
        dto.setFloor(room.getFloor());
        dto.setHasBalcony(room.getHasBalcony());
        dto.setHasView(room.getHasView());
        return dto;
    }

    public Room toEntity(RoomDTO dto) {
        if (dto == null) return null;
        
        Room room = new Room();
        room.setId(dto.getId());
        room.setRoomNumber(dto.getRoomNumber());
        room.setRoomType(dto.getRoomType());
        room.setPricePerNight(dto.getPricePerNight());
        room.setStatus(dto.getStatus() != null ? dto.getStatus() : Room.RoomStatus.AVAILABLE);
        room.setMaxOccupancy(dto.getMaxOccupancy());
        room.setAmenities(dto.getAmenities());
        room.setDescription(dto.getDescription());
        room.setFloor(dto.getFloor());
        room.setHasBalcony(dto.getHasBalcony());
        room.setHasView(dto.getHasView());
        return room;
    }
}

