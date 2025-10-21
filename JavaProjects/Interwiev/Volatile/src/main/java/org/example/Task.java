package org.example;

public class Task extends Thread{
    private volatile boolean running = true;
    private final String name;

    public Task(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        while(running) {
            System.out.println("Поток " + name + " работает...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Поток \" + name + \" прерван");
            }
        }
        System.out.println("Поток \" + name + \" остановлен");
    }

    public void stopThread() {
        running = false;
    }
}
