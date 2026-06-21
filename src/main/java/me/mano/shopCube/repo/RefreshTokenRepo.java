package me.mano.shopCube.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import me.mano.shopCube.entity.RefreshToken;
import me.mano.shopCube.entity.Users;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByToken(String token);

  Optional<RefreshToken> findByUser(Users user);
}
