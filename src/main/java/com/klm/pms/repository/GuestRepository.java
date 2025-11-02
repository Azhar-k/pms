package com.klm.pms.repository;

import com.klm.pms.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long>, JpaSpecificationExecutor<Guest> {
    Optional<Guest> findByEmail(String email);
    Optional<Guest> findByPhoneNumber(String phoneNumber);
}

