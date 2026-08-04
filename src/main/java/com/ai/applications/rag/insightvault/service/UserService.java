package com.ai.applications.rag.insightvault.service;

import com.ai.applications.rag.insightvault.config.BootstrapProperties;
import com.ai.applications.rag.insightvault.models.AppUser;
import com.ai.applications.rag.insightvault.models.RoleName;
import com.ai.applications.rag.insightvault.repository.AppUserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties bootstrapProperties;

    public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, BootstrapProperties bootstrapProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapProperties = bootstrapProperties;
    }

    @PostConstruct
    @Transactional
    public void bootstrapUsers() {
        if (!userRepository.existsByUsername(bootstrapProperties.adminUsername())) {
            userRepository.save(new AppUser(
                    bootstrapProperties.adminUsername(),
                    passwordEncoder.encode(bootstrapProperties.adminPassword()),
                    RoleName.ROLE_ADMIN));
            log.info("Bootstrapped admin user '{}'", bootstrapProperties.adminUsername());
        }

        if (!userRepository.existsByUsername(bootstrapProperties.userUsername())) {
            userRepository.save(new AppUser(
                    bootstrapProperties.userUsername(),
                    passwordEncoder.encode(bootstrapProperties.userPassword()),
                    RoleName.ROLE_USER));
            log.info("Bootstrapped standard user '{}'", bootstrapProperties.userUsername());
        }
    }
}
