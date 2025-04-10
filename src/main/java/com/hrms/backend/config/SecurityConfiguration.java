package com.hrms.backend.config;

import com.hrms.backend.entities.Role;
import com.hrms.backend.services.impl.UserServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;


@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserServiceImpl userService;

    public SecurityConfiguration( @Lazy  JwtAuthenticationFilter jwtAuthenticationFilter, @Lazy  UserServiceImpl userService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(request -> request
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/oauth2/success").permitAll()
                      //  .requestMatchers("/ws").permitAll()
                        .requestMatchers("/public").permitAll()
                        .requestMatchers("/api/v1/password/forgot-password").permitAll()
                        .requestMatchers("/api/v1/password/reset-password").permitAll()
                        .requestMatchers("/api/v1/password/change-password").authenticated()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/admin/**").hasAuthority(Role.ADMIN.name())
                        .requestMatchers("/api/v1/manager/**").hasAnyAuthority(Role.MANAGER.name())
                        .requestMatchers("/api/v1/user/details").authenticated()
                        .requestMatchers("/api/v1/user/my-leave-balance").authenticated()
                        .requestMatchers("/api/v1/user/manager/leave-balances").authenticated()
                        .requestMatchers("/api/v1/user/assigned-employees").authenticated()
                        .requestMatchers("/api/v1/user/**").hasAnyAuthority(Role.ADMIN.name(), Role.SUPER_ADMIN.name())
                        .requestMatchers("/api/attendance/**").authenticated()
                        .requestMatchers("/api/leave/apply").permitAll()
                        .requestMatchers("/api/leave/approve/**").hasAnyAuthority(Role.ADMIN.name(), Role.MANAGER.name())
                        .requestMatchers("/api/leave/all").hasAnyAuthority(Role.ADMIN.name(), Role.MANAGER.name())
                        .requestMatchers("/api/leave/**").authenticated()
                        .requestMatchers("/api/requests/profile").authenticated()
                        .requestMatchers("/api/requests/**").permitAll()
                        .requestMatchers("/api/companies").permitAll()
                        .requestMatchers("/api/v1/salaries/my-salary").authenticated()
                        .requestMatchers("api/v1/notification/**").permitAll()
                        .requestMatchers("/api/generate/attendance/report").authenticated()
                        .requestMatchers("/api/v1/salaries/manager-payroll").hasAuthority(Role.MANAGER.name())
                        .requestMatchers("/api/v1/salaries/generate/**").authenticated()
                        .requestMatchers("/api/v1/salaries/**").hasAuthority(Role.ADMIN.name())
                        .requestMatchers("/api/announcements/create").hasAuthority(Role.ADMIN.name())
                        .requestMatchers("/api/notifications/unread").authenticated()
                        .requestMatchers("/api/notifications/mark-as-read/**").authenticated()
                        .requestMatchers("/api/consolidated-salaries").hasAuthority(Role.ADMIN.name())


                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/oauth2/success")
                        .failureUrl("/login?error=true")
                )
                .sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable());

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

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:3000")); // Frontend URL
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:3000")); // Frontend URL
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
