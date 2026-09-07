package com.quantplatform.core.common.config;

import com.quantplatform.core.user.domain.Role;
import com.quantplatform.core.user.domain.RoleName;
import com.quantplatform.core.user.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName, roleName.name() + " default role"));
            }
        }
    }
}
