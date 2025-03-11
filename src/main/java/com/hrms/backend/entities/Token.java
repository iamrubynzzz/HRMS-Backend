package com.hrms.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String accessToken;

    @Column(unique = true)
    private String refreshToken;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private boolean loggedOut  = false;  // Default value is false (token is active)
}
