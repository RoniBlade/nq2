/*
 * Вы — backend-разработчик в интернет-магазине «Рога и Копыта».
 * Дела идут в гору и магазин решил повысить лояльность покупателей,
 * предоставляя им персональные скидки.
 * К вам обратился product owner с задачей создать простую систему лояльности,
 * которая предоставляет процентную скидку на корзину.
 * Размер скидки зависит от покупателя.
 * Аналитики уже определили, какие скидки должны быть предоставлены покупателям.
 * 
 * 
 * ## Определения
 * 
 * Корзина - список покупок покупателя.
 * 
 * Покупка:
 * - id товара
 * - цена
 * - итоговая стоимость c учетом скидки
 * 
 * Скидка. Для покупателя может быть задан % скидки (целое число).
 * 
 * 
 * ## Задача
 * Написать часть новой системы лояльности, которая:
 * - на вход получает id покупателя и корзину
 * - вычисляет и применяет скидки
 * - возвращает корзину, в которой учтены скидки. Скидка учитывается в стоимости
 * покупки
 * 
 */

import java.util.List;
import java.util.stream.Collectors;

public class LoyaltySystem {

    public List<Purchase> applyDiscount(Long id, List<Purchase> basket) {
        Integer discount = getDiscount(id);
        System.out.println("Discount " + discount);

        basket.stream().map(p -> {
            p.setFinalPrice(subtractDiscount(p.getPrice(), discount));
            return p;
        }).collect(Collectors.toList());

        return basket;

    }

    private Integer getDiscount(Long id) {
        return (int) (Math.random() * 100);
    }

    private Integer subtractDiscount(Integer price, Integer discount) {
        return price - (price * discount) / 100;
    }

    public static void main(String[] args) {
        List<Purchase> basket = List.of(
                new Purchase(1L, 100),
                new Purchase(2L, 25000));

        LoyaltySystem system = new LoyaltySystem();
        List<Purchase> result = system.applyDiscount(123L, basket);
        result.forEach(System.out::println);
    }

}

class Purchase {
    Long id;
    Integer price;
    Integer discountPrice;

    public Purchase(Long id, Integer price) {
        this.id = id;
        this.price = price;
    }

    public void setFinalPrice(Integer discountPrice) {
        this.discountPrice = discountPrice;
    }

    public Integer getPrice() {
        return price;
    }

    public String toString() {
        return "Purchase {id=" + id + ", price=" + price + ", final=" + discountPrice + "}";
    }
}