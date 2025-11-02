package com.klm.pms.repository.specification;

import com.klm.pms.dto.GuestFilterRequest;
import com.klm.pms.model.Guest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class GuestSpecification {
    
    public static Specification<Guest> withFilters(GuestFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter.getFirstName() != null && !filter.getFirstName().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("firstName")), 
                    "%" + filter.getFirstName().toLowerCase() + "%"));
            }
            
            if (filter.getLastName() != null && !filter.getLastName().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("lastName")), 
                    "%" + filter.getLastName().toLowerCase() + "%"));
            }
            
            if (filter.getEmail() != null && !filter.getEmail().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("email")), 
                    "%" + filter.getEmail().toLowerCase() + "%"));
            }
            
            if (filter.getPhoneNumber() != null && !filter.getPhoneNumber().isEmpty()) {
                predicates.add(cb.like(root.get("phoneNumber"), "%" + filter.getPhoneNumber() + "%"));
            }
            
            if (filter.getCity() != null && !filter.getCity().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("city")), 
                    "%" + filter.getCity().toLowerCase() + "%"));
            }
            
            if (filter.getState() != null && !filter.getState().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("state")), 
                    "%" + filter.getState().toLowerCase() + "%"));
            }
            
            if (filter.getCountry() != null && !filter.getCountry().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("country")), 
                    "%" + filter.getCountry().toLowerCase() + "%"));
            }
            
            if (filter.getIdentificationType() != null && !filter.getIdentificationType().isEmpty()) {
                predicates.add(cb.equal(root.get("identificationType"), filter.getIdentificationType()));
            }
            
            if (filter.getSearchTerm() != null && !filter.getSearchTerm().isEmpty()) {
                String searchTerm = filter.getSearchTerm().toLowerCase();
                Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("firstName")), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("lastName")), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("email")), "%" + searchTerm + "%"),
                    cb.like(root.get("phoneNumber"), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("address")), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("city")), "%" + searchTerm + "%")
                );
                predicates.add(searchPredicate);
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

