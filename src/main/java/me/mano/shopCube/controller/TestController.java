package me.mano.shopCube.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.mano.shopCube.exception.EmailAlreadyExistsException;
import me.mano.shopCube.exception.ProductNotFoundException;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping
    public String test() {
        return "JWT Working";
    }

    @GetMapping("/emailAlreadyExist")
    public String testException() {
        throw new EmailAlreadyExistsException("Email Already Exsist");
    }

    @GetMapping("/prductNotFound")
    public String testProductNotFound() {
        throw new ProductNotFoundException("Product Not Found");
    }
}
