package com.klm.pms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klm.pms.dto.GuestDTO;
import com.klm.pms.dto.GuestFilterRequest;
import com.klm.pms.dto.PageResponse;
import com.klm.pms.exception.DuplicateEntityException;
import com.klm.pms.exception.EntityNotFoundException;
import com.klm.pms.mapper.GuestMapper;
import com.klm.pms.model.Guest;
import com.klm.pms.repository.GuestRepository;
import com.klm.pms.repository.specification.GuestSpecification;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GuestService {

    private static final Logger logger = LoggerFactory.getLogger(GuestService.class);

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private GuestMapper guestMapper;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ObjectMapper objectMapper;

    public GuestDTO createGuest(GuestDTO guestDTO) {
        logger.info("Creating new guest - email: {}", guestDTO != null ? guestDTO.getEmail() : "null");
        
        // Defensive checks
        ValidationUtil.requireNonNull(guestDTO, "guestDTO");
        ValidationUtil.requireNonBlank(guestDTO.getEmail(), "email");
        
        // Validate email format (basic check)
        if (!guestDTO.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            logger.warn("Invalid email format: {}", guestDTO.getEmail());
            throw new com.klm.pms.exception.ValidationException("email", "Invalid email format");
        }
        
        // Check for duplicate email
        if (guestRepository.findByEmail(guestDTO.getEmail()).isPresent()) {
            logger.warn("Failed to create guest: Email {} already exists", guestDTO.getEmail());
            throw new DuplicateEntityException(Constants.AUDIT_ENTITY_GUEST, "email", guestDTO.getEmail());
        }
        
        logger.debug("Guest validation passed for email: {}", guestDTO.getEmail());
        
        Guest guest = guestMapper.toEntity(guestDTO);
        Guest savedGuest = guestRepository.save(guest);
        logger.info("Successfully created guest with ID: {} and email: {}", savedGuest.getId(), savedGuest.getEmail());
        
        // Audit log
        try {
            auditService.logCreate(Constants.AUDIT_ENTITY_GUEST, savedGuest.getId(), savedGuest);
        } catch (Exception e) {
            logger.error("Failed to create audit log for guest creation, but guest was created successfully. Guest ID: {}", 
                    savedGuest.getId(), e);
            // Don't fail the operation if audit logging fails
        }
        
        return guestMapper.toDTO(savedGuest);
    }

    public GuestDTO updateGuest(Long id, GuestDTO guestDTO) {
        logger.info("Updating guest with ID: {}", id);
        
        // Defensive checks
        ValidationUtil.requireNonNull(id, "id");
        ValidationUtil.requireNonNull(guestDTO, "guestDTO");
        
        Guest existingGuest = guestRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Guest not found with ID: {}", id);
                    return new EntityNotFoundException(Constants.AUDIT_ENTITY_GUEST, id);
                });
        
        // Store old state for audit BEFORE any modifications - create a deep copy
        Guest oldGuest = null;
        try {
            // Use ObjectMapper to create a deep copy of the entity
            String json = objectMapper.writeValueAsString(existingGuest);
            oldGuest = objectMapper.readValue(json, Guest.class);
            logger.debug("Created deep copy of guest {} for audit logging", id);
        } catch (Exception e) {
            logger.warn("Failed to create deep copy of guest for audit, will use partial copy", e);
            // Fallback to partial copy if deep copy fails
            oldGuest = new Guest();
            oldGuest.setId(existingGuest.getId());
            oldGuest.setFirstName(existingGuest.getFirstName());
            oldGuest.setLastName(existingGuest.getLastName());
            oldGuest.setEmail(existingGuest.getEmail());
            oldGuest.setPhoneNumber(existingGuest.getPhoneNumber());
            oldGuest.setAddress(existingGuest.getAddress());
            oldGuest.setCity(existingGuest.getCity());
            oldGuest.setState(existingGuest.getState());
            oldGuest.setCountry(existingGuest.getCountry());
            oldGuest.setPostalCode(existingGuest.getPostalCode());
            oldGuest.setIdentificationType(existingGuest.getIdentificationType());
            oldGuest.setIdentificationNumber(existingGuest.getIdentificationNumber());
        }
        
        // Check email uniqueness if it's being changed
        if (guestDTO.getEmail() != null && !guestDTO.getEmail().equals(existingGuest.getEmail())) {
            ValidationUtil.requireNonBlank(guestDTO.getEmail(), "email");
            
            // Validate email format
            if (!guestDTO.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                logger.warn("Invalid email format: {}", guestDTO.getEmail());
                throw new com.klm.pms.exception.ValidationException("email", "Invalid email format");
            }
            
            if (guestRepository.findByEmail(guestDTO.getEmail()).isPresent()) {
                logger.warn("Failed to update guest ID {}: Email {} already exists", id, guestDTO.getEmail());
                throw new DuplicateEntityException(Constants.AUDIT_ENTITY_GUEST, "email", guestDTO.getEmail());
            }
            logger.debug("Email changed from {} to {}", existingGuest.getEmail(), guestDTO.getEmail());
        }
        
        existingGuest.setFirstName(guestDTO.getFirstName());
        existingGuest.setLastName(guestDTO.getLastName());
        existingGuest.setEmail(guestDTO.getEmail());
        existingGuest.setPhoneNumber(guestDTO.getPhoneNumber());
        existingGuest.setAddress(guestDTO.getAddress());
        existingGuest.setCity(guestDTO.getCity());
        existingGuest.setState(guestDTO.getState());
        existingGuest.setCountry(guestDTO.getCountry());
        existingGuest.setPostalCode(guestDTO.getPostalCode());
        existingGuest.setIdentificationType(guestDTO.getIdentificationType());
        existingGuest.setIdentificationNumber(guestDTO.getIdentificationNumber());
        
        Guest updatedGuest = guestRepository.save(existingGuest);
        logger.info("Successfully updated guest with ID: {}", id);
        
        // Audit log
        try {
            auditService.logUpdate(Constants.AUDIT_ENTITY_GUEST, id, oldGuest, updatedGuest);
        } catch (Exception e) {
            logger.error("Failed to create audit log for guest update, but guest was updated successfully. Guest ID: {}", 
                    id, e);
            // Don't fail the operation if audit logging fails
        }
        
        return guestMapper.toDTO(updatedGuest);
    }

    @Transactional(readOnly = true)
    public GuestDTO getGuestById(Long id) {
        logger.debug("Fetching guest with ID: {}", id);
        ValidationUtil.requireNonNull(id, "id");
        
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Guest not found with ID: {}", id);
                    return new EntityNotFoundException(Constants.AUDIT_ENTITY_GUEST, id);
                });
        logger.debug("Successfully retrieved guest with ID: {}", id);
        return guestMapper.toDTO(guest);
    }

    @Transactional(readOnly = true)
    public GuestDTO getGuestByEmail(String email) {
        logger.debug("Fetching guest with email: {}", email);
        ValidationUtil.requireNonBlank(email, "email");
        
        Guest guest = guestRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Guest not found with email: {}", email);
                    return new EntityNotFoundException(Constants.AUDIT_ENTITY_GUEST, email);
                });
        logger.debug("Successfully retrieved guest with email: {}", email);
        return guestMapper.toDTO(guest);
    }

    @Transactional(readOnly = true)
    public List<GuestDTO> getAllGuests() {
        logger.debug("Fetching all guests");
        List<GuestDTO> guests = guestRepository.findAll().stream()
                .map(guestMapper::toDTO)
                .collect(Collectors.toList());
        logger.info("Retrieved {} guest(s)", guests.size());
        return guests;
    }

    @Transactional(readOnly = true)
    public PageResponse<GuestDTO> getAllGuestsPaginated(GuestFilterRequest filter, int page, int size, String sortBy, String sortDir) {
        logger.debug("Fetching guests with pagination - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
        
        // Validate and normalize pagination parameters
        int[] pagination = ValidationUtil.validateAndNormalizePagination(page, size);
        int normalizedPage = pagination[0];
        int normalizedSize = pagination[1];
        
        // Default sorting
        Sort sort = Sort.by(Sort.Direction.ASC, "lastName");
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = sortDir != null && sortDir.equalsIgnoreCase("desc") 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
            sort = Sort.by(direction, sortBy);
        }
        
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, sort);
        
        // Build specification for filtering
        Specification<Guest> spec = GuestSpecification.withFilters(filter);
        
        Page<Guest> guestPage = guestRepository.findAll(spec, pageable);
        
        List<GuestDTO> guestDTOs = guestPage.getContent().stream()
                .map(guestMapper::toDTO)
                .collect(Collectors.toList());
        
        PageResponse<GuestDTO> response = new PageResponse<>(
            guestDTOs,
            guestPage.getNumber(),
            guestPage.getSize(),
            guestPage.getTotalElements()
        );
        
        logger.info("Retrieved {} guest(s) out of {} total", guestDTOs.size(), guestPage.getTotalElements());
        return response;
    }

    public void deleteGuest(Long id) {
        logger.info("Deleting guest with ID: {}", id);
        ValidationUtil.requireNonNull(id, "id");
        
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Failed to delete: Guest not found with ID: {}", id);
                    return new EntityNotFoundException(Constants.AUDIT_ENTITY_GUEST, id);
                });
        
        // Audit log before deletion
        try {
            auditService.logDelete(Constants.AUDIT_ENTITY_GUEST, id, guest);
        } catch (Exception e) {
            logger.error("Failed to create audit log for guest deletion, but proceeding with deletion. Guest ID: {}", 
                    id, e);
            // Don't fail the operation if audit logging fails
        }
        
        guestRepository.deleteById(id);
        logger.info("Successfully deleted guest with ID: {}", id);
    }
}

