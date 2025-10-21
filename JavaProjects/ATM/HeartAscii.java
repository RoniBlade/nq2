public class HeartAscii {
    // Настройки
    static final int WIDTH = 80; // ширина в символах
    static final int HEIGHT = 30; // высота в строках
    static final char FILL = '█'; // можно заменить на '#'
    static final String RED = "\u001B[31m";
    static final String RESET = "\u001B[0m";
    static final String HOME = "\u001B[H"; // курсор в левый верх
    static final String HIDE_CURSOR = "\u001B[?25l";
    static final String SHOW_CURSOR = "\u001B[?25h";

    // 60 FPS
    static final long FRAME_NS = 16_666_667L;

    public static void main(String[] args) throws Exception {
        // Предвычисляем координаты сетки в диапазоне [-1.5, 1.5]
        final double[] xs = new double[WIDTH];
        final double[] ys = new double[HEIGHT];
        for (int cx = 0; cx < WIDTH; cx++) {
            xs[cx] = -1.5 + 3.0 * cx / (WIDTH - 1);
        }
        for (int cy = 0; cy < HEIGHT; cy++) {
            ys[cy] = 1.5 - 3.0 * cy / (HEIGHT - 1);
        }

        // Спрячем курсор и вернём при выходе
        System.out.print(HIDE_CURSOR);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.print(SHOW_CURSOR + RESET)));

        long next = System.nanoTime();
        long start = next;

        while (true) {
            // Пульс по времени: от 1.0 до 1.3 (≈ 1.5 Гц)
            double t = (System.nanoTime() - start) / 1_000_000_000.0;
            double scale = 1.0 + 0.15 * (1.0 + Math.sin(2 * Math.PI * 1.5 * t)); // 1.5 «ударов»/с

            StringBuilder sb = new StringBuilder(WIDTH * (HEIGHT + 2));
            sb.append(HOME).append(RED);

            for (int cy = 0; cy < HEIGHT; cy++) {
                double y = ys[cy] / scale;
                for (int cx = 0; cx < WIDTH; cx++) {
                    double x = xs[cx] / scale;
                    // Имплицитное уравнение сердца: (x^2 + y^2 - 1)^3 - x^2 * y^3 <= 0
                    double a = x * x + y * y - 1.0;
                    double v = a * a * a - (x * x) * (y * y * y);
                    sb.append(v <= 0.0 ? FILL : ' ');
                }
                sb.append('\n');
            }
            sb.append(RESET);

            System.out.print(sb); // единичный вывод кадра

            // Точно держим 60 FPS
            next += FRAME_NS;
            long sleepNs = next - System.nanoTime();
            if (sleepNs > 0) {
                // Преобразуем в миллисекунды+наносекунды для sleep
                long ms = sleepNs / 1_000_000L;
                int ns = (int) (sleepNs % 1_000_000L);
                try {
                    if (ms > 0 || ns > 0)
                        Thread.sleep(ms, ns);
                } catch (InterruptedException ignored) {
                }
            } else {
                // Если просели по времени — синхронизируемся на текущий момент
                next = System.nanoTime();
            }
        }
    }
}
