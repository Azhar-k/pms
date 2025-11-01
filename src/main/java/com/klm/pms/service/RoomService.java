package com.klm.pms.service;

import com.klm.pms.dto.RoomDTO;
import com.klm.pms.mapper.RoomMapper;
import com.klm.pms.model.Room;
import com.klm.pms.model.Room.RoomStatus;
import com.klm.pms.model.RoomType;
import com.klm.pms.repository.RoomRepository;
import com.klm.pms.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomMapper roomMapper;

    public RoomDTO createRoom(RoomDTO roomDTO) {
        // Check if room number already exists
        if (roomRepository.findByRoomNumber(roomDTO.getRoomNumber()).isPresent()) {
            throw new RuntimeException("Room with number " + roomDTO.getRoomNumber() + " already exists");
        }
        
        // Fetch and validate room type
        RoomType roomType = roomTypeRepository.findById(roomDTO.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Room type not found with id: " + roomDTO.getRoomTypeId()));
        
        Room room = roomMapper.toEntity(roomDTO);
        room.setRoomType(roomType);
        Room savedRoom = roomRepository.save(room);
        return roomMapper.toDTO(savedRoom);
    }

    public RoomDTO updateRoom(Long id, RoomDTO roomDTO) {
        Room existingRoom = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
        
        // Check room number uniqueness if it's being changed
        if (!roomDTO.getRoomNumber().equals(existingRoom.getRoomNumber())) {
            if (roomRepository.findByRoomNumber(roomDTO.getRoomNumber()).isPresent()) {
                throw new RuntimeException("Room with number " + roomDTO.getRoomNumber() + " already exists");
            }
        }
        
        // Fetch and validate room type if it's being changed
        if (roomDTO.getRoomTypeId() != null && 
            (existingRoom.getRoomType() == null || !roomDTO.getRoomTypeId().equals(existingRoom.getRoomType().getId()))) {
            RoomType roomType = roomTypeRepository.findById(roomDTO.getRoomTypeId())
                    .orElseThrow(() -> new RuntimeException("Room type not found with id: " + roomDTO.getRoomTypeId()));
            existingRoom.setRoomType(roomType);
        }
        
        existingRoom.setRoomNumber(roomDTO.getRoomNumber());
        existingRoom.setPricePerNight(roomDTO.getPricePerNight());
        if (roomDTO.getStatus() != null) {
            existingRoom.setStatus(roomDTO.getStatus());
        }
        existingRoom.setMaxOccupancy(roomDTO.getMaxOccupancy());
        existingRoom.setAmenities(roomDTO.getAmenities());
        existingRoom.setDescription(roomDTO.getDescription());
        existingRoom.setFloor(roomDTO.getFloor());
        existingRoom.setHasBalcony(roomDTO.getHasBalcony());
        existingRoom.setHasView(roomDTO.getHasView());
        
        Room updatedRoom = roomRepository.save(existingRoom);
        return roomMapper.toDTO(updatedRoom);
    }

    @Transactional(readOnly = true)
    public RoomDTO getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
        return roomMapper.toDTO(room);
    }

    @Transactional(readOnly = true)
    public RoomDTO getRoomByNumber(String roomNumber) {
        Room room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new RuntimeException("Room not found with number: " + roomNumber));
        return roomMapper.toDTO(room);
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(roomMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getAvailableRooms() {
        return roomRepository.findByStatus(RoomStatus.AVAILABLE).stream()
                .map(roomMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getRoomsByType(Long roomTypeId) {
        // Validate room type exists
        if (!roomTypeRepository.existsById(roomTypeId)) {
            throw new RuntimeException("Room type not found with id: " + roomTypeId);
        }
        return roomRepository.findByRoomTypeId(roomTypeId).stream()
                .map(roomMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new RuntimeException("Room not found with id: " + id);
        }
        roomRepository.deleteById(id);
    }
}

