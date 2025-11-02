package com.klm.pms.repository;

import com.klm.pms.model.Reservation;
import com.klm.pms.model.Reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {
    Optional<Reservation> findByReservationNumber(String reservationNumber);
    List<Reservation> findByGuestId(Long guestId);
    List<Reservation> findByRoomId(Long roomId);
    List<Reservation> findByStatus(ReservationStatus status);
    
    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId AND " +
           "((r.checkInDate < :checkOutDate AND r.checkOutDate > :checkInDate) AND " +
           "r.status NOT IN ('CANCELLED', 'NO_SHOW', 'CHECKED_OUT'))")
    List<Reservation> findConflictingReservations(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );
    
    @Query("SELECT r FROM Reservation r WHERE r.checkInDate BETWEEN :startDate AND :endDate " +
           "OR r.checkOutDate BETWEEN :startDate AND :endDate")
    List<Reservation> findReservationsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

