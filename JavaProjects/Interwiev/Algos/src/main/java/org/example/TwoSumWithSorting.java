package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

public class TwoSumWithSorting {

    public static int[] findTwoSum(int[] nums, int target) {
        HashMap<Integer, Integer> numToIndex = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];

            if (numToIndex.containsKey(complement)) {
                return new int[]{numToIndex.get(complement), i};
            }

            numToIndex.put(nums[i], i);
        }

        throw new IllegalArgumentException("No solution found");
    }

    public static void main(String[] args) {
//        int[] nums = {2, 8, 5, 4};
//        int target = 9;
//        int[] result = findTwoSum(nums, target);
//        System.out.println("Indices: [" + result[0] + ", " + result[1] + "]");


//        IntStream.range(1, 11)
//                .mapToObj(i -> CompletableFuture.runAsync(() -> System.out.println(i)))
//                .forEach(CompletableFuture::join);

        List a = new ArrayList();

        a.add(34);
        a.add("asda");
        System.out.println(a);

    }
}
