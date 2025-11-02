package com.klm.pms.mapper;

import com.klm.pms.dto.RoomDTO;
import com.klm.pms.model.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {
    
    @Autowired
    private RoomTypeMapper roomTypeMapper;
    
    public RoomDTO toDTO(Room room) {
        if (room == null) return null;
        
        RoomDTO dto = new RoomDTO();
        dto.setId(room.getId());
        dto.setRoomNumber(room.getRoomNumber());
        dto.setRoomTypeId(room.getRoomType() != null ? room.getRoomType().getId() : null);
        dto.setStatus(room.getStatus());
        dto.setMaxOccupancy(room.getMaxOccupancy());
        dto.setAmenities(room.getAmenities());
        dto.setDescription(room.getDescription());
        dto.setFloor(room.getFloor());
        dto.setHasBalcony(room.getHasBalcony());
        dto.setHasView(room.getHasView());
        
        // Optionally include nested room type object
        if (room.getRoomType() != null) {
            dto.setRoomType(roomTypeMapper.toDTO(room.getRoomType()));
        }
        
        return dto;
    }

    public Room toEntity(RoomDTO dto) {
        if (dto == null) return null;
        
        Room room = new Room();
        room.setId(dto.getId());
        room.setRoomNumber(dto.getRoomNumber());
        // Note: roomType will be set by service layer after fetching from repository
        room.setStatus(dto.getStatus() != null ? dto.getStatus() : Room.RoomStatus.READY);
        room.setMaxOccupancy(dto.getMaxOccupancy());
        room.setAmenities(dto.getAmenities());
        room.setDescription(dto.getDescription());
        room.setFloor(dto.getFloor());
        room.setHasBalcony(dto.getHasBalcony());
        room.setHasView(dto.getHasView());
        return room;
    }
}

