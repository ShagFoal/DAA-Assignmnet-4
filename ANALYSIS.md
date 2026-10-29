# Assignment 4 — Analysis

## Environment and commands

Java: Eclipse Temurin 17.0.21+9, HotSpot; Python 3.15.0. Local environment: Fedora Linux 45, x86_64, kernel 7.2.11-300.fc45.x86_64, Intel Core i5-12500H, 15.3 GiB RAM. Tests and benchmark use a 384 MiB Java heap. Measurements were taken on 29 October 2026.

Run from the repository root; the launcher compiles the Java sources with `--release 17`:

```sh
export PATH=/tmp/daa-a4-jdk/bin:$PATH
python3 tools/run.py --check
python3 tools/run.py --input examples/lcs.jsonl
python3 tests/student/check.py
javac --release 17 -cp Java/build -d Java/build tests/student/SearchBenchmark.java
java -Xmx384m -cp Java/build SearchBenchmark
```

## Algorithms and correctness

**LCS.** `dp[i][j]` is the optimum for prefixes `a[0..i)` and `b[0..j)`. Empty prefixes have value zero. Equal final characters extend `dp[i-1][j-1]`; otherwise the optimum is `max(dp[i-1][j], dp[i][j-1])`, since at least one unmatched final character can be excluded. Increasing prefix lengths make dependencies available. Backtracking follows a matching diagonal or a maximum predecessor, so the returned subsequence has optimal length. Ties prefer decreasing `i`; an empty input returns an empty sequence.

**Merge.** `dp[l][r]` is the minimum cost for the closed interval of original blocks. Single blocks cost zero; at most one block returns an empty plan. Increasing interval lengths evaluate every last split `k`: `dp[l][k] + dp[k+1][r] + prefix[r+1] - prefix[l]`. Every legal final merge has this form, and either subplan must be optimal, proving the recurrence. The first minimum split is retained. Reconstruction emits both child plans before `[l,k,r]`, ensuring chronological adjacency. Costs and prefix sums use `long`.

**Tree.** An iterative traversal rooted at 0 records parents and an order. Processing that order backwards computes `take[u] = weights[u] + sum(skip[child])` and `skip[u] = sum(max(take[child], skip[child]))`. Child subtrees are independent once the parent choice is fixed, so these two cases cover every independent set. Forward reconstruction excludes children of selected parents; otherwise it selects `u` only when `take[u] > skip[u]`, skipping ties. Zero-initialized skip states allow the empty set and all-negative inputs; an empty tree returns zero. All totals use `long`, and deep chains require no recursive calls.

**Naive.** Try every start from 0 through `n-m`, comparing characters until a mismatch or a complete match. This checks exactly every possible window, including the last and overlapping ones. With `m=0`, every boundary `0..n` is reported; with `m>n`, the loop is empty.

**KMP.** `pi[i]` is the longest proper prefix of the pattern that is also a suffix ending at `i`. It is computed left to right using already known prefix fallbacks. During the text scan, `j` is the matched prefix length. On mismatch, `pi[j-1]` retains the next possible border without moving the text index backwards. After a full match the same fallback preserves overlaps. An empty pattern uses the boundary case of naive search.

**Rabin–Karp.** The window hash is a polynomial in base 31 modulo 101 using ASCII codes, with the highest power on the left. After removing the outgoing character times `31^(m-1)`, the remainder is normalized, multiplied by 31 and extended with the incoming character. This preserves the hash of each current window. Equal hashes trigger full character verification, so collisions cannot create false matches. Empty and oversized patterns are handled before forming a window. Rolling arithmetic uses `long`.

**Suffix array and Kasai.** Prefix doubling sorts suffix indices by `(rank[i], rank[i+width])`, using -1 beyond the text. Ranks initially represent characters; sorting and compressing equal pairs correctly doubles the represented prefix length each round. Once ranks are unique or prefixes cover the text, the order is the full suffix order. `Arrays.sort` compares boxed indices; suffix strings are not materialized. Kasai uses inverse ranks to compare each suffix with its previous neighbour, reusing the previous common length minus one. The reused prefix remains valid, and the total number of extensions is linear. The maximum LCP gives the longest repeat because suffixes sharing a prefix form a consecutive range. Strict improvements retain one valid answer on ties. Empty input returns empty arrays; no sentinel is output.

**Aho–Corasick.** A trie stores 95 transitions per state for ASCII 32–126. BFS builds failure links to the longest proper suffix that is a trie prefix and fills missing transitions through those links. Thus each text transition retains the longest relevant suffix. Visits are counted at reached states and propagated through failure links in reverse BFS order, counting suffix patterns and overlaps without enumerating all matches. Each dictionary position reads its terminal state's count; duplicates share a state. Every text starts at the root with fresh `long` counters. Empty dictionaries produce empty rows, empty text lists produce no rows, and empty texts produce zero counts.

### Complexity

Bounds exclude JSON parsing. `A,B` are LCS lengths; `n` is the block/vertex/text count as applicable, `m` is pattern length, and `z` is the number of reported starts. For Aho, `L` is total dictionary length, `S` includes the trie root, `p` is dictionary size, `q` is text count and `T` is total text length. Stored input, auxiliary storage and output are separated below. Arrays hold DP/ranks/transitions; `ArrayList` holds adjacency lists and variable-length results.

| Command | Time | Stored input | Auxiliary memory | Output memory |
|---|---|---|---|---|
| LCS | O((A+1)(B+1)) | O(A+B+1) | O((A+1)(B+1)) | O(min(A,B)+1) |
| merge | O(n³+1) | O(n+1) | O(n²+1), including the reconstruction stack | O(n+1) |
| tree | O(n+1) | O(n+1) | O(n+1) | O(n+1) |
| naive | O(nm+n+1) | O(n+m+1) | O(1) | O(z+1) |
| KMP | O(n+m+z+1) | O(n+m+1) | O(m+1) | O(z+1) |
| RK | O(n+m+cm) for 1≤m≤n; c = equal-hash windows | O(n+m+1) | O(1) | O(z+1) |
| suffix | O(n log²(n+1)+1) doubling; O(n+1) Kasai | O(n+1) | O(n+1), including comparison-sort storage | O(n+1) |
| Aho | O(L+95S+p+T+q(S+p)) | O(L+T+p+q+1) | O(95S+p) | O(q(p+1)+1) |

RK takes O(n+1) for an empty pattern and O(1) for an oversized pattern after parsing. Its general worst-case bound is O(nm+n+m+1), not unconditional linear time.

## Own tests

`tests/student/check.py` uses seed 42 and independent small oracles: exhaustive subsequences, adjacent merge orders and vertex subsets, direct suffix sorting/LCP, and built-in prefix comparisons for search/count results. The public validator also checks reconstructed answers.

Actual results: all 44 public cases and the repeated 88-request public batch passed. All 850 own small/boundary requests repeated in one process passed (1700 results), as did 14 scale cases. Scale fixtures include 1000×1000 LCS, 200 blocks, 100000-vertex chains/stars, 200000-character KMP, 50000-character suffixes, and Aho cases at the pattern/text limits with dense matches and duplicates.

Representative inputs below are included in `check.py`. Expected and actual results agree:

| Command | Input summary | Expected = actual | Defect detected |
|---|---|---|---|
| LCS | `tea_time`, `team` | length 4, sequence `team` | Confusing subsequence with substring |
| merge | `[1,100,1]` | cost 203; plan `[[1,1,2],[0,0,2]]` | Merging non-adjacent small blocks; invalid chronology |
| tree | Seven-vertex chain, all weights 10⁹ | weight 4000000000, selected `[0,2,4,6]` | 32-bit overflow or adjacent selections |
| naive | `aaaaa`, `aaa` | positions `[0,1,2]` | Missing overlaps or the last start |
| KMP | `abc`, empty pattern | positions `[0,1,2,3]` | Accessing an empty pattern or losing the final boundary |
| RK | `abdj`, `ab` | positions `[0]` | Accepting the `ab`/`dj` hash collision |
| suffix | `banana` | sa `[5,3,1,0,4,2]`, lcp `[0,1,3,0,0,2]`, repeated `ana` | Wrong suffix order, LCP or overlapping repeat |
| Aho | patterns `[he,she,he,e]`, texts `[sheshe,sheshe,empty]` | counts `[[2,2,2,2],[2,2,2,2],[0,0,0,0]]` | Missing suffix patterns, duplicates or counter reset |

The fixtures also cover empty inputs, zero/tied optima, negative weights, case/punctuation, oversized patterns and repeated mixed requests.

## Search comparison

Reproduce with `tests/student/SearchBenchmark.java` and the commands above. Each size resets `java.util.Random(42)` and draws text characters uniformly from `abcd`; the pattern is the length-`m` substring starting at `(n-m)/2`, guaranteeing a match. Repetitive cases use `a^n` and `a^m`. Sizes and patterns are identical for all three methods. Before timing each case, every result is checked against `String.startsWith`; all checks passed.

HotSpot uses its default tiered JIT. Each method/case has 2000 untimed warm-up calls, followed by three samples of 100 calls. `System.nanoTime` measures the calls, including dispatch, result allocation and assignment to a volatile sink. Compilation, JVM startup, input generation, validation, JSON and printing are excluded. Values below are milliseconds per call: each sample's total elapsed time divided by 100; the median is the middle sample.

| Case | n | m | Method | Run 1 | Run 2 | Run 3 | Median |
|---|---:|---:|---|---:|---:|---:|---:|
| random | 500 | 50 | naive | 0.007060 | 0.007742 | 0.007210 | 0.007210 |
| random | 500 | 50 | kmp | 0.001000 | 0.001062 | 0.000969 | 0.001000 |
| random | 500 | 50 | rk | 0.003561 | 0.003548 | 0.003528 | 0.003548 |
| random | 1500 | 150 | naive | 0.002960 | 0.003007 | 0.003061 | 0.003007 |
| random | 1500 | 150 | kmp | 0.003090 | 0.003029 | 0.002874 | 0.003029 |
| random | 1500 | 150 | rk | 0.010599 | 0.010567 | 0.010572 | 0.010572 |
| random | 3000 | 300 | naive | 0.006469 | 0.006640 | 0.006649 | 0.006640 |
| random | 3000 | 300 | kmp | 0.005982 | 0.006123 | 0.006233 | 0.006123 |
| random | 3000 | 300 | rk | 0.020849 | 0.021077 | 0.021328 | 0.021077 |
| repetitive | 500 | 50 | naive | 0.013796 | 0.013746 | 0.013845 | 0.013796 |
| repetitive | 500 | 50 | kmp | 0.009250 | 0.006628 | 0.004865 | 0.006628 |
| repetitive | 500 | 50 | rk | 0.012918 | 0.012733 | 0.020876 | 0.012918 |
| repetitive | 1500 | 150 | naive | 0.107590 | 0.115439 | 0.119058 | 0.115439 |
| repetitive | 1500 | 150 | kmp | 0.009136 | 0.007936 | 0.007506 | 0.007936 |
| repetitive | 1500 | 150 | rk | 0.061128 | 0.061242 | 0.060814 | 0.061128 |
| repetitive | 3000 | 300 | naive | 0.443852 | 0.433914 | 0.460152 | 0.443852 |
| repetitive | 3000 | 300 | kmp | 0.014226 | 0.013859 | 0.013721 | 0.013859 |
| repetitive | 3000 | 300 | rk | 0.216331 | 0.217517 | 0.222464 | 0.217517 |

Random windows usually fail early; repetitive windows match fully, making naive and exact RK repeatedly compare `m` characters. KMP avoids those repeated comparisons and has the lowest repetitive-case medians. Modulus 101 permits many hash collisions, so RK must verify candidates; these results do not describe every production hashing implementation. Small-input timings are non-monotonic and may reflect JIT, GC and scheduling effects. Medians do not prove asymptotic bounds.

## Sources and AI assistance

Sources: the supplied `CONTRACTS.md`, `docs/Assignment_4_RU.md` and `TESTING.md`; the supplied JSON transport and public validator. OpenAI ChatGPT assisted with the own tests, benchmark and this analysis, and executed the checks and measurements reported here
