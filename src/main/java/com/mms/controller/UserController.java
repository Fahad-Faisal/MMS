// src/main/java/com/mms/controller/UserController.java
package com.mms.controller;

import com.mms.dto.request.UserRequest;
import com.mms.dto.request.UserUpdateRequest;
import com.mms.dto.response.PaginatedResponse;
import com.mms.entity.User;
import com.mms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<User>> getUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        Page<User> users = userService.getUsers(q, role, page, pageSize);
        return ResponseEntity.ok(PaginatedResponse.<User>builder()
                .items(users.getContent())
                .total(users.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
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