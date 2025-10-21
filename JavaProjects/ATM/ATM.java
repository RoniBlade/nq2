import java.util.*;

/**
 * 
 * Банкомат.
 * Инициализируется набором купюр и умеет выдавать купюры для заданной суммы,
 * либо отвечать отказом.
 * При выдаче купюры списываются с баланса банкомата.
 * Допустимые номиналы: 50₽, 100₽, 500₽, 1000₽, 5000₽.
 * <p>
 * Другие валюты и номиналы должны легко добавляться разработчиками в будущем.
 * Многопоточные сценарии могут быть добавлены позже (например резервирование).
 */
public class ATM {

    Map<Currency, TreeMap<Integer, Integer>> atmStorage = new HashMap<>();
    TreeMap<Integer, Integer> balance = new TreeMap<>(Comparator.reverseOrder());

    public String withdraw(int amount, Currency currency) {

        Map<Integer, Integer> result = new HashMap<>();

        if (amount <= 0 || amount % 50 != 0) {
            return "Invalid amount: must be > 0 and divisible by 50";
        }

        Map<Integer, Integer> original = atmStorage.get(currency);

        if (original == null) {
            return "Storage is empty";
        }

        TreeMap<Integer, Integer> sorted = new TreeMap<>(Comparator.reverseOrder());
        sorted.putAll(original);

        int remaining = amount;

        for (Map.Entry<Integer, Integer> entry : sorted.entrySet()) {

            int denomination = entry.getKey();
            int availible = entry.getValue();

            int nedeed = remaining / denomination;

            if (nedeed > 0) {
                int used = Math.min(availible, nedeed);
                result.put(denomination, used);
                remaining -= denomination * used;
            }

            if (remaining == 0) {
                break;
            }

        }

        if (remaining == 0) {

            for (Map.Entry<Integer, Integer> entry : result.entrySet()) {

                int denomination = entry.getKey();
                int used = entry.getValue();

                atmStorage.get(currency).put(denomination, original.get(denomination) - used);

            }
        } else
            return "Can't withdraw this amount";

        return result.toString();

    }

    public static void main(String[] args) {
        // Подготовим начальный баланс банкомата
        TreeMap<Integer, Integer> rubBills = new TreeMap<>(Comparator.reverseOrder());
        rubBills.put(5000, 3); // две купюры по 5000
        rubBills.put(1000, 3); // три по 1000
        rubBills.put(500, 5); // пять по 500
        rubBills.put(100, 10); // десять по 100
        rubBills.put(50, 20); // двадцать по 50

        Map<Currency, TreeMap<Integer, Integer>> initialStorage = new HashMap<>();
        initialStorage.put(Currency.RUB, rubBills);

        // Создаем банкомат
        ATM atm = new ATM(initialStorage);

        // Пробуем снять деньги
        int amountToWithdraw = 20000;
        String result = atm.withdraw(amountToWithdraw, Currency.RUB);

        System.out.println("Попытка снять: " + amountToWithdraw + "₽");
        System.out.println("Результат: " + result);

        amountToWithdraw = 150;
        result = atm.withdraw(amountToWithdraw, Currency.RUB);

        System.out.println("Попытка снять: " + amountToWithdraw + "₽");
        System.out.println("Результат: " + result);

    }

    public ATM(Map<Currency, TreeMap<Integer, Integer>> atmStorage) {
        this.atmStorage = atmStorage;
    }

}

enum Currency {
    RUB,
    USD
}