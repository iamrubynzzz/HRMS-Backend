package com.hrms.backend.services;

import com.hrms.backend.dto.UserDTO;
import com.hrms.backend.dto.UserRequestDTO;
import com.hrms.backend.entities.User;
import com.hrms.backend.entities.UserInfo;  // Keeping UserInfo as you mentioned
import com.hrms.backend.entities.UserStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    UserDetailsService userDetailsService();

    // New method to process OAuth2User
    String processOAuth2User(OAuth2User oauth2User);

    // Get a list of users by their status (used for showing pending approvals)
    List<UserDTO> getUsersByStatus(UserStatus userStatus);

    // Approve a user and add the user details
    UserDTO approveUser(Integer userId, UserRequestDTO userInfo);

    // Reject a user
    void rejectUser(Integer userId);



}
