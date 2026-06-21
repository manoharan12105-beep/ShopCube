package me.mano.shopCube.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import me.mano.shopCube.entity.Users;

@Repository
public interface UserRepo extends JpaRepository<Users, Long> {
  
  Optional<Users> findByEmail(String email);
}
