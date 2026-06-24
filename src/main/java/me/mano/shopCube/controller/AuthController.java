package me.mano.shopCube.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import me.mano.shopCube.dto.authDto.AuthResponseDto;
import me.mano.shopCube.dto.authDto.LoginRequestDto;
import me.mano.shopCube.dto.authDto.RefreshTokenRequestDto;
import me.mano.shopCube.dto.authDto.RegisterRequestDto;
import me.mano.shopCube.entity.Users;
import me.mano.shopCube.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }


  @PostMapping("/register")
  public Users register(@Valid @RequestBody RegisterRequestDto user) {
    return authService.register(user);
  }
  


  @PostMapping("/login")
  public AuthResponseDto login(@Valid @RequestBody LoginRequestDto dto) {
      return authService.login(dto);
  }


  @PostMapping("/refresh")
  public AuthResponseDto refreshToken(@Valid @RequestBody RefreshTokenRequestDto dto) {
    return authService.refreshAccessToken(dto);
  }
}
