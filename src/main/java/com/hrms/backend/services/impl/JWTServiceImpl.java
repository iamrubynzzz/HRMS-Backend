package com.hrms.backend.services.impl;

import com.hrms.backend.services.JWTService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service("JWTServiceImpl")

public class JWTServiceImpl implements JWTService {
    public String generateToken(UserDetails userDetails){
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24)) // valid for  7 days
                .signWith(getSignKey(), SignatureAlgorithm.HS256)  // Correct usage of signing with a Key object
                .compact();

    }

    public String generateRefreshToken(Map<String,Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() +604800000))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)  // Correct usage of signing with a Key object
                .compact();
    }


    //return email stored in the particular token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
        final Claims claims = extractAllClaims(token);
        return claimsResolvers.apply(claims);  // Use the provided function to resolve the claim
    }

    private Key getSignKey() {
        byte[] key = Decoders.BASE64.decode("d2VsY29tZV9zdHJvbmdfc2VjcmV0X2tleV9mb3JfdGVzdF9qd3Q1NjY=");
        return Keys.hmacShaKeyFor(key);

    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
    }
     public boolean isTokenValid(String token, UserDetails userDetails){
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
     }

    private boolean isTokenExpired(String token) {
        return extractClaim(token,Claims::getExpiration).before(new Date());
    }
}
