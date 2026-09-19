package com.payvault.transaction.client;

/**
 * Mirrors user-service's UserResponse shape. Kept local to this service
 * (not a shared library) on purpose - see the equivalent note in
 * auth-service's UserProfileRequest for why each service owns its own
 * contract types rather than sharing DTO classes across service boundaries.
 */
public class UserDto {

    private String userId;
    private String name;
    private String email;
    private String phone;
    private String status;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
