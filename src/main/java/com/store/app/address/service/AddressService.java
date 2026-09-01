package com.store.app.address.service;

import com.store.app.address.dto.AddressRequest;
import com.store.app.address.dto.AddressResponse;

import java.util.List;

/**
 * Address book operations. All methods are scoped to the acting user;
 * addresses of other users are unreachable by construction.
 */
public interface AddressService {

    List<AddressResponse> getAddresses(Long userId);

    AddressResponse getAddress(Long userId, Long addressId);

    /**
     * Creates an address. The user's first address automatically becomes
     * the default; requesting default on a later address moves it.
     */
    AddressResponse createAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    /**
     * Deletes an address. When the default address is deleted, the
     * oldest remaining address is promoted to default.
     */
    void deleteAddress(Long userId, Long addressId);

    /** Marks one address as default and unsets all others. */
    AddressResponse setDefaultAddress(Long userId, Long addressId);
}
