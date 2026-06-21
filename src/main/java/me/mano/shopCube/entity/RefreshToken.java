package me.mano.shopCube.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class RefreshToken {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Column(unique = true, nullable = false)
  private String token;

  private LocalDateTime  expiryDate;

  @OneToOne
  private Users user;
}
