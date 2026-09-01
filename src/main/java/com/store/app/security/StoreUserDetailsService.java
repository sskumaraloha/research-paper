package com.store.app.security;

import com.store.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads users by phone number for both form login and JWT validation.
 */
@Service
@RequiredArgsConstructor
public class StoreUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public StoreUserDetails loadUserByUsername(String phoneNumber)
            throws UsernameNotFoundException {
        return userRepository.findByPhoneNumber(phoneNumber)
                .map(StoreUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with phone number: " + phoneNumber));
    }
}
