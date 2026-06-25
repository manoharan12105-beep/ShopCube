package me.mano.shopCube.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import me.mano.shopCube.dto.productDto.ProductRequestDto;
import me.mano.shopCube.dto.productDto.ProductResponseDto;
import me.mano.shopCube.entity.Product;
import me.mano.shopCube.service.ProductService;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/product")
public class ProductController {

  private final ProductService productService;
  
  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @PostMapping("/addProducts")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public List<Product> addProducts(@RequestBody List<@Valid Product> prodList) {
    return productService.addProducts(prodList);
  }

  @GetMapping("/get/{id}")
  public ProductResponseDto getProduct(@PathVariable Long id) {
    return productService.getProduct(id);
  }


  @GetMapping("/getAll/")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public List<ProductResponseDto> getAllProducts(@RequestParam(defaultValue = "1") int pageNo,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(defaultValue = "id") String sortBy,
                                                @RequestParam(defaultValue = "asc") String sortDir,
                                                @RequestParam() String search) {

    Sort sort = null;
    if(sortDir.equalsIgnoreCase("asc")){
      sort = Sort.by(sortBy).ascending();
    }
    else {
      sort = sort.by(sortBy).descending();
    }

    return productService.getAllProducts(PageRequest.of(pageNo - 1, pageSize, sort), search);
  }

  @PutMapping("/update/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Product updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDto dto) {
      return productService.updateProduct(id, dto);
  }
  
  @DeleteMapping("/remove/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public String deleteProduct(@PathVariable Long id) {
    return productService.deleteProduct(id);
  }

  @GetMapping("/get/category/{category}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public List<ProductResponseDto> getByCategory(@PathVariable String category) {
    return productService.getByCategory(category);
  }


  @GetMapping("/get/name/{name}")
  public List<ProductResponseDto> getProductByName(@PathVariable String name) {
    return productService.getProductByName(name);

  }
}
