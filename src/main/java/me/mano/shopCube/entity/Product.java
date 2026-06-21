package me.mano.shopCube.entity;

import java.time.LocalDateTime;
import me.mano.shopCube.enums.Category;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @NotBlank
  private String name;

  @NotBlank
  private String description;

  @Positive
  private double price;

  @PositiveOrZero
  private int stockQuantity;

  @Enumerated(EnumType.STRING)
  private Category category;

  private String imageUrl;

  private LocalDateTime createdAt  = LocalDateTime.now();

  private LocalDateTime updatedAt = LocalDateTime.now();
}
