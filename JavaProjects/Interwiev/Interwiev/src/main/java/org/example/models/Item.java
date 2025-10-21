package org.example.models;

import lombok.*;

import java.math.BigDecimal;


@Data
public class Item {

    private Integer id;
    private BigDecimal price;
    private BigDecimal discountedPrice;


    public Item(Integer id, BigDecimal price) {
        this.id = id;
        this.price = price;
    }
}
