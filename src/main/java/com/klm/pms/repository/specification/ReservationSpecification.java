package com.klm.pms.repository.specification;

import com.klm.pms.dto.ReservationFilterRequest;
import com.klm.pms.model.Reservation;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ReservationSpecification {
    
    public static Specification<Reservation> withFilters(ReservationFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter.getReservationNumber() != null && !filter.getReservationNumber().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("reservationNumber")), 
                    "%" + filter.getReservationNumber().toLowerCase() + "%"));
            }
            
            if (filter.getGuestId() != null) {
                predicates.add(cb.equal(root.get("guest").get("id"), filter.getGuestId()));
            }
            
            if (filter.getRoomId() != null) {
                predicates.add(cb.equal(root.get("room").get("id"), filter.getRoomId()));
            }
            
            if (filter.getRateTypeId() != null) {
                predicates.add(cb.equal(root.get("rateType").get("id"), filter.getRateTypeId()));
            }
            
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            
            if (filter.getCheckInDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkInDate"), filter.getCheckInDateFrom()));
            }
            
            if (filter.getCheckInDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("checkInDate"), filter.getCheckInDateTo()));
            }
            
            if (filter.getCheckOutDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkOutDate"), filter.getCheckOutDateFrom()));
            }
            
            if (filter.getCheckOutDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("checkOutDate"), filter.getCheckOutDateTo()));
            }
            
            if (filter.getMinNumberOfGuests() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("numberOfGuests"), filter.getMinNumberOfGuests()));
            }
            
            if (filter.getMaxNumberOfGuests() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("numberOfGuests"), filter.getMaxNumberOfGuests()));
            }
            
            if (filter.getPaymentStatus() != null && !filter.getPaymentStatus().isEmpty()) {
                predicates.add(cb.equal(root.get("paymentStatus"), filter.getPaymentStatus()));
            }
            
            if (filter.getSearchTerm() != null && !filter.getSearchTerm().isEmpty()) {
                String searchTerm = filter.getSearchTerm().toLowerCase();
                Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("reservationNumber")), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("specialRequests")), "%" + searchTerm + "%")
                );
                predicates.add(searchPredicate);
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

