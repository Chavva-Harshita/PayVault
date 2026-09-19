package com.payvault.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/users - internal endpoint.
 *
 * Called by auth-service right after a successful registration to create the
 * matching profile record, using the same userId auth-service just generated.
 * This is not meant to be reachable by the frontend through the API Gateway -
 * the gateway's route table (Phase 7) will only expose /api/users/me and
 * /api/users/{userId}, not a public POST /api/users.
 */
public class CreateUserRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "phone is required")
    private String phone;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
