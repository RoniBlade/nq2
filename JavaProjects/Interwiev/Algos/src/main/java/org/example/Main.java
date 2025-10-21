package org.example;

import org.example.Trees.TreeNode;
import org.example.Trees.TreeService;

public class Main {
    public static void main(String[] args) {

//        int[] sortedArray = {2, 4, 7, 10, 13, 19, 23, 29, 31};
//        int target = 19; // Элемент, который нужно найти
//
//        // Вызов бинарного поиска
//        int result = binarySearch(sortedArray, target);
//
//        // Вывод результата
//        if (result != -1) {
//            System.out.println("Элемент найден на индексе: " + result);
//        } else {
//            System.out.println("Элемент не найден в массиве.");
//        }


//        int[][] intervals = {{2, 6}, {1, 3}, {8, 10}, {15, 18}};
//        int[][] result = merge(intervals);
//
//        System.out.println("Результат:");
//        for (int[] interval : result) {
//            System.out.println(Arrays.toString(interval));
//        }

//        String input = "abcdabcbb";
//        int result = lengthOfLongestSubstring(input);
//        System.out.println("Длина самой длинной подстроки без повторяющихся символов: " + result);

        TreeNode root = new TreeNode(4);
        root.left = new TreeNode(2);
        root.right = new TreeNode(6);

        root.left.left = new TreeNode(1);
        root.left.right = new TreeNode(3);
        root.right.left = new TreeNode(5);
        root.right.right = new TreeNode(7);

        TreeService bstService = new TreeService();

//        root = bstService.reverseList(root);




////        TreeNode result = bst.searchBST(root, 12);
////        System.out.println(result != null ? result.val : "Not found");
//
        System.out.println("InOrdeer");
        bstService.inorder(root);
////
////        System.out.println("PreOrder");
////        bstService.preorder(root);
////
////        System.out.println("PostOrder");
////        bstService.postorder(root);
//
//        System.out.println(bstService.isBalanced(root));

//        LRUCache<Integer, String> cache = new LRUCache<>(2);
//        cache.put(1, "A");
//        cache.put(2, "B");
//        cache.get(1); // 1 становится "самым новым"
//        cache.put(3, "C"); // Удаляет 2 (наименее используемый)
//
//        System.out.println(cache); // {1=A, 3=C}




    }



}
