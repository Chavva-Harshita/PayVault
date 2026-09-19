package com.payvault.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/users/me body. All fields optional (partial update) - only
 * non-null fields are applied. name/phone/profileImage only: email and
 * status are never editable through this endpoint.
 */
public class UpdateUserRequest {

    @Size(min = 1, max = 100, message = "name must be between 1 and 100 characters")
    private String name;

    @Pattern(regexp = "^[0-9+\\-() ]{7,20}$", message = "phone must be a valid phone number")
    private String phone;

    private String profileImage;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
