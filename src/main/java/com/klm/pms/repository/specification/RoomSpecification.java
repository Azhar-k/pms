package com.klm.pms.repository.specification;

import com.klm.pms.dto.RoomFilterRequest;
import com.klm.pms.model.Room;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class RoomSpecification {
    
    public static Specification<Room> withFilters(RoomFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter.getRoomNumber() != null && !filter.getRoomNumber().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("roomNumber")), 
                    "%" + filter.getRoomNumber().toLowerCase() + "%"));
            }
            
            if (filter.getRoomTypeId() != null) {
                predicates.add(cb.equal(root.get("roomType").get("id"), filter.getRoomTypeId()));
            }
            
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            
            if (filter.getMinMaxOccupancy() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxOccupancy"), filter.getMinMaxOccupancy()));
            }
            
            if (filter.getMaxMaxOccupancy() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("maxOccupancy"), filter.getMaxMaxOccupancy()));
            }
            
            if (filter.getFloor() != null) {
                predicates.add(cb.equal(root.get("floor"), filter.getFloor()));
            }
            
            if (filter.getHasBalcony() != null) {
                predicates.add(cb.equal(root.get("hasBalcony"), filter.getHasBalcony()));
            }
            
            if (filter.getHasView() != null) {
                predicates.add(cb.equal(root.get("hasView"), filter.getHasView()));
            }
            
            if (filter.getSearchTerm() != null && !filter.getSearchTerm().isEmpty()) {
                String searchTerm = filter.getSearchTerm().toLowerCase();
                Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("roomNumber")), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("amenities")), "%" + searchTerm + "%")
                );
                predicates.add(searchPredicate);
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

