package me.mano.shopCube.repo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import me.mano.shopCube.entity.Product;
import me.mano.shopCube.enums.Category;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {

  List<Product> findByCategory(Category category);

  List<Product> findByNameContainingIgnoreCase(String name);

  Page<Product> findByName(String name, Pageable pageable);

  List<Product> findByPriceLessThan(double price);
  
}
