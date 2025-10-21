package org.example;

public class BankAccount {

    private int balance;

    public BankAccount(int balance) {
        this.balance = balance;
    }

    public int deposit(int amount) {
        if(amount <= 0) System.out.println("Сумма депозита должна быть положительной");
        synchronized(this) {
            balance += amount;
            System.out.println("Успешный депозит: " + amount + ". Текущий баланс: " + balance);
        }
        return balance;
    }


    public int getBalance() {
        return balance;
    }
}
