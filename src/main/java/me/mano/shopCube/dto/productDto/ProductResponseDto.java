package me.mano.shopCube.dto.productDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProductResponseDto {
  
  private Long id;

  @NotBlank
  private String name;

  @NotBlank
  private String description;

  @Positive
  private double price;
}
