package com.mip.user.service;

import com.mip.audit.service.AuditService;
import com.mip.common.entity.BaseEntity;
import com.mip.exception.BusinessRuleViolationException;
import com.mip.exception.DuplicateResourceException;
import com.mip.exception.InvalidRequestException;
import com.mip.exception.ResourceNotFoundException;
import com.mip.plant.entity.Plant;
import com.mip.plant.repository.PlantRepository;
import com.mip.user.dto.CreateUserRequest;
import com.mip.user.dto.RoleDefinitionResponse;
import com.mip.user.dto.UpdateUserRequest;
import com.mip.user.dto.UserProfileResponse;
import com.mip.user.dto.UserSummaryResponse;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import com.mip.security.MipUserDetails;
import com.mip.user.repository.RefreshTokenRepository;
import com.mip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PlantRepository plantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = getUser(userId);
        List<Long> plantIds = user.getPlants().stream().map(BaseEntity::getId).sorted().toList();
        return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getRole().name(), plantIds);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listUsers() {
        return userRepository.findAllByOrderByFullNameAsc().stream()
                .map(this::toSummary)
                .toList();
    }

    public List<RoleDefinitionResponse> listRoles() {
        return Arrays.stream(RoleName.values())
                .map(r -> new RoleDefinitionResponse(r.name(), r.getDescription()))
                .toList();
    }

    @Transactional
    public UserSummaryResponse createUser(CreateUserRequest request, MipUserDetails principal) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("A user with this email already exists");
        }
        User user = new User(request.fullName().trim(), request.email().trim().toLowerCase(Locale.ROOT),
                passwordEncoder.encode(request.password()), parseRole(request.role()));
        applyPhoneNumber(user, request.phoneNumber());
        if (request.plantIds() != null) {
            user.getPlants().addAll(resolvePlants(request.plantIds()));
        }
        User saved = userRepository.save(user);
        auditService.log(principal, "USER_CREATED", "USER", saved.getId(), null,
                saved.getEmail() + " as " + saved.getRole());
        log.info("User {} created with role {}", saved.getId(), saved.getRole());
        return toSummary(saved);
    }

    @Transactional
    public UserSummaryResponse updateUser(Long userId, UpdateUserRequest request,
                                          MipUserDetails principal) {
        User user = getUser(userId);
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.role() != null) {
            user.setRole(parseRole(request.role()));
        }
        if (request.phoneNumber() != null) {
            applyPhoneNumber(user, request.phoneNumber());
        }
        if (request.plantIds() != null) {
            user.getPlants().clear();
            user.getPlants().addAll(resolvePlants(request.plantIds()));
        }
        if (request.active() != null && request.active() != user.isActive()) {
            user.setActive(request.active());
            if (!request.active()) {
                refreshTokenRepository.revokeAllForUser(user.getId());
            }
        }
        auditService.log(principal, "USER_UPDATED", "USER", user.getId(), null, user.getEmail());
        return toSummary(user);
    }

    /** Deactivation revokes every outstanding refresh token; access dies within token TTL. */
    @Transactional
    public UserSummaryResponse deactivateUser(Long userId, MipUserDetails principal) {
        if (userId.equals(principal.getId())) {
            throw new BusinessRuleViolationException("You cannot deactivate your own account");
        }
        User user = getUser(userId);
        user.setActive(false);
        refreshTokenRepository.revokeAllForUser(user.getId());
        auditService.log(principal, "USER_DEACTIVATED", "USER", user.getId(), null, user.getEmail());
        log.info("User {} deactivated by user {}", userId, principal.getId());
        return toSummary(user);
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void applyPhoneNumber(User user, String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return;
        }
        String phone = rawPhone.trim();
        boolean takenByOther = userRepository.findByPhoneNumber(phone)
                .map(existing -> !existing.getId().equals(user.getId()))
                .orElse(false);
        if (takenByOther) {
            throw new DuplicateResourceException("This phone number is already assigned to another user");
        }
        user.setPhoneNumber(phone);
    }

    private List<Plant> resolvePlants(List<Long> plantIds) {
        return plantIds.stream()
                .map(id -> plantRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Plant", id)))
                .toList();
    }

    private RoleName parseRole(String role) {
        try {
            return RoleName.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Unknown role: " + role);
        }
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getPhoneNumber(), user.getRole().name(), user.isActive(),
                user.getPlants().stream().map(BaseEntity::getId).sorted().toList());
    }
}
