package org.example;


public class Main {
    public static void main(String[] args) {

        Converter converter = new Converter();

        double value = 2.50;

        System.out.println(converter.convertFromDoubleToLong(value));
        System.out.println(converter.convertFromDoubleToByte(value));
        System.out.println(converter.convertFromDoubleToLong(value));

    }
}