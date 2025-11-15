package com.klm.pms.service;

import com.klm.pms.dto.InvoiceDTO;
import com.klm.pms.dto.InvoiceFilterRequest;
import com.klm.pms.dto.PageResponse;
import com.klm.pms.exception.BusinessLogicException;
import com.klm.pms.exception.EntityNotFoundException;
import com.klm.pms.mapper.ReservationMapper;
import com.klm.pms.model.Invoice;
import com.klm.pms.model.Invoice.InvoiceStatus;
import com.klm.pms.model.InvoiceItem;
import com.klm.pms.model.Reservation;
import com.klm.pms.repository.InvoiceItemRepository;
import com.klm.pms.repository.InvoiceRepository;
import com.klm.pms.repository.ReservationRepository;
import com.klm.pms.repository.specification.InvoiceSpecification;
import com.klm.pms.util.Constants;
import com.klm.pms.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private RateTypeService rateTypeService;

    public InvoiceDTO generateInvoice(Long reservationId) {
        logger.info("Generating invoice for reservation ID: {}", reservationId);
        
        ValidationUtil.requireNonNull(reservationId, "reservationId");
        
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    logger.error("Reservation not found with ID: {}", reservationId);
                    return new EntityNotFoundException(Constants.AUDIT_ENTITY_RESERVATION, reservationId);
                });
        
        // Check if invoice already exists
        List<Invoice> existingInvoices = invoiceRepository.findByReservationId(reservationId);
        if (!existingInvoices.isEmpty()) {
            logger.warn("Failed to generate invoice: Invoice already exists for reservation ID: {}", reservationId);
            throw new BusinessLogicException("Invoice already exists for this reservation");
        }
        
        // Calculate room charges
        long nights = ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate());
        // Get rate from rate type for this room type
        BigDecimal ratePerNight = rateTypeService.getRateForRoomType(
                reservation.getRateType().getId(), 
                reservation.getRoom().getRoomType().getId());
        BigDecimal roomCharge = ratePerNight.multiply(BigDecimal.valueOf(nights));
        logger.debug("Calculated room charge: {} for {} night(s) at rate: {}", 
                roomCharge, nights, ratePerNight);
        
        // Calculate totals
        BigDecimal subtotal = roomCharge;
        BigDecimal taxAmount = subtotal.multiply(Constants.TAX_RATE);
        BigDecimal totalAmount = subtotal.add(taxAmount);
        logger.debug("Invoice totals - Subtotal: {}, Tax: {}, Total: {}", subtotal, taxAmount, totalAmount);
        
        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setReservation(reservation);
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setDiscountAmount(Constants.ZERO_AMOUNT);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setIssuedDate(LocalDateTime.now());
        
        // Add room charge item
        InvoiceItem roomItem = new InvoiceItem();
        roomItem.setInvoice(invoice);
        roomItem.setDescription("Room charge for " + nights + " night(s) - " + reservation.getRoom().getRoomNumber());
        roomItem.setQuantity((int) nights);
        roomItem.setUnitPrice(ratePerNight);
        roomItem.setAmount(roomCharge);
        roomItem.setCategory("ROOM_CHARGE");
        
        invoice.getItems().add(roomItem);
        
        Invoice savedInvoice = invoiceRepository.save(invoice);
        invoiceItemRepository.save(roomItem);
        logger.info("Successfully generated invoice with ID: {} and number: {} for total amount: {}", 
                savedInvoice.getId(), savedInvoice.getInvoiceNumber(), totalAmount);
        
        return toDTO(savedInvoice);
    }

    public InvoiceDTO addInvoiceItem(Long invoiceId, InvoiceDTO.InvoiceItemDTO itemDTO) {
        logger.info("Adding invoice item to invoice ID: {}", invoiceId);
        
        ValidationUtil.requireNonNull(invoiceId, "invoiceId");
        ValidationUtil.requireNonNull(itemDTO, "itemDTO");
        ValidationUtil.requireNonNull(itemDTO.getDescription(), "description");
        ValidationUtil.requireNonNull(itemDTO.getQuantity(), "quantity");
        ValidationUtil.requireNonNull(itemDTO.getUnitPrice(), "unitPrice");
        ValidationUtil.requirePositive(itemDTO.getQuantity(), "quantity");
        ValidationUtil.requireNonNegative(itemDTO.getUnitPrice(), "unitPrice");
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> {
                    logger.error("Invoice not found with ID: {}", invoiceId);
                    return new EntityNotFoundException(Constants.AUDIT_ENTITY_INVOICE, invoiceId);
                });
        
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            logger.warn("Failed to add item: Invoice ID {} is already paid", invoiceId);
            throw new BusinessLogicException("Cannot add items to a paid invoice");
        }
        
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(itemDTO.getDescription());
        item.setQuantity(itemDTO.getQuantity());
        item.setUnitPrice(itemDTO.getUnitPrice());
        item.setAmount(itemDTO.getAmount());
        item.setCategory(itemDTO.getCategory());
        
        invoice.getItems().add(item);
        logger.debug("Added item: {} - Quantity: {}, Amount: {}", itemDTO.getDescription(), itemDTO.getQuantity(), itemDTO.getAmount());
        
        // Recalculate totals
        BigDecimal subtotal = invoice.getItems().stream()
                .map(InvoiceItem::getAmount)
                .reduce(Constants.ZERO_AMOUNT, BigDecimal::add);
        
        BigDecimal taxAmount = subtotal.multiply(Constants.TAX_RATE);
        BigDecimal totalAmount = subtotal.add(taxAmount).subtract(invoice.getDiscountAmount());
        
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);
        logger.debug("Recalculated totals - Subtotal: {}, Tax: {}, Total: {}", subtotal, taxAmount, totalAmount);
        
        invoiceItemRepository.save(item);
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        logger.info("Successfully added invoice item to invoice ID: {}", invoiceId);
        
        return toDTO(updatedInvoice);
    }

    public InvoiceDTO removeInvoiceItem(Long invoiceId, Long itemId) {
        logger.info("Removing invoice item ID: {} from invoice ID: {}", itemId, invoiceId);
        
        ValidationUtil.requireNonNull(invoiceId, "invoiceId");
        ValidationUtil.requireNonNull(itemId, "itemId");
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> {
                    logger.error("Invoice not found with ID: {}", invoiceId);
                    return new EntityNotFoundException(Constants.AUDIT_ENTITY_INVOICE, invoiceId);
                });
        
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            logger.warn("Failed to remove item: Invoice ID {} is already paid", invoiceId);
            throw new BusinessLogicException("Cannot remove items from a paid invoice");
        }
        
        // Find the item in the invoice's items list (which is already loaded)
        InvoiceItem item = invoice.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    logger.error("Invoice item ID {} not found in invoice ID {}", itemId, invoiceId);
                    return new EntityNotFoundException("InvoiceItem", itemId);
                });
        
        // Remove item from invoice's items list
        invoice.getItems().remove(item);
        logger.debug("Removed item: {} - Amount: {}", item.getDescription(), item.getAmount());
        
        // Delete the item from database
        invoiceItemRepository.delete(item);
        
        // Recalculate totals
        BigDecimal subtotal = invoice.getItems().stream()
                .map(InvoiceItem::getAmount)
                .reduce(Constants.ZERO_AMOUNT, BigDecimal::add);
        
        BigDecimal taxAmount = subtotal.multiply(Constants.TAX_RATE);
        BigDecimal totalAmount = subtotal.add(taxAmount).subtract(invoice.getDiscountAmount());
        
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);
        logger.debug("Recalculated totals - Subtotal: {}, Tax: {}, Total: {}", subtotal, taxAmount, totalAmount);
        
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        logger.info("Successfully removed invoice item ID: {} from invoice ID: {}", itemId, invoiceId);
        
        return toDTO(updatedInvoice);
    }

    public InvoiceDTO markInvoiceAsPaid(Long invoiceId, String paymentMethod) {
        logger.info("Marking invoice ID: {} as paid with payment method: {}", invoiceId, paymentMethod);
        
        ValidationUtil.requireNonNull(invoiceId, "invoiceId");
        ValidationUtil.requireNonBlank(paymentMethod, "paymentMethod");
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> {
                    logger.error("Invoice not found with ID: {}", invoiceId);
                    return new EntityNotFoundException(Constants.AUDIT_ENTITY_INVOICE, invoiceId);
                });
        
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            logger.warn("Failed to mark as paid: Invoice ID {} is already paid", invoiceId);
            throw new BusinessLogicException("Invoice is already paid");
        }
        
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidDate(LocalDateTime.now());
        invoice.setPaymentMethod(paymentMethod);
        logger.debug("Invoice status updated to PAID");
        
        // Update reservation payment status
        Reservation reservation = invoice.getReservation();
        reservation.setPaymentStatus("PAID");
        reservationRepository.save(reservation);
        logger.debug("Reservation ID: {} payment status updated to PAID", reservation.getId());
        
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        logger.info("Successfully marked invoice ID: {} as paid", invoiceId);
        return toDTO(updatedInvoice);
    }

    @Transactional(readOnly = true)
    public InvoiceDTO getInvoiceById(Long id) {
        logger.debug("Fetching invoice with ID: {}", id);
        ValidationUtil.requireNonNull(id, "id");
        
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Invoice not found with ID: {}", id);
                    return new EntityNotFoundException(Constants.AUDIT_ENTITY_INVOICE, id);
                });
        logger.debug("Successfully retrieved invoice with ID: {}", id);
        return toDTO(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceDTO getInvoiceByNumber(String invoiceNumber) {
        logger.debug("Fetching invoice with number: {}", invoiceNumber);
        ValidationUtil.requireNonBlank(invoiceNumber, "invoiceNumber");
        
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> {
                    logger.error("Invoice not found with number: {}", invoiceNumber);
                    return new EntityNotFoundException(Constants.AUDIT_ENTITY_INVOICE, invoiceNumber);
                });
        logger.debug("Successfully retrieved invoice with number: {}", invoiceNumber);
        return toDTO(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByReservation(Long reservationId) {
        logger.debug("Fetching invoices for reservation ID: {}", reservationId);
        List<InvoiceDTO> invoices = invoiceRepository.findByReservationId(reservationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        logger.info("Retrieved {} invoice(s) for reservation ID: {}", invoices.size(), reservationId);
        return invoices;
    }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByStatus(InvoiceStatus status) {
        logger.debug("Fetching invoices with status: {}", status);
        List<InvoiceDTO> invoices = invoiceRepository.findByStatus(status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        logger.info("Retrieved {} invoice(s) with status: {}", invoices.size(), status);
        return invoices;
    }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> getAllInvoices() {
        logger.debug("Fetching all invoices");
        List<InvoiceDTO> invoices = invoiceRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        logger.info("Retrieved {} invoice(s)", invoices.size());
        return invoices;
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceDTO> getAllInvoicesPaginated(InvoiceFilterRequest filter, int page, int size, String sortBy, String sortDir) {
        logger.debug("Fetching invoices with pagination - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
        
        // Validate and normalize pagination parameters
        int[] pagination = ValidationUtil.validateAndNormalizePagination(page, size);
        int normalizedPage = pagination[0];
        int normalizedSize = pagination[1];
        
        // Default sorting
        Sort sort = Sort.by(Sort.Direction.DESC, "issuedDate");
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = sortDir != null && sortDir.equalsIgnoreCase("desc") 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
            sort = Sort.by(direction, sortBy);
        }
        
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, sort);
        
        // Build specification for filtering
        Specification<Invoice> spec = InvoiceSpecification.withFilters(filter);
        
        Page<Invoice> invoicePage = invoiceRepository.findAll(spec, pageable);
        
        List<InvoiceDTO> invoiceDTOs = invoicePage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        PageResponse<InvoiceDTO> response = new PageResponse<>(
            invoiceDTOs,
            invoicePage.getNumber(),
            invoicePage.getSize(),
            invoicePage.getTotalElements()
        );
        
        logger.info("Retrieved {} invoice(s) out of {} total", invoiceDTOs.size(), invoicePage.getTotalElements());
        return response;
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

