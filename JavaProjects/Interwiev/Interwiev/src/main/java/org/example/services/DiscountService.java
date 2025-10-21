package org.example.services;

import org.example.models.Cart;
import org.example.models.Item;

import java.math.BigDecimal;

public class DiscountService {

    DiscountCalculateService calculateService = new DiscountCalculateService();


    public DiscountService(DiscountCalculateService calculateService) {
        this.calculateService = calculateService;
    }

    public Cart doDiscount(Integer id, Cart cart) {

        Integer discount = calculateService.getDiscount(id);

        for(Item item : cart.getItems() ) {
            item.setDiscountedPrice(item.getPrice().multiply(new BigDecimal(100 - discount)).divide(new BigDecimal(100)));
        }

        return cart;
    }

}
