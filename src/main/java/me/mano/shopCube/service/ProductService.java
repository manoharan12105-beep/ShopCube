package me.mano.shopCube.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import me.mano.shopCube.dto.productDto.ProductRequestDto;
import me.mano.shopCube.dto.productDto.ProductResponseDto;
import me.mano.shopCube.entity.Product;
import me.mano.shopCube.exception.ProductNotFoundException;
import me.mano.shopCube.repo.ProductRepo;

@Service
public class ProductService {
  
  private final ProductRepo productRepo;

  ProductService(ProductRepo productRepo) {
    this.productRepo = productRepo;
  }

  // Get all products list
  public List<ProductResponseDto> getAllProducts() {
    List <Product> product = productRepo.findAll();
    List<ProductResponseDto> dtoList = new ArrayList<>();

    for (Product prod : product) {
      dtoList.add(mapToDto(prod));
    }

    return dtoList;
  }



  public List<Product> addProducts(List<Product> prodList) {

    return productRepo.saveAll(prodList);
  }



  public ProductResponseDto getProduct(Long id) {
    Product product = productRepo.findById(id).orElseThrow(() -> new ProductNotFoundException("There is no product with the id : " + id));

    return mapToDto(product);
  }


  public ProductResponseDto mapToDto(Product product) {
    ProductResponseDto dto = new ProductResponseDto();

    dto.setId(product.getId());
    dto.setName(product.getName());
    dto.setDescription(product.getDescription());
    dto.setPrice(product.getPrice());

    return dto;
  }

  public Product updateProduct(Long id, ProductRequestDto dto) {
    Product product = productRepo.findById(id).orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found"));

    product.setName(dto.getName());
    product.setDescription(dto.getDescription());
    product.setPrice(dto.getPrice());
    product.setStockQuantity(dto.getStockQuantity());
    product.setCategory(dto.getCategory());
    product.setImageUrl(dto.getImageUrl());

    return productRepo.save(product);
  }

  

  public String deleteProduct(Long id) {
    Product product = productRepo.findById(id).orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found"));

    productRepo.delete(product);

    return "Product with id " + id + " has been deleted.";
  }
}
