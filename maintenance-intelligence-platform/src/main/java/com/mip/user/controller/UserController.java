package com.mip.user.controller;

import com.mip.security.MipUserDetails;
import com.mip.user.dto.CreateUserRequest;
import com.mip.user.dto.RoleDefinitionResponse;
import com.mip.user.dto.UpdateUserRequest;
import com.mip.user.dto.UserSummaryResponse;
import com.mip.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserSummaryResponse> listUsers() {
        return userService.listUsers();
    }

    @GetMapping("/roles")
    public List<RoleDefinitionResponse> listRoles() {
        return userService.listRoles();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserSummaryResponse createUser(@Valid @RequestBody CreateUserRequest request,
                                          @AuthenticationPrincipal MipUserDetails principal) {
        return userService.createUser(request, principal);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserSummaryResponse updateUser(@PathVariable Long userId,
                                          @Valid @RequestBody UpdateUserRequest request,
                                          @AuthenticationPrincipal MipUserDetails principal) {
        return userService.updateUser(userId, request, principal);
    }

    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserSummaryResponse deactivateUser(@PathVariable Long userId,
                                              @AuthenticationPrincipal MipUserDetails principal) {
        return userService.deactivateUser(userId, principal);
    }
}
