package com.hrms.backend.config;

import com.hrms.backend.entities.Role;
import com.hrms.backend.security.CustomOAuth2AuthenticationSuccessHandler;
import com.hrms.backend.services.impl.UserServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.access.AccessDeniedHandler;
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
                .authorizeHttpRequests(request -> request
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Allow OPTIONS requests
                       // .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll() // Allow login & refresh token
                        .requestMatchers("/api/v1/auth/logout").authenticated() // Require authentication for logout
                        .requestMatchers("/api/v1/admin/**").hasAuthority(Role.ADMIN.name())
                        .requestMatchers("/api/v1/manager/**").hasAnyAuthority(Role.MANAGER.name())
                        .requestMatchers("/api/v1/user/details").authenticated()
                        .requestMatchers("/api/v1/user/**").hasAnyAuthority(Role.ADMIN.name(), Role.SUPER_ADMIN.name())
                        .requestMatchers("/api/attendance/**").authenticated()
                        .requestMatchers("/api/leave/apply").permitAll()
                        .requestMatchers("/api/leave/approve/**").hasAnyAuthority(Role.ADMIN.name(), Role.MANAGER.name())
                        .requestMatchers("/api/leave/all").hasAnyAuthority(Role.ADMIN.name(), Role.MANAGER.name())
                        .requestMatchers("/api/leave/**").authenticated()
                        .requestMatchers("/api/requests/**").permitAll()
                        .requestMatchers("/api/companies").permitAll()

                        .anyRequest().authenticated())
                .sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(customOAuth2AuthenticationSuccessHandler)
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
