import java.util.Arrays;

public class WindowsAlgos {

    int maxSumWindow(int[] numbersArray, int k) {

        if (k <= 0 || k > numbersArray.length) {
            return -1;
        }

        if (numbersArray.length == k) {
            return Arrays.stream(numbersArray).sum();
        }

        int currSum = 0, resultSum = 0;

        for (int i = 0; i < k; i++) {
            currSum += numbersArray[i];
        }

        resultSum = currSum;

        for (int r = k; r < numbersArray.length; r++) {
            currSum += numbersArray[r] - numbersArray[r - k];
            resultSum = Math.max(resultSum, currSum);
        }

        return resultSum;
    }

    public static void main(String[] args) {
        WindowsAlgos windowsAlgos = new WindowsAlgos();

        int[] a1 = { 1 };
        assert windowsAlgos.maxSumWindow(a1, 4) == -1 : "Test 1 failed";
        int[] a2 = { 100, -1, -1, -1, 1000 };
        assert windowsAlgos.maxSumWindow(a2, 2) == 999 : "Test 2 failed";
        int[] a3 = { 1, 2, 3, 4, 5 };
        assert windowsAlgos.maxSumWindow(a3, 3) == 12 : "Test 3 failed";
        int[] a4 = { 1, 2, 3, 4, 1000, -1000 };
        assert windowsAlgos.maxSumWindow(a4, 2) == 1004 : "Test 4 failed";
        int[] a5 = {};
        assert windowsAlgos.maxSumWindow(a5, 4) == -1 : "Test 5 failed";

        System.out.println("All tests passed successfully!");

    }

}
