package me.mano.shopCube.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import me.mano.shopCube.entity.Product;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {
  
}
