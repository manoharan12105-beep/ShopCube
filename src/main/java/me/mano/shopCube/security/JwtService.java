package me.mano.shopCube.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import me.mano.shopCube.entity.Users;

@Service
public class JwtService {
  private static final String ACCESS_SECRET = "shopCubeAccessSecretKeyShopCubeAccessSecretKey123WhyNotMyNameManoharan";

  private static final String REFRESH_SECRET = "shopCubeRefreshSecretKeyShopCubeRefreshSecretKey123MyFriendManikandan";

  // Secret key for ACCESS_SECRET
  private SecretKey getAccessSigningKey() {
    return Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes());
  }

  // Secret key for REFRESH_SECRET
  private  SecretKey getRefreshSigningKey() {
    return Keys.hmacShaKeyFor(REFRESH_SECRET.getBytes());
  }

  // Generates JWT Access token
  public String generateAccessToken(Users user) {
    
    return Jwts.builder()
              .subject(user.getEmail())
              .claim(
                "role", user.getRole().name()
              )
              .issuedAt(new Date())
              .expiration(
                new Date(
                  System.currentTimeMillis() + 1000 * 60 * 15 // 15 Mins
                )
              )
              .signWith(getAccessSigningKey())
              .compact();

    /*
     {
      "sub":"mano@gmail.com",
      "role":"USER",
      "iat":"...",
      "exp":"..."
     }

      Purpose : 
            Authentication
            Authorization

      Lifetime : 15 Minutes 
    */
  }

  public String generateRefreshToken(Users user) {
    return Jwts.builder()
            .subject(user.getEmail())
            .issuedAt(new Date())
            .expiration(
              new Date(
                System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 14 // 2 Weeks 
              )
            )
            .signWith(getRefreshSigningKey())
            .compact();

    /*
     {
      "sub":"mano@gmail.com",
      "iat":"...",
      "exp":"..."
     }

      Purpose : 
            Generate New Access Token

      Lifetime : 14 Days
    */
  }

  // Extracts Email from Access Token
  public String extractEmailFromAccessToken(String token) {
    return Jwts.parser()
              .verifyWith(getAccessSigningKey())
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .getSubject();

    /*
    .verifyWith(getAccessSigningKey()) -> verify signautre

    .parseSignedClaims(token) -> Parses JWT 

    .getPayload() -> 
            {
              "sub":"mano@gmail.com",
              "role":"USER",
              "iat":"...",
              "exp":"..."
            }
      
    .getSubject() -> mano@gmail.com
    */
  }

  // Extract Email from Refresh Token
  public String extractEmailFromRefreshToken(String token) {
    return Jwts.parser()
            .verifyWith(getRefreshSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
  }


  // Validate Access Token
  public boolean validateAccessToken(String token) {
    try {
      Jwts.parser()
          .verifyWith(getAccessSigningKey())
          .build()
          .parseSignedClaims(token);

      return true;
    } catch (Exception e) {
      return false;
    }
  }


  // Validate Refresh Token
  public boolean validateRefreshToken(String token) {
    try {
      Jwts.parser()
          .verifyWith(getRefreshSigningKey())
          .build()
          .parseSignedClaims(token);

      return true;
    } catch (Exception e) {
      return false;
    }
  }
}