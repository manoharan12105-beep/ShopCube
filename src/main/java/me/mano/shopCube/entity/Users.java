package me.mano.shopCube.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import me.mano.shopCube.enums.Role;

@Data
@Entity
public class Users {
  
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @NotBlank
  private String username;

  @Email
  @Column(unique = true)
  private String email;

  @NotBlank
  private String password;

  @Enumerated(EnumType.STRING)
  private Role role;

  private LocalDateTime createdAt = LocalDateTime.now();

  
}
