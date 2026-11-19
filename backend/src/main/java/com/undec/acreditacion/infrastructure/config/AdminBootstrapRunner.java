package com.undec.acreditacion.infrastructure.config;

import com.undec.acreditacion.application.output.PasswordHasher;
import com.undec.acreditacion.application.output.RoleRepository;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Profile("!test")
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final String ADMIN_ROLE_CODE = "AEA";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordHasher passwordHasher,
            @Value("${security.admin.initial-email}") String adminEmail,
            @Value("${security.admin.initial-password}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Administrator user '{}' already exists. Skipping bootstrap.", adminEmail);
            return;
        }

        Optional<Role> adminRoleOpt = roleRepository.findByCode(ADMIN_ROLE_CODE);
        if (adminRoleOpt.isEmpty()) {
            log.warn("Role '{}' not found in database. Ensure Flyway migrations have executed.", ADMIN_ROLE_CODE);
            return;
        }

        Role adminRole = adminRoleOpt.get();
        String passwordHash = passwordHasher.hash(adminPassword);

        User adminUser = User.register("admin", adminEmail, passwordHash);
        adminUser.assignRole(adminRole);

        userRepository.save(adminUser);
        log.info("Initial administrator user '{}' created successfully with role '{}'.", adminEmail, ADMIN_ROLE_CODE);
    }
}
