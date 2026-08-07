package com.dinepilot.user.service;

import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.user.dto.UpdateProfileRequest;
import com.dinepilot.user.dto.UserProfileResponse;
import com.dinepilot.user.entity.User;
import com.dinepilot.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getProfile(String userId) {
        return toResponse(findUser(userId));
    }

    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        userRepository.save(user);
        return toResponse(user);
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), user.getRole());
    }
}
