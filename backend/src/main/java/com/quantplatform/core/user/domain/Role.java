package com.quantplatform.core.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents a named access-control role. Backed by a fixed set defined in
 * {@link RoleName}; new roles are added via migration, not runtime creation,
 * to keep authorization semantics predictable.
 */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleName name;

    @Column
    private String description;

    public Role(RoleName name, String description) {
        this.name = name;
        this.description = description;
    }
}