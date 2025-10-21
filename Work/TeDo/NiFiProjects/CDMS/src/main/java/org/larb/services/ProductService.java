package org.larb.services;


import org.larb.model.Product;
import org.larb.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    /*
        TODO
            Зарегистрировать товар
            Получить товар\товары
            Получить информацию о товаре
     */

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }


    /**
     *
     * @param product Товар для регистрации
     * @return Зарегистрированный товар
     */
    public Product registerProduct(Product product) {
        productRepository.save(product);
        return product;
    }

    /**
     *
     * @return Список всех пользователей
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * @param productId ID товара
     * @return Информация о товаре или пустой Optional, если товар не найден
     */
    public Optional<Product> getProductById(Long productId) {
        return productRepository.findById(productId);
    }

}