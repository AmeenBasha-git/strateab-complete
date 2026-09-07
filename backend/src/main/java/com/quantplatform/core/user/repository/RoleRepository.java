package com.quantplatform.core.user.repository;

import com.quantplatform.core.user.domain.Role;
import com.quantplatform.core.user.domain.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(RoleName name);
}