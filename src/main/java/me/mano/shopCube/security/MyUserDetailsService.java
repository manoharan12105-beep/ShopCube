package me.mano.shopCube.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import me.mano.shopCube.entity.Users;
import me.mano.shopCube.repo.UserRepo;
@Service
public class MyUserDetailsService implements UserDetailsService{

  @Autowired
  private UserRepo userRepo;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    Users user = userRepo.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
    
    return new UserPrincipal(user);

  }
}
