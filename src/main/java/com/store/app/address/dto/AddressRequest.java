package com.store.app.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Input for creating or updating an address. Mutable class because it
 * also backs the customer Thymeleaf form.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddressRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 150, message = "Address line 1 must not exceed 150 characters")
    private String addressLine1;

    @Size(max = 150, message = "Address line 2 must not exceed 150 characters")
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 80, message = "City must not exceed 80 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 80, message = "State must not exceed 80 characters")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    private String pincode;

    @NotBlank(message = "Country is required")
    @Size(max = 80, message = "Country must not exceed 80 characters")
    private String country = "India";

    private boolean defaultAddress;
}
