package org.example;

public class Main {
    public static void main(String[] args) {

        BankAccount bank = new BankAccount(1000);

        Thread t1 = new Thread(() -> {
            for(int i = 0; i < 3; i++)
                bank.deposit(100);
        });

        Thread t2 = new Thread(() -> {
            for(int i = 0; i < 6; i++)
                bank.deposit(200);
        });

        t1.start();
        t2.start();

        System.out.println("1");

        try {
            t1.join();
            System.out.println("2");
            t2.join();
        } catch (InterruptedException e) {
            System.out.println("Поток был прерван.");
        }

        System.out.println("Итоговый баланс: " + bank.getBalance());

    }
}