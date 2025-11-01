package com.klm.pms.service;

import com.klm.pms.dto.ReservationDTO;
import com.klm.pms.mapper.ReservationMapper;
import com.klm.pms.model.Guest;
import com.klm.pms.model.RateType;
import com.klm.pms.model.Reservation;
import com.klm.pms.model.Reservation.ReservationStatus;
import com.klm.pms.model.Room;
import com.klm.pms.repository.GuestRepository;
import com.klm.pms.repository.RateTypeRepository;
import com.klm.pms.repository.ReservationRepository;
import com.klm.pms.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RateTypeRepository rateTypeRepository;

    @Autowired
    private RateTypeService rateTypeService;

    @Autowired
    private ReservationMapper reservationMapper;

    public ReservationDTO createReservation(ReservationDTO reservationDTO) {
        // Validate dates
        if (reservationDTO.getCheckInDate().isAfter(reservationDTO.getCheckOutDate())) {
            throw new RuntimeException("Check-in date must be before check-out date");
        }
        
        if (reservationDTO.getCheckInDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Check-in date cannot be in the past");
        }
        
        // Get guest and room
        Guest guest = guestRepository.findById(reservationDTO.getGuestId())
                .orElseThrow(() -> new RuntimeException("Guest not found with id: " + reservationDTO.getGuestId()));
        
        Room room = roomRepository.findById(reservationDTO.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + reservationDTO.getRoomId()));
        
        // Get rate type
        RateType rateType = rateTypeRepository.findById(reservationDTO.getRateTypeId())
                .orElseThrow(() -> new RuntimeException("Rate type not found with id: " + reservationDTO.getRateTypeId()));
        
        // Check room availability
        List<Reservation> conflictingReservations = reservationRepository.findConflictingReservations(
                room.getId(), reservationDTO.getCheckInDate(), reservationDTO.getCheckOutDate());
        
        if (!conflictingReservations.isEmpty()) {
            throw new RuntimeException("Room is not available for the selected dates");
        }
        
        // Check room capacity
        if (room.getMaxOccupancy() != null && reservationDTO.getNumberOfGuests() > room.getMaxOccupancy()) {
            throw new RuntimeException("Number of guests exceeds room capacity");
        }
        
        // Create reservation
        Reservation reservation = reservationMapper.toEntity(reservationDTO);
        reservation.setGuest(guest);
        reservation.setRoom(room);
        reservation.setRateType(rateType);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        
        // Get rate from rate type for this room type
        BigDecimal ratePerNight = rateTypeService.getRateForRoomType(rateType.getId(), room.getRoomType().getId());
        
        // Calculate total amount based on rate type rate
        long nights = ChronoUnit.DAYS.between(reservationDTO.getCheckInDate(), reservationDTO.getCheckOutDate());
        BigDecimal totalAmount = ratePerNight.multiply(BigDecimal.valueOf(nights));
        reservation.setTotalAmount(totalAmount);
        
        // Update room status
        room.setStatus(Room.RoomStatus.RESERVED);
        roomRepository.save(room);
        
        Reservation savedReservation = reservationRepository.save(reservation);
        return reservationMapper.toDTO(savedReservation);
    }

    public ReservationDTO checkIn(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found with id: " + reservationId));
        
        if (reservation.getStatus() != ReservationStatus.CONFIRMED && 
            reservation.getStatus() != ReservationStatus.PENDING) {
            throw new RuntimeException("Reservation cannot be checked in. Current status: " + reservation.getStatus());
        }
        
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setActualCheckInTime(LocalDateTime.now());
        
        // Update room status
        Room room = reservation.getRoom();
        room.setStatus(Room.RoomStatus.OCCUPIED);
        roomRepository.save(room);
        
        Reservation updatedReservation = reservationRepository.save(reservation);
        return reservationMapper.toDTO(updatedReservation);
    }

    public ReservationDTO checkOut(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found with id: " + reservationId));
        
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new RuntimeException("Reservation must be checked in before checkout. Current status: " + reservation.getStatus());
        }
        
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservation.setActualCheckOutTime(LocalDateTime.now());
        
        // Update room status
        Room room = reservation.getRoom();
        room.setStatus(Room.RoomStatus.CLEANING);
        roomRepository.save(room);
        
        Reservation updatedReservation = reservationRepository.save(reservation);
        return reservationMapper.toDTO(updatedReservation);
    }

    public ReservationDTO cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found with id: " + reservationId));
        
        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new RuntimeException("Cannot cancel a reservation that has been checked out");
        }
        
        reservation.setStatus(ReservationStatus.CANCELLED);
        
        // Update room status
        Room room = reservation.getRoom();
        if (room.getStatus() == Room.RoomStatus.RESERVED || room.getStatus() == Room.RoomStatus.OCCUPIED) {
            room.setStatus(Room.RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }
        
        Reservation updatedReservation = reservationRepository.save(reservation);
        return reservationMapper.toDTO(updatedReservation);
    }

    @Transactional(readOnly = true)
    public ReservationDTO getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found with id: " + id));
        return reservationMapper.toDTO(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationDTO getReservationByNumber(String reservationNumber) {
        Reservation reservation = reservationRepository.findByReservationNumber(reservationNumber)
                .orElseThrow(() -> new RuntimeException("Reservation not found with number: " + reservationNumber));
        return reservationMapper.toDTO(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getReservationsByGuest(Long guestId) {
        return reservationRepository.findByGuestId(guestId).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getReservationsByStatus(ReservationStatus status) {
        return reservationRepository.findByStatus(status).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getReservationsByDateRange(LocalDate startDate, LocalDate endDate) {
        return reservationRepository.findReservationsByDateRange(startDate, endDate).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }
}

