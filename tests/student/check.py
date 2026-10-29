import functools
import json
import pathlib
import random
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
from public_validate import check
from run import build, execute


def positions(text, pattern):
    return [i for i in range(len(text) + 1) if text.startswith(pattern, i)]


def lcs_length(a, b):
    if len(a) > len(b):
        a, b = b, a
    best = 0
    for mask in range(1 << len(a)):
        sequence = [a[i] for i in range(len(a)) if mask & (1 << i)]
        remaining = iter(b)
        if len(sequence) > best and all(c in remaining for c in sequence):
            best = len(sequence)
    return best


@functools.lru_cache(None)
def merge_cost(sizes):
    if len(sizes) < 2:
        return 0
    return min(sizes[i] + sizes[i + 1]
               + merge_cost(sizes[:i] + (sizes[i] + sizes[i + 1],) + sizes[i + 2:])
               for i in range(len(sizes) - 1))


def tree_weight(weights, edges):
    return max(sum(w for i, w in enumerate(weights) if mask & (1 << i))
               for mask in range(1 << len(weights))
               if all(not (mask & (1 << u) and mask & (1 << v)) for u, v in edges))


def suffix_result(text):
    sa = sorted(range(len(text)), key=lambda i: text[i:])
    lcp = [0] * len(text)
    repeated = ""
    for r in range(1, len(sa)):
        i, j = sa[r - 1], sa[r]
        common = 0
        while i + common < len(text) and j + common < len(text) and text[i + common] == text[j + common]:
            common += 1
        lcp[r] = common
        if common > len(repeated):
            repeated = text[i:i + common]
    return {"sa": sa, "lcp": lcp, "repeated": repeated}


def expected(request):
    task = request["task"]
    if task == "lcs":
        return {"length": lcs_length(request["a"], request["b"])}
    if task == "merge":
        return {"cost": merge_cost(tuple(request["sizes"]))}
    if task == "tree":
        return {"weight": tree_weight(request["weights"], request["edges"])}
    if task in ("naive", "kmp", "rk"):
        return {"positions": positions(request["text"], request["pattern"])}
    if task == "suffix":
        return suffix_result(request["text"])
    return {"counts": [[len(positions(text, pattern)) for pattern in request["patterns"]]
                       for text in request["texts"]]}


def small_cases():
    rng = random.Random(42)
    alphabet = 'abAB !~\\"09'

    def word(limit):
        return "".join(rng.choice(alphabet) for _ in range(rng.randrange(limit + 1)))

    cases = []
    for _ in range(100):
        cases.append({"task": "lcs", "a": word(8), "b": word(8)})
        cases.append({"task": "merge", "sizes": [rng.randrange(6) for _ in range(rng.randrange(7))]})
        n = rng.randrange(12)
        labels = list(range(n))
        rng.shuffle(labels)
        edges = [[labels[i], labels[rng.randrange(i)]] for i in range(1, n)]
        rng.shuffle(edges)
        cases.append({"task": "tree", "n": n, "weights": [rng.randrange(-10, 11) for _ in range(n)], "edges": edges})
        text, pattern = word(40), word(8)
        if text and rng.randrange(2):
            start = rng.randrange(len(text))
            pattern = text[start:start + rng.randrange(9)]
        for task in ("naive", "kmp", "rk"):
            cases.append({"task": task, "text": text, "pattern": pattern})
        cases.append({"task": "suffix", "text": word(40)})
        patterns = [word(5) or "a" for _ in range(rng.randrange(12))]
        if patterns:
            patterns += [patterns[0], patterns[0][-1:]]
        cases.append({"task": "aho", "patterns": patterns, "texts": [word(40) for _ in range(rng.randrange(5))]})

    cases += [{"task": "lcs", "a": a, "b": b}
              for a, b in [("", ""), ("tea_time", "team"), ("ABC", "BAC"), ("aaaa", "aa")]]
    cases += [{"task": "merge", "sizes": sizes}
              for sizes in [[], [7], [0, 0, 0], [1, 100, 1], [10**9] * 4]]
    cases += [{"task": "tree", "n": len(weights), "weights": weights,
               "edges": [[i - 1, i] for i in range(1, len(weights))]}
              for weights in [[], [-5], [-5, -1, -9], [0, 0], [4, 9, 6], [10**9] * 7]]
    for task in ("naive", "kmp", "rk"):
        for text, pattern in [("", ""), ("abc", ""), ("", "a"), ("ab", "abc"),
                              ("aaaaa", "aaa"), ("abdj", "ab"), ("~ !~ !", "~ !"),
                              ("a" * 80 + "b", "a" * 20 + "b")]:
            cases.append({"task": task, "text": text, "pattern": pattern})
    cases += [{"task": "suffix", "text": text}
              for text in ["", "a", "aaaaaa", "banana", "abcdef", " !~ !~", "abababa"]]
    cases += [{"task": "aho", "patterns": patterns, "texts": texts}
              for patterns, texts in [([], ["", "abc"]), (["a"], []),
                                       (["he", "she", "he", "e"], ["sheshe", "sheshe", ""]),
                                       (["a", "aa", "aaa", "a"], ["aaaaa", "", "aaaaa"])]]
    return [{"input": request, "output": expected(request)} for request in cases]


def verify(command, cases, label):
    answers = execute(command, [case["input"] for case in cases])
    for case, answer in zip(cases, answers):
        if not check(case["input"], answer, case["output"]):
            raise SystemExit(f"FAIL {label}: {json.dumps(case['input'])[:300]} -> {str(answer)[:300]}")
    print(f"PASS {label}: {len(cases)} results")


def scale_cases():
    n = 100_000
    yield {"task": "lcs", "a": "a" * 1000, "b": "a" * 1000}, {"length": 1000}
    yield {"task": "merge", "sizes": [10**9] * 200}, {"cost": 1544 * 10**9}
    yield {"task": "tree", "n": n, "weights": [10**9] * n,
           "edges": [[i - 1, i] for i in range(1, n)]}, {"weight": n // 2 * 10**9}
    yield {"task": "tree", "n": n, "weights": [10**9] * n,
           "edges": [[0, i] for i in range(1, n)]}, {"weight": (n - 1) * 10**9}
    for task, length, pattern_length in [("naive", 3000, 1500), ("kmp", 200_000, 100_000), ("rk", 10_000, 2000)]:
        yield {"task": task, "text": "a" * length, "pattern": "a" * pattern_length}, {"positions": list(range(length - pattern_length + 1))}
    yield {"task": "kmp", "text": "a" * 200_000, "pattern": ""}, {"positions": list(range(200_001))}
    yield {"task": "kmp", "text": "a" * 200_000, "pattern": "a" * 99_999 + "b"}, {"positions": []}
    yield {"task": "suffix", "text": "a" * 50_000}, {"sa": list(range(49_999, -1, -1)), "lcp": list(range(50_000)), "repeated": "a" * 49_999}
    yield {"task": "suffix", "text": "ab" * 25_000}, {
        "sa": list(range(49_998, -1, -2)) + list(range(49_999, 0, -2)),
        "lcp": list(range(0, 50_000, 2)) + [0] + list(range(1, 49_999, 2)), "repeated": "ab" * 24_999}
    yield {"task": "aho", "patterns": ["a" * 100_000], "texts": ["a" * 200_000]}, {"counts": [[100_001]]}
    yield {"task": "aho", "patterns": ["a" * 10] * 10_000, "texts": ["a" * 2000] * 100}, {"counts": [[1991] * 10_000] * 100}
    yield {"task": "aho", "patterns": ["a" * i for i in range(1, 447)], "texts": ["a" * 200_000]}, {"counts": [[200_001 - i for i in range(1, 447)]]}


if __name__ == "__main__":
    command = build("Java")
    cases = small_cases()
    verify(command, cases + cases, "small oracles and repeated mixed requests (seed 42)")
    for i, (request, output) in enumerate(scale_cases(), 1):
        verify(command, [{"input": request, "output": output}], f"scale {i}: {request['task']}")
