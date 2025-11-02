package com.klm.pms.repository.specification;

import com.klm.pms.dto.InvoiceFilterRequest;
import com.klm.pms.model.Invoice;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class InvoiceSpecification {
    
    public static Specification<Invoice> withFilters(InvoiceFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter.getInvoiceNumber() != null && !filter.getInvoiceNumber().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("invoiceNumber")), 
                    "%" + filter.getInvoiceNumber().toLowerCase() + "%"));
            }
            
            if (filter.getReservationId() != null) {
                predicates.add(cb.equal(root.get("reservation").get("id"), filter.getReservationId()));
            }
            
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            
            if (filter.getIssuedDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("issuedDate"), filter.getIssuedDateFrom()));
            }
            
            if (filter.getIssuedDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("issuedDate"), filter.getIssuedDateTo()));
            }
            
            if (filter.getPaidDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("paidDate"), filter.getPaidDateFrom()));
            }
            
            if (filter.getPaidDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("paidDate"), filter.getPaidDateTo()));
            }
            
            if (filter.getDueDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), filter.getDueDateFrom()));
            }
            
            if (filter.getDueDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), filter.getDueDateTo()));
            }
            
            if (filter.getPaymentMethod() != null && !filter.getPaymentMethod().isEmpty()) {
                predicates.add(cb.equal(root.get("paymentMethod"), filter.getPaymentMethod()));
            }
            
            if (filter.getSearchTerm() != null && !filter.getSearchTerm().isEmpty()) {
                String searchTerm = filter.getSearchTerm().toLowerCase();
                Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("invoiceNumber")), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("notes")), "%" + searchTerm + "%")
                );
                predicates.add(searchPredicate);
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

