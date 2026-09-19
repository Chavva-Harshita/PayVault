package com.payvault.user.controller;

import com.payvault.user.dto.CreateUserRequest;
import com.payvault.user.dto.UpdateUserRequest;
import com.payvault.user.dto.UserResponse;
import com.payvault.user.exception.MissingUserContextException;
import com.payvault.user.mapper.UserMapper;
import com.payvault.user.model.User;
import com.payvault.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/users/me
     *
     * Reads the caller's identity from the X-User-Id header. Today (Phase 3)
     * nothing sets this header automatically, so you test this endpoint by
     * setting it yourself (see the README testing steps). From Phase 7
     * onward, the API Gateway validates the caller's JWT and injects this
     * header itself - user-service trusts it because, in the target
     * deployment, this service is not reachable from outside the gateway.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        requireUserId(userId);
        User user = userService.getByUserId(userId);
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }

    /**
     * PUT /api/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                                  @Valid @RequestBody UpdateUserRequest request) {
        requireUserId(userId);
        User updated = userService.update(userId, request);
        return ResponseEntity.ok(UserMapper.toResponse(updated));
    }

    /**
     * GET /api/users/{userId} - internal.
     *
     * Not meant to be routed through the gateway for public use. Called by
     * transaction-service (Phase 6) to confirm a transfer's receiverId
     * corresponds to a real, active user before any money moves.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getByUserId(@PathVariable String userId) {
        User user = userService.getByUserId(userId);
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }

    /**
     * POST /api/users - internal.
     *
     * Called by auth-service (Phase 4) immediately after a successful
     * registration, using the same userId auth-service just generated.
     */
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User created = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toResponse(created));
    }

    private void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new MissingUserContextException();
        }
    }
}
