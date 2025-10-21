import java.util.Random;
import java.util.Scanner;

public class GuessNumber {

    private final Scanner scanner = new Scanner(System.in);
    private final Random random = new Random();

    public static void main(String[] args) {

        GuessNumber game = new GuessNumber();

        int guessedNumber = game.getRandom().nextInt(10000);

        Boolean play = true;

        while (play) {

            System.out.println("Пожалуйста введите число: ");

            int inputNumber = game.scanner.nextInt();

            if (inputNumber < guessedNumber) {
                System.out.println("Ваше число меньше загаданного, попробуйте еще раз");
            } else if (inputNumber > guessedNumber) {
                System.out.println("Ваше число больше загаданного, попробуйте еще раз");
            } else {
                System.out.println("Вы угадали!");
                play = false;
            }

        }

    }

    public Random getRandom() {
        return random;
    }

}
