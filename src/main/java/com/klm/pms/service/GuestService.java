package com.klm.pms.service;

import com.klm.pms.dto.GuestDTO;
import com.klm.pms.dto.GuestFilterRequest;
import com.klm.pms.dto.PageResponse;
import com.klm.pms.mapper.GuestMapper;
import com.klm.pms.model.Guest;
import com.klm.pms.repository.GuestRepository;
import com.klm.pms.repository.specification.GuestSpecification;
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

    public GuestDTO createGuest(GuestDTO guestDTO) {
        logger.info("Creating new guest with email: {}", guestDTO.getEmail());

        if (guestDTO.getEmail() != null && guestRepository.findByEmail(guestDTO.getEmail()).isPresent()) {
            logger.warn("Failed to create guest: Email {} already exists", guestDTO.getEmail());
            throw new RuntimeException("Guest with email " + guestDTO.getEmail() + " already exists");
        }
        
        logger.debug("Guest validation passed for email: {}", guestDTO.getEmail());
        
        Guest guest = guestMapper.toEntity(guestDTO);
        Guest savedGuest = guestRepository.save(guest);
        logger.info("Successfully created guest with ID: {} and email: {}", savedGuest.getId(), savedGuest.getEmail());
        return guestMapper.toDTO(savedGuest);
    }

    public GuestDTO updateGuest(Long id, GuestDTO guestDTO) {
        logger.info("Updating guest with ID: {}", id);
        
        Guest existingGuest = guestRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Guest not found with ID: {}", id);
                    return new RuntimeException("Guest not found with id: " + id);
                });
        
        // Check email uniqueness if it's being changed
        if (guestDTO.getEmail() != null && !guestDTO.getEmail().equals(existingGuest.getEmail())) {
            if (guestRepository.findByEmail(guestDTO.getEmail()).isPresent()) {
                logger.warn("Failed to update guest ID {}: Email {} already exists", id, guestDTO.getEmail());
                throw new RuntimeException("Guest with email " + guestDTO.getEmail() + " already exists");
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
        return guestMapper.toDTO(updatedGuest);
    }

    @Transactional(readOnly = true)
    public GuestDTO getGuestById(Long id) {
        logger.debug("Fetching guest with ID: {}", id);
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Guest not found with ID: {}", id);
                    return new RuntimeException("Guest not found with id: " + id);
                });
        logger.debug("Successfully retrieved guest with ID: {}", id);
        return guestMapper.toDTO(guest);
    }

    @Transactional(readOnly = true)
    public GuestDTO getGuestByEmail(String email) {
        logger.debug("Fetching guest with email: {}", email);
        Guest guest = guestRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Guest not found with email: {}", email);
                    return new RuntimeException("Guest not found with email: " + email);
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
        
        // Default sorting
        Sort sort = Sort.by(Sort.Direction.ASC, "lastName");
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = sortDir != null && sortDir.equalsIgnoreCase("desc") 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
            sort = Sort.by(direction, sortBy);
        }
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
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
        if (!guestRepository.existsById(id)) {
            logger.error("Failed to delete: Guest not found with ID: {}", id);
            throw new RuntimeException("Guest not found with id: " + id);
        }
        guestRepository.deleteById(id);
        logger.info("Successfully deleted guest with ID: {}", id);
    }
}

