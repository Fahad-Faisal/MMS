// src/main/java/com/mms/controller/UserController.java
package com.mms.controller;

import com.mms.dto.request.UserRequest;
import com.mms.dto.request.UserUpdateRequest;
import com.mms.dto.response.ApiError;
import com.mms.dto.response.PaginatedResponse;
import com.mms.entity.User;
import com.mms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        try {
            log.debug("Fetching users - q: {}, role: {}, page: {}, pageSize: {}", q, role, page, pageSize);
            Page<User> users = userService.getUsers(q, role, page, pageSize);

            return ResponseEntity.ok(PaginatedResponse.<User>builder()
                    .items(users.getContent())
                    .total(users.getTotalElements())
                    .page(page)
                    .pageSize(pageSize)
                    .build());
        } catch (Exception e) {
            log.error("Failed to fetch users", e);
            return ResponseEntity.status(400)
                    .body(ApiError.builder()
                            .status(400)
                            .message("Failed to fetch users: " + e.getMessage())
                            .code("FETCH_USERS_FAILED")
                            .build());
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRequest request) {
        try {
            log.info("Creating new user: {}", request.getUsername());
            User created = userService.createUser(request);
            log.info("User created successfully with id: {}", created.getId());

            // Return the created user
            return ResponseEntity.status(201).body(created);
        } catch (RuntimeException e) {
            log.error("Failed to create user: {}", e.getMessage());
            return ResponseEntity.status(400)
                    .body(ApiError.builder()
                            .status(400)
                            .message(e.getMessage())
                            .code("CREATE_USER_FAILED")
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error creating user", e);
            return ResponseEntity.status(500)
                    .body(ApiError.builder()
                            .status(500)
                            .message("An unexpected error occurred: " + e.getMessage())
                            .code("INTERNAL_ERROR")
                            .build());
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable String id,
                                              @RequestBody Map<String, String> request) {
        userService.resetPassword(id, request.get("newPassword"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}