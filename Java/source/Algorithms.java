import java.util.*;

public final class Algorithms {
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = (String) input.get("a");
        String b = (String) input.get("b");
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }

        StringBuilder sequence = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                sequence.append(a.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        return Map.of("length", dp[n][m], "sequence", sequence.reverse().toString());
    }

    static Map<String, Object> merge(Map<String, Object> input) {
        List<?> sizes = (List<?>) input.get("sizes");
        int n = sizes.size();
        List<List<Integer>> plan = new ArrayList<>();
        if (n <= 1) return Map.of("cost", 0L, "plan", plan);

        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + ((Number) sizes.get(i)).longValue();
        }
        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int length = 2; length <= n; length++) {
            for (int l = 0; l + length <= n; l++) {
                int r = l + length - 1;
                dp[l][r] = Long.MAX_VALUE;
                for (int k = l; k < r; k++) {
                    long cost = dp[l][k] + dp[k + 1][r] + prefix[r + 1] - prefix[l];
                    if (cost < dp[l][r]) {
                        dp[l][r] = cost;
                        split[l][r] = k;
                    }
                }
            }
        }
        mergePlan(0, n - 1, split, plan);
        return Map.of("cost", dp[0][n - 1], "plan", plan);
    }

    private static void mergePlan(int l, int r, int[][] split, List<List<Integer>> plan) {
        if (l == r) return;
        int k = split[l][r];
        mergePlan(l, k, split, plan);
        mergePlan(k + 1, r, split, plan);
        plan.add(List.of(l, k, r));
    }

    static Map<String, Object> tree(Map<String, Object> input) {
        int n = ((Number) input.get("n")).intValue();
        List<Integer> selected = new ArrayList<>();
        if (n == 0) return Map.of("weight", 0L, "selected", selected);

        List<?> weights = (List<?>) input.get("weights");
        List<List<Integer>> graph = new ArrayList<>(n);
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
        for (Object item : (List<?>) input.get("edges")) {
            List<?> edge = (List<?>) item;
            int u = ((Number) edge.get(0)).intValue();
            int v = ((Number) edge.get(1)).intValue();
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        int[] parent = new int[n];
        int[] order = new int[n];
        parent[0] = -1;
        int size = 1;
        for (int i = 0; i < size; i++) {
            int u = order[i];
            for (int v : graph.get(u)) {
                if (v != parent[u]) {
                    parent[v] = u;
                    order[size++] = v;
                }
            }
        }

        long[] take = new long[n];
        long[] skip = new long[n];
        for (int i = n - 1; i >= 0; i--) {
            int u = order[i];
            take[u] += ((Number) weights.get(u)).longValue();
            int p = parent[u];
            if (p != -1) {
                take[p] += skip[u];
                skip[p] += Math.max(take[u], skip[u]);
            }
        }

        boolean[] chosen = new boolean[n];
        for (int u : order) {
            chosen[u] = (parent[u] == -1 || !chosen[parent[u]]) && take[u] > skip[u];
            if (chosen[u]) selected.add(u);
        }
        return Map.of("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    static Map<String, Object> naive(Map<String, Object> input) {
        String text = (String) input.get("text");
        String pattern = (String) input.get("pattern");
        int n = text.length(), m = pattern.length();
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i <= n - m; i++) {
            int j = 0;
            while (j < m && text.charAt(i + j) == pattern.charAt(j)) j++;
            if (j == m) positions.add(i);
        }
        return Map.of("positions", positions);
    }

    static Map<String, Object> kmp(Map<String, Object> input) {
        String text = (String) input.get("text");
        String pattern = (String) input.get("pattern");
        int m = pattern.length();
        if (m == 0) return naive(input);

        int[] pi = new int[m];
        for (int i = 1; i < m; i++) {
            int j = pi[i - 1];
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (pattern.charAt(i) == pattern.charAt(j)) j++;
            pi[i] = j;
        }

        List<Integer> positions = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < text.length(); i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == m) {
                positions.add(i - m + 1);
                j = pi[j - 1];
            }
        }
        return Map.of("positions", positions);
    }

    static Map<String, Object> rk(Map<String, Object> input) {
        String text = (String) input.get("text");
        String pattern = (String) input.get("pattern");
        int n = text.length(), m = pattern.length();
        if (m == 0 || m > n) return naive(input);

        final long base = 31, modulus = 101;
        long power = 1, patternHash = 0, windowHash = 0;
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * base + pattern.charAt(i)) % modulus;
            windowHash = (windowHash * base + text.charAt(i)) % modulus;
            if (i > 0) power = power * base % modulus;
        }

        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i <= n - m; i++) {
            if (windowHash == patternHash) {
                int j = 0;
                while (j < m && text.charAt(i + j) == pattern.charAt(j)) j++;
                if (j == m) positions.add(i);
            }
            if (i < n - m) {
                windowHash = (windowHash - text.charAt(i) * power % modulus + modulus) % modulus;
                windowHash = (windowHash * base + text.charAt(i + m)) % modulus;
            }
        }
        return Map.of("positions", positions);
    }

    static Map<String, Object> suffix(Map<String, Object> input) {
        String text = (String) input.get("text");
        int n = text.length();
        if (n == 0) return Map.of("sa", List.of(), "lcp", List.of(), "repeated", "");

        Integer[] sa = new Integer[n];
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = text.charAt(i);
        }
        for (int step = 1; step < n; step *= 2) {
            final int width = step;
            final int[] currentRank = rank;
            Comparator<Integer> compare = (a, b) -> {
                int first = Integer.compare(currentRank[a], currentRank[b]);
                if (first != 0) return first;
                int secondA = a + width < n ? currentRank[a + width] : -1;
                int secondB = b + width < n ? currentRank[b + width] : -1;
                return Integer.compare(secondA, secondB);
            };
            Arrays.sort(sa, compare);
            int[] nextRank = new int[n];
            for (int i = 1; i < n; i++) {
                nextRank[sa[i]] = nextRank[sa[i - 1]]
                        + (compare.compare(sa[i - 1], sa[i]) < 0 ? 1 : 0);
            }
            rank = nextRank;
            if (rank[sa[n - 1]] == n - 1) break;
        }

        int[] position = new int[n];
        int[] lcp = new int[n];
        for (int i = 0; i < n; i++) position[sa[i]] = i;
        int common = 0, bestLength = 0, bestStart = 0;
        for (int i = 0; i < n; i++) {
            int r = position[i];
            if (r == 0) {
                common = 0;
                continue;
            }
            int j = sa[r - 1];
            while (i + common < n && j + common < n
                    && text.charAt(i + common) == text.charAt(j + common)) common++;
            lcp[r] = common;
            if (common > bestLength) {
                bestLength = common;
                bestStart = i;
            }
            if (common > 0) common--;
        }
        return Map.of("sa", sa, "lcp", lcp,
                "repeated", text.substring(bestStart, bestStart + bestLength));
    }

    static Map<String, Object> aho(Map<String, Object> input) {
        List<?> patterns = (List<?>) input.get("patterns");
        List<?> texts = (List<?>) input.get("texts");
        List<int[]> trie = new ArrayList<>();
        trie.add(new int[95]);
        int[] terminal = new int[patterns.size()];

        for (int i = 0; i < patterns.size(); i++) {
            String pattern = (String) patterns.get(i);
            int state = 0;
            for (int j = 0; j < pattern.length(); j++) {
                int c = pattern.charAt(j) - 32;
                int[] next = trie.get(state);
                if (next[c] == 0) {
                    next[c] = trie.size();
                    trie.add(new int[95]);
                }
                state = next[c];
            }
            terminal[i] = state;
        }

        int[] failure = new int[trie.size()];
        int[] order = new int[trie.size()];
        int size = 0;
        for (int child : trie.get(0)) {
            if (child != 0) order[size++] = child;
        }
        for (int i = 0; i < size; i++) {
            int state = order[i];
            int[] next = trie.get(state);
            int[] fallback = trie.get(failure[state]);
            for (int c = 0; c < 95; c++) {
                int child = next[c];
                if (child == 0) {
                    next[c] = fallback[c];
                } else {
                    failure[child] = fallback[c];
                    order[size++] = child;
                }
            }
        }

        List<long[]> counts = new ArrayList<>();
        for (Object item : texts) {
            String text = (String) item;
            long[] visits = new long[trie.size()];
            int state = 0;
            for (int i = 0; i < text.length(); i++) {
                state = trie.get(state)[text.charAt(i) - 32];
                visits[state]++;
            }
            for (int i = size - 1; i >= 0; i--) {
                int node = order[i];
                visits[failure[node]] += visits[node];
            }
            long[] row = new long[patterns.size()];
            for (int i = 0; i < row.length; i++) row[i] = visits[terminal[i]];
            counts.add(row);
        }
        return Map.of("counts", counts);
    }
}
