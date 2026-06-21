package me.mano.shopCube.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.mano.shopCube.dto.AuthResponseDto;
import me.mano.shopCube.dto.LoginRequestDto;
import me.mano.shopCube.dto.RefreshTokenRequestDto;
import me.mano.shopCube.dto.RegisterRequestDto;
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
  public Users register(@RequestBody RegisterRequestDto user) {
    return authService.register(user);
  }
  


  @PostMapping("/login")
  public AuthResponseDto login(@RequestBody LoginRequestDto dto) {
      return authService.login(dto);
  }


  @PostMapping("/refresh")
  public AuthResponseDto refreshToken(@RequestBody RefreshTokenRequestDto dto) {
    return authService.refreshAccessToken(dto);
  }
}
