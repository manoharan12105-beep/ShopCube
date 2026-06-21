package me.mano.shopCube.exception;

public class EmailAlreadyExistsException extends RuntimeException{
  public EmailAlreadyExistsException(String message) {
    super(message);
  }
}
