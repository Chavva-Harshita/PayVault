package com.payvault.auth.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * payvault_auth.users_credentials
 *
 * userId is generated here (this service owns identity creation) and is
 * then handed to user-service to create the matching profile record. The
 * passwordHash is a BCrypt hash and must never be included in any API
 * response - AuthResponse and every other DTO in this service deliberately
 * omit it.
 */
@Document(collection = "users_credentials")
public class UserCredentials {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private List<String> roles;

    private Instant createdAt;

    private Instant updatedAt;

    public UserCredentials() {
    }

    public UserCredentials(String userId, String email, String passwordHash, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
