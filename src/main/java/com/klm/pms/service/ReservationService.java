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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

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
        logger.info("Creating new reservation for guest ID: {}, room ID: {}, rate type ID: {}, check-in: {}, check-out: {}", 
                reservationDTO.getGuestId(), reservationDTO.getRoomId(), reservationDTO.getRateTypeId(),
                reservationDTO.getCheckInDate(), reservationDTO.getCheckOutDate());
        
        // Validate dates
        if (reservationDTO.getCheckInDate().isAfter(reservationDTO.getCheckOutDate())) {
            logger.warn("Failed to create reservation: Check-in date {} is after check-out date {}", 
                    reservationDTO.getCheckInDate(), reservationDTO.getCheckOutDate());
            throw new RuntimeException("Check-in date must be before check-out date");
        }
        
        if (reservationDTO.getCheckInDate().isBefore(LocalDate.now())) {
            logger.warn("Failed to create reservation: Check-in date {} is in the past", reservationDTO.getCheckInDate());
            throw new RuntimeException("Check-in date cannot be in the past");
        }
        
        // Get guest and room
        Guest guest = guestRepository.findById(reservationDTO.getGuestId())
                .orElseThrow(() -> {
                    logger.error("Guest not found with ID: {}", reservationDTO.getGuestId());
                    return new RuntimeException("Guest not found with id: " + reservationDTO.getGuestId());
                });
        logger.debug("Guest found: {} {}", guest.getFirstName(), guest.getLastName());
        
        Room room = roomRepository.findById(reservationDTO.getRoomId())
                .orElseThrow(() -> {
                    logger.error("Room not found with ID: {}", reservationDTO.getRoomId());
                    return new RuntimeException("Room not found with id: " + reservationDTO.getRoomId());
                });
        logger.debug("Room found: {}", room.getRoomNumber());
        
        // Get rate type
        RateType rateType = rateTypeRepository.findById(reservationDTO.getRateTypeId())
                .orElseThrow(() -> {
                    logger.error("Rate type not found with ID: {}", reservationDTO.getRateTypeId());
                    return new RuntimeException("Rate type not found with id: " + reservationDTO.getRateTypeId());
                });
        logger.debug("Rate type found: {}", rateType.getName());
        
        // Check room availability
        List<Reservation> conflictingReservations = reservationRepository.findConflictingReservations(
                room.getId(), reservationDTO.getCheckInDate(), reservationDTO.getCheckOutDate());
        
        if (!conflictingReservations.isEmpty()) {
            logger.warn("Failed to create reservation: Room {} has {} conflicting reservation(s)", 
                    room.getRoomNumber(), conflictingReservations.size());
            throw new RuntimeException("Room is not available for the selected dates");
        }
        
        // Check room capacity
        if (room.getMaxOccupancy() != null && reservationDTO.getNumberOfGuests() > room.getMaxOccupancy()) {
            logger.warn("Failed to create reservation: Number of guests {} exceeds room capacity {}", 
                    reservationDTO.getNumberOfGuests(), room.getMaxOccupancy());
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
        logger.debug("Rate per night: {} for rate type: {} and room type: {}", 
                ratePerNight, rateType.getName(), room.getRoomType().getName());
        
        // Calculate total amount based on rate type rate
        long nights = ChronoUnit.DAYS.between(reservationDTO.getCheckInDate(), reservationDTO.getCheckOutDate());
        BigDecimal totalAmount = ratePerNight.multiply(BigDecimal.valueOf(nights));
        reservation.setTotalAmount(totalAmount);
        logger.debug("Calculated total amount: {} for {} night(s)", totalAmount, nights);
        
        // Room status is not updated here - availability is determined by date range and reservations
        
        Reservation savedReservation = reservationRepository.save(reservation);
        logger.info("Successfully created reservation with ID: {} and number: {} for total amount: {}", 
                savedReservation.getId(), savedReservation.getReservationNumber(), totalAmount);
        return reservationMapper.toDTO(savedReservation);
    }

    public ReservationDTO checkIn(Long reservationId) {
        logger.info("Processing check-in for reservation ID: {}", reservationId);
        
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    logger.error("Reservation not found with ID: {}", reservationId);
                    return new RuntimeException("Reservation not found with id: " + reservationId);
                });
        
        if (reservation.getStatus() != ReservationStatus.CONFIRMED && 
            reservation.getStatus() != ReservationStatus.PENDING) {
            logger.warn("Failed to check in reservation ID {}: Invalid status {}", reservationId, reservation.getStatus());
            throw new RuntimeException("Reservation cannot be checked in. Current status: " + reservation.getStatus());
        }
        
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setActualCheckInTime(LocalDateTime.now());
        logger.debug("Reservation {} status updated to CHECKED_IN", reservation.getReservationNumber());
        
        // Room status is not updated here - availability is determined by date range and reservations
        
        Reservation updatedReservation = reservationRepository.save(reservation);
        logger.info("Successfully checked in reservation ID: {} for room: {}", 
                reservationId, reservation.getRoom().getRoomNumber());
        return reservationMapper.toDTO(updatedReservation);
    }

    public ReservationDTO checkOut(Long reservationId) {
        logger.info("Processing check-out for reservation ID: {}", reservationId);
        
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    logger.error("Reservation not found with ID: {}", reservationId);
                    return new RuntimeException("Reservation not found with id: " + reservationId);
                });
        
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            logger.warn("Failed to check out reservation ID {}: Status is not CHECKED_IN, current status: {}", 
                    reservationId, reservation.getStatus());
            throw new RuntimeException("Reservation must be checked in before checkout. Current status: " + reservation.getStatus());
        }
        
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservation.setActualCheckOutTime(LocalDateTime.now());
        logger.debug("Reservation {} status updated to CHECKED_OUT", reservation.getReservationNumber());
        
        // Update room status
        Room room = reservation.getRoom();
        room.setStatus(Room.RoomStatus.CLEANING);
        roomRepository.save(room);
        logger.debug("Room {} status updated to CLEANING", room.getRoomNumber());
        
        Reservation updatedReservation = reservationRepository.save(reservation);
        logger.info("Successfully checked out reservation ID: {} for room: {}", reservationId, room.getRoomNumber());
        return reservationMapper.toDTO(updatedReservation);
    }

    public ReservationDTO cancelReservation(Long reservationId) {
        logger.info("Cancelling reservation ID: {}", reservationId);
        
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    logger.error("Reservation not found with ID: {}", reservationId);
                    return new RuntimeException("Reservation not found with id: " + reservationId);
                });
        
        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            logger.warn("Failed to cancel reservation ID {}: Already checked out", reservationId);
            throw new RuntimeException("Cannot cancel a reservation that has been checked out");
        }
        
        reservation.setStatus(ReservationStatus.CANCELLED);
        logger.debug("Reservation {} status updated to CANCELLED", reservation.getReservationNumber());
        
        // Room status is not updated here - availability is determined by date range and reservations
        // If room status is MAINTENANCE or CLEANING, it remains as is. If it's READY, it stays READY.
        
        Reservation updatedReservation = reservationRepository.save(reservation);
        logger.info("Successfully cancelled reservation ID: {}", reservationId);
        return reservationMapper.toDTO(updatedReservation);
    }

    @Transactional(readOnly = true)
    public ReservationDTO getReservationById(Long id) {
        logger.debug("Fetching reservation with ID: {}", id);
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Reservation not found with ID: {}", id);
                    return new RuntimeException("Reservation not found with id: " + id);
                });
        logger.debug("Successfully retrieved reservation with ID: {}", id);
        return reservationMapper.toDTO(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationDTO getReservationByNumber(String reservationNumber) {
        logger.debug("Fetching reservation with number: {}", reservationNumber);
        Reservation reservation = reservationRepository.findByReservationNumber(reservationNumber)
                .orElseThrow(() -> {
                    logger.error("Reservation not found with number: {}", reservationNumber);
                    return new RuntimeException("Reservation not found with number: " + reservationNumber);
                });
        logger.debug("Successfully retrieved reservation with number: {}", reservationNumber);
        return reservationMapper.toDTO(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getAllReservations() {
        logger.debug("Fetching all reservations");
        List<ReservationDTO> reservations = reservationRepository.findAll().stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
        logger.info("Retrieved {} reservation(s)", reservations.size());
        return reservations;
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getReservationsByGuest(Long guestId) {
        logger.debug("Fetching reservations for guest ID: {}", guestId);
        List<ReservationDTO> reservations = reservationRepository.findByGuestId(guestId).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
        logger.info("Retrieved {} reservation(s) for guest ID: {}", reservations.size(), guestId);
        return reservations;
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getReservationsByStatus(ReservationStatus status) {
        logger.debug("Fetching reservations with status: {}", status);
        List<ReservationDTO> reservations = reservationRepository.findByStatus(status).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
        logger.info("Retrieved {} reservation(s) with status: {}", reservations.size(), status);
        return reservations;
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getReservationsByDateRange(LocalDate startDate, LocalDate endDate) {
        logger.debug("Fetching reservations from {} to {}", startDate, endDate);
        List<ReservationDTO> reservations = reservationRepository.findReservationsByDateRange(startDate, endDate).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
        logger.info("Retrieved {} reservation(s) in date range {} to {}", reservations.size(), startDate, endDate);
        return reservations;
    }
}

