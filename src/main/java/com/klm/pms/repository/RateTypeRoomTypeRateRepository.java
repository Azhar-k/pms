package com.klm.pms.repository;

import com.klm.pms.model.RateTypeRoomTypeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateTypeRoomTypeRateRepository extends JpaRepository<RateTypeRoomTypeRate, Long> {
    List<RateTypeRoomTypeRate> findByRateTypeId(Long rateTypeId);
    Optional<RateTypeRoomTypeRate> findByRateTypeIdAndRoomTypeId(Long rateTypeId, Long roomTypeId);
    void deleteByRateTypeId(Long rateTypeId);
}

