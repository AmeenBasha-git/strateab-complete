package com.quantplatform.core.user.service;

import com.quantplatform.core.audit.service.AuditLogService;
import com.quantplatform.core.common.exception.ResourceNotFoundException;
import com.quantplatform.core.user.domain.Role;
import com.quantplatform.core.user.domain.RoleName;
import com.quantplatform.core.user.domain.User;
import com.quantplatform.core.user.domain.UserStatus;
import com.quantplatform.core.user.repository.RoleRepository;
import com.quantplatform.core.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
    }

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public Page<User> search(String query, Pageable pageable) {
        return userRepository.search(query, pageable);
    }

    @Transactional
    public User changeStatus(UUID userId, UserStatus newStatus, UUID actingAdminId) {
        User user = getById(userId);
        if (newStatus == UserStatus.SUSPENDED) {
            user.suspend();
        } else if (newStatus == UserStatus.ACTIVE) {
            user.activate();
        }
        userRepository.save(user);
        auditLogService.record(actingAdminId, "USER_STATUS_CHANGED", "User", userId.toString());
        return user;
    }

    @Transactional
    public User assignRole(UUID userId, RoleName roleName, UUID actingAdminId) {
        User user = getById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
        user.assignRole(role);
        userRepository.save(user);
        auditLogService.record(actingAdminId, "ROLE_ASSIGNED", "User", userId.toString());
        return user;
    }
}
