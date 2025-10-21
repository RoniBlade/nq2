package org.example;

public class Person {

    private int age;

    public Person(int age) throws InvalidAgeException {
        if(age < 0) throw new InvalidAgeException("Возраст не может быть отрицательным: " + age);
        this.age = age;
    }

    public int getAge() {
        return age;
    }
}
