package org.example;

public class BinarySearchExample {

    public static int binarySearch(int[] array, int target) {

        int left = 0;
        int right = array.length - 1;

        int mid = (left + right) / 2;

        while (left < right) {
            if (array[mid] == target) return mid;
            if (array[mid] < target) left = mid;
            if (array[mid] > target) right = mid;

            mid = (left + right) / 2;
        }

        return -1;
    }
}
