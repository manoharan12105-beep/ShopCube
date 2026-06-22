package me.mano.shopCube.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter{

  private final JwtService jwtService;
  private final MyUserDetailsService userDetailsService;
  

  public JwtFilter(JwtService jwtService, MyUserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
  }


  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");

    if(authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    if(!jwtService.validateAccessToken(token)) {
      filterChain.doFilter(request, response);
      return;
    }

    String email = jwtService.extractEmailFromAccessToken(token);

    if(SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = userDetailsService.loadUserByUsername(email);

      UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

      System.out.println(userDetails.getAuthorities());

      SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    filterChain.doFilter(request, response);
  }
  
}
