package org.larb.controllers;


import org.larb.model.Product;
import org.larb.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductController {


    @Autowired
    private ProductService productService;

    /**
     * Регистрация нового товара
     *
     * @param product Товар для регистрации
     * @return Зарегистрированный товар
     */
    @PostMapping("/register")
    public ResponseEntity<Product> registerProduct(@RequestBody Product product) {
        Product registeredProduct = productService.registerProduct(product);
        return new ResponseEntity<>(registeredProduct, HttpStatus.CREATED);
    }

    /**
     * Получение списка всех товаров
     *
     * @return Список всех товаров
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    /**
     * Получения товара по id
     *
     * @param id
     * @return Информация о продукте
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductByid(Long id) {
        Optional<Product> product = productService.getProductById(id);
        return product.map((p) -> new ResponseEntity<>(p, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.OK));
    }

}