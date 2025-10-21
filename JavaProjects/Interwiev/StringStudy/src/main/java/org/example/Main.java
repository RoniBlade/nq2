package org.example;


public class Main {
    public static void main(String[] args) {
        String s1 = "1234567";
        System.out.println(s1.indexOf('2'));

        String s2 = "1 2 3 4 5 6";
        System.out.println(s2.replace(" ", ""));

        String s3 = "lower case";
        System.out.println(s3.toUpperCase());

        String s4 = "111ssss111";
        System.out.println(isPalindrom(s4));

        String [] sArray = {s1,s2,s3,s4};
        System.out.println(joinStrings(sArray));

    }

    public static String joinStrings(String[] strs) {

        StringBuilder sb = new StringBuilder();

        for (String s : strs) {
            sb.append(s).append(" ");
        }

        return sb.toString();

    }

    public static boolean isPalindrom(String s) {

        if (s.isEmpty()) return false;

        int l = 0, r = s.length() - 1;
        while (l < r) {
            if(s.charAt(l) != s.charAt(r)) return false;
            r--;
            l++;
        }
        return true;
    }

}