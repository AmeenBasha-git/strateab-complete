package com.quantplatform.core.auth.service;

import com.quantplatform.core.audit.service.AuditLogService;
import com.quantplatform.core.auth.dto.*;
import com.quantplatform.core.auth.security.JwtService;
import com.quantplatform.core.common.exception.DuplicateResourceException;
import com.quantplatform.core.common.exception.InvalidCredentialsException;
import com.quantplatform.core.user.domain.Role;
import com.quantplatform.core.user.domain.RoleName;
import com.quantplatform.core.user.domain.User;
import com.quantplatform.core.user.repository.RoleRepository;
import com.quantplatform.core.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken");
        }

        Role defaultRole = roleRepository.findByName(RoleName.STANDARD_USER)
                .orElseThrow(() -> new IllegalStateException("STANDARD_USER role not seeded"));

        User user = User.register(
                request.email(),
                request.username(),
                passwordEncoder.encode(request.password()),
                defaultRole
        );
        userRepository.save(user);
        auditLogService.record(user.getId(), "USER_REGISTERED", "User", user.getId().toString());

        return issueTokenPair(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.usernameOrEmail())
                .or(() -> userRepository.findByUsername(request.usernameOrEmail()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username/email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username/email or password");
        }
        if (!user.isActive()) {
            throw new InvalidCredentialsException("Account is not active");
        }

        auditLogService.record(user.getId(), "USER_LOGIN", "User", user.getId().toString());
        return issueTokenPair(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(request.refreshToken());
        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new InvalidCredentialsException("User no longer exists"));

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getUsername(), roleNames(user)
        );

        return new TokenResponse(accessToken, rotation.newRefreshToken(), jwtService.getAccessTokenTtlSeconds());
    }

    @Transactional
    public void logout(java.util.UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
        auditLogService.record(userId, "USER_LOGOUT", "User", userId.toString());
    }

    private TokenResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), roleNames(user));
        String refreshToken = refreshTokenService.issue(user.getId());
        return new TokenResponse(accessToken, refreshToken, jwtService.getAccessTokenTtlSeconds());
    }

    private Set<String> roleNames(User user) {
        return user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet());
    }
}
