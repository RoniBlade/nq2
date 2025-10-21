package org.example;

public class Main {
    public static void main(String[] args) {

        try {
            Person person = new Person(-1);

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }

    }
}