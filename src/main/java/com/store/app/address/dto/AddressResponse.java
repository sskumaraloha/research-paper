package com.store.app.address.dto;

/**
 * A customer's address as returned by the API and pages.
 */
public record AddressResponse(
        Long id,
        String fullName,
        String phoneNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String pincode,
        String country,
        boolean defaultAddress
) {
}
