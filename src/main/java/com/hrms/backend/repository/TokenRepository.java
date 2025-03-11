package com.hrms.backend.repository;

import com.hrms.backend.entities.Token;
import com.hrms.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    List<Token> findAllByUserAndLoggedOutFalse(User user);

    Optional<Token> findByAccessToken(String token);
}
