package com.dinepilot.user.service;

import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.user.dto.AddressRequest;
import com.dinepilot.user.dto.AddressResponse;
import com.dinepilot.user.entity.Address;
import com.dinepilot.user.repository.AddressRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<AddressResponse> list(String userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public AddressResponse create(String userId, AddressRequest request) {
        Address address = new Address();
        address.setUserId(userId);
        applyRequest(address, request);
        addressRepository.save(address);
        return toResponse(address);
    }

    public AddressResponse update(String userId, String addressId, AddressRequest request) {
        Address address = findOwned(userId, addressId);
        applyRequest(address, request);
        addressRepository.save(address);
        return toResponse(address);
    }

    public void delete(String userId, String addressId) {
        findOwned(userId, addressId);
        addressRepository.deleteByIdAndUserId(addressId, userId);
    }

    private Address findOwned(String userId, String addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    private void applyRequest(Address address, AddressRequest request) {
        address.setLabel(request.label());
        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        address.setDefaultAddress(request.defaultAddress());
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefaultAddress()
        );
    }
}
