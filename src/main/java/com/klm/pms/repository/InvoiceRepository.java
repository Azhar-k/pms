package com.klm.pms.repository;

import com.klm.pms.model.Invoice;
import com.klm.pms.model.Invoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    List<Invoice> findByReservationId(Long reservationId);
    List<Invoice> findByStatus(InvoiceStatus status);
}

