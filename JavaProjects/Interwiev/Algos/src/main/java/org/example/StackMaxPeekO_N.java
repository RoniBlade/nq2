package org.example;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

//2) написать свой класс, который бы реализовывал стек, с методами push, pop,  и peekMax, который бы возращал max Элемент в стеке за О(1)
public class StackMaxPeekO_N<T extends Comparable<T>> {

    Deque<Node<T>> stack = new LinkedList<>();

    public void push(T value) {
        if(stack.isEmpty()) {
            stack.push(new Node<T>(value, value));
        } else {
            T currentMax = stack.peek().max;
            T newMax = currentMax.compareTo(value) > 0 ? value : currentMax;
            stack.push(new Node<T>(value, newMax));
        }
    }

    public T pop() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        return stack.pop().value;
    }

    public T peekMax() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        return stack.peek().max;
    }



}

class Node<T> {
    T value;
    T max;


    public Node(T value, T max) {
        this.value = value;
        this.max = max;
    }
}