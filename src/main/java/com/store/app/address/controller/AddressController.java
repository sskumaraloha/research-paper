package com.store.app.address.controller;

import com.store.app.address.dto.AddressRequest;
import com.store.app.address.dto.AddressResponse;
import com.store.app.address.service.AddressService;
import com.store.app.security.StoreUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for the authenticated customer's address book
 * (JWT + ROLE_CUSTOMER via /api/customer/**).
 */
@RestController
@RequestMapping("/api/customer/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(
            @AuthenticationPrincipal StoreUserDetails principal) {
        return ResponseEntity.ok(addressService.getAddresses(userId(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddress(
            @AuthenticationPrincipal StoreUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(addressService.getAddress(userId(principal), id));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @AuthenticationPrincipal StoreUserDetails principal,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.createAddress(userId(principal), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @AuthenticationPrincipal StoreUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(userId(principal), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal StoreUserDetails principal,
            @PathVariable Long id) {
        addressService.deleteAddress(userId(principal), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<AddressResponse> setDefault(
            @AuthenticationPrincipal StoreUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(addressService.setDefaultAddress(userId(principal), id));
    }

    private Long userId(StoreUserDetails principal) {
        return principal.getUser().getId();
    }
}
