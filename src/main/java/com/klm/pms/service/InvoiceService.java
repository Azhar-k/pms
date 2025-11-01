package com.klm.pms.service;

import com.klm.pms.dto.InvoiceDTO;
import com.klm.pms.mapper.ReservationMapper;
import com.klm.pms.model.Invoice;
import com.klm.pms.model.Invoice.InvoiceStatus;
import com.klm.pms.model.InvoiceItem;
import com.klm.pms.model.Reservation;
import com.klm.pms.repository.InvoiceItemRepository;
import com.klm.pms.repository.InvoiceRepository;
import com.klm.pms.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private ReservationMapper reservationMapper;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.10"); // 10% tax rate

    public InvoiceDTO generateInvoice(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found with id: " + reservationId));
        
        // Check if invoice already exists
        List<Invoice> existingInvoices = invoiceRepository.findByReservationId(reservationId);
        if (!existingInvoices.isEmpty()) {
            throw new RuntimeException("Invoice already exists for this reservation");
        }
        
        // Calculate room charges
        long nights = ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate());
        BigDecimal roomCharge = reservation.getRoom().getPricePerNight().multiply(BigDecimal.valueOf(nights));
        
        // Calculate totals
        BigDecimal subtotal = roomCharge;
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE);
        BigDecimal totalAmount = subtotal.add(taxAmount);
        
        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setReservation(reservation);
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setIssuedDate(LocalDateTime.now());
        
        // Add room charge item
        InvoiceItem roomItem = new InvoiceItem();
        roomItem.setInvoice(invoice);
        roomItem.setDescription("Room charge for " + nights + " night(s) - " + reservation.getRoom().getRoomNumber());
        roomItem.setQuantity((int) nights);
        roomItem.setUnitPrice(reservation.getRoom().getPricePerNight());
        roomItem.setAmount(roomCharge);
        roomItem.setCategory("ROOM_CHARGE");
        
        invoice.getItems().add(roomItem);
        
        Invoice savedInvoice = invoiceRepository.save(invoice);
        invoiceItemRepository.save(roomItem);
        
        return toDTO(savedInvoice);
    }

    public InvoiceDTO addInvoiceItem(Long invoiceId, InvoiceDTO.InvoiceItemDTO itemDTO) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
        
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new RuntimeException("Cannot add items to a paid invoice");
        }
        
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(itemDTO.getDescription());
        item.setQuantity(itemDTO.getQuantity());
        item.setUnitPrice(itemDTO.getUnitPrice());
        item.setAmount(itemDTO.getAmount());
        item.setCategory(itemDTO.getCategory());
        
        invoice.getItems().add(item);
        
        // Recalculate totals
        BigDecimal subtotal = invoice.getItems().stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE);
        BigDecimal totalAmount = subtotal.add(taxAmount).subtract(invoice.getDiscountAmount());
        
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);
        
        invoiceItemRepository.save(item);
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        
        return toDTO(updatedInvoice);
    }

    public InvoiceDTO markInvoiceAsPaid(Long invoiceId, String paymentMethod) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
        
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new RuntimeException("Invoice is already paid");
        }
        
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidDate(LocalDateTime.now());
        invoice.setPaymentMethod(paymentMethod);
        
        // Update reservation payment status
        Reservation reservation = invoice.getReservation();
        reservation.setPaymentStatus("PAID");
        reservationRepository.save(reservation);
        
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        return toDTO(updatedInvoice);
    }

    @Transactional(readOnly = true)
    public InvoiceDTO getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
        return toDTO(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceDTO getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found with number: " + invoiceNumber));
        return toDTO(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByReservation(Long reservationId) {
        return invoiceRepository.findByReservationId(reservationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByStatus(InvoiceStatus status) {
        return invoiceRepository.findByStatus(status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private InvoiceDTO toDTO(Invoice invoice) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setReservationId(invoice.getReservation() != null ? invoice.getReservation().getId() : null);
        dto.setSubtotal(invoice.getSubtotal());
        dto.setTaxAmount(invoice.getTaxAmount());
        dto.setDiscountAmount(invoice.getDiscountAmount());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setStatus(invoice.getStatus());
        dto.setIssuedDate(invoice.getIssuedDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setPaidDate(invoice.getPaidDate());
        dto.setPaymentMethod(invoice.getPaymentMethod());
        dto.setNotes(invoice.getNotes());
        
        // Include reservation details
        if (invoice.getReservation() != null) {
            dto.setReservation(reservationMapper.toDTO(invoice.getReservation()));
        }
        
        // Include invoice items
        if (invoice.getItems() != null) {
            List<InvoiceDTO.InvoiceItemDTO> itemDTOs = invoice.getItems().stream()
                    .map(item -> {
                        InvoiceDTO.InvoiceItemDTO itemDTO = new InvoiceDTO.InvoiceItemDTO();
                        itemDTO.setId(item.getId());
                        itemDTO.setDescription(item.getDescription());
                        itemDTO.setQuantity(item.getQuantity());
                        itemDTO.setUnitPrice(item.getUnitPrice());
                        itemDTO.setAmount(item.getAmount());
                        itemDTO.setCategory(item.getCategory());
                        return itemDTO;
                    })
                    .collect(Collectors.toList());
            dto.setItems(itemDTOs);
        }
        
        return dto;
    }
}

