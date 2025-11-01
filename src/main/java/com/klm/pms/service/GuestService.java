package com.klm.pms.service;

import com.klm.pms.dto.GuestDTO;
import com.klm.pms.mapper.GuestMapper;
import com.klm.pms.model.Guest;
import com.klm.pms.repository.GuestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GuestService {

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private GuestMapper guestMapper;

    public GuestDTO createGuest(GuestDTO guestDTO) {
        // Check if email already exists
        if (guestDTO.getEmail() != null && guestRepository.findByEmail(guestDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Guest with email " + guestDTO.getEmail() + " already exists");
        }
        
        Guest guest = guestMapper.toEntity(guestDTO);
        Guest savedGuest = guestRepository.save(guest);
        return guestMapper.toDTO(savedGuest);
    }

    public GuestDTO updateGuest(Long id, GuestDTO guestDTO) {
        Guest existingGuest = guestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guest not found with id: " + id));
        
        // Check email uniqueness if it's being changed
        if (guestDTO.getEmail() != null && !guestDTO.getEmail().equals(existingGuest.getEmail())) {
            if (guestRepository.findByEmail(guestDTO.getEmail()).isPresent()) {
                throw new RuntimeException("Guest with email " + guestDTO.getEmail() + " already exists");
            }
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
        return guestMapper.toDTO(updatedGuest);
    }

    @Transactional(readOnly = true)
    public GuestDTO getGuestById(Long id) {
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guest not found with id: " + id));
        return guestMapper.toDTO(guest);
    }

    @Transactional(readOnly = true)
    public GuestDTO getGuestByEmail(String email) {
        Guest guest = guestRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Guest not found with email: " + email));
        return guestMapper.toDTO(guest);
    }

    @Transactional(readOnly = true)
    public List<GuestDTO> getAllGuests() {
        return guestRepository.findAll().stream()
                .map(guestMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void deleteGuest(Long id) {
        if (!guestRepository.existsById(id)) {
            throw new RuntimeException("Guest not found with id: " + id);
        }
        guestRepository.deleteById(id);
    }
}

