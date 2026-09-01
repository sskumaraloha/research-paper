package com.store.app.customer.controller;

import com.store.app.security.StoreUserDetails;
import com.store.app.user.dto.ChangePasswordRequest;
import com.store.app.user.dto.UpdateProfileRequest;
import com.store.app.user.dto.UserResponse;
import com.store.app.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the authenticated customer's own profile
 * (JWT + ROLE_CUSTOMER via /api/customer/**). The acting user always
 * comes from the security principal — there is no user id parameter,
 * so no other account can be addressed.
 */
@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserResponse> getProfile(
            @AuthenticationPrincipal StoreUserDetails principal) {
        return ResponseEntity.ok(userService.getUserById(userId(principal)));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal StoreUserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId(principal), request));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal StoreUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId(principal), request);
        return ResponseEntity.noContent().build();
    }

    private Long userId(StoreUserDetails principal) {
        return principal.getUser().getId();
    }
}
