package com.store.app.order.dto;

/**
 * The shipping-address snapshot of an order.
 */
public record OrderAddressResponse(
        String fullName,
        String phoneNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String pincode,
        String country
) {
}
