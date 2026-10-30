
package com.quickcash.dto;

import com.quickcash.entity.User;

public record ManagerResponse(

        Long userId,
        String fullName,
        String email,
        String phone,
        String role,
        String status

) {

    public static ManagerResponse from(User user) {
        return new ManagerResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }
}

