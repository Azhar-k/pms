package com.klm.pms.mapper;

import com.klm.pms.dto.ReservationDTO;
import com.klm.pms.model.Reservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {
    
    @Autowired
    private GuestMapper guestMapper;
    
    @Autowired
    private RoomMapper roomMapper;
    
    public ReservationDTO toDTO(Reservation reservation) {
        if (reservation == null) return null;
        
        ReservationDTO dto = new ReservationDTO();
        dto.setId(reservation.getId());
        dto.setReservationNumber(reservation.getReservationNumber());
        dto.setGuestId(reservation.getGuest() != null ? reservation.getGuest().getId() : null);
        dto.setRoomId(reservation.getRoom() != null ? reservation.getRoom().getId() : null);
        dto.setCheckInDate(reservation.getCheckInDate());
        dto.setCheckOutDate(reservation.getCheckOutDate());
        dto.setNumberOfGuests(reservation.getNumberOfGuests());
        dto.setStatus(reservation.getStatus());
        dto.setActualCheckInTime(reservation.getActualCheckInTime());
        dto.setActualCheckOutTime(reservation.getActualCheckOutTime());
        dto.setSpecialRequests(reservation.getSpecialRequests());
        dto.setPaymentStatus(reservation.getPaymentStatus());
        dto.setTotalAmount(reservation.getTotalAmount());
        dto.setDepositAmount(reservation.getDepositAmount());
        
        // Optionally include nested objects
        if (reservation.getGuest() != null) {
            dto.setGuest(guestMapper.toDTO(reservation.getGuest()));
        }
        if (reservation.getRoom() != null) {
            dto.setRoom(roomMapper.toDTO(reservation.getRoom()));
        }
        
        return dto;
    }

    public Reservation toEntity(ReservationDTO dto) {
        if (dto == null) return null;
        
        Reservation reservation = new Reservation();
        reservation.setId(dto.getId());
        reservation.setReservationNumber(dto.getReservationNumber());
        reservation.setCheckInDate(dto.getCheckInDate());
        reservation.setCheckOutDate(dto.getCheckOutDate());
        reservation.setNumberOfGuests(dto.getNumberOfGuests());
        reservation.setStatus(dto.getStatus() != null ? dto.getStatus() : Reservation.ReservationStatus.PENDING);
        reservation.setActualCheckInTime(dto.getActualCheckInTime());
        reservation.setActualCheckOutTime(dto.getActualCheckOutTime());
        reservation.setSpecialRequests(dto.getSpecialRequests());
        reservation.setPaymentStatus(dto.getPaymentStatus());
        reservation.setTotalAmount(dto.getTotalAmount());
        reservation.setDepositAmount(dto.getDepositAmount());
        return reservation;
    }
}

