package com.payvault.user.service;

import com.payvault.user.dto.CreateUserRequest;
import com.payvault.user.dto.UpdateUserRequest;
import com.payvault.user.exception.UserAlreadyExistsException;
import com.payvault.user.exception.UserNotFoundException;
import com.payvault.user.model.User;
import com.payvault.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void getByUserId_throwsWhenNotFound() {
        when(userRepository.findByUserId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByUserId("missing"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void create_throwsWhenUserIdAlreadyHasAProfile() {
        when(userRepository.existsByUserId("user-1")).thenReturn(true);

        CreateUserRequest request = createRequest("user-1", "new@example.com");

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("user-1");

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_throwsWhenEmailAlreadyTaken() {
        when(userRepository.existsByUserId("user-1")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        CreateUserRequest request = createRequest("user-1", "taken@example.com");

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("taken@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_savesNewProfile_whenUserIdAndEmailAreBothFree() {
        when(userRepository.existsByUserId("user-1")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUserRequest request = createRequest("user-1", "new@example.com");
        User result = userService.create(request);

        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void update_onlyChangesFieldsThatWerePassed() {
        User existing = new User("user-1", "Original Name", "user@example.com", "1111111111");
        existing.setProfileImage("https://example.com/original.png");
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setPhone("2222222222");
        // name and profileImage intentionally left null - should be untouched.

        User result = userService.update("user-1", request);

        assertThat(result.getPhone()).isEqualTo("2222222222");
        assertThat(result.getName()).isEqualTo("Original Name");
        assertThat(result.getProfileImage()).isEqualTo("https://example.com/original.png");
    }

    @Test
    void update_throwsWhenUserDoesNotExist() {
        when(userRepository.findByUserId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update("missing", new UpdateUserRequest()))
                .isInstanceOf(UserNotFoundException.class);
    }

    private CreateUserRequest createRequest(String userId, String email) {
        CreateUserRequest request = new CreateUserRequest();
        request.setUserId(userId);
        request.setName("Test User");
        request.setEmail(email);
        request.setPhone("9999999999");
        return request;
    }
}
