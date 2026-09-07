package com.quantplatform.core.strategy.controller;

import com.quantplatform.core.common.response.ApiResponse;
import com.quantplatform.core.strategy.domain.Strategy;
import com.quantplatform.core.strategy.domain.StrategyVersion;
import com.quantplatform.core.strategy.dto.CreateVersionRequest;
import com.quantplatform.core.strategy.dto.StrategyVersionResponse;
import com.quantplatform.core.strategy.mapper.StrategyVersionMapper;
import com.quantplatform.core.strategy.service.StrategyAuthorizationService;
import com.quantplatform.core.strategy.service.StrategyService;
import com.quantplatform.core.strategy.service.StrategyVersionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/strategies/{strategyId}/versions")
@Tag(name = "Strategy Versions", description = "Immutable version history for a strategy")
public class StrategyVersionController {

    private final StrategyService strategyService;
    private final StrategyVersionService versionService;
    private final StrategyAuthorizationService authorizationService;
    private final StrategyVersionMapper versionMapper;

    public StrategyVersionController(
            StrategyService strategyService,
            StrategyVersionService versionService,
            StrategyAuthorizationService authorizationService,
            StrategyVersionMapper versionMapper
    ) {
        this.strategyService = strategyService;
        this.versionService = versionService;
        this.authorizationService = authorizationService;
        this.versionMapper = versionMapper;
    }

    @PostMapping
    public ApiResponse<StrategyVersionResponse> create(
            @PathVariable UUID strategyId,
            @Valid @RequestBody CreateVersionRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        Strategy strategy = strategyService.getById(strategyId);
        authorizationService.assertCanWrite(strategy, userId, currentRoles());

        StrategyVersion version = versionService.createVersion(
                strategyId, request.sourceCode(), request.parametersSchema(), request.changelogNote(), userId
        );
        return ApiResponse.success(versionMapper.toResponse(version), "Version created");
    }

    @GetMapping
    public ApiResponse<List<StrategyVersionResponse>> list(
            @PathVariable UUID strategyId,
            @AuthenticationPrincipal UUID userId
    ) {
        Strategy strategy = strategyService.getById(strategyId);
        authorizationService.assertCanRead(strategy, userId, currentRoles());

        List<StrategyVersionResponse> versions = versionService.listVersions(strategyId).stream()
                .map(versionMapper::toResponse)
                .toList();
        return ApiResponse.success(versions, "OK");
    }

    @GetMapping("/{versionNumber}")
    public ApiResponse<StrategyVersionResponse> getByVersionNumber(
            @PathVariable UUID strategyId,
            @PathVariable int versionNumber,
            @AuthenticationPrincipal UUID userId
    ) {
        Strategy strategy = strategyService.getById(strategyId);
        authorizationService.assertCanRead(strategy, userId, currentRoles());

        StrategyVersion version = versionService.getByVersionNumber(strategyId, versionNumber);
        return ApiResponse.success(versionMapper.toResponse(version), "OK");
    }

    private Set<String> currentRoles() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());
    }
}
