package com.mip.user.service;

import com.mip.common.entity.BaseEntity;
import com.mip.exception.ResourceNotFoundException;
import com.mip.user.dto.RoleDefinitionResponse;
import com.mip.user.dto.UserProfileResponse;
import com.mip.user.dto.UserSummaryResponse;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import com.mip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
                .map(u -> new UserSummaryResponse(u.getId(), u.getFullName(), u.getEmail(),
                        u.getRole().name(), u.isActive()))
                .toList();
    }

    public List<RoleDefinitionResponse> listRoles() {
        return Arrays.stream(RoleName.values())
                .map(r -> new RoleDefinitionResponse(r.name(), r.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
