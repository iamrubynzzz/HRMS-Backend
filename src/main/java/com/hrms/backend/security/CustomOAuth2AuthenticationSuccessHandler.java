package com.hrms.backend.security;
import com.hrms.backend.services.impl.UserServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component

public class CustomOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private final UserServiceImpl userService;

    public CustomOAuth2AuthenticationSuccessHandler(UserServiceImpl userService) {
        this.userService = userService;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            System.out.println("User attributes: " + oauth2User.getAttributes());

            // Process the OAuth2 user and generate a JWT token
            String token = userService.processOAuth2User(oauth2User);

            // Send the token in the JSON response
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"token\": \"" + token + "\"}");
            response.getWriter().flush();
        } catch (Exception ex) {
            // Log the error
            ex.printStackTrace();
            response.sendRedirect("/api/v1/auth/error");
        }
    }
}
