package me.mano.shopCube.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
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

  @CreationTimestamp
  private LocalDateTime createdAt  = LocalDateTime.now();

  @UpdateTimestamp
  private LocalDateTime updatedAt = LocalDateTime.now();
}
