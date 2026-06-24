package me.mano.shopCube.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import me.mano.shopCube.dto.ErrorResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler {
  
  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ErrorResponseDto> handleEmailAlreadyExistException (EmailAlreadyExistsException ex) {
    ErrorResponseDto error = new ErrorResponseDto(409, ex.getMessage(), LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(ProductNotFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleProductNotFoundException (ProductNotFoundException ex) {
    ErrorResponseDto error = new ErrorResponseDto(404, ex.getMessage(), LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }
}
