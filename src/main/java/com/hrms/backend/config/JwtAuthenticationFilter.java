package com.hrms.backend.config;

import com.hrms.backend.services.JWTService;
import com.hrms.backend.services.impl.UserServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Qualifier("JWTServiceImpl")
    private final JWTService jwtService;
    private final UserServiceImpl userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
      /*  String requestURI = request.getRequestURI();

        // Skip filter for specific endpoints (e.g., clock-in/out API)
        if (requestURI.startsWith("/api/v1/attendance/attendance")) {
            filterChain.doFilter(request, response);
            return;
        }*/

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Check if the request has a valid Authorization header starting with Bearer
        if (StringUtils.isEmpty(authHeader) || !org.apache.commons.lang3.StringUtils.startsWith(authHeader, "Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            // Extract username (email) from JWT
            userEmail = jwtService.extractUsername(jwt);

            if (StringUtils.hasText(userEmail) && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.userDetailsService().loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Create a new authentication token
                    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );

                    token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set the authentication context
                    SecurityContextHolder.getContext().setAuthentication(token);
                }
            }
        } catch (ExpiredJwtException e) {
            handleAuthError(response, "Token has expired. Please log in again.");
            return; // Prevent further processing
        } catch (Exception e) {
            handleAuthError(response, "Invalid token.");
            return; // Prevent further processing
        }

        // Proceed with the filter chain
        filterChain.doFilter(request, response);
    }

    private void handleAuthError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

}
