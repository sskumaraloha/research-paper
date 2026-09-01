package com.store.app.user.service;

import com.store.app.user.dto.ChangePasswordRequest;
import com.store.app.user.dto.CreateUserRequest;
import com.store.app.user.dto.UpdateProfileRequest;
import com.store.app.user.dto.UpdateUserRequest;
import com.store.app.user.dto.UserResponse;

import java.util.List;

/**
 * User account management operations.
 */
public interface UserService {

    /**
     * Creates a new user with the default {@code ROLE_CUSTOMER} role.
     * The password is BCrypt-encoded before it is stored.
     *
     * @throws com.store.app.exception.DuplicateResourceException
     *         if the email or phone number is already registered
     */
    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(Long id);

    UserResponse getUserByEmail(String email);

    UserResponse getUserByPhoneNumber(String phoneNumber);

    List<UserResponse> getAllUsers();

    /**
     * Updates profile fields (name, phone, email) of an existing user.
     *
     * @throws com.store.app.exception.ResourceNotFoundException if the user does not exist
     * @throws com.store.app.exception.DuplicateResourceException
     *         if the new email or phone number belongs to another user
     */
    UserResponse updateUser(Long id, UpdateUserRequest request);

    /** Enables or disables a user account. */
    UserResponse setUserEnabled(Long id, boolean enabled);

    /**
     * Self-service profile update: name and email only. Roles and the
     * verified phone number are untouchable through this path.
     *
     * @throws com.store.app.exception.DuplicateResourceException
     *         if the new email belongs to another account
     */
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * Self-service password change. The new password is BCrypt-encoded
     * and {@code passwordChangedAt} is stamped, revoking all previously
     * issued JWT access tokens.
     *
     * @throws com.store.app.exception.AuthenticationFailedException
     *         if the current password is wrong
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}
