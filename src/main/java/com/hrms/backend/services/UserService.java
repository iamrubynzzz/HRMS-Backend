package com.hrms.backend.services;

import com.hrms.backend.dto.UserDTO;
import com.hrms.backend.dto.UserRequestDTO;
import com.hrms.backend.entities.Status;
import com.hrms.backend.entities.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface UserService {
    UserDetailsService userDetailsService();

    // New method to process OAuth2User
    String processOAuth2User(OAuth2User oauth2User);

    // Get a list of users by their status (used for showing pending approvals)
    List<UserDTO> getUsersByStatus(Status userStatus);

    // Approve a user and add the user details
    UserDTO approveUser(Integer userId, UserRequestDTO userInfo);
    User getUserByEmail(String email);
    User getUserById(int id);

    // Reject a user
    void rejectUser(Integer userId);

}
