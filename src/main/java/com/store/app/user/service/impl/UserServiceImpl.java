package com.store.app.user.service.impl;

import com.store.app.exception.DuplicateResourceException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.user.dto.CreateUserRequest;
import com.store.app.user.dto.UpdateUserRequest;
import com.store.app.user.dto.UserResponse;
import com.store.app.user.entity.Role;
import com.store.app.user.entity.RoleName;
import com.store.app.user.entity.User;
import com.store.app.user.mapper.UserMapper;
import com.store.app.user.repository.RoleRepository;
import com.store.app.user.repository.UserRepository;
import com.store.app.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered: " + email);
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicateResourceException(
                    "Phone number is already registered: " + request.phoneNumber());
        }

        User user = userMapper.toEntity(request, passwordEncoder.encode(request.password()));
        user.addRole(getRole(RoleName.ROLE_CUSTOMER));

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(findUserById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByPhoneNumber(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with phone number: " + phoneNumber));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserById(id);

        String newEmail = request.email().toLowerCase().trim();
        if (!user.getEmail().equals(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("Email is already registered: " + newEmail);
        }
        if (!user.getPhoneNumber().equals(request.phoneNumber())
                && userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicateResourceException(
                    "Phone number is already registered: " + request.phoneNumber());
        }

        // A changed phone number must be verified again.
        if (!user.getPhoneNumber().equals(request.phoneNumber())) {
            user.setPhoneVerified(false);
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhoneNumber(request.phoneNumber());
        user.setEmail(newEmail);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse setUserEnabled(Long id, boolean enabled) {
        User user = findUserById(id);
        user.setEnabled(enabled);
        return userMapper.toResponse(userRepository.save(user));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private Role getRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException(
                        "Role not found in database: " + roleName
                                + ". Ensure the role seed initializer has run."));
    }
}
