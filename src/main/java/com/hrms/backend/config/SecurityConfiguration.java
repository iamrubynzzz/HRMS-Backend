package com.hrms.backend.config;

import com.hrms.backend.entities.Role;
import com.hrms.backend.security.CustomOAuth2AuthenticationSuccessHandler;
import com.hrms.backend.services.impl.UserServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private final CustomOAuth2AuthenticationSuccessHandler customOAuth2AuthenticationSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserServiceImpl userService;

    public SecurityConfiguration(@Lazy CustomOAuth2AuthenticationSuccessHandler customOAuth2AuthenticationSuccessHandler,@Lazy  JwtAuthenticationFilter jwtAuthenticationFilter,@Lazy  UserServiceImpl userService) {
        this.customOAuth2AuthenticationSuccessHandler = customOAuth2AuthenticationSuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request.requestMatchers("/api/v1/auth/**")
                        .permitAll()
                        .requestMatchers("/api/v1/admin").hasRole(Role.ADMIN.name())
                        .requestMatchers("/api/v1/manager").hasAnyAuthority(Role.MANAGER.name())
                        .requestMatchers("/api/v1/employee").hasAnyAuthority(Role.EMPLOYEE.name())
                        .requestMatchers("/api/attendance/**").authenticated()
                        .requestMatchers("/api/leave/apply").permitAll() // Permit all for applying for leave
                        .requestMatchers("/api/leave/approve/**").hasAnyAuthority(Role.ADMIN.name(), Role.MANAGER.name()) // Restrict approval to ADMIN and MANAGER
                        .requestMatchers("/api/leave/all").hasAnyAuthority(Role.ADMIN.name(), Role.MANAGER.name()) // Only ADMIN and MANAGER can view all leave requests
                        .requestMatchers("/api/leave/**").authenticated() // All other leave-related APIs require authentication
                        .requestMatchers("/api/requests/**").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // OAuth2 login configuration
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(customOAuth2AuthenticationSuccessHandler)  // Register success handler here
                        .failureUrl("/api/v1/auth/error")
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userService.userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
