package org.example.tests;

import org.example.models.Cart;
import org.example.models.Item;
import org.example.services.DiscountCalculateService;
import org.example.services.DiscountService;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DiscountServiceTest {

    DiscountCalculateService calculateService = new DiscountCalculateService();
    DiscountService service = new DiscountService(calculateService);

    List<Item> items = new ArrayList<>();

    List<Item> test_items = new ArrayList<>();

    @Test
    public void test_doDiscount() {

        items.add(new Item(1, new BigDecimal("12.00")));
        items.add(new Item(2, new BigDecimal("13.00")));
        items.add(new Item(3, new BigDecimal("14.00")));
        items.add(new Item(4, new BigDecimal("15.00")));

        Cart cart = new Cart(items);

        cart = service.doDiscount(4, cart);

        Assert.assertEquals(cart.getItems().get(0).getDiscountedPrice(), new BigDecimal("11.52"));

    }

/* #TODO:

  Проверка на адекватность скидки (0 < discount < 99)




 */
}
