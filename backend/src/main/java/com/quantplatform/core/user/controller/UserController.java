package com.quantplatform.core.user.controller;

import com.quantplatform.core.common.response.ApiResponse;
import com.quantplatform.core.common.response.PagedResponse;
import com.quantplatform.core.user.domain.UserStatus;
import com.quantplatform.core.user.dto.AssignRoleRequest;
import com.quantplatform.core.user.dto.UserResponse;
import com.quantplatform.core.user.mapper.UserMapper;
import com.quantplatform.core.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile and administration")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.success(userMapper.toResponse(userService.getById(userId)), "OK");
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(userMapper.toResponse(userService.getById(id)), "OK");
    }

    @GetMapping
    public ApiResponse<PagedResponse<UserResponse>> search(
            @RequestParam(required = false) String query,
            Pageable pageable
    ) {
        var page = userService.search(query, pageable);
        var response = new PagedResponse<>(
                page.getContent().stream().map(userMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
        return ApiResponse.success(response, "OK");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> changeStatus(
            @PathVariable UUID id,
            @RequestParam UserStatus status,
            @AuthenticationPrincipal UUID actingAdminId
    ) {
        var user = userService.changeStatus(id, status, actingAdminId);
        return ApiResponse.success(userMapper.toResponse(user), "Status updated");
    }

    @PatchMapping("/{id}/roles")
    public ApiResponse<UserResponse> assignRole(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal UUID actingAdminId
    ) {
        var user = userService.assignRole(id, request.role(), actingAdminId);
        return ApiResponse.success(userMapper.toResponse(user), "Role assigned");
    }
}
