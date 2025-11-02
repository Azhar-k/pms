package com.klm.pms.service;

import com.klm.pms.dto.PageResponse;
import com.klm.pms.dto.ReservationDTO;
import com.klm.pms.dto.ReservationFilterRequest;
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
import com.klm.pms.repository.specification.ReservationSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    public PageResponse<ReservationDTO> getAllReservationsPaginated(ReservationFilterRequest filter, int page, int size, String sortBy, String sortDir) {
        logger.debug("Fetching reservations with pagination - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
        
        // Default sorting
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = sortDir != null && sortDir.equalsIgnoreCase("desc") 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
            sort = Sort.by(direction, sortBy);
        }
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Build specification for filtering
        Specification<Reservation> spec = ReservationSpecification.withFilters(filter);
        
        Page<Reservation> reservationPage = reservationRepository.findAll(spec, pageable);
        
        List<ReservationDTO> reservationDTOs = reservationPage.getContent().stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
        
        PageResponse<ReservationDTO> response = new PageResponse<>(
            reservationDTOs,
            reservationPage.getNumber(),
            reservationPage.getSize(),
            reservationPage.getTotalElements()
        );
        
        logger.info("Retrieved {} reservation(s) out of {} total", reservationDTOs.size(), reservationPage.getTotalElements());
        return response;
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

    public ReservationDTO updateReservation(Long id, ReservationDTO reservationDTO) {
        logger.info("Updating reservation ID: {}", id);
        
        Reservation existingReservation = reservationRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Reservation not found with ID: {}", id);
                    return new RuntimeException("Reservation not found with id: " + id);
                });
        
        // Cannot update checked out reservations
        if (existingReservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            logger.warn("Failed to update reservation ID {}: Already checked out", id);
            throw new RuntimeException("Cannot update a reservation that has been checked out");
        }
        
        // Validate dates
        if (reservationDTO.getCheckInDate() != null && reservationDTO.getCheckOutDate() != null) {
            if (reservationDTO.getCheckInDate().isAfter(reservationDTO.getCheckOutDate())) {
                logger.warn("Failed to update reservation: Check-in date {} is after check-out date {}", 
                        reservationDTO.getCheckInDate(), reservationDTO.getCheckOutDate());
                throw new RuntimeException("Check-in date must be before check-out date");
            }
        }
        
        // Get guest if changed
        Guest guest = existingReservation.getGuest();
        if (reservationDTO.getGuestId() != null && 
            !reservationDTO.getGuestId().equals(existingReservation.getGuest().getId())) {
            guest = guestRepository.findById(reservationDTO.getGuestId())
                    .orElseThrow(() -> {
                        logger.error("Guest not found with ID: {}", reservationDTO.getGuestId());
                        return new RuntimeException("Guest not found with id: " + reservationDTO.getGuestId());
                    });
            logger.debug("Guest changed from {} {} to {} {}", 
                    existingReservation.getGuest().getFirstName(), existingReservation.getGuest().getLastName(),
                    guest.getFirstName(), guest.getLastName());
        }
        
        // Get room if changed
        Room room = existingReservation.getRoom();
        boolean roomChanged = reservationDTO.getRoomId() != null && 
                             !reservationDTO.getRoomId().equals(existingReservation.getRoom().getId());
        
        if (roomChanged) {
            room = roomRepository.findById(reservationDTO.getRoomId())
                    .orElseThrow(() -> {
                        logger.error("Room not found with ID: {}", reservationDTO.getRoomId());
                        return new RuntimeException("Room not found with id: " + reservationDTO.getRoomId());
                    });
            logger.debug("Room changed from {} to {}", existingReservation.getRoom().getRoomNumber(), room.getRoomNumber());
        }
        
        // Get rate type if changed
        RateType rateType = existingReservation.getRateType();
        if (reservationDTO.getRateTypeId() != null && 
            !reservationDTO.getRateTypeId().equals(existingReservation.getRateType().getId())) {
            rateType = rateTypeRepository.findById(reservationDTO.getRateTypeId())
                    .orElseThrow(() -> {
                        logger.error("Rate type not found with ID: {}", reservationDTO.getRateTypeId());
                        return new RuntimeException("Rate type not found with id: " + reservationDTO.getRateTypeId());
                    });
            logger.debug("Rate type changed to: {}", rateType.getName());
        }
        
        // Save original dates for comparison
        LocalDate originalCheckInDate = existingReservation.getCheckInDate();
        LocalDate originalCheckOutDate = existingReservation.getCheckOutDate();
        
        // Determine dates to use for availability check
        LocalDate checkInDate = reservationDTO.getCheckInDate() != null ? 
                reservationDTO.getCheckInDate() : originalCheckInDate;
        LocalDate checkOutDate = reservationDTO.getCheckOutDate() != null ? 
                reservationDTO.getCheckOutDate() : originalCheckOutDate;
        
        // Check room availability if room or dates changed
        boolean datesChanged = !checkInDate.equals(originalCheckInDate) || !checkOutDate.equals(originalCheckOutDate);
        if (roomChanged || datesChanged) {
            List<Reservation> conflictingReservations = reservationRepository.findConflictingReservations(
                    room.getId(), checkInDate, checkOutDate);
            
            // Exclude current reservation from conflicts
            conflictingReservations = conflictingReservations.stream()
                    .filter(r -> !r.getId().equals(existingReservation.getId()))
                    .collect(Collectors.toList());
            
            if (!conflictingReservations.isEmpty()) {
                logger.warn("Failed to update reservation: Room {} has {} conflicting reservation(s)", 
                        room.getRoomNumber(), conflictingReservations.size());
                throw new RuntimeException("Room is not available for the selected dates");
            }
        }
        
        // Check room capacity if number of guests changed
        if (reservationDTO.getNumberOfGuests() != null && 
            !reservationDTO.getNumberOfGuests().equals(existingReservation.getNumberOfGuests())) {
            if (room.getMaxOccupancy() != null && reservationDTO.getNumberOfGuests() > room.getMaxOccupancy()) {
                logger.warn("Failed to update reservation: Number of guests {} exceeds room capacity {}", 
                        reservationDTO.getNumberOfGuests(), room.getMaxOccupancy());
                throw new RuntimeException("Number of guests exceeds room capacity");
            }
        }
        
        // Update reservation fields
        if (reservationDTO.getCheckInDate() != null) {
            existingReservation.setCheckInDate(reservationDTO.getCheckInDate());
        }
        if (reservationDTO.getCheckOutDate() != null) {
            existingReservation.setCheckOutDate(reservationDTO.getCheckOutDate());
        }
        if (reservationDTO.getNumberOfGuests() != null) {
            existingReservation.setNumberOfGuests(reservationDTO.getNumberOfGuests());
        }
        if (reservationDTO.getSpecialRequests() != null) {
            existingReservation.setSpecialRequests(reservationDTO.getSpecialRequests());
        }
        
        existingReservation.setGuest(guest);
        existingReservation.setRoom(room);
        existingReservation.setRateType(rateType);
        
        // Recalculate total amount if dates, room type, or rate type changed
        boolean rateTypeChanged = !rateType.getId().equals(existingReservation.getRateType().getId());
        boolean roomTypeChanged = roomChanged || 
                                  !room.getRoomType().getId().equals(existingReservation.getRoom().getRoomType().getId());
        
        if (datesChanged || roomTypeChanged || rateTypeChanged) {
            BigDecimal ratePerNight = rateTypeService.getRateForRoomType(rateType.getId(), room.getRoomType().getId());
            long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            BigDecimal totalAmount = ratePerNight.multiply(BigDecimal.valueOf(nights));
            existingReservation.setTotalAmount(totalAmount);
            logger.debug("Recalculated total amount: {} for {} night(s) at rate: {}", totalAmount, nights, ratePerNight);
        }
        
        Reservation updatedReservation = reservationRepository.save(existingReservation);
        logger.info("Successfully updated reservation ID: {}", id);
        return reservationMapper.toDTO(updatedReservation);
    }
}

