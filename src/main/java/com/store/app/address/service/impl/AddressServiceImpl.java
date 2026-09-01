package com.store.app.address.service.impl;

import com.store.app.address.dto.AddressRequest;
import com.store.app.address.dto.AddressResponse;
import com.store.app.address.entity.Address;
import com.store.app.address.mapper.AddressMapper;
import com.store.app.address.repository.AddressRepository;
import com.store.app.address.service.AddressService;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.user.entity.User;
import com.store.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findAllByUserIdOrderByDefaultAddressDescIdAsc(userId).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddress(Long userId, Long addressId) {
        return addressMapper.toResponse(requiredAddress(userId, addressId));
    }

    @Override
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        // The first address is always the default.
        boolean makeDefault = request.isDefaultAddress()
                || addressRepository.countByUserId(userId) == 0;
        if (makeDefault) {
            clearDefault(userId);
        }

        Address address = new Address(
                user,
                request.getFullName().trim(),
                request.getPhoneNumber(),
                request.getAddressLine1().trim(),
                normalize(request.getAddressLine2()),
                request.getCity().trim(),
                request.getState().trim(),
                request.getPincode(),
                request.getCountry().trim(),
                makeDefault
        );
        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = requiredAddress(userId, addressId);

        address.setFullName(request.getFullName().trim());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setAddressLine1(request.getAddressLine1().trim());
        address.setAddressLine2(normalize(request.getAddressLine2()));
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim());
        address.setPincode(request.getPincode());
        address.setCountry(request.getCountry().trim());

        if (request.isDefaultAddress() && !address.isDefaultAddress()) {
            clearDefault(userId);
            address.setDefaultAddress(true);
        }
        // Un-ticking default on the current default is ignored: some address
        // must stay default; the user moves it by defaulting another one.

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        Address address = requiredAddress(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);

        if (wasDefault) {
            addressRepository.findAllByUserIdOrderByDefaultAddressDescIdAsc(userId).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefaultAddress(true);
                        addressRepository.save(next);
                    });
        }
    }

    @Override
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        Address address = requiredAddress(userId, addressId);
        if (!address.isDefaultAddress()) {
            clearDefault(userId);
            address.setDefaultAddress(true);
            addressRepository.save(address);
        }
        return addressMapper.toResponse(address);
    }

    // ------------------------------------------------------------------

    private Address requiredAddress(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + addressId));
    }

    private void clearDefault(Long userId) {
        List<Address> defaults = addressRepository.findAllByUserIdAndDefaultAddressTrue(userId);
        defaults.forEach(a -> a.setDefaultAddress(false));
        addressRepository.saveAll(defaults);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
