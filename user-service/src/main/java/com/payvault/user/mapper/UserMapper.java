package com.payvault.user.mapper;

import com.payvault.user.dto.UserResponse;
import com.payvault.user.model.User;

public class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfileImage(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
