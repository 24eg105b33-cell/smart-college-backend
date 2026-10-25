package com.smartcollege.demo.config;

import com.smartcollege.demo.entity.Role;
import com.smartcollege.demo.entity.User;
import com.smartcollege.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-admin.enabled:true}")
    private boolean enabled;

    @Value("${app.initial-admin.email:admin@college.edu}")
    private String adminEmail;

    @Value("${app.initial-admin.password:Admin@12345}")
    private String adminPassword;

    @Value("${app.initial-admin.name:System Administrator}")
    private String adminName;

    @Value("${app.initial-admin.department:Campus Administration}")
    private String adminDepartment;

    public InitialAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (!userRepository.existsByEmail(normalizedEmail)) {
            User admin = new User();
            admin.setFullName(adminName);
            admin.setEmail(normalizedEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setDepartment(adminDepartment);
            admin.setActive(true);
            userRepository.save(admin);
            log.info("Initial administrator account initialized: {}", normalizedEmail);
        } else {
            log.info("Administrator account already exists: {}", normalizedEmail);
        }
    }
}
