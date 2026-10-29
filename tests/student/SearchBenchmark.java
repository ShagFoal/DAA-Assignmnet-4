import java.util.*;

public final class SearchBenchmark {
    private static volatile Object result;
    private static final int WARMUP = 2000;
    private static final int CALLS = 100;

    private static Map<String, Object> search(String algorithm, Map<String, Object> input) {
        return switch (algorithm) {
            case "naive" -> Algorithms.naive(input);
            case "kmp" -> Algorithms.kmp(input);
            case "rk" -> Algorithms.rk(input);
            default -> throw new IllegalArgumentException(algorithm);
        };
    }

    public static void main(String[] args) {
        System.out.println("| Case | n | m | Method | Run 1 | Run 2 | Run 3 | Median |");
        System.out.println("|---|---:|---:|---|---:|---:|---:|---:|");
        for (String type : List.of("random", "repetitive")) {
            for (int n : new int[]{500, 1500, 3000}) {
                int m = n / 10;
                String text;
                if (type.equals("random")) {
                    Random random = new Random(42);
                    StringBuilder builder = new StringBuilder(n);
                    for (int i = 0; i < n; i++) builder.append((char) ('a' + random.nextInt(4)));
                    text = builder.toString();
                } else {
                    text = "a".repeat(n);
                }
                int start = (n - m) / 2;
                String pattern = text.substring(start, start + m);
                Map<String, Object> input = Map.of("text", text, "pattern", pattern);
                List<Integer> expected = new ArrayList<>();
                for (int i = 0; i <= n - m; i++) {
                    if (text.startsWith(pattern, i)) expected.add(i);
                }
                for (String algorithm : List.of("naive", "kmp", "rk")) {
                    if (!expected.equals(search(algorithm, input).get("positions"))) {
                        throw new AssertionError(type + " " + n + " " + algorithm);
                    }
                }
                for (String algorithm : List.of("naive", "kmp", "rk")) {
                    for (int i = 0; i < WARMUP; i++) result = search(algorithm, input);
                    double[] samples = new double[3];
                    for (int run = 0; run < samples.length; run++) {
                        long before = System.nanoTime();
                        for (int i = 0; i < CALLS; i++) result = search(algorithm, input);
                        samples[run] = (System.nanoTime() - before) / (CALLS * 1_000_000.0);
                    }
                    double[] sorted = samples.clone();
                    Arrays.sort(sorted);
                    System.out.printf(Locale.ROOT,
                            "| %s | %d | %d | %s | %.6f | %.6f | %.6f | %.6f |%n",
                            type, n, m, algorithm, samples[0], samples[1], samples[2], sorted[1]);
                }
            }
        }
    }
}
