package org.example;

import java.util.HashSet;
import java.util.Set;

public class StringService {



    public int lengthOfLongestSubstring(String s) {
        int left = 0, maxLength = 0;
        HashSet<Character> uniqueChars = new HashSet<>();

        for (int right = 0; right < s.length(); right++) {
            while (uniqueChars.contains(s.charAt(right))) {
                uniqueChars.remove(s.charAt(left));
                left++;
            }

            uniqueChars.add(s.charAt(right));
            maxLength = Math.max(maxLength, right - left + 1);

        }


        return maxLength;
    }
}
