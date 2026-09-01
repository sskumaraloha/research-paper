package com.store.app.config;

import com.store.app.user.entity.Role;
import com.store.app.user.entity.RoleName;
import com.store.app.user.entity.User;
import com.store.app.user.repository.RoleRepository;
import com.store.app.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds reference data at application startup.
 * <ul>
 *   <li>Roles ({@code ROLE_CUSTOMER}, {@code ROLE_ADMIN}) — seeded in every profile,
 *       since the application cannot function without them.</li>
 *   <li>A default admin user — seeded in the <b>dev profile only</b>, with
 *       well-known credentials that must never reach production.</li>
 * </ul>
 */
@Slf4j
@Configuration
public class DataInitializer {

    // =====================================================================
    // DEVELOPMENT-ONLY CREDENTIALS — for local testing convenience.
    // This user is created only when the "dev" profile is active and must
    // NEVER be seeded in production. Rotate immediately if ever exposed.
    // =====================================================================
    private static final String DEV_ADMIN_FIRST_NAME = "Store";
    private static final String DEV_ADMIN_LAST_NAME = "Admin";
    private static final String DEV_ADMIN_PHONE = "9999999999";
    private static final String DEV_ADMIN_EMAIL = "admin@store.com";
    private static final String DEV_ADMIN_PASSWORD = "Admin@123";

    @Bean
    @Order(1)
    public CommandLineRunner roleSeeder(RoleRepository roleRepository) {
        return args -> {
            for (RoleName roleName : RoleName.values()) {
                if (!roleRepository.existsByName(roleName)) {
                    roleRepository.save(new Role(roleName));
                    log.info("Seeded role: {}", roleName);
                }
            }
        };
    }

    @Bean
    @Order(2)
    @Profile("dev")
    public CommandLineRunner devAdminSeeder(UserRepository userRepository,
                                            RoleRepository roleRepository,
                                            PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.existsByEmail(DEV_ADMIN_EMAIL)) {
                return;
            }

            User admin = new User(
                    DEV_ADMIN_FIRST_NAME,
                    DEV_ADMIN_LAST_NAME,
                    DEV_ADMIN_PHONE,
                    DEV_ADMIN_EMAIL,
                    passwordEncoder.encode(DEV_ADMIN_PASSWORD)
            );
            admin.setPhoneVerified(true);

            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new IllegalStateException(
                            "ROLE_ADMIN not found; role seeder must run first"));
            admin.addRole(adminRole);

            userRepository.save(admin);
            log.warn("Seeded DEVELOPMENT-ONLY admin user ({} / phone {}). "
                            + "These credentials are for local development and must never be used in production.",
                    DEV_ADMIN_EMAIL, DEV_ADMIN_PHONE);
        };
    }
}
