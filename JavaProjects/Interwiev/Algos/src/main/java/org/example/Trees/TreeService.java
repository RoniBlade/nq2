package org.example.Trees;

public class TreeService {

    public TreeNode searchBST(TreeNode root, int val) {
        if (root == null || root.val == val) return root;
        return val < root.val ? searchBST(root.left, val) : searchBST(root.right, val);
    }


    public void inorder(TreeNode root) {
        if(root == null) return;
        inorder(root.left);
        System.out.println(root.val + " ");
        inorder(root.right);
    }


    public void preorder(TreeNode root) {
        if(root == null) return;
        System.out.println(root.val + " ");
        preorder(root.left);
        preorder(root.right);
    }

    public void postorder(TreeNode root) {
        if(root == null) return;
        postorder(root.left);
        postorder(root.right);
        System.out.println(root.val + " ");
    }

    public int maxDepth(TreeNode root) {
        if(root == null) return 0;
        return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
    }


    public boolean isBalanced(TreeNode root) {
        return checkHeight(root) > 1;
    }

    int checkHeight(TreeNode root) {
        if(root == null) return 0;

        int heightLeft = checkHeight(root.left);
        int heightRight = checkHeight(root.right);

        if(heightRight == -1 || heightLeft == -1 || Math.abs(heightRight - heightLeft) > 1) {
            return -1;
        }

        return Math.max(heightRight, heightLeft) + 1;
    }


    public TreeNode reverseList(TreeNode head) {
        if (head == null) return null;
        TreeNode prev = null;
        TreeNode curr = head;
        while (curr != null) {
            TreeNode nextTemp = curr.right;
            curr.right = prev;
            prev = curr;
            curr = nextTemp;
        }
        return prev;
    }



}
