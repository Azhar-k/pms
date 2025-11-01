package com.klm.pms.repository;

import com.klm.pms.model.Room;
import com.klm.pms.model.Room.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomNumber(String roomNumber);
    List<Room> findByStatus(RoomStatus status);
    List<Room> findByRoomType(String roomType);
    List<Room> findByStatusAndRoomType(RoomStatus status, String roomType);
}

