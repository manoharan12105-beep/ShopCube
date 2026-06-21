package me.mano.shopCube.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.mano.shopCube.entity.Product;
import me.mano.shopCube.service.ProductService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/product")
public class ProductController {

  private final ProductService productService;

  
  
  public ProductController(ProductService productService) {
    this.productService = productService;
  }



  @GetMapping("/getAll")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public List<Product> getAllProducts() {
    return productService.getAllProducts();
  }
  
}
