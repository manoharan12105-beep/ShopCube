package me.mano.shopCube.service;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import me.mano.shopCube.security.JwtService;
import me.mano.shopCube.dto.AuthResponseDto;
import me.mano.shopCube.dto.LoginRequestDto;
import me.mano.shopCube.dto.RefreshTokenRequestDto;
import me.mano.shopCube.dto.RegisterRequestDto;
import me.mano.shopCube.entity.RefreshToken;
import me.mano.shopCube.entity.Users;
import me.mano.shopCube.enums.Role;
import me.mano.shopCube.exception.EmailAlreadyExistsException;
import me.mano.shopCube.repo.RefreshTokenRepo;
import me.mano.shopCube.repo.UserRepo;

@Service
public class AuthService {
  private final UserRepo userRepo;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenRepo refreshTokenRepo;

  public AuthService(UserRepo userRepo, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
      JwtService jwtService, RefreshTokenRepo refreshTokenRepo) {
    this.userRepo = userRepo;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.refreshTokenRepo = refreshTokenRepo;
  }

  // Register Method
  public Users register(RegisterRequestDto dto) {
    if(userRepo.findByEmail(dto.getEmail()).isPresent()) {
      throw new EmailAlreadyExistsException("Oops Email already exist");
    }

    Users user = new Users();
    
    user.setUsername(dto.getUsername());
    user.setEmail(dto.getEmail());
    user.setPassword(passwordEncoder.encode(dto.getPassword()));
    user.setRole(Role.USER); // Never allow client to choose role.Hackers can easily misuse the feature.

    return userRepo.save(user);
  }




  // Login Method
  public AuthResponseDto login(LoginRequestDto dto) {
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));

    Users user = userRepo.findByEmail(dto.getEmail()).orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

    String accessToken = jwtService.generateAccessToken(user);

    String refreshToken = jwtService.generateRefreshToken(user);

   refreshTokenRepo.findByUser(user)
    .ifPresent(existingToken -> {
        refreshTokenRepo.delete(existingToken);
    });

    RefreshToken refreshTokenEntity = new RefreshToken();

    refreshTokenEntity.setToken(refreshToken);

    refreshTokenEntity.setExpiryDate(LocalDateTime.now().plusDays(14));

    refreshTokenEntity.setUser(user);

    refreshTokenRepo.save(refreshTokenEntity);

    return new AuthResponseDto(accessToken, refreshToken);
  }



  public AuthResponseDto refreshAccessToken(RefreshTokenRequestDto dto) {
    RefreshToken refreshTokenEntity = refreshTokenRepo.findByToken(dto.getRefreshToken()).orElseThrow(() -> new RuntimeException("Refresh Token Not Found"));

    if(refreshTokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
      refreshTokenRepo.delete(refreshTokenEntity);

      throw new RuntimeException("Refresh Token Expired");
    }

    if(!jwtService.validateRefreshToken(dto.getRefreshToken())) {
      throw new RuntimeException("Invalid refresh Token");
    }

    String email = jwtService.extractEmailFromRefreshToken(dto.getRefreshToken());

    Users user = userRepo.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

    String newAccessToken = jwtService.generateAccessToken(user);

    return new AuthResponseDto(newAccessToken, dto.getRefreshToken());
  }

}
