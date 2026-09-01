package com.store.app.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable snapshot of the delivery address at order time. Copied by
 * value so later edits or deletion of the address book entry cannot
 * change where an order was shipped.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ShippingAddress {

    @Column(name = "ship_full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "ship_phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "ship_address_line1", nullable = false, length = 150)
    private String addressLine1;

    @Column(name = "ship_address_line2", length = 150)
    private String addressLine2;

    @Column(name = "ship_city", nullable = false, length = 80)
    private String city;

    @Column(name = "ship_state", nullable = false, length = 80)
    private String state;

    @Column(name = "ship_pincode", nullable = false, length = 10)
    private String pincode;

    @Column(name = "ship_country", nullable = false, length = 80)
    private String country;
}
