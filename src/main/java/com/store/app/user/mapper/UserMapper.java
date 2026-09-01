package com.store.app.user.mapper;

import com.store.app.user.dto.CreateUserRequest;
import com.store.app.user.dto.UserResponse;
import com.store.app.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps between {@link User} entities and user DTOs.
 */
@Component
public class UserMapper {

    /**
     * Builds a new {@link User} from a registration request.
     *
     * @param request         validated registration input
     * @param encodedPassword the BCrypt-encoded password (never the raw one)
     */
    public User toEntity(CreateUserRequest request, String encodedPassword) {
        return new User(
                request.firstName().trim(),
                request.lastName().trim(),
                request.phoneNumber(),
                request.email().toLowerCase().trim(),
                encodedPassword
        );
    }

    public UserResponse toResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.isEnabled(),
                user.isPhoneVerified(),
                roleNames,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
