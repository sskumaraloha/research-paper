package com.store.app.address.mapper;

import com.store.app.address.dto.AddressResponse;
import com.store.app.address.entity.Address;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link Address} entities and address DTOs.
 */
@Component
public class AddressMapper {

    public AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getFullName(),
                address.getPhoneNumber(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPincode(),
                address.getCountry(),
                address.isDefaultAddress()
        );
    }
}
