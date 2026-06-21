package me.mano.shopCube.service;

import java.util.List;

import org.springframework.stereotype.Service;

import me.mano.shopCube.entity.Product;
import me.mano.shopCube.repo.ProductRepo;

@Service
public class ProductService {
  
  private final ProductRepo productRepo;

  ProductService(ProductRepo productRepo) {
    this.productRepo = productRepo;
  }

  // Get all products list
  public List<Product> getAllProducts() {
    return productRepo.findAll();
  }

  
}
