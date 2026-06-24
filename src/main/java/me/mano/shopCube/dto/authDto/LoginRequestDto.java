package me.mano.shopCube.dto.authDto;

import lombok.Data;

@Data
public class LoginRequestDto {
  
  private String email;

  private String password;
}
