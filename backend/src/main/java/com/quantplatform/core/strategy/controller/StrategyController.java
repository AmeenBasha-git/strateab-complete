package com.quantplatform.core.strategy.controller;

import com.quantplatform.core.common.response.ApiResponse;
import com.quantplatform.core.common.response.PagedResponse;
import com.quantplatform.core.strategy.domain.Strategy;
import com.quantplatform.core.strategy.domain.StrategyType;
import com.quantplatform.core.strategy.dto.*;
import com.quantplatform.core.strategy.mapper.StrategyMapper;
import com.quantplatform.core.strategy.service.StrategyAuthorizationService;
import com.quantplatform.core.strategy.service.StrategyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/strategies")
@Tag(name = "Strategies", description = "Strategy creation, metadata, and access")
public class StrategyController {

    private final StrategyService strategyService;
    private final StrategyAuthorizationService authorizationService;
    private final StrategyMapper strategyMapper;

    public StrategyController(
            StrategyService strategyService,
            StrategyAuthorizationService authorizationService,
            StrategyMapper strategyMapper
    ) {
        this.strategyService = strategyService;
        this.authorizationService = authorizationService;
        this.strategyMapper = strategyMapper;
    }

    @PostMapping
    public ApiResponse<StrategyResponse> create(
            @Valid @RequestBody CreateStrategyRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        Strategy strategy = strategyService.createUserStrategy(
                userId, request.name(), request.description(), request.sourceCode(), request.parametersSchema()
        );
        return ApiResponse.success(strategyMapper.toResponse(strategy), "Strategy created");
    }

    @GetMapping("/{id}")
    public ApiResponse<StrategyResponse> getById(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        Strategy strategy = strategyService.getById(id);
        authorizationService.assertCanRead(strategy, userId, currentRoles());
        return ApiResponse.success(strategyMapper.toResponse(strategy), "OK");
    }

    @GetMapping
    public ApiResponse<PagedResponse<StrategySummaryResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) StrategyType type,
            @AuthenticationPrincipal UUID userId,
            Pageable pageable
    ) {
        var page = strategyService.search(userId, query, type, pageable);
        var response = new PagedResponse<>(
                page.getContent().stream().map(strategyMapper::toSummary).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast()
        );
        return ApiResponse.success(response, "OK");
    }

    @PatchMapping("/{id}")
    public ApiResponse<StrategyResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStrategyRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        Strategy strategy = strategyService.getById(id);
        authorizationService.assertCanWrite(strategy, userId, currentRoles());
        Strategy updated = strategyService.updateMetadata(id, request.name(), request.description(), request.visibility(), userId);
        return ApiResponse.success(strategyMapper.toResponse(updated), "Strategy updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        Strategy strategy = strategyService.getById(id);
        authorizationService.assertCanWrite(strategy, userId, currentRoles());
        strategyService.softDelete(id, userId);
        return ApiResponse.success(null, "Strategy deleted");
    }

    @PatchMapping("/{id}/active-version")
    public ApiResponse<StrategyResponse> setActiveVersion(
            @PathVariable UUID id,
            @RequestParam UUID versionId,
            @AuthenticationPrincipal UUID userId
    ) {
        Strategy strategy = strategyService.getById(id);
        authorizationService.assertCanWrite(strategy, userId, currentRoles());
        Strategy updated = strategyService.setActiveVersion(id, versionId, userId);
        return ApiResponse.success(strategyMapper.toResponse(updated), "Active version updated");
    }

    private Set<String> currentRoles() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());
    }
}
