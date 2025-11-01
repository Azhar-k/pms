package com.klm.pms.repository;

import com.klm.pms.model.RateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RateTypeRepository extends JpaRepository<RateType, Long> {
    Optional<RateType> findByName(String name);
    boolean existsByName(String name);
}

