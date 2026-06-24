package me.mano.shopCube.dto.productDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import me.mano.shopCube.enums.Category;

@Data
public class ProductRequestDto {
  @NotBlank
  private String name;

  @NotBlank
  private String description;

  @Positive
  private double price;

  @PositiveOrZero
  private int stockQuantity;

  private Category category;

  private String imageUrl;
}
