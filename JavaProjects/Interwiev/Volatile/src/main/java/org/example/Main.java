package org.example;


public class Main {
    public static void main(String[] args) throws InterruptedException {

        Counter counter = new Counter();

        Runnable task = () -> {
            for(int i = 0; i < 100; i++) {
                counter.increment();
                System.out.println("t1 " + counter.getCount());
            }
        };

        Thread t1 = new Thread(task);

        Thread t2 = new Thread(() -> {
            for(int i = 0; i < 100; i++) {
                counter.increment();
                System.out.println("t2 " + counter.getCount());
            }
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            System.out.println("Поток был приостановлен " + e.getMessage());
        }


        System.out.println(counter.getCount());
    }
}