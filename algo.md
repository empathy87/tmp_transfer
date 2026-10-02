# Algorithms & Data Structures — Interview Reference (Pseudocode)

The language-agnostic master for the LeetCode-style coding round: every pattern, data
structure, approach, and trick, in memorizable pseudocode. **Concepts live here; the
runnable implementations live in the language mirrors.**

> **Companion files:** `algo-java.md` (the same patterns in idiomatic Java 25 +
> `java.util.*`) · `algo-python.md` (the same patterns in Python 3.13 + `heapq` /
> `bisect` / `collections` / `sortedcontainers`). Streams/functional pipelines →
> `micro-java.md`. Concurrency is out of scope (single-threaded round) → `concurrent-java.md`.

**How to use this file.** §0 routes a symptom to a technique. §1 turns the input size
into a target complexity *before* you write code. §2–§3 are the conventions and the
naming discipline the rest of the file uses. §4–§17 are the pattern catalog. §18 is the
data structures you must **code from memory** (not in any standard library); §19 is the
ones you only ever **call** (know the API, never reimplement). §20 is the senior
trick bag; §21–§22 are the night-before cheat sheet and the write-blind drill; §23 is how to
trace & explain on a shared editor; §24 is a frequency-ranked, free-only company problem bank.

---

## 0. The 30-Second Decision Table

Read the problem, find the row, jump to the section. The single highest-leverage habit:
**read the constraints first (§1)** — the input size usually names the algorithm before
you've understood the problem.

| When you see / need… | Reach for | §
|---|---|---|
| Sorted array → find a pair/triplet to a target | Two pointers (opposite ends) | §4 |
| Sorted array → find element / boundary / insertion point | Binary search | §5 |
| "Minimize the max", "max feasible X", answer is monotonic | Binary search **on the answer** | §5 |
| Contiguous subarray/substring satisfying a constraint | Sliding window | §4 |
| Subarray sum (or count) equals k | Prefix sum + hash map | §4, §7 |
| Many range-sum queries, array immutable | Prefix sums | §4 |
| Range queries **with updates** (sum/min/max) | Fenwick / segment tree | §18 |
| Need complement / seen-before / dedup / frequency | Hash map or set | §7 |
| Element occurring > n/2 (or > n/3) times | Boyer–Moore majority vote | §7 |
| XOR / single number / count bits / enumerate subsets of ≤20 items | Bit manipulation | §7 |
| Next greater/smaller element; histogram rectangle | Monotonic stack | §8 |
| Sliding-window maximum / minimum | Monotonic deque | §8 |
| Matching brackets, nesting, expression evaluation | Stack | §8 |
| Reverse / detect cycle / merge a linked list | Pointer surgery, fast & slow pointers | §9 |
| Aggregate a value up from a tree's children | DFS recursion ("return info up") | §10 |
| Tree/graph level order, or shortest path on an **unweighted** graph | BFS | §10, §15 |
| Top-k or kth largest/smallest | Heap (size-k) or quickselect | §11, §12 |
| Running median of a stream | Two heaps | §11 |
| Merge k sorted lists/streams | Min-heap of heads | §11 |
| All subsets / permutations / combinations / fill a board | Backtracking | §12 |
| Count ways / min cost / reachable, with overlapping subproblems | Dynamic programming | §14 |
| Align two strings/sequences (LCS, edit distance) | 2D DP | §14 |
| Pick a subset to hit a target/capacity | Knapsack DP | §14 |
| Optimum over an interval `[i,j]` built from sub-intervals | Interval DP | §14 |
| N ≤ ~20 and the state is "which subset is done" | Bitmask DP | §14 |
| Shortest path, **non-negative** weights | Dijkstra (min-heap) | §15 |
| Shortest path, **negative** edges / detect negative cycle | Bellman–Ford | §15 |
| Shortest path between **all pairs** | Floyd–Warshall | §15 |
| Ordering with prerequisites / cycle in a directed graph | Topological sort | §15 |
| "Are these connected?", dynamic union, MST | Union-Find (DSU) | §18, §15 |
| 2-colorable / bipartite check | BFS/DFS coloring | §15 |
| A locally-optimal choice is provably globally optimal | Greedy (prove it!) | §16 |
| Overlapping intervals: merge / max concurrent / min rooms | Sort + sweep line, or heap | §6 |
| Substring pattern matching, faster than O(nm) | KMP / Rabin–Karp / Z | §17 |
| Longest palindromic substring | Expand-around-center / Manacher | §17 |
| Prefix lookup / autocomplete / word dictionary | Trie (prefix tree) | §18 |
| Range min/max on **immutable** data, O(1) query | Sparse table | §18 |
| O(1) get/put with eviction | LRU / LFU cache (hash map + DLL) | §18 |
| GCD / primes / modular arithmetic / fast power / nCr | Number theory toolkit | §13 |
| Subset problem with n ≤ ~40 (2^n too big, 2^(n/2) ok) | Meet in the middle | §20 |
| "What complexity am I even allowed?" | Constraints → Big-O table | §1 |

> **Say this in the room:** "Before I code, let me look at the constraints to fix my
> target complexity, then pick the pattern." It signals you optimize on purpose, not by luck.

## 1. Reading the constraints (target complexity)

The constraints leak the algorithm. Read `n ≤ …` **before** you understand the problem
and you usually know the intended complexity — which prunes the pattern list in §0 to a
handful. Do this out loud in the interview; it shows intent.

### Input size → the complexity you're allowed

Assume ~10⁸ simple operations per second as the rough ceiling for a 1–2 s limit.

| n (largest input) | Target complexity | Implies (typical) |
|---|---|---|
| n ≤ 10–12 | O(n!) | brute permutations, Held–Karp TSP (§14) |
| n ≤ 18–22 | O(2ⁿ · n) | bitmask DP, subset enumeration (§7, §14) |
| n ≤ 40 | O(2^(n/2)) | meet in the middle (§20) |
| n ≤ 100 | O(n³), O(n⁴) | Floyd–Warshall, interval DP, MCM (§14, §15) |
| n ≤ 500–1,000 | O(n²) | 2D DP, pairwise loops, Bellman–Ford (§14, §15) |
| n ≤ 10⁵ | O(n log n) | sort, heap, binary search, balanced-tree ops |
| n ≤ 10⁶ | O(n) or O(n log n) | one pass, two pointers, prefix sums, counting |
| n ≤ 10⁷–10⁸ | O(n), tiny constant | linear scan, no per-element allocation |
| n up to 10⁹–10¹⁸ | O(log n) or O(√n) | binary search on answer, number theory, matrix power |
| "huge" / streaming | O(1) extra space | two pointers, reservoir sampling, Boyer–Moore |

### Reading other tells

| Constraint clue | Likely technique |
|---|---|
| Array is **sorted** | binary search (§5) or two pointers (§4) |
| Values bounded (e.g. `0 ≤ a[i] ≤ 100`) | counting/bucket sort, frequency array (§7) |
| Asks for **count of ways** or **min/max cost** | DP (§14) or greedy (§16) |
| Asks for **all** solutions (subsets/perms) | backtracking (§12) — output size *is* exponential |
| "k-th smallest/largest", "top k" | heap or quickselect (§11, §12) |
| Answer is **yes/no feasible** and monotone in a parameter | binary search on the answer (§5) |
| Mentions **modulo 1e9+7** | counting/DP with modular arithmetic (§13) |
| Online / streaming / can't store all input | one-pass + O(1)–O(k) memory (§7, §11, §20) |
| Graph with `V, E` given | think adjacency list + BFS/DFS/Dijkstra (§15) |

### Complexity reminders

- **Amortized** ≠ per-op: a monotonic stack is O(n) total because each element is
  pushed and popped at most once, even though one step may pop many (§8, §20).
- **Space counts too.** "O(1) extra space" forbids the hash map; reach for in-place
  marking, two pointers, or pointer reversal (§4, §9).
- **Recursion = stack space** O(depth). Deep recursion on n = 10⁵ can stack-overflow —
  convert to iterative or increase the limit (§10, §12).
- **`log` is base 2 and tiny:** log₂(10⁹) ≈ 30, log₂(10¹⁸) ≈ 60. An O(n log n) at
  n = 10⁶ is ~2·10⁷ ops — comfortable.

> **Say this in the room:** "n is 10⁵, so I'm targeting O(n log n); that rules out the
> O(n²) DP and points me at sorting or a heap." You've now justified the approach before
> writing a line.

## 2. Pseudocode conventions

Every code block in this file is **pseudocode**, not a real language — close enough to
transcribe into Java (`algo-java.md`) or Python (`algo-python.md`) in seconds. The
conventions, once:

- **Functions:** `function name(params):` with indentation for blocks (Python-like). A
  recursion helper is usually `function dfs(node):` or `function backtrack(i, path):`.
- **Assignment vs comparison:** `=` assigns, `==` compares equal, `!=` is not-equal.
- **Loops:** `for i in 0..n-1:` runs `i = 0,1,…,n-1` (**both ends inclusive**).
  `for each x in items:` iterates values. `while cond:` as usual. `break` / `continue`.
- **Conditionals:** `if / elif / else`. Inline form: `a if cond else b`.
- **Comments:** `// like this`.
- **Sentinels:** `true`, `false`, `null`, `INF`, `-INF`.
- **Arrays are 0-indexed:** `A[i]`, length `len(A)`; an inclusive slice is `A[i..j]`.

**Assumed built-ins** (these are the standard-library structures of §19 — used freely,
never reimplemented except in §18):

| Form | Operations |
|---|---|
| hash map `map = {}` | `map[k]`, `k in map`, `map.get(k, default)`, `map.remove(k)`, `for k, v in map:` |
| hash set `seen = {}` | `seen.add(x)`, `x in seen`, `seen.remove(x)` |
| stack `st = []` | `st.push(x)`, `st.pop()`, `st.top()`, `st.empty()` |
| queue `q` | `q.push(x)`, `q.pop()` (FIFO front), `q.front()`, `q.empty()` |
| deque `dq` | `dq.pushFront/pushBack/popFront/popBack/front/back` |
| **min-heap** `h` (default) | `h.push(x)`, `h.pop()` (removes min), `h.top()`, `len(h)` |
| dynamic array `A` | `A.append(x)`, `A.pop()`, `len(A)`, `A.sort()`, `sort(A)`, `A.sort(key = fn)` |

- **Max-heap:** there isn't one by default — push negated keys, or write "max-heap" and
  a comparator. (In real code: Java `PriorityQueue` with a reverse comparator; Python
  `heapq` with negated values.)
- **Tuples:** `(a, b)`; destructure with `x, y = pair`.
- **Math:** `min`, `max`, `abs`, `floor`, `ceil`, `gcd`, plus `INF` / `-INF`.
- **`swap(a, b)`** exchanges two values/slots.

Pseudocode hides language traps you must restore when transcribing: **integer overflow**
(Java `long` vs Python bigint), **signed shifts**, and **`mid = lo + (hi - lo) / 2`** to
avoid overflow on `(lo + hi)`.

## 3. Naming functions & local variables

Naming is a seniority signal the interviewer reads in real time. Single-letter soup
(`a`, `b`, `t`, `tmp`, `arr2`) says junior; intent-revealing names say "I do this for a
living." The rule: **a name should say what the thing *means*, not what *type* it is.**
You lose zero speed — you type the name once and read it ten times.

### Functions — name them after what they return or decide

| Function computes… | Good name | Avoid |
|---|---|---|
| a boolean | `isValid`, `canPartition`, `hasCycle`, `canReach`, `exists` | `check`, `test`, `flag` |
| a count | `countPaths`, `numIslands`, `countSmaller` | `solve`, `cnt` (as the fn name) |
| a search result | `findFirstBadVersion`, `lowerBound`, `searchInsert` | `find2`, `bs` |
| a transform | `reverseBetween`, `mergeSorted`, `compress` | `doIt`, `process` |
| a recursive worker | `dfs`, `bfs`, `backtrack`, `solve(i, remaining)` | `helper`, `recurse`, `go` (only if truly generic) |

- **Booleans read as yes/no questions:** prefix `is` / `has` / `can` / `should`
  (`isLeaf`, `hasNext`, `canJump`). The `if` then reads like English: `if canReach(end):`.
- **`dfs` / `bfs` / `backtrack` are *good* names** for the recursive engine — they're
  idioms every interviewer parses instantly. Make the *parameters* meaningful:
  `dfs(node, depth)`, `backtrack(start, path, remaining)`, not `dfs(a, b, c)`.
- Name a helper by its job, not "helper": `expandAroundCenter(left, right)`,
  `relax(u, v, weight)`, `pushChildren(node)`.

### Local variables — conventional names that interviewers expect

These idioms are *so* standard that the right name doubles as documentation:

| Role | Conventional name(s) |
|---|---|
| two-pointer bounds | `left` / `right`, or `lo` / `hi` / `mid` for binary search |
| fast/slow (cycle, middle) | `slow` / `fast` |
| in-place two-pointer (overwrite) | `read` / `write` (or `slow` / `fast`) |
| loop indices (tight numeric loops) | `i`, `j`, `k` — acceptable here, and only here |
| current node / previous node | `node` / `cur` / `prev`, `prev` / `cur` / `next` for list reversal |
| visited / seen membership | `seen`, `visited` (a set) |
| frequency / counts | `freq`, `count`, `counts`, `degree`, `indegree` |
| memo / DP table | `memo` (top-down), `dp` (bottom-up), `cache` |
| graph adjacency | `adj`, `graph`, `neighbors` |
| accumulated answer | `result`, `ans`, `best`, `total` |
| a running window aggregate | `windowSum`, `windowStart`, `need` / `have` |
| sentinel list node | `dummy` (the dummy head) |
| running min/max | `minSoFar`, `maxSoFar`, `bestSoFar` |
| constants | `MOD = 1e9 + 7`, `INF`, `DIRS = [(0,1),(1,0),(0,-1),(-1,0)]` |

### Make the DP/recurrence self-documenting

The most valuable naming you do in the whole round is **stating what `dp[i]` means** —
in a comment and out loud — before you fill it (§14):

```
// dp[i] = length of the longest increasing subsequence ENDING at index i
// dp[i][j] = min edit distance between a[0..i-1] and b[0..j-1]
// dp[mask] = min cost to have visited exactly the cities in `mask`
```

If you can't name `dp[i]` in one English sentence, you don't yet have the state — that's
the bug, not the code.

### Anti-patterns to drop

- `temp`, `tmp`, `data`, `val2`, `arr2`, `res2` → name the second thing
  (`reversedHalf`, `nextNode`, `oddHead`).
- `flag` / `done` / `ok` → say what's true: `foundTarget`, `isSorted`.
- Reusing `i` for two different meanings in nested loops → use `row`/`col`, `i`/`j` with
  a comment, or `outer`/`inner`.
- Cryptic math single-letters in business logic → fine for `gcd(a, b)` or coordinates
  `(x, y)`; not fine for `n2`, `t`, `q2` standing in for real concepts.

> **Say this in the room:** narrate the name as you type — "I'll call this `seen` because
> it's the set of visited states, and `dp[i]` is the best score ending at `i`." You're
> handing the interviewer your mental model for free, and it makes off-by-one bugs visible
> to *both* of you.

## 4. Arrays & strings

The bread-and-butter of the coding round: linear-scan tricks that turn an O(n²) brute force into O(n) by maintaining an invariant as you walk one or two indices across the array.

| Symptom | Reach for |
|---|---|
| Sorted array, find a pair/triple meeting a condition | Two pointers — opposite ends |
| Best/longest/count of contiguous subarray meeting a window condition | Sliding window |
| Compact / dedup / partition an array in place | Two pointers — same direction (read/write) |
| Answer for many range queries `sum(i..j)`, or "subarray sums to k" | Prefix sum (+ hashmap) |
| Many range *updates* `add v to [l..r]`, then read once | Difference array |
| Max/min subarray sum or product | Kadane |
| Sort/partition into ≤3 buckets in one pass | Dutch national flag |
| Values are a permutation/range `[1..n]`, find missing/dup | Cyclic sort or sign-marking |

> **Say this in the room:** "Prefix-sum when the window boundary isn't monotonic — I need arbitrary `sum(i..j)` or to count subarrays by sum, and I lean on a hashmap of prefix→count. Sliding window when the window is contiguous *and* the validity check is monotone, so I can grow the right edge and shrink the left edge without ever backing up. If shrinking the left can never repair validity (e.g., subarray sum = k with negatives), the window breaks and I fall back to prefix-sum + hashmap."

**Overflow note:** running sums (`prefix`, window sum, Kadane accumulator) overflow 32-bit ints fast — `n` up to 10⁵ with values up to 10⁵ is 10¹⁰, well past `int`'s ~2.1·10⁹. Use 64-bit. Prefix XOR never overflows.

### Two pointers — opposite ends
**LeetCode:** [Valid Palindrome](https://leetcode.com/problems/valid-palindrome/) · [Two Sum II - Input Array Is Sorted](https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/) · [3Sum](https://leetcode.com/problems/3sum/) · [Container With Most Water](https://leetcode.com/problems/container-with-most-water/) · [Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/)
**When:** the array is **sorted** (or symmetric) and you want a pair/area defined by a left and a right index that converge.
Move the pointer that can only improve the objective; the discarded half is provably dominated, so each step eliminates a candidate in O(1).

```
// Pair sum == target in a sorted array
function pairSum(A, target):
    lo = 0; hi = len(A) - 1
    while lo < hi:
        s = A[lo] + A[hi]
        if s == target: return (lo, hi)
        elif s < target: lo = lo + 1   // need bigger → raise floor
        else: hi = hi - 1              // need smaller → lower ceiling
    return null
```

```
// Reverse in place
function reverse(A):
    lo = 0; hi = len(A) - 1
    while lo < hi:
        swap(A[lo], A[hi]); lo = lo + 1; hi = hi - 1
```

```
// Valid palindrome (skip non-alphanumeric, case-insensitive)
function isPalindrome(s):
    lo = 0; hi = len(s) - 1
    while lo < hi:
        while lo < hi and not isAlnum(s[lo]): lo = lo + 1
        while lo < hi and not isAlnum(s[hi]): hi = hi - 1
        if lower(s[lo]) != lower(s[hi]): return false
        lo = lo + 1; hi = hi - 1
    return true
```

```
// Container with most water — area limited by the shorter wall
function maxArea(H):
    lo = 0; hi = len(H) - 1; best = 0
    while lo < hi:
        best = max(best, (hi - lo) * min(H[lo], H[hi]))
        if H[lo] < H[hi]: lo = lo + 1   // move the shorter wall: only it can grow area
        else: hi = hi - 1
    return best
```

```
// 3-sum == 0: sort, fix one, two-pointer the rest, skip duplicates
function threeSum(A):
    sort(A); res = []
    for i in 0..len(A)-1:
        if i > 0 and A[i] == A[i-1]: continue        // skip dup anchor
        lo = i + 1; hi = len(A) - 1
        while lo < hi:
            s = A[i] + A[lo] + A[hi]
            if s < 0: lo = lo + 1
            elif s > 0: hi = hi - 1
            else:
                res.append((A[i], A[lo], A[hi]))
                lo = lo + 1; hi = hi - 1
                while lo < hi and A[lo] == A[lo-1]: lo = lo + 1   // skip dup
                while lo < hi and A[hi] == A[hi+1]: hi = hi - 1
    return res
```

```
// Trapping rain water — two-pointer O(1) space.
// Water above i = min(maxLeft, maxRight) - H[i]. Advance the side
// whose running max is the SMALLER, because that side bounds the water.
function trap(H):
    lo = 0; hi = len(H) - 1
    leftMax = 0; rightMax = 0; water = 0
    while lo < hi:
        if H[lo] < H[hi]:
            leftMax = max(leftMax, H[lo])
            water = water + (leftMax - H[lo])   // left wall is the limiter here
            lo = lo + 1
        else:
            rightMax = max(rightMax, H[hi])
            water = water + (rightMax - H[hi])
            hi = hi - 1
    return water
```

**Complexity:** time O(n) (O(n log n) for 3-sum due to the sort) / space O(1).
**Traps:** • requires sorted input for pair/3-sum — sorting changes indices, so return values not original positions unless you tracked them. • Off-by-one: loop is `while lo < hi`, never `<=`, or you compare an element with itself. • 3-sum: skip duplicates at the anchor **and** after a hit on both pointers, or you emit dup triples. • Container/trapping: always move the pointer at the **shorter** wall — moving the taller one can never increase the result.

### Two pointers — same direction (read/write)
**LeetCode:** [Remove Element](https://leetcode.com/problems/remove-element/) · [Remove Duplicates from Sorted Array](https://leetcode.com/problems/remove-duplicates-from-sorted-array/) · [Move Zeroes](https://leetcode.com/problems/move-zeroes/) · [Sort Colors](https://leetcode.com/problems/sort-colors/)
**When:** compact, dedup, or partition an array **in place** — a `write` pointer marks the next slot to fill, a `read` pointer scans.
The prefix `A[0..write-1]` is always the finished result; `read` runs ahead deciding what survives.

```
// Remove duplicates from a SORTED array; return new length
function dedupSorted(A):
    if len(A) == 0: return 0
    write = 1
    for read in 1..len(A)-1:
        if A[read] != A[write-1]:
            A[write] = A[read]; write = write + 1
    return write   // A[0..write-1] is the unique prefix
```

```
// Move zeroes to the end, keep order of non-zeroes
function moveZeroes(A):
    write = 0
    for read in 0..len(A)-1:
        if A[read] != 0:
            swap(A[write], A[read]); write = write + 1
```

```
// Remove all occurrences of val; return new length
function removeElement(A, val):
    write = 0
    for read in 0..len(A)-1:
        if A[read] != val:
            A[write] = A[read]; write = write + 1
    return write
```

```
// Stable partition by predicate: pred-true elements first
function partition(A, pred):
    write = 0
    for read in 0..len(A)-1:
        if pred(A[read]):
            swap(A[write], A[read]); write = write + 1
    return write   // [0..write-1] satisfy pred
```

**Complexity:** time O(n) / space O(1).
**Traps:** • `write` lags `read`; never read from an index you've already overwritten. • Dedup needs **sorted** input — for unsorted use a hash set (see §7). • Order preservation: `swap` keeps relative order of survivors when scanning left→right; plain overwrite also works for moveZeroes only if you then zero-fill `A[write..]`. • Return the *length* `write`, not the array — the tail past `write` is garbage.

### Fast/slow in array (value-as-pointer)
**LeetCode:** [Linked List Cycle](https://leetcode.com/problems/linked-list-cycle/) · [Find the Duplicate Number](https://leetcode.com/problems/find-the-duplicate-number/)
**When:** array of `n+1` values in `[1..n]`, exactly one duplicate, must use O(1) space and not modify the array.
Treat `i → A[i]` as a linked list; the duplicate forces a cycle, so Floyd's tortoise-and-hare finds the cycle entrance (= the duplicate). Full cycle-detection mechanics in §9.

```
// LeetCode 287 — find the duplicate without modifying or extra space
function findDuplicate(A):
    slow = A[0]; fast = A[0]
    repeat:
        slow = A[slow]; fast = A[A[fast]]
    until slow == fast
    slow = A[0]                      // reset one pointer to the start
    while slow != fast:
        slow = A[slow]; fast = A[fast]
    return slow                      // entrance = duplicate value
```

**Complexity:** time O(n) / space O(1). **Traps:** • indices must be valid pointers — values in `[1..n]`, array length `n+1`. • Distinct from sign-marking (below), which is O(n) too but mutates the array.

### Sliding window — fixed size
**LeetCode:** [Maximum Average Subarray I](https://leetcode.com/problems/maximum-average-subarray-i/) · [Find All Anagrams in a String](https://leetcode.com/problems/find-all-anagrams-in-a-string/) · [Permutation in String](https://leetcode.com/problems/permutation-in-string/)
**When:** "subarray/substring of **exactly k**" — max/min sum, average, or count.
Add the entering element, drop the leaving element; the window of width `k` slides in O(1) per step.

```
// Max sum of any contiguous subarray of size k
function maxSumK(A, k):
    s = 0
    for i in 0..k-1: s = s + A[i]         // first window
    best = s
    for i in k..len(A)-1:
        s = s + A[i] - A[i-k]             // slide: + new, - old
        best = max(best, s)
    return best
```
Average of each size-k window is just `s / k` per step. **Complexity:** time O(n) / space O(1). **Traps:** • guard `len(A) >= k`. • Use 64-bit for the running sum. • Don't recompute the window from scratch — that's the O(n·k) mistake.

### Sliding window — variable size (grow-right / shrink-left)
**LeetCode:** [Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/) · [Longest Repeating Character Replacement](https://leetcode.com/problems/longest-repeating-character-replacement/) · [Fruit Into Baskets](https://leetcode.com/problems/fruit-into-baskets/) · [Subarrays with K Different Integers](https://leetcode.com/problems/subarrays-with-k-different-integers/) · [Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/)
**When:** "longest/shortest/count of contiguous subarray such that <condition>" where the condition is **monotone** — extending the window can only break validity, shrinking can only restore it.
ONE template: expand `right` every step; while the window is invalid, advance `left` to restore the invariant; record the answer when valid. Memorize this:

```
// Canonical variable-size window
function variableWindow(A):
    left = 0
    init window state (counts/sum/freq map)
    best = 0  // or INF for "shortest"
    for right in 0..len(A)-1:
        add A[right] to window state
        while window is INVALID:          // condition violated
            remove A[left] from window state
            left = left + 1
        // here [left..right] is the largest valid window ending at right
        best = max(best, right - left + 1)
    return best
```
For **"shortest valid"** flip it: shrink while the window *stays* valid, recording `right-left+1` each shrink, then stop when it just becomes invalid.

```
// Longest substring without repeating chars
function longestUnique(s):
    last = {}            // char -> last index seen
    left = 0; best = 0
    for right in 0..len(s)-1:
        c = s[right]
        if c in last and last[c] >= left:
            left = last[c] + 1            // jump left past the previous c
        last[c] = right
        best = max(best, right - left + 1)
    return best
```

```
// Minimum window substring: smallest window of s containing all of t
function minWindow(s, t):
    need = {}; for each c in t: need[c] = need.get(c, 0) + 1
    required = len(need)   // distinct chars still to satisfy
    have = 0
    window = {}
    left = 0; bestLen = INF; bestL = 0
    for right in 0..len(s)-1:
        c = s[right]; window[c] = window.get(c, 0) + 1
        if c in need and window[c] == need[c]: have = have + 1
        while have == required:                       // valid → try to shrink
            if right - left + 1 < bestLen:
                bestLen = right - left + 1; bestL = left
            d = s[left]; window[d] = window[d] - 1
            if d in need and window[d] < need[d]: have = have - 1
            left = left + 1
    return "" if bestLen == INF else s[bestL .. bestL + bestLen - 1]   // inclusive
```

```
// Longest substring with at most k distinct chars
function atMostKDistinct(s, k):
    freq = {}; left = 0; best = 0
    for right in 0..len(s)-1:
        freq[s[right]] = freq.get(s[right], 0) + 1
        while len(freq) > k:                          // too many distinct → shrink
            d = s[left]; freq[d] = freq[d] - 1
            if freq[d] == 0: freq.remove(d)
            left = left + 1
        best = max(best, right - left + 1)
    return best
```

```
// Longest repeating char replacement: ≤ k changes to make all-same.
// Window valid while (windowLen - countOfMostFrequentChar) <= k.
function characterReplacement(s, k):
    freq = {}; left = 0; best = 0; maxFreq = 0
    for right in 0..len(s)-1:
        freq[s[right]] = freq.get(s[right], 0) + 1
        maxFreq = max(maxFreq, freq[s[right]])
        if (right - left + 1) - maxFreq > k:          // need too many replacements
            freq[s[left]] = freq[s[left]] - 1; left = left + 1
        best = max(best, right - left + 1)
    return best
```
**Fruit into baskets** = "at most 2 distinct" → the `atMostKDistinct` template with `k = 2`. **"Subarrays with exactly k distinct"** = `atMost(k) - atMost(k-1)`.

**Complexity:** time O(n) (each index enters and leaves the window once) / space O(alphabet or window size).
**Traps:** • `left` only ever moves right — never reset it to 0 inside the loop. • In `longestUnique`, guard `last[c] >= left` or a stale earlier index drags `left` backward. • `characterReplacement` deliberately never decrements `maxFreq` when shrinking; it can be stale-high, but `best` is still correct because the window only shrinks by one and only grows when a *new* max is found — don't "fix" it. • "Exactly k" is not a single window — use the at-most-difference trick. • Negatives break the monotonicity assumption — for "subarray sum = k" with negatives, do **not** use a window; use prefix-sum + hashmap (next).

### Prefix sums — 1D
**LeetCode:** [Range Sum Query - Immutable](https://leetcode.com/problems/range-sum-query-immutable/) · [Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/) · [Contiguous Array](https://leetcode.com/problems/contiguous-array/)
**When:** many `sum(i..j)` queries on an immutable array, or counting subarrays by their sum.
`P[0]=0`, `P[i]=A[0]+…+A[i-1]`; then `sum(i..j) inclusive = P[j+1] - P[i]`. For "subarray sums to k," store seen prefix sums in a hashmap and look for `prefix - k` (see §7 for the hashing pattern).

```
// Immutable range-sum query
function buildPrefix(A):
    P = array of len(A)+1 zeros
    for i in 0..len(A)-1: P[i+1] = P[i] + A[i]
    return P
// rangeSum(i, j) inclusive = P[j+1] - P[i]
```

```
// Count subarrays whose sum == k  (works with negatives)
function subarraySumEqualsK(A, k):
    count = {0: 1}        // prefix value -> how many times seen; empty prefix = 1
    prefix = 0; ans = 0
    for x in A:
        prefix = prefix + x
        ans = ans + count.get(prefix - k, 0)   // a prior prefix making sum k
        count[prefix] = count.get(prefix, 0) + 1
    return ans
```

```
// Prefix XOR: count subarrays with XOR == k
function subarrayXorEqualsK(A, k):
    count = {0: 1}; pre = 0; ans = 0
    for x in A:
        pre = pre XOR x
        ans = ans + count.get(pre XOR k, 0)    // pre ^ target == prior  ⇒  prior == pre ^ k
        count[pre] = count.get(pre, 0) + 1
    return ans
```

**Complexity:** build O(n), query O(1); counting O(n) / space O(n).
**Traps:** • seed the hashmap with `{0: 1}` (the empty prefix) or you miss subarrays starting at index 0. • Use 64-bit for `prefix` to avoid overflow. • `sum(i..j)` uses `P[j+1] - P[i]` — the `+1` offset is the classic bug. • For XOR, the identity is `pre ^ k` because `a ^ b == k ⇔ a == k ^ b`.

### Prefix sums — 2D
**LeetCode:** [Range Sum Query 2D - Immutable](https://leetcode.com/problems/range-sum-query-2d-immutable/)
**When:** repeated rectangular region-sum queries on an **immutable** matrix.
`P[r][c]` = sum of all cells above-and-left (inclusive of row r-1, col c-1). Region sums fall out of inclusion-exclusion.

```
function build2D(M):                       // M is rows x cols
    P = (rows+1) x (cols+1) zeros
    for r in 0..rows-1:
        for c in 0..cols-1:
            P[r+1][c+1] = M[r][c] + P[r][c+1] + P[r+1][c] - P[r][c]
    return P

// sum of rectangle with corners (r1,c1)..(r2,c2) inclusive:
function regionSum(P, r1, c1, r2, c2):
    return P[r2+1][c2+1] - P[r1][c2+1] - P[r2+1][c1] + P[r1][c1]
```
**Complexity:** build O(rows·cols), query O(1) / space O(rows·cols).
**Traps:** • the `-P[r][c]` term (in build and query) un-double-counts the overlap region — drop it and every sum is wrong. • All indices in the query are shifted by +1 because of the zero border.

### Difference array
**LeetCode:** [Corporate Flight Bookings](https://leetcode.com/problems/corporate-flight-bookings/) · [Car Pooling](https://leetcode.com/problems/car-pooling/)
**When:** apply **many range updates** `add v to A[l..r]`, then read the final array once (no interleaved reads).
Record each update as two point edits on a diff array; one prefix-sum pass reconstructs the result. Turns O(updates × n) into O(updates + n).

```
// Apply updates, then materialize
function applyRangeUpdates(n, updates):     // each update = (l, r, v), inclusive r
    diff = array of n+1 zeros
    for (l, r, v) in updates:
        diff[l] = diff[l] + v
        diff[r+1] = diff[r+1] - v           // cancel just past the range end
    A = array of n zeros; run = 0
    for i in 0..n-1:
        run = run + diff[i]
        A[i] = run
    return A
```
Used by **corporate flight bookings** (`(first, last, seats)`) and **car pooling** (`(passengers, start, end)` → add at `start`, subtract at `end`, then check no prefix exceeds capacity). **Complexity:** O(updates + n) / space O(n). **Traps:** • size the diff array `n+1` so `r+1` is in range; if `r == n-1` you still write `diff[n]`. • Car pooling: subtract at `end` (drop-off), not `end+1` — passengers leave *at* the end stop.

### Kadane's max subarray sum (+ max product variant)
**LeetCode:** [Maximum Subarray](https://leetcode.com/problems/maximum-subarray/) · [Maximum Product Subarray](https://leetcode.com/problems/maximum-product-subarray/)
**When:** maximum sum (or product) of a **contiguous** subarray.
Carry the best subarray *ending here*: either extend the previous one or restart at the current element. For products, sign-flips mean you must also track the running **min**.

```
// Max subarray SUM
function maxSubarray(A):
    cur = A[0]; best = A[0]
    for i in 1..len(A)-1:
        cur = max(A[i], cur + A[i])     // restart vs extend
        best = max(best, cur)
    return best
```

```
// Max subarray PRODUCT — a negative can turn the smallest into the largest
function maxProduct(A):
    curMax = A[0]; curMin = A[0]; best = A[0]
    for i in 1..len(A)-1:
        x = A[i]
        cand = (x, curMax * x, curMin * x)
        curMax = max(cand)
        curMin = min(cand)
        best = max(best, curMax)
    return best
```
**Complexity:** time O(n) / space O(1).
**Traps:** • initialize from `A[0]`, not 0 — a 0 seed gives the wrong answer for all-negative arrays (the answer is the largest single element). • Product: you must swap-aware track min and max; a zero resets both candidates implicitly because `(x, …)` includes `x` itself. • Overflow on product — use 64-bit, and note even that can overflow with large magnitudes.

### Dutch national flag / 3-way partition
**LeetCode:** [Sort Colors](https://leetcode.com/problems/sort-colors/)
**When:** sort/partition into **three** groups in one pass with O(1) space (e.g., sort an array of 0/1/2 — "sort colors"), or 3-way quicksort partitioning around a pivot.
Three pointers: `low` = boundary of the 0s, `high` = boundary of the 2s, `i` scans. Invariant: `[0..low-1]=0`, `[low..i-1]=1`, `[high+1..]=2`, `[i..high]` unknown.

```
function sortColors(A):           // values in {0,1,2}
    low = 0; i = 0; high = len(A) - 1
    while i <= high:
        if A[i] == 0:
            swap(A[low], A[i]); low = low + 1; i = i + 1
        elif A[i] == 2:
            swap(A[i], A[high]); high = high - 1     // do NOT advance i
        else:
            i = i + 1
    return A
```
**Complexity:** time O(n) / space O(1), single pass.
**Traps:** • after swapping with `high`, **do not** increment `i` — the value pulled from the back is unexamined. • After swapping with `low`, you *do* increment `i` (the value coming from `low` is a known 1). • Loop condition is `i <= high`, inclusive.

### Cyclic sort
**LeetCode:** [Find All Numbers Disappeared in an Array](https://leetcode.com/problems/find-all-numbers-disappeared-in-an-array/) · [Set Mismatch](https://leetcode.com/problems/set-mismatch/) · [Find the Duplicate Number](https://leetcode.com/problems/find-the-duplicate-number/) · [First Missing Positive](https://leetcode.com/problems/first-missing-positive/)
**When:** array holds a permutation/near-permutation of `[1..n]` (or `[0..n-1]`); find the missing, the duplicate, all disappeared, or the first missing positive — O(n) time, O(1) space.
Each value `v` belongs at index `v-1`; repeatedly swap until every value sits home, then scan for the mismatch.

```
// Place every v in [1..n] at index v-1
function cyclicSort(A):
    i = 0
    while i < len(A):
        correct = A[i] - 1                       // home index for value A[i]
        if A[i] >= 1 and A[i] <= len(A) and A[i] != A[correct]:
            swap(A[i], A[correct])               // send it home; do NOT advance i
        else:
            i = i + 1
```

```
// All numbers disappeared from [1..n]
function findDisappeared(A):
    cyclicSort(A)
    res = []
    for i in 0..len(A)-1:
        if A[i] != i + 1: res.append(i + 1)      // index i should hold i+1
    return res
```

```
// First missing positive (values may be arbitrary ints)
function firstMissingPositive(A):
    n = len(A); i = 0
    while i < n:
        v = A[i]
        if v >= 1 and v <= n and A[v-1] != v:
            swap(A[i], A[v-1])                    // place v at v-1
        else:
            i = i + 1
    for i in 0..n-1:
        if A[i] != i + 1: return i + 1           // first hole
    return n + 1                                  // 1..n all present
```
**Complexity:** time O(n) amortized (each swap puts ≥1 value home) / space O(1).
**Traps:** • after a swap, **stay** on `i` and re-check — the newly arrived value may also be out of place. • Guard the value range (`1..n`) and the self/duplicate case `A[i] != A[correct]` or you infinite-loop on duplicates. • For first-missing-positive, ignore values ≤0 or > n.

### In-place encoding (sign-marking / index-as-hash)
**LeetCode:** [Find All Duplicates in an Array](https://leetcode.com/problems/find-all-duplicates-in-an-array/) · [Set Matrix Zeroes](https://leetcode.com/problems/set-matrix-zeroes/) · [Game of Life](https://leetcode.com/problems/game-of-life/) · [First Missing Positive](https://leetcode.com/problems/first-missing-positive/)
**When:** values are in `[1..n]` and you may mutate the array; encode "seen index k" by negating `A[k-1]` — uses the sign bit as a free visited array, O(1) extra space.
The magnitude stays the original value (use `abs` when reading); a sign already negative means that index was visited before → it's a duplicate.

```
// Find all duplicates / all missing in [1..n] via sign marking
function markBySign(A):
    dups = []
    for i in 0..len(A)-1:
        idx = abs(A[i]) - 1                       // home index of this value
        if A[idx] < 0: dups.append(abs(A[i]))     // already visited ⇒ duplicate
        else: A[idx] = -A[idx]                     // mark visited
    missing = []
    for i in 0..len(A)-1:
        if A[i] > 0: missing.append(i + 1)         // never marked ⇒ missing
    return (dups, missing)
```
**Complexity:** time O(n) / space O(1).
**Traps:** • always read through `abs()` — the value you need may already be negated. • Only works when values are positive and in range; mixed-sign input needs cyclic sort or a hashmap instead. • Destroys the original array (signs) — restore with a second `abs` pass if the caller needs it back.

### String basics worth a line
**LeetCode:** [Valid Anagram](https://leetcode.com/problems/valid-anagram/) · [Reverse Words in a String](https://leetcode.com/problems/reverse-words-in-a-string/) · [Backspace String Compare](https://leetcode.com/problems/backspace-string-compare/)
**When:** the input is a string and the trick is an array/two-pointer move, not a full string algorithm (KMP/Z/Manacher live in §17).

```
// Char frequency for lowercase a..z — size-26 int array beats a hashmap
function charCounts(s):
    cnt = array of 26 zeros
    for each ch in s: cnt[ch - 'a'] = cnt[ch - 'a'] + 1
    return cnt
// Two strings are anagrams ⇔ their count arrays are equal.
```

```
// Reverse the WORDS of a sentence ("the sky" -> "sky the")
function reverseWords(s):
    words = split(s) on whitespace, dropping empties
    return join(reverse(words), " ")
// In-place O(1)-space variant: reverse whole string, then reverse each word.
```
Two-pointer string compare (backspace strings, `#` = delete): walk both from the **right**, skipping over backspaced chars, comparing surviving chars pairwise. **Complexity:** O(n) / space O(1) for the size-26 array. **Traps:** • the size-26 array assumes lowercase ASCII only — Unicode or mixed case needs a hashmap. • `ch - 'a'` indexing breaks for non-letters. • Watch leading/trailing/multiple spaces when reversing words.

## 5. Binary search

Core insight: binary search is **not** "search a sorted array" — it's **find the boundary of a monotonic predicate**. If there's a value `x*` such that `pred(x)` is false for everything below it and true for everything at/above it (a `F F F T T T` pattern), binary search finds that boundary in O(log range). The array being sorted is just the most common way to get a monotone predicate; the powerful version searches the **answer space**.

**The ONE template — lower bound / "first true".** Half-open interval `[lo, hi)`, invariant: **the answer lies in `[lo, hi)`**, with everything `< lo` failing `pred` and everything `>= hi` passing it. The loop shrinks the window until `lo == hi`, which is the boundary.

```
// Returns the first index in [0, n] where pred(i) becomes true.
// pred must be monotone: F F F T T T. If always false, returns n.
function firstTrue(n, pred):
    lo = 0; hi = n                       // half-open: [lo, hi); hi is "one past"
    while lo < hi:
        mid = lo + (hi - lo) / 2         // floor; overflow-safe (no lo+hi)
        if pred(mid):
            hi = mid                     // mid might be the answer → keep it in range
        else:
            lo = mid + 1                 // mid fails → answer is strictly right
    return lo                            // == hi; first index where pred holds
```

**Why this template and not the `[lo, hi]` inclusive one?** Half-open kills the two classic infinite loops: `mid` is always `< hi` (since `lo < hi`), so `hi = mid` strictly shrinks; and `lo = mid + 1` strictly grows. There is no `mid - 1` / `mid` ambiguity to get wrong. Memorize exactly one shape and reframe every problem as "what is `pred`?"

**Upper bound differs by one character in the predicate.** Lower bound finds the first index with `A[i] >= target` (`pred = A[i] >= target`). Upper bound finds the first index with `A[i] > target` (`pred = A[i] > target`). That's it — same template, strict vs non-strict comparison.

| Goal | Predicate `pred(i)` | Returns |
|---|---|---|
| lower_bound (first `>= target`) | `A[i] >= target` | insertion point keeping order; first occurrence if present |
| upper_bound (first `> target`) | `A[i] > target` | one past the last occurrence |
| first occurrence of target | `A[i] >= target`, then check `A[lo] == target` | index or "not found" |
| last occurrence of target | `upper_bound(target) - 1` | index or "not found" |
| count of target | `upper_bound - lower_bound` | a frequency |
| smallest feasible X (answer space) | `isFeasible(x)` | minimum X with `isFeasible` true |

### lower_bound / upper_bound / first & last occurrence
**LeetCode:** [Search Insert Position](https://leetcode.com/problems/search-insert-position/) · [Find First and Last Position of Element in Sorted Array](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/)
**When:** sorted array; you need an insertion point, membership, the first/last index of a value, or how many times it appears.
All four are the one template with a different predicate; never write a custom loop.

```
function lowerBound(A, target):          // first i with A[i] >= target
    return firstTrue(len(A), function(i): return A[i] >= target)

function upperBound(A, target):          // first i with A[i] > target
    return firstTrue(len(A), function(i): return A[i] > target)

function firstOccurrence(A, target):
    i = lowerBound(A, target)
    return i if i < len(A) and A[i] == target else -1

function lastOccurrence(A, target):
    i = upperBound(A, target) - 1
    return i if i >= 0 and A[i] == target else -1

function countOf(A, target):
    return upperBound(A, target) - lowerBound(A, target)
```
**Complexity:** time O(log n) / space O(1). **Traps:** • after `lowerBound`, you **must** re-check `A[lo] == target` and `lo < len(A)` — the boundary can land past the array or on a different value. • Insertion point can equal `len(A)` (target bigger than everything) — that's valid, don't index with it. • Plain "does target exist" is just `firstOccurrence != -1`.

### Binary search on the ANSWER space
**LeetCode:** [Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/) · [Capacity To Ship Packages Within D Days](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/) · [Find the Smallest Divisor Given a Threshold](https://leetcode.com/problems/find-the-smallest-divisor-given-a-threshold/) · [Minimum Number of Days to Make m Bouquets](https://leetcode.com/problems/minimum-number-of-days-to-make-m-bouquets/) · [Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/)
**When:** the phrasing is "**minimum/maximum X such that it's possible to …**" with a monotone feasibility — if X works, every larger X works too (or vice versa). You're not searching an array; you're searching a numeric range `[loX, hiX]` and asking `isFeasible(x)`.
Reusable "search smallest feasible X" template — pick `lo`/`hi` as the value bounds, write `isFeasible(x)` as the only domain-specific code:

```
// Smallest X in [lo, hi] (inclusive value range) with isFeasible(X) true,
// where isFeasible is monotone: false…false true…true.
function smallestFeasible(lo, hi, isFeasible):
    while lo < hi:
        mid = lo + (hi - lo) / 2
        if isFeasible(mid):
            hi = mid                     // mid works → maybe smaller works
        else:
            lo = mid + 1                 // mid too small → go bigger
    return lo                            // smallest feasible X
```
For "**largest** feasible X" (feasibility is `true…true false…false`), use `mid = lo + (hi - lo + 1) / 2` (round **up**) and set `lo = mid` on feasible, `hi = mid - 1` otherwise — the ceil-mid prevents the `lo = mid` infinite loop.

Worked predicates (all plug straight into `smallestFeasible`):

```
// Koko eating bananas: min speed to finish piles within h hours
function isFeasibleKoko(piles, h, speed):
    hours = 0
    for p in piles: hours = hours + ceil(p / speed)   // ceil division
    return hours <= h
// answer = smallestFeasible(1, max(piles), λspeed. isFeasibleKoko(piles,h,speed))
```

```
// Ship within D days: min daily capacity. cap must be >= max(weights).
function isFeasibleShip(weights, days, cap):
    used = 1; cur = 0
    for w in weights:
        if cur + w > cap: used = used + 1; cur = 0   // start a new day
        cur = cur + w
    return used <= days
// answer = smallestFeasible(max(weights), sum(weights), λcap. isFeasibleShip(...))
```

```
// Split array into m subarrays, minimize the largest subarray sum.
// Same predicate as ship-packages: "can I split with each part <= cap into <= m parts?"
function isFeasibleSplit(A, m, cap):
    parts = 1; cur = 0
    for x in A:
        if cur + x > cap: parts = parts + 1; cur = 0
        cur = cur + x
    return parts <= m
// answer = smallestFeasible(max(A), sum(A), λcap. isFeasibleSplit(A, m, cap))
```
**Allocate books / pages** (minimize the max pages a student reads, contiguous books) is *exactly* `isFeasibleSplit` with students = m. **Aggressive cows / magnetic force** (maximize the **minimum** gap between placed items) is the *largest-feasible* variant: `canPlace(gap)` greedily places items ≥ `gap` apart and checks `count >= k` — feasibility is `true…true false…false`, so use the ceil-mid largest-feasible template, range `[1, max - min]`.

**Complexity:** time O(n · log(range)) — each feasibility check is O(n). / space O(1). **Traps:** • get the **value bounds** right: `lo = max(element)` when a single item must fit in one bucket (ship/split), `lo = 1` when rate 1 is genuinely possible (Koko). `hi = sum(...)` or `max(...)` as the loosest bound. • The greedy `isFeasible` must be monotone — sanity-check that more capacity never *reduces* feasibility. • Use the right rounding: floor-mid + `hi=mid` for *smallest*-feasible, ceil-mid + `lo=mid` for *largest*-feasible, or you infinite-loop. • Use 64-bit for `sum(...)` bounds.

### Rotated sorted array
**LeetCode:** [Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/) · [Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/) · [Search in Rotated Sorted Array II](https://leetcode.com/problems/search-in-rotated-sorted-array-ii/)
**When:** a sorted array rotated at an unknown pivot; search a target or find the minimum in O(log n).
At any `mid`, **one half is still sorted** — detect which by comparing `A[mid]` to an endpoint, then decide whether the target lies in that sorted half.

```
// Search target in a rotated sorted array (distinct values)
function searchRotated(A, target):
    lo = 0; hi = len(A) - 1               // inclusive here — we return an index
    while lo <= hi:
        mid = lo + (hi - lo) / 2
        if A[mid] == target: return mid
        if A[lo] <= A[mid]:               // left half [lo..mid] is sorted
            if A[lo] <= target and target < A[mid]: hi = mid - 1
            else: lo = mid + 1
        else:                             // right half [mid..hi] is sorted
            if A[mid] < target and target <= A[hi]: lo = mid + 1
            else: hi = mid - 1
    return -1
```

```
// Find the minimum (= rotation point), distinct values
function findMin(A):
    lo = 0; hi = len(A) - 1
    while lo < hi:
        mid = lo + (hi - lo) / 2
        if A[mid] > A[hi]: lo = mid + 1   // min is strictly right of mid
        else: hi = mid                    // min is at mid or left
    return A[lo]
```
**With duplicates:** the `A[lo] <= A[mid]` test becomes ambiguous when `A[lo] == A[mid] == A[hi]`. Handle by shrinking one step (`lo = lo + 1`, or in findMin `hi = hi - 1`) when `A[mid] == A[hi]`, which degrades worst case to **O(n)** (e.g., all-equal array). **Complexity:** O(log n) distinct, O(n) worst with duplicates / space O(1). **Traps:** • compare against the correct endpoint consistently — mixing `A[lo]` and `A[hi]` tests flips the logic. • `findMin` compares `A[mid]` to `A[hi]`, not `A[lo]` (comparing to `A[lo]` fails on a non-rotated array). • Use `<=` carefully: `A[lo] <= A[mid]` (not `<`) so a 2-element sorted half still registers as sorted.

### Find peak, 2D matrix search
**LeetCode:** [Find Peak Element](https://leetcode.com/problems/find-peak-element/) · [Search a 2D Matrix](https://leetcode.com/problems/search-a-2d-matrix/) · [Search a 2D Matrix II](https://leetcode.com/problems/search-a-2d-matrix-ii/)
**When:** find any local peak in an unsorted-but-bounded array, or search a matrix that's sorted by row and/or column.
Peak: even without global sortedness, the slope is locally monotone — walk **uphill** and you must hit a peak (treat out-of-bounds as `-INF`).

```
// Find a peak element: A[i] > both neighbors (any peak). O(log n).
function findPeak(A):
    lo = 0; hi = len(A) - 1
    while lo < hi:
        mid = lo + (hi - lo) / 2
        if A[mid] < A[mid + 1]: lo = mid + 1   // ascending → peak to the right
        else: hi = mid                          // descending/equal → peak at mid or left
    return lo
```

```
// Search a row+col sorted matrix (each row asc L→R, each col asc top→bottom).
// Staircase from the top-right: O(rows + cols).
function searchMatrix(M, target):
    r = 0; c = cols - 1                          // start top-right
    while r < rows and c >= 0:
        if M[r][c] == target: return true
        elif M[r][c] > target: c = c - 1          // too big → drop a column
        else: r = r + 1                           // too small → drop a row
    return false
```

```
// Fully sorted matrix (each row sorted AND row[i][-1] < row[i+1][0]):
// treat as one flat sorted array of rows*cols, binary search the index.
function searchFlatMatrix(M, target):
    lo = 0; hi = rows * cols - 1
    while lo <= hi:
        mid = lo + (hi - lo) / 2
        v = M[mid / cols][mid % cols]            // 1D index -> 2D
        if v == target: return true
        elif v < target: lo = mid + 1
        else: hi = mid - 1
    return false
```
**Complexity:** peak O(log n); staircase O(rows + cols); flat O(log(rows·cols)) / space O(1). **Traps:** • staircase **must** start at top-right (or bottom-left) — top-left/bottom-right give no monotone direction. • Two different "sorted matrix" problems — staircase for row+col sorted; flat binary search only when rows are globally chained. • Flat search: `mid / cols` and `mid % cols` map index→(row,col); off-by-one in `rows*cols - 1` is common.

### Median of two sorted arrays  *(advanced)*
**LeetCode:** [Median of Two Sorted Arrays](https://leetcode.com/problems/median-of-two-sorted-arrays/)
**When:** the overall median of two sorted arrays in O(log(min(m, n))) — the hard partition problem.
Binary-search a **partition cut** in the smaller array; the cut in the other array is forced so that the left halves together hold exactly half the elements. Adjust the cut until the max of the left parts ≤ the min of the right parts.

```
// O(log(min(m,n))). Binary search the cut in A (the shorter array).
function medianTwoSorted(A, B):
    if len(A) > len(B): return medianTwoSorted(B, A)   // ensure A is shorter
    m = len(A); n = len(B); half = (m + n + 1) / 2
    lo = 0; hi = m                                       // cut position in A: [0..m]
    while lo <= hi:
        i = lo + (hi - lo) / 2          // A's left part = A[0..i-1]
        j = half - i                    // B's left part = B[0..j-1] (forced)
        aLeft  = -INF if i == 0 else A[i-1]
        aRight =  INF if i == m else A[i]
        bLeft  = -INF if j == 0 else B[j-1]
        bRight =  INF if j == n else B[j]
        if aLeft <= bRight and bLeft <= aRight:          // correct partition
            if (m + n) is odd: return max(aLeft, bLeft)
            return (max(aLeft, bLeft) + min(aRight, bRight)) / 2.0
        elif aLeft > bRight: hi = i - 1                   // A's cut too far right
        else: lo = i + 1                                  // A's cut too far left
    // unreachable for valid sorted inputs
```
**Complexity:** time O(log(min(m, n))) / space O(1). **Traps:** • always search the **shorter** array so `j = half - i` stays in bounds. • `±INF` sentinels handle empty left/right parts (cut at the very edge) — forgetting them crashes on boundary cuts. • `half = (m+n+1)/2` (with the `+1`) makes the odd-length median land in the **left** part, so `max(aLeft, bLeft)` is the answer. • This is rarely required — a clean O(m+n) merge-to-median is acceptable unless the interviewer explicitly demands log time.

> **Say this in the room:** "I reframe binary search as finding the boundary of a monotone predicate, then I only ever write one template — half-open `[lo, hi)`, floor-mid with `lo + (hi - lo)/2` for overflow safety, `hi = mid` on true and `lo = mid + 1` on false. Lower vs upper bound is just `>=` vs `>`. For optimization problems I binary-search the answer space with an `isFeasible(x)` greedy check — Koko, ship-packages, split-array and book-allocation are all the same skeleton with a different feasibility function."

**Cross-cutting traps:** • **Infinite loop** from a non-shrinking update — with inclusive `[lo, hi]` you need `hi = mid - 1` / `lo = mid + 1`, and for "largest feasible" you need ceil-mid; the half-open template sidesteps both. • **Off-by-one**: be deliberate about inclusive `[lo, hi]` (returning an index, `while lo <= hi`) vs half-open `[lo, hi)` (finding a boundary, `while lo < hi`) — don't mix the two styles in one function. • **Overflow**: always `lo + (hi - lo)/2`, never `(lo + hi)/2`; use 64-bit for answer-space sums. • **Duplicates in rotated arrays** silently break the "which half is sorted" test and force an O(n) fallback — call that out rather than assuming O(log n).

## 6. Intervals & sweep line

Problems on `[start, end]` ranges: merging, overlap detection, max concurrency. Almost everything here reduces to **sort first, then one linear pass**. The only real decision is which key to sort on. A "sweep line" generalizes this: turn each interval into two events (`+1` at start, `−1` at end), sort the events, and walk a running counter.

### The two sort keys: start vs end
**When:** every interval problem starts here — pick the key before writing any loop.
Sort by **start** when you process intervals left-to-right and need to *combine/extend* the current run (merge, insert, sweep). Sort by **end** when you make a *greedy keep/drop* choice and want the option that "frees up the future soonest" (max non-overlapping count, fewest arrows/points). Mismatching the key is the #1 bug.

```
// merge / sweep / insert  → sort by start
A.sort(key = lambda iv: iv.start)
// greedy "keep earliest finisher" → sort by end
A.sort(key = lambda iv: iv.end)
```

**Complexity:** time O(n log n) for the sort, O(n) for the pass / space O(1)–O(n).
**Traps:** • sorting by the wrong key gives a plausible-but-wrong answer, not a crash. • decide whether endpoints touching (`[1,2]` and `[2,3]`) count as overlapping — *problem-dependent*; for merge they usually do, for "non-overlapping count" usually not. • ties: when sorting events, decide if `+1` or `−1` wins at equal coordinate (see event-sweep below).

### Merge intervals
**LeetCode:** [Merge Intervals](https://leetcode.com/problems/merge-intervals/) · [Insert Interval](https://leetcode.com/problems/insert-interval/)
**When:** collapse a set of intervals into the minimal set of disjoint intervals.
Sort by start. Keep a `cur` interval; for each next interval, if it starts ≤ `cur.end`, extend `cur.end = max(cur.end, next.end)`; otherwise flush `cur` and start a new one.

```
function merge(A):
  if len(A) == 0: return []
  A.sort(key = lambda iv: iv[0])
  res = []
  cur = A[0]
  for i in 1..len(A)-1:
    s, e = A[i]
    if s <= cur[1]:               // overlap (or touch) → extend
      cur = (cur[0], max(cur[1], e))
    else:
      res.append(cur)
      cur = (s, e)
  res.append(cur)                 // flush last
  return res
```

**Complexity:** time O(n log n) / space O(n) output (O(1) extra ignoring output).
**Traps:** • forgetting to flush `cur` after the loop drops the last interval. • use `max(cur.end, e)` — `next.end` can be *smaller* than `cur.end` when `next` is fully contained. • use `<=` not `<` if touching intervals must merge.

### Insert interval (into sorted disjoint set)
**LeetCode:** [Insert Interval](https://leetcode.com/problems/insert-interval/) · [Merge Intervals](https://leetcode.com/problems/merge-intervals/)
**When:** existing intervals are already sorted and non-overlapping; insert one new interval and re-merge in O(n) (no re-sort).
Three phases in one pass: copy all intervals ending **before** the new one starts; absorb all that overlap into a widened new interval; copy the rest.

```
function insert(A, new):
  res = []
  i = 0; n = len(A)
  // 1. left of new, no overlap
  while i < n and A[i][1] < new[0]:
    res.append(A[i]); i += 1
  // 2. overlap → merge into new
  while i < n and A[i][0] <= new[1]:
    new = (min(new[0], A[i][0]), max(new[1], A[i][1]))
    i += 1
  res.append(new)
  // 3. right of new
  while i < n: res.append(A[i]); i += 1
  return res
```

**Complexity:** time O(n) / space O(n) output.
**Traps:** • boundary comparisons (`A[i].end < new.start` vs `<=`) decide whether touching merges. • don't append `new` until phase 2 finishes widening it. • works *only* because input is pre-sorted and disjoint.

### Interval list intersection (two sorted lists)
**LeetCode:** [Interval List Intersections](https://leetcode.com/problems/interval-list-intersections/) · [Merge Intervals](https://leetcode.com/problems/merge-intervals/)
**When:** two lists, each already sorted & disjoint; return every pairwise overlap.
Two pointers. The intersection of `A[i]` and `B[j]` is `[max(starts), min(ends)]` — valid only if `lo <= hi`. Advance the pointer whose interval **ends first** (it can't intersect anything later).

```
function intervalIntersection(A, B):
  res = []
  i = 0; j = 0
  while i < len(A) and j < len(B):
    lo = max(A[i][0], B[j][0])
    hi = min(A[i][1], B[j][1])
    if lo <= hi: res.append((lo, hi))
    if A[i][1] < B[j][1]: i += 1     // A ends first → drop A[i]
    else: j += 1
  return res
```

**Complexity:** time O(m + n) / space O(output).
**Traps:** • advance based on **end**, not start. • `lo <= hi` allows single-point intersections (`[2,2]`); use `<` if those don't count. • equal ends: advancing either pointer is fine, but advance exactly one.

### Non-overlapping intervals / minimum removals
**LeetCode:** [Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/) · [Minimum Number of Arrows to Burst Balloons](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/)
**When:** "remove the fewest intervals so the rest don't overlap" — equivalently, *keep the most*. This is interval scheduling (activity selection); the greedy is provably optimal (see §16, exchange argument).
Sort by **end**. Greedily keep an interval iff it starts ≥ the last kept end. Removals = `n − kept`.

```
function eraseOverlap(A):
  A.sort(key = lambda iv: iv[1])    // by END
  kept = 0
  lastEnd = -INF
  for s, e in A:
    if s >= lastEnd:                // no overlap with last kept
      kept += 1
      lastEnd = e
  return len(A) - kept
```

**Complexity:** time O(n log n) / space O(1).
**Traps:** • sorting by start here is wrong (a long early interval blocks many short ones). • `>=` vs `>` for touching endpoints — LeetCode 435 treats `[1,2],[2,3]` as non-overlapping, so use `>=`. • "min removals" = total − "max keep"; don't recount.

### Minimum arrows to burst balloons
**LeetCode:** [Minimum Number of Arrows to Burst Balloons](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/) · [Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/)
**When:** intervals on a line; one point can "hit" all intervals it lies inside; minimize points (arrows). Same greedy skeleton as above with a `>` boundary twist.
Sort by **end**. Shoot at the current end; skip every interval that starts ≤ that arrow position (it's hit); when an interval starts *after* the arrow, fire a new one.

```
function findMinArrows(A):
  if len(A) == 0: return 0
  A.sort(key = lambda iv: iv[1])
  arrows = 1
  arrowAt = A[0][1]
  for s, e in A[1:]:
    if s > arrowAt:                 // not covered → new arrow
      arrows += 1
      arrowAt = e
  return arrows
```

**Complexity:** time O(n log n) / space O(1).
**Traps:** • here a *touching* balloon (`start == arrowAt`) IS burst, so the guard is `s > arrowAt` (strict), unlike the `>=` in non-overlapping. • this is "min points to stab all intervals" = `n − maxNonOverlapping`; same problem, opposite framing. • watch integer overflow if coordinates are near INT bounds and you ever subtract.

### Meeting rooms I — can attend all?
**LeetCode:** [My Calendar I](https://leetcode.com/problems/my-calendar-i/) · [Car Pooling](https://leetcode.com/problems/car-pooling/)
**When:** detect whether *any* two intervals overlap (boolean).
Sort by start; if any interval starts before the previous one ends, return false.

```
function canAttendAll(A):
  A.sort(key = lambda iv: iv[0])
  for i in 1..len(A)-1:
    if A[i][0] < A[i-1][1]: return false
  return true
```

**Complexity:** time O(n log n) / space O(1).
**Traps:** • `[1,2]` then `[2,3]` don't conflict → use strict `<`. • only adjacency-after-sort matters; no need to compare all pairs.

### Meeting rooms II — minimum rooms (min-heap of end times)
**LeetCode:** [My Calendar I](https://leetcode.com/problems/my-calendar-i/) · [My Calendar II](https://leetcode.com/problems/my-calendar-ii/) · [Meeting Rooms III](https://leetcode.com/problems/meeting-rooms-iii/)
**When:** max number of meetings simultaneously in progress = rooms needed. Method A: min-heap of end times.
Sort by start. Walk meetings; the heap holds end times of rooms currently busy. Before assigning a room to a new meeting, pop every room that has freed (`end <= newStart`). Push the new meeting's end. Answer = max heap size ever (or final size if you only ever push when no room frees).

```
function minMeetingRooms(A):
  A.sort(key = lambda iv: iv[0])
  h = []                            // min-heap of end times
  best = 0
  for s, e in A:
    while len(h) > 0 and h.top() <= s:
      h.pop()                       // a room freed up
    h.push(e)
    best = max(best, len(h))
  return best
```

**Complexity:** time O(n log n) / space O(n).
**Traps:** • free a room with `<=` if a meeting ending at `t` lets another start at `t`; use `<` if not — match the problem. • `best` = peak concurrency, which equals final heap size here only because we never pop below the true concurrency; tracking `max(len(h))` is the safe invariant. • heap orders by **end**; pushing starts is wrong.

### Meeting rooms II — minimum rooms (event sweep)
**LeetCode:** [Car Pooling](https://leetcode.com/problems/car-pooling/) · [My Calendar III](https://leetcode.com/problems/my-calendar-iii/) · [The Skyline Problem](https://leetcode.com/problems/the-skyline-problem/)
**When:** same answer via the sweep-line / chronological-ordering trick — often cleaner and generalizes to "max concurrent X."
Split each `[s, e]` into events `(s, +1)` and `(e, −1)`. Sort events by coordinate; **on ties process `−1` before `+1`** (a room freed at `t` is reusable at `t`). Walk a running counter; the max it reaches is the answer.

```
function minMeetingRoomsSweep(A):
  ev = []
  for s, e in A:
    ev.append((s, +1))
    ev.append((e, -1))
  // tie-break: -1 (end) before +1 (start) at equal coord
  ev.sort(key = lambda x: (x[0], x[1]))
  cur = 0; best = 0
  for coord, delta in ev:
    cur += delta
    best = max(best, cur)
  return best
```

Equivalent two-array form: sort `starts[]` and `ends[]` separately, two pointers; `if starts[i] < ends[j]: cur += 1; i += 1 else: cur -= 1; j += 1`.
**Complexity:** time O(n log n) / space O(n).
**Traps:** • the tie-break is the whole game: `(coord, delta)` with `−1 = delta` sorting first means ends process first. If a meeting *can't* reuse a same-instant freed room, flip to process `+1` first. • two-array form must compare `starts[i] < ends[j]` with the matching strictness.

> Say this in the room: "Meeting-rooms-II is just *max overlap*. I'll turn intervals into ±1 events, sort, and sweep a running count — ends before starts on ties. The peak counter is the room count."

### Employee free time
**LeetCode:** [Corporate Flight Bookings](https://leetcode.com/problems/corporate-flight-bookings/) · [Car Pooling](https://leetcode.com/problems/car-pooling/)
**When:** given each employee's sorted busy intervals, find the gaps free for *everyone* (intersection of free time = complement of the union of busy time).
Flatten all intervals, sort by start, merge (as in merge intervals); the **gaps between consecutive merged intervals** are the common free time.

```
function employeeFreeTime(schedules):
  all = flatten(schedules)               // every busy interval
  all.sort(key = lambda iv: iv[0])
  res = []
  end = all[0][1]
  for s, e in all[1:]:
    if s > end:                          // gap between merged blocks
      res.append((end, s))
    end = max(end, e)
  return res
```

**Complexity:** time O(n log n) (or O(n log k) with a k-way merge of already-sorted lists, see §11) / space O(n).
**Traps:** • each employee's list is sorted but the *combined* list isn't — must re-sort or k-way merge. • gap exists only when `s > end` (strict); equal endpoints leave no free time. • result excludes the unbounded gaps before the first/after the last interval.

### My Calendar I (double-booking)
**LeetCode:** [My Calendar I](https://leetcode.com/problems/my-calendar-i/) · [My Calendar II](https://leetcode.com/problems/my-calendar-ii/)
**When:** stream of bookings; reject a new `[s, e)` if it overlaps any existing booking. Detect a single overlap.
Two half-open intervals `[s1,e1)`, `[s2,e2)` overlap iff `s1 < e2 and s2 < e1`. Keep a balanced BST / ordered map keyed by start; a new booking conflicts only with the neighbor whose start is just before/after — `O(log n)` per book. The brute-force list scan is `O(n)` per book.

```
// ordered map keyed by start; for production use a balanced tree.
function book(s, e):                      // half-open [s, e)
  for ds, de in calendar:                 // O(n) brute; O(log n) with a tree
    if s < de and ds < e: return false    // overlap → reject
  calendar.add((s, e))
  return true
```

**Complexity:** brute time O(n) per book; with a balanced BST O(log n) per book / space O(n).
**Traps:** • the half-open overlap test `s < de and ds < e` is the canonical formula — memorize it. • with closed intervals use `<=`. • the tree version only needs to check the predecessor and successor by start, not all entries.

### My Calendar II (triple-booking) — difference idea
**LeetCode:** [My Calendar II](https://leetcode.com/problems/my-calendar-ii/) · [My Calendar III](https://leetcode.com/problems/my-calendar-iii/)
**When:** allow double bookings but reject any **triple** overlap.
Keep two lists: `bookings` and `overlaps` (regions already double-booked). On a new `[s,e)`: if it intersects any `overlaps` region → reject (would be triple). Otherwise add the intersections with existing `bookings` into `overlaps`, then add the booking. This is the manual version of a sweep; the general "K-booking" answer is a sweep over a difference structure (see Car Pooling, §4).

```
function book2(s, e):
  for os, oe in overlaps:                 // already-double regions
    if s < oe and os < e: return false    // would be triple
  for bs, be in bookings:                 // record new double regions
    lo = max(s, bs); hi = min(e, be)
    if lo < hi: overlaps.append((lo, hi))
  bookings.append((s, e))
  return true
```

**Complexity:** time O(n) per book / space O(n).
**Traps:** • check `overlaps` *before* mutating it. • intersection valid only when `lo < hi` (half-open). • generalizes to "MyCalendarK" via a sweep/`SortedDict` of ±1 deltas — see below and §4.

### Car pooling / corporate flight bookings (difference array)
**LeetCode:** [Car Pooling](https://leetcode.com/problems/car-pooling/) · [Corporate Flight Bookings](https://leetcode.com/problems/corporate-flight-bookings/) · [My Calendar III](https://leetcode.com/problems/my-calendar-iii/)
**When:** many range-update events (`+passengers` over `[start, end)`), then ask a capacity/total question. Classic **difference array** (see §4) when coordinates are small/dense; a `SortedDict` sweep when sparse.
Add `+v` at `start` and `−v` at `end`; the prefix sum at each point is the active total. For car pooling, check it never exceeds capacity.

```
function carPooling(trips, capacity):
  diff = array of zeros, size = MAXLOC+1   // dense coords (e.g. ≤ 1000)
  for v, s, e in trips:
    diff[s] += v
    diff[e] -= v                           // passengers leave at e (half-open)
  cur = 0
  for x in 0..MAXLOC:
    cur += diff[x]                         // running active count
    if cur > capacity: return false
  return true
```

For sparse coords use an ordered map: `delta[s] += v; delta[e] -= v`, then walk keys in sorted order accumulating. Corporate Flight Bookings is identical but returns the final per-index totals (range add, then one prefix-sum pass).
**Complexity:** dense: time O(n + range) / space O(range). Sparse map: time O(n log n) / space O(n).
**Traps:** • subtract the delta at `end`, not `end+1`, for half-open `[s,e)`; for **inclusive** `[s,e]` (flight-bookings uses 1-indexed inclusive) subtract at `e+1`. • off-by-one between half-open and inclusive is the bug. • difference array only when the coordinate range is bounded; otherwise sweep a sorted map.

### Sweep line / event counting → max overlap & skyline
**LeetCode:** [My Calendar III](https://leetcode.com/problems/my-calendar-iii/) · [Car Pooling](https://leetcode.com/problems/car-pooling/) · [The Skyline Problem](https://leetcode.com/problems/the-skyline-problem/)
**When:** "maximum number of overlapping intervals," "max CPU/booking concurrency," or anything needing the running count of active intervals — and its harder cousin, the **skyline**.
Generic max-overlap: emit `(start,+1)/(end,−1)` events, sort (ends-before-starts on ties for half-open), prefix-sum, track max — the Meeting-Rooms-II sweep above. **Skyline (LC 218):** events at each building edge; sweep x left→right keeping a **max-heap of active heights** (push on left edge, lazily discard on right edge); whenever the current max height changes after processing all events at an x, emit a key point `(x, newMaxHeight)`.

```
function getSkyline(buildings):            // buildings: (L, R, H)
  ev = []
  for L, R, H in buildings:
    ev.append((L, -H))                     // left edge: negative height = "add"
    ev.append((R, +H))                     // right edge: positive = "remove"
  ev.sort()                                // x asc; at equal x, adds (more neg) first
  res = []
  live = max-heap with {0}                 // ground level
  prevMax = 0
  for x, h in ev:
    if h < 0: live.push(-h)                // add height
    else: live.removeLazy(h)               // mark for lazy deletion
    curMax = live.top()                    // top after discarding stale
    if curMax != prevMax:
      res.append((x, curMax))
      prevMax = curMax
  return res
```

**Complexity:** time O(n log n) / space O(n).
**Traps:** • the `(L, −H)/(R, +H)` encoding makes one `sort()` order edges correctly: at equal x, **left edges (adds) before right edges**, and *taller adds first* so a key point isn't emitted twice. • the max-heap needs lazy deletion (mark removed, pop stale from top) or a multiset; a plain heap can't delete an interior height. • always seed the heap with `0` (ground) so the skyline can drop back to 0.

### Comparison: heap-of-ends vs event-sweep for "max concurrent"
Both compute peak concurrency in O(n log n); choose by *what else* the problem asks.

| Aspect | Min-heap of end times | Event sweep (±1, sort, prefix) |
|---|---|---|
| Answer it gives | room count = max/final heap size | max running counter |
| Tracks *which* interval uses which "room"? | yes (heap entry per active interval) | no — just the count |
| Tie handling (touch endpoints) | `pop while end <= start` (`<` if not reusable) | sort key `(coord, delta)`, ends-first via `delta` |
| Needs heap? | yes | no — plain sort + counter |
| Best when | you must assign/label resources, or report the busiest interval | you only need the peak count, K-booking, or to also sum deltas |
| Generalizes to weighted (`+v`/`−v`) | awkward | trivially (difference array / sorted deltas) |

> Say this in the room: "If I just need *how many* overlap, I sweep ±1 events and prefix-sum. If I must know *which* meetings share a room or assign resources, I keep a min-heap of end times and pop freed rooms."

## 7. Hashing, counting & bit tricks

Hash structures buy O(1) average membership/frequency; the trick is choosing the *right key*. This section covers the "seen" set, frequency maps, the prefix-sum + hashmap family (the single highest-yield array pattern), counting/bucket sort, Boyer-Moore vote, and the full bit-manipulation + bitmask-enumeration toolkit. Pseudocode is language-agnostic, but every integer trick flags 32/64-bit overflow and signed-shift caution for the Java/Python reader.

### Hash-set "seen" pattern
**LeetCode:** [Two Sum](https://leetcode.com/problems/two-sum/) · [Contains Duplicate](https://leetcode.com/problems/contains-duplicate/) · [Longest Consecutive Sequence](https://leetcode.com/problems/longest-consecutive-sequence/)
**When:** "has this value appeared?", "is the complement present?", "is there a duplicate?" — any membership question over a stream of values.
One pass, decide using O(1) lookup, then record. The art is *what to store* and *when to check vs add*.

```
// dedup in place (order-preserving)
seen = {}
out = []
for each x in A:
    if x not in seen:
        seen.add(x)
        out.append(x)

// two-sum: complement lookup BEFORE insert (avoids using same index twice)
function twoSum(A, target):
    idx = {}                      // value -> index
    for i in 0..len(A)-1:
        need = target - A[i]
        if need in idx:
            return (idx[need], i)
        idx[A[i]] = i             // insert after checking
    return null

// contains duplicate
function hasDup(A):
    seen = {}
    for each x in A:
        if x in seen: return true
        seen.add(x)
    return false
```
**Complexity:** time O(n) / space O(n).
**Traps:** • Two-sum: check the complement *before* inserting `A[i]`, or `target == 2*A[i]` falsely matches index i with itself. • Storing the *index* (not just presence) is what lets you return the pair / a distance (e.g. "duplicate within k" → store last index, check `i - idx[x] <= k`). • If values can repeat and you need *all* pairs, a set loses count — use a frequency map. • Hash of a custom/compound key (a pair, a tuple) must be value-based, not identity-based.

### Frequency map (counting)
**LeetCode:** [Valid Anagram](https://leetcode.com/problems/valid-anagram/) · [Group Anagrams](https://leetcode.com/problems/group-anagrams/) · [First Unique Character in a String](https://leetcode.com/problems/first-unique-character-in-a-string/)
**When:** "how many times", "most/least common", "are these two collections the same multiset", "is there a bijection between characters".
A `map value -> count`. Increment on add, decrement on remove; treat count==0 as absent. The *key design* is everything.

```
// build counts
cnt = {}
for each x in A:
    cnt[x] = cnt.get(x, 0) + 1

// group anagrams — key by a canonical signature
function groupAnagrams(words):
    groups = {}                       // signatureKey -> list of words
    for each w in words:
        key = signature(w)            // see two options below
        groups.setdefault(key, []).append(w)
    return [g for _, g in groups]

// signature option A: sorted characters  -> O(L log L) per word
function signature(w): return sort(chars(w)) as string
// signature option B: 26-int count vector -> O(L), better for long/lowercase-only words
function signature(w):
    c = array of 26 zeros
    for each ch in w: c[ch - 'a'] += 1
    return tuple(c)                   // or join with a separator into a string

// first unique character: count, then first with count==1
function firstUniq(s):
    cnt = {}
    for each ch in s: cnt[ch] = cnt.get(ch,0)+1
    for i in 0..len(s)-1:
        if cnt[s[i]] == 1: return i
    return -1

// isomorphic strings / word pattern — bijection needs TWO maps
function isIsomorphic(s, t):
    if len(s) != len(t): return false
    f = {}; g = {}                    // s->t and t->s
    for i in 0..len(s)-1:
        a = s[i]; b = t[i]
        if a in f and f[a] != b: return false
        if b in g and g[b] != a: return false
        f[a] = b; g[b] = a
    return true
```
**Complexity:** counting O(n); group anagrams O(N·L) with the count signature, O(N·L log L) with sorted-string.
**Traps:** • Anagram via sorted string is simplest but slower; the 26-int vector is the senior answer for lowercase-only. • A tuple/array used as a map key must hash by value — in Java, wrap in a record or build a string; in Python, use a `tuple`. • Isomorphic/word-pattern needs a **two-way** map; one map admits two different sources mapping to the same target. • Multiset equality: compare frequency maps, or count up for A and down for B and assert all zero. • Top-k frequent: build counts here, then select — heap or bucket (see §11 and below).

### Prefix-sum + hashmap
**LeetCode:** [Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/) · [Contiguous Array](https://leetcode.com/problems/contiguous-array/) · [Continuous Subarray Sum](https://leetcode.com/problems/continuous-subarray-sum/) · [Subarray Sums Divisible by K](https://leetcode.com/problems/subarray-sums-divisible-by-k/)
**When:** "count/find subarrays whose sum has property P" where P is "== k", "divisible by k", or "equal counts of two symbols" — and the array may contain negatives (so sliding window fails). Cross-ref §4 for the prefix-sum array itself.
Let `P[i]` = sum of `A[0..i-1]`. A subarray `A[i..j]` sums to `P[j+1] - P[i]`. To make `sum == k`, you need a prior prefix equal to `P[j+1] - k`; store *seen prefixes* (a count, or a first-seen index) in a map and look up as you sweep — one pass, no nested loop.

```
// (a) COUNT subarrays with sum == k  (negatives allowed)
function subarraySumK(A, k):
    seen = {0: 1}                     // prefix 0 seen once (empty prefix)
    run = 0; ans = 0
    for each x in A:
        run += x
        ans += seen.get(run - k, 0)   // how many prior prefixes = run-k
        seen[run] = seen.get(run, 0) + 1
    return ans

// (b) COUNT subarrays with sum divisible by k  (remainder map)
function subarrayDivByK(A, k):
    seen = {0: 1}
    run = 0; ans = 0
    for each x in A:
        run += x
        r = ((run % k) + k) % k       // normalize negative remainder to [0,k)
        ans += seen.get(r, 0)
        seen[r] = seen.get(r, 0) + 1
    return ans

// (c) LONGEST subarray with equal #0 and #1: map +1/-1, store FIRST index of each prefix
function longestEqual01(A):
    first = {0: -1}                   // prefix 0 first "seen" at index -1
    run = 0; best = 0
    for i in 0..len(A)-1:
        run += (1 if A[i]==1 else -1)
        if run in first:
            best = max(best, i - first[run])
        else:
            first[run] = i            // keep EARLIEST -> longest span
    return best

// (d) continuous subarray sum: exists len>=2 subarray with sum multiple of k
function checkSubarraySum(A, k):
    first = {0: -1}                   // remainder -> earliest index
    run = 0
    for i in 0..len(A)-1:
        run += A[i]
        r = run % k                   // (normalize if A has negatives)
        if r in first:
            if i - first[r] >= 2: return true
        else:
            first[r] = i
    return false
```
**Complexity:** time O(n) / space O(n).
**Traps:** • **Seed the map with `{0:1}`** (count) or `{0:-1}` (index) — the empty prefix is what catches subarrays starting at index 0. • Count problems store a **count**; longest/shortest problems store the **first (earliest) index** and never overwrite it. • Remainder of a negative number is implementation-defined — normalize with `((run % k) + k) % k`. • `run` can overflow 32-bit; use 64-bit (Java `long`; Python is fine). • This pattern *replaces* sliding window precisely when negatives break the monotonicity window needs.

### Sliding window with a count map
**LeetCode:** [Find All Anagrams in a String](https://leetcode.com/problems/find-all-anagrams-in-a-string/) · [Permutation in String](https://leetcode.com/problems/permutation-in-string/) · [Longest Repeating Character Replacement](https://leetcode.com/problems/longest-repeating-character-replacement/) · [Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/)
**When:** the window constraint is about *character/element multiplicities* — "longest substring with at most k distinct", "minimum window containing all of T", "permutation/anagram of T inside S", "longest substring with no repeats". Cross-ref §4 for the bare two-pointer window.
Maintain `cnt` over the current window plus a scalar (distinct-count, or `missing` = how many required chars still unmet). Expand right, and while the invariant is violated, shrink from the left, updating `cnt` and the scalar.

```
// longest substring with at most K distinct characters
function longestAtMostKDistinct(s, K):
    cnt = {}; left = 0; best = 0
    for right in 0..len(s)-1:
        cnt[s[right]] = cnt.get(s[right], 0) + 1
        while len(cnt) > K:                  // too many distinct -> shrink
            cnt[s[left]] -= 1
            if cnt[s[left]] == 0: cnt.remove(s[left])
            left += 1
        best = max(best, right - left + 1)
    return best

// minimum window substring containing all chars of T (with multiplicity)
function minWindow(s, t):
    need = {}; for each ch in t: need[ch] = need.get(ch,0)+1
    missing = len(t)                          // total required slots still unmet
    left = 0; bestLen = INF; bestL = 0
    for right in 0..len(s)-1:
        c = s[right]
        if need.get(c, 0) > 0: missing -= 1
        need[c] = need.get(c, 0) - 1           // can go negative (surplus)
        while missing == 0:                    // valid -> try to shrink
            if right - left + 1 < bestLen:
                bestLen = right - left + 1; bestL = left
            need[s[left]] += 1
            if need[s[left]] > 0: missing += 1  // we removed a required char
            left += 1
    return "" if bestLen == INF else s[bestL .. bestL+bestLen-1]
```
**Complexity:** time O(n) (each index enters/leaves once); space O(alphabet).
**Traps:** • Delete the key when its count hits 0, or `len(cnt)` (the distinct count) lies. • In min-window, `need` is allowed to go **negative** for surplus chars; `missing` only changes when crossing the 0 boundary. • "Exactly K distinct" = atMost(K) − atMost(K−1). • Fixed-size window (permutation-in-string) compares `cnt` to `need` of fixed size — slide one in, one out, no inner while.

### Counting sort / bucket sort
**LeetCode:** [Sort Colors](https://leetcode.com/problems/sort-colors/) · [Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/) · [H-Index](https://leetcode.com/problems/h-index/) · [Maximum Gap](https://leetcode.com/problems/maximum-gap/)
**When:** keys are integers in a small/bounded range, or you want top-k by frequency without a heap — anything where you can index *by the value/frequency itself*.
Counting sort: tally each key, then emit in key order — O(n+R), R = range. Bucket-by-frequency: index a bucket array by *count*, so a single descending scan yields most-frequent-first.

```
// counting sort, values in [0, R)
function countingSort(A, R):
    cnt = array of R zeros
    for each x in A: cnt[x] += 1
    out = []
    for v in 0..R-1:
        repeat cnt[v] times: out.append(v)
    return out

// top-K frequent elements via frequency buckets (no heap, O(n))
function topKFrequent(A, K):
    cnt = {}
    for each x in A: cnt[x] = cnt.get(x,0)+1
    n = len(A)
    bucket = array of (n+1) empty lists       // bucket[f] = values with frequency f
    for v, f in cnt:
        bucket[f].append(v)
    res = []
    for f in n..1:                            // descending frequency
        for each v in bucket[f]:
            res.append(v)
            if len(res) == K: return res
    return res
```
**Complexity:** counting sort O(n+R); bucket top-k O(n) — beats heap's O(n log k) when an O(n)-size bucket array is acceptable.
**Traps:** • Counting sort dies on large/sparse ranges (R≫n) or non-integer keys — that's a radix/comparison-sort cue. • Frequency can't exceed n, so bucket size n+1 is exact. • Bucket sort for *real numbers in [0,1)* maps `value -> floor(value·n)`, sorts each bucket, then concatenates — average O(n), worst O(n²) if skewed.

> **Say this in the room:** "Bounded integer keys → counting/radix sort, O(n); top-k → frequency buckets give O(n) vs the heap's O(n log k)."

### Boyer-Moore majority vote
**LeetCode:** [Majority Element](https://leetcode.com/problems/majority-element/) · [Majority Element II](https://leetcode.com/problems/majority-element-ii/)
**When:** find an element appearing **> n/2** times (or the up-to-two appearing **> n/3**) in O(1) extra space — interviewers ask precisely because the hashmap solution is "too easy."
Keep a candidate and a counter. A matching element bumps the count; a non-match cancels one out; count 0 adopts the current element as the new candidate. The true majority survives all cancellations.

```
// > n/2 majority (assumes one exists; else verify with a 2nd pass)
function majority(A):
    cand = null; count = 0
    for each x in A:
        if count == 0: cand = x; count = 1
        elif x == cand: count += 1
        else: count -= 1
    return cand

// > n/3 : at most two such elements -> two candidates, two counters
function majorityN3(A):
    c1 = null; n1 = 0; c2 = null; n2 = 0
    for each x in A:
        if c1 != null and x == c1: n1 += 1
        elif c2 != null and x == c2: n2 += 1
        elif n1 == 0: c1 = x; n1 = 1
        elif n2 == 0: c2 = x; n2 = 1
        else: n1 -= 1; n2 -= 1
    // verify: count real occurrences of c1, c2; keep those > n/3
    return [c for c in (c1, c2) if c != null and realCount(A, c) > len(A)/3]
```
**Complexity:** time O(n) / space O(1).
**Traps:** • The decrement order in the n/3 version matters — check "is it c1?", "is it c2?", *then* the two "empty slot" cases, *then* decrement both; reordering breaks it. • > n/3 admits **at most two** answers; always do the verification pass since a guaranteed-majority assumption doesn't hold for the n/3 variant. • Initialize candidates to a sentinel and guard `c == cand` so a real value never accidentally equals the uninitialized candidate. • Generalizes: > n/k uses k−1 candidates (Misra–Gries).

### Bit manipulation toolkit
**LeetCode:** [Single Number](https://leetcode.com/problems/single-number/) · [Number of 1 Bits](https://leetcode.com/problems/number-of-1-bits/) · [Counting Bits](https://leetcode.com/problems/counting-bits/) · [Missing Number](https://leetcode.com/problems/missing-number/) · [Single Number II](https://leetcode.com/problems/single-number-ii/)
**When:** sets of ≤ 64 elements as a mask, parity/XOR puzzles ("the one number that appears once"), power-of-two checks, or squeezing memory/time. Pure integer arithmetic, no hashing.

```
// single-bit ops on integer x, bit i (0-indexed from LSB)
getBit(x, i)     = (x >> i) & 1
setBit(x, i)     = x | (1 << i)
clearBit(x, i)   = x & ~(1 << i)
toggleBit(x, i)  = x ^ (1 << i)

lowestSetBit(x)  = x & (-x)          // isolates the lowest 1-bit (two's complement)
clearLowest(x)   = x & (x - 1)       // turns off the lowest 1-bit
isPowerOfTwo(x)  = x > 0 and (x & (x - 1)) == 0

// popcount via Brian Kernighan: loops once per set bit
function popcount(x):
    c = 0
    while x != 0:
        x = x & (x - 1)              // drop lowest set bit
        c += 1
    return c
// (prefer the hardware popcount: Java Integer/Long.bitCount, Python int.bit_count() 3.10+)

// XOR family ------------------------------------------------------
// single number: every value twice except one  -> XOR all (a^a = 0)
function singleNumber(A):
    r = 0
    for each x in A: r = r ^ x
    return r

// missing number in 0..n: XOR indices and values
function missingNumber(A):
    r = len(A)
    for i in 0..len(A)-1: r = r ^ i ^ A[i]
    return r

// TWO single numbers (rest twice): XOR all, split by any set bit of the result
function twoSingles(A):
    xorAll = 0
    for each x in A: xorAll = xorAll ^ x   // = a ^ b, and a != b so xorAll != 0
    bit = xorAll & (-xorAll)               // a bit where a and b differ
    a = 0; b = 0
    for each x in A:
        if (x & bit) != 0: a = a ^ x       // group with that bit set
        else:              b = b ^ x       // group without
    return (a, b)

// build a number bit by bit (e.g. reverse 32 bits)
function reverseBits(x):
    r = 0
    for i in 0..31:
        r = (r << 1) | (x & 1)
        x = x >> 1
    return r
```
**Complexity:** all O(1) per scalar op; XOR scans O(n) / O(1) space; popcount O(set bits).
**Traps:** • **Overflow:** `1 << 31` overflows a signed 32-bit int — in Java use `1L << i` for masks beyond bit 30, or unsigned shift `>>>`; Python ints are arbitrary-precision so the danger inverts (no natural 32/64 wraparound — mask with `& 0xFFFFFFFF` when emulating fixed width). • **Signed shift:** Java `>>` is arithmetic (sign-extends); use `>>>` for logical/zero-fill when treating x as unsigned. • `x & -x` relies on two's-complement representation — fine in Java; in Python `-x` is conceptually two's-complement of an infinite-width int, so `x & -x` still gives the lowest set bit, but `~x` is `-x-1`. • XOR "two singles" needs the inputs to differ (they do, by problem statement) so `xorAll != 0` and a distinguishing bit exists. • **swap without temp:** `a ^= b; b ^= a; a ^= b` works but *discouraged* — unreadable, and breaks (zeros the value) if `a` and `b` are the same memory location; just use `swap(a,b)`.

### Subset / bitmask enumeration
**LeetCode:** [Subsets](https://leetcode.com/problems/subsets/) · [Subsets II](https://leetcode.com/problems/subsets-ii/) · [Maximum Product of Word Lengths](https://leetcode.com/problems/maximum-product-of-word-lengths/) · [Partition Equal Subset Sum](https://leetcode.com/problems/partition-equal-subset-sum/)
**When:** n is tiny (≤ ~20), and you must consider *every subset* of n items, or every *sub-subset* of a chosen set — the cue for bitmask DP (cross-ref §14) and exhaustive combinatorial search.
Represent a subset of n items as an n-bit integer (bit i set ⇔ item i included). Iterate masks `0..2^n−1` to hit all subsets; iterate **submasks** of a fixed mask with the `(sub-1) & mask` trick to visit only subsets of that mask.

```
// all 2^n subsets of n items
for mask in 0 .. (1<<n) - 1:
    members = []
    for i in 0..n-1:
        if (mask >> i) & 1: members.append(items[i])
    process(members)

// iterate all NON-EMPTY submasks of `mask` (descending)
sub = mask
while sub > 0:
    process(sub)
    sub = (sub - 1) & mask            // next lower submask; ends at 0 -> loop stops
// (to include the empty submask, handle 0 after the loop)

// sum over submasks pairs (mask, submask) is O(3^n) total over all masks, not O(4^n)

// Gray code: i-th value has exactly one bit changed from (i-1)-th
function grayCode(i): return i ^ (i >> 1)
function graySequence(n):
    return [ grayCode(i) for i in 0 .. (1<<n) - 1 ]
```
**Complexity:** all subsets O(2^n · n) (the inner bit scan); all (mask, submask) pairs across every mask O(3^n); Gray code O(2^n).
**Traps:** • `1 << n` for n=31 overflows signed 32-bit — use `1L << n` in Java; keep n ≤ ~20 anyway or 2^n is hopeless. • The submask loop *excludes* the empty set (stops when `sub` hits 0); add an explicit `process(0)` if you need it. • Submask enumeration over all masks is **O(3^n)**, not O(4^n) — each pair (i in mask, i in submask, i in neither) gives 3 states per element; quote this when justifying a SOS/bitmask-DP cost. • Gray code `i ^ (i>>1)` guarantees adjacent codes differ in exactly one bit — handy for "minimum-change" enumeration. • Don't confuse "iterate subsets of the universe" (`0..2^n-1`) with "iterate subsets of a specific set" (the `(sub-1)&mask` trick).

## 8. Stacks & queues

A stack handles **nesting and "most recent first"** (matching, evaluation, undo of decisions); a queue handles FIFO order. The high-value senior topic here is the **monotonic stack** ("next greater/smaller element" and everything built on it) and its sibling the **monotonic deque** (sliding-window extrema). Everything below amortizes to O(n) because each element is pushed and popped exactly once — say that out loud.

### Stack for matching / nesting
**LeetCode:** [Valid Parentheses](https://leetcode.com/problems/valid-parentheses/) · [Remove All Adjacent Duplicates In String](https://leetcode.com/problems/remove-all-adjacent-duplicates-in-string/) · [Minimum Remove to Make Valid Parentheses](https://leetcode.com/problems/minimum-remove-to-make-valid-parentheses/) · [Decode String](https://leetcode.com/problems/decode-string/) · [Basic Calculator II](https://leetcode.com/problems/basic-calculator-ii/)
**When:** brackets/tags must balance, an expression has nested parentheses, or a string is built from nested repeats — anything where the **most recently opened thing closes first** (LIFO).
Push openers / pending state; on a closer, pop and validate the match. The stack *is* the partial parse.

```
// valid parentheses: ()[]{}
function isValid(s):
    pair = {')':'(', ']':'[', '}':'{'}
    st = []
    for each ch in s:
        if ch in "([{": st.push(ch)
        else:
            if st.empty() or st.pop() != pair[ch]: return false
    return st.empty()                  // leftover openers = invalid

// min add to make parentheses valid: count unmatched
function minAddToMakeValid(s):
    open = 0; adds = 0
    for each ch in s:
        if ch == '(': open += 1
        else:
            if open > 0: open -= 1
            else: adds += 1            // unmatched ')' needs a '(' before it
    return adds + open                 // plus unmatched '('

// evaluate Reverse Polish Notation
function evalRPN(tokens):
    st = []
    for each t in tokens:
        if t is an operator:
            b = st.pop(); a = st.pop()    // ORDER: a OP b
            st.push(apply(t, a, b))
        else: st.push(int(t))
    return st.pop()

// basic calculator: + - with parentheses (no precedence between + and -)
function calculate(s):
    st = []; result = 0; sign = 1; i = 0
    while i < len(s):
        ch = s[i]
        if ch is a digit:
            num = parse the full number starting at i; advance i past it
            result += sign * num; continue
        elif ch == '+': sign = 1
        elif ch == '-': sign = -1
        elif ch == '(':
            st.push(result); st.push(sign)   // save context
            result = 0; sign = 1
        elif ch == ')':
            result = result * st.pop() + st.pop()   // pop sign, then saved result
        i += 1
    return result

// decode string: "3[a2[c]]" -> "accaccacc"
function decodeString(s):
    countSt = []; strSt = []; cur = ""; k = 0
    for each ch in s:
        if ch is a digit: k = k*10 + digit(ch)
        elif ch == '[':
            countSt.push(k); strSt.push(cur)
            k = 0; cur = ""
        elif ch == ']':
            rep = countSt.pop(); prev = strSt.pop()
            cur = prev + cur * rep
        else: cur = cur + ch
    return cur

// simplify unix path: /a/./b/../c -> /c
function simplifyPath(path):
    st = []
    for each part in path.split('/'):
        if part == "" or part == ".": continue
        elif part == "..":
            if not st.empty(): st.pop()      // go up; ignore if already at root
        else: st.push(part)
    return "/" + join(st, "/")

// asteroid collision: + right-moving, - left-moving
function asteroidCollision(A):
    st = []
    for each a in A:
        alive = true
        while alive and a < 0 and not st.empty() and st.top() > 0:
            if st.top() < -a: st.pop()             // top explodes, a continues
            elif st.top() == -a: st.pop(); alive = false   // both explode
            else: alive = false                    // a explodes
        if alive: st.push(a)
    return st

// remove all adjacent duplicates (collapse pairs)
function removeAdjacent(s):
    st = []
    for each ch in s:
        if not st.empty() and st.top() == ch: st.pop()
        else: st.push(ch)
    return join(st)
```
**Complexity:** time O(n) / space O(n).
**Traps:** • RPN/calculator: **operand order** — pop `b` first then `a`, compute `a OP b` (subtraction/division are non-commutative). • Decode-string: keep *separate* stacks for the repeat counts and the partial strings; multi-digit counts need `k = k*10 + digit`. • Calculator with `* /` and precedence is a *different, harder* problem (keep a running `prevNum` to apply `* /` immediately) — don't conflate it with the +/- version. • Unix path `..` at root is a no-op, not an error; trailing slash and `//` collapse to nothing. • Always check `st.empty()` before `pop()`/`top()`.

### Monotonic stack — the template
**LeetCode:** [Next Greater Element I](https://leetcode.com/problems/next-greater-element-i/) · [Daily Temperatures](https://leetcode.com/problems/daily-temperatures/) · [Online Stock Span](https://leetcode.com/problems/online-stock-span/) · [Sum of Subarray Minimums](https://leetcode.com/problems/sum-of-subarray-minimums/) · [Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/)
**When:** for each element, find the **nearest greater/smaller element** to one side, or any "span until a bigger value" — daily temperatures, histogram rectangles, stock span, trapping water, subarray-min sums.
Keep the stack **monotonic** (e.g. decreasing). Before pushing element i, pop everything the new element "dominates"; each popped element has just found *its* answer (i is its next-greater). Each index is pushed and popped once.

```
// NEXT GREATER ELEMENT to the right (strict). ans[i] = value of next strictly greater, else -1.
function nextGreater(A):
    n = len(A)
    ans = array of n filled with -1
    st = []                                  // holds INDICES; values NON-INCREASING bottom->top (equals stay)
    for i in 0..n-1:
        while not st.empty() and A[i] > A[st.top()]:
            j = st.pop()
            ans[j] = A[i]                    // i is the first element to the right > A[j]
        st.push(i)
    return ans
```
Flip the four knobs to get every variant:

| Want | Stack monotonic… | Pop while | Scan direction |
|---|---|---|---|
| next **greater** to right | non-increasing (equals stay) | `A[i] > A[top]` | left → right |
| next **greater-or-equal** to right | strictly decreasing | `A[i] >= A[top]` | left → right |
| next **smaller** to right | non-decreasing (equals stay) | `A[i] < A[top]` | left → right |
| **previous** greater (to left) | strictly decreasing | `A[i] >= A[top]` | left → right (answer = top after popping) |
| previous smaller (to left) | strictly increasing | `A[i] <= A[top]` | left → right |

(Equivalently, keep direction left→right and just change `>`/`>=`/`<`/`<=`, reading "next" off the popped element and "previous" off the surviving top.)

Applications built on this single template:

```
// daily temperatures: distance to a warmer day. Store indices; answer = i - j.
function dailyTemperatures(T):
    n = len(T); ans = array of n zeros; st = []
    for i in 0..n-1:
        while not st.empty() and T[i] > T[st.top()]:
            j = st.pop(); ans[j] = i - j
        st.push(i)
    return ans

// largest rectangle in histogram: for each bar, span = (next smaller) - (prev smaller) - 1
function largestRectangle(H):
    st = []                                  // indices, heights non-decreasing
    best = 0
    n = len(H)
    for i in 0..n:                           // i == n acts as a height-0 sentinel to flush
        h = (0 if i == n else H[i])
        while not st.empty() and h < H[st.top()]:
            top = st.pop()
            left = (-1 if st.empty() else st.top())   // prev smaller index
            width = i - left - 1                       // (next smaller = i)
            best = max(best, H[top] * width)
        st.push(i)
    return best

// maximal rectangle in 0/1 matrix: build per-row histogram heights, run the above each row
function maximalRectangle(M):
    if M empty: return 0
    cols = number of columns
    heights = array of cols zeros
    best = 0
    for each row in M:
        for c in 0..cols-1:
            heights[c] = (heights[c] + 1) if row[c] == 1 else 0   // reset on 0
        best = max(best, largestRectangle(heights))
    return best

// trapping rain water (stack of "walls"): water trapped on horizontal layers
function trapStack(H):
    st = []; water = 0
    for i in 0..len(H)-1:
        while not st.empty() and H[i] > H[st.top()]:
            bottom = st.pop()
            if st.empty(): break             // no left wall -> nothing traps
            left = st.top()
            width = i - left - 1
            bounded = min(H[left], H[i]) - H[bottom]
            water += width * bounded
        st.push(i)
    return water

// sum of subarray minimums: each A[i] is the min of (left span)*(right span) subarrays
function sumSubarrayMins(A):
    MOD = 1e9+7; n = len(A); st = []; total = 0
    // prevLess: strictly smaller to the left; nextLess: smaller-OR-equal to the right
    // (asymmetric strictness avoids double-counting equal values)
    prevLess = array of n; nextLess = array of n
    for i in 0..n-1:
        while not st.empty() and A[st.top()] >= A[i]: st.pop()
        prevLess[i] = (-1 if st.empty() else st.top()); st.push(i)
    st = []
    for i in n-1..0:
        while not st.empty() and A[st.top()] > A[i]: st.pop()
        nextLess[i] = (n if st.empty() else st.top()); st.push(i)
    for i in 0..n-1:
        left = i - prevLess[i]; right = nextLess[i] - i
        total = (total + A[i] * left * right) % MOD
    return total

// online stock span: consecutive days <= today's price (monotonic stack of (price, span))
function StockSpan:
    st = []                                  // (price, span), prices strictly decreasing
    function next(price):
        span = 1
        while not st.empty() and st.top().price <= price:
            span += st.pop().span            // absorb spans of dominated days
        st.push((price, span))
        return span
```
**Complexity:** every variant **amortized O(n)** time / O(n) space — each index enters and leaves the stack once. Maximal rectangle is O(rows·cols).
**Traps:** • **Store indices, not values**, whenever you need a distance/width (temperatures, histogram). • Strict vs non-strict (`>` vs `>=`) controls how *equal* values are treated; for "sum of subarray minimums" you **must** use strict on one side and non-strict on the other or equal minima are double-counted. • Histogram needs a **sentinel** (loop to `i == n` with height 0, or push −1 first) to flush the remaining increasing stack. • Trapping water: if the stack empties after popping the bottom, there's no left wall — break, don't add water. • "Previous smaller" reads off the **surviving** top after popping; "next smaller" reads off the **popped** element when the new one triggers the pop.

### Monotonic stack + greedy lexicographic
**LeetCode:** [Remove K Digits](https://leetcode.com/problems/remove-k-digits/) · [Remove Duplicate Letters](https://leetcode.com/problems/remove-duplicate-letters/) · [Smallest Subsequence of Distinct Characters](https://leetcode.com/problems/smallest-subsequence-of-distinct-characters/)
**When:** build the **lexicographically smallest** result by deleting/keeping characters under a budget or a uniqueness constraint — "remove k digits to minimize", "smallest subsequence with all distinct letters".
Greedily pop a larger element off the stack when a smaller one arrives *and* you're still allowed to (budget left, or the popped char appears again later). The stack is your answer-in-progress, kept as small-as-possible from the front.

```
// remove k digits to make the smallest number
function removeKdigits(num, k):
    st = []
    for each d in num:
        while k > 0 and not st.empty() and st.top() > d:
            st.pop(); k -= 1                 // dropping a larger leading digit helps
        st.push(d)
    while k > 0: st.pop(); k -= 1            // budget left -> trim from the back
    res = join(st) with leading zeros stripped
    return "0" if res is empty else res

// remove duplicate letters -> smallest subsequence containing each letter once
function removeDuplicateLetters(s):
    lastIdx = {}; for i in 0..len(s)-1: lastIdx[s[i]] = i   // last occurrence
    inStack = {}; st = []
    for i in 0..len(s)-1:
        c = s[i]
        if c in inStack: continue            // already placed
        while not st.empty() and st.top() > c and lastIdx[st.top()] > i:
            removed = st.pop(); inStack.remove(removed)   // safe: it reappears later
        st.push(c); inStack.add(c)
    return join(st)
```
**Complexity:** time O(n) (each char pushed/popped once; alphabet-sized bookkeeping); space O(n).
**Traps:** • After the main loop, if budget `k` remains (input already non-decreasing), trim from the **back**. • Strip leading zeros and handle the empty-result → "0" edge case. • Duplicate-letters: only pop a char if it **occurs again later** (`lastIdx[top] > i`) — otherwise you'd lose your only copy; and skip chars already in the stack via an `inStack` set. • This is greedy + stack, not DP — the local "pop if it improves the prefix and you can afford it" choice is globally optimal here.

### Min-stack / max-stack
**LeetCode:** [Min Stack](https://leetcode.com/problems/min-stack/) · [Maximum Frequency Stack](https://leetcode.com/problems/maximum-frequency-stack/)
**When:** a stack that also returns its current minimum (or maximum) in **O(1)** — classic warm-up, and a building block elsewhere.
Either store `(value, minSoFar)` together on each push, or keep a second stack tracking the running min. Both give O(1) min, push, pop.

```
// approach A: each entry carries the running min
function MinStack:
    st = []                                  // entries (val, curMin)
    function push(x):
        m = x if st.empty() else min(x, st.top().curMin)
        st.push((x, m))
    function pop(): st.pop()
    function top(): return st.top().val
    function getMin(): return st.top().curMin

// approach B: auxiliary min-stack (push to it only when <= current min)
function MinStack2:
    st = []; mins = []
    function push(x):
        st.push(x)
        if mins.empty() or x <= mins.top(): mins.push(x)
    function pop():
        x = st.pop()
        if x == mins.top(): mins.pop()
    function getMin(): return mins.top()
```
**Complexity:** all ops O(1) / space O(n).
**Traps:** • Approach B must push to `mins` on **`<=`** (not `<`) and pop on equality, or duplicate minima desync the two stacks. • A max-stack is the mirror image (track `maxSoFar`, push on `>=`). • A *queue* with O(1) min is harder — use a monotonic deque (next item).

### Monotonic deque
**LeetCode:** [Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/) · [Shortest Subarray with Sum at Least K](https://leetcode.com/problems/shortest-subarray-with-sum-at-least-k/) · [Constrained Subsequence Sum](https://leetcode.com/problems/constrained-subsequence-sum/) · [Jump Game VI](https://leetcode.com/problems/jump-game-vi/)
**When:** the **min or max over every fixed-size sliding window**, or the shortest subarray meeting a prefix-sum threshold — where a heap would be O(n log n) but a deque gives O(n). Cross-ref §4 for the window framing.
Keep a deque of **indices** whose values are monotonic. Before pushing i to the back, pop back while it's dominated (so the front is always the window's extreme); pop the front when it slides out of the window.

```
// sliding window maximum: max of each window of size k
function maxSlidingWindow(A, k):
    dq = deque of indices                    // A[dq.front] is the window max; values DECREASING
    out = []
    for i in 0..len(A)-1:
        while not dq.empty() and A[dq.back()] <= A[i]: dq.popBack()   // i dominates them
        dq.pushBack(i)
        if dq.front() <= i - k: dq.popFront()                         // front fell out of window
        if i >= k - 1: out.append(A[dq.front()])                     // window fully formed
    return out

// shortest subarray with sum >= K (negatives allowed) — deque on prefix sums
function shortestSubarrayAtLeastK(A, K):
    n = len(A)
    P = prefix sums, P[0]=0, P[i]=A[0..i-1] sum         // length n+1
    dq = deque of indices into P                         // P values INCREASING
    best = INF
    for i in 0..n:
        while not dq.empty() and P[i] - P[dq.front()] >= K:
            best = min(best, i - dq.popFront())          // shortest -> consume from front
        while not dq.empty() and P[dq.back()] >= P[i]:
            dq.popBack()                                  // a larger-or-equal prefix is useless
        dq.pushBack(i)
    return -1 if best == INF else best
```
**Complexity:** time O(n) (each index pushed/popped once) / space O(k) or O(n).
**Traps:** • Store **indices** so you can both test the window bound (`front <= i-k`) and recover positions. • Window-max pops the back on **`<=`** (keep the leftmost among equals so it stays valid longer); strictness rarely changes correctness for max but affects which equal index survives. • Emit output only once the window is full (`i >= k-1`). • Shortest-subarray-≥-K is the deque's *hard* showcase: front-pop *consumes* candidates (they can't help a later, longer window), back-pop discards non-increasing prefixes — both are mandatory for O(n).

### Queue ⇄ stack inter-implementation
**LeetCode:** [Implement Queue using Stacks](https://leetcode.com/problems/implement-queue-using-stacks/) · [Implement Stack using Queues](https://leetcode.com/problems/implement-stack-using-queues/)
**When:** an interviewer pins you to the wrong primitive ("implement a queue using only stacks" / "a stack using only queues") — tests amortized-analysis fluency.
Queue-from-two-stacks: an `in` stack for pushes, an `out` stack for pops; transfer in→out only when `out` is empty (each element moves at most twice ⇒ amortized O(1)). Stack-from-two-queues: make one operation do the reversal work.

```
// queue using two stacks — amortized O(1) per op
function QueueFromStacks:
    inS = []; outS = []
    function push(x): inS.push(x)
    function pop():
        if outS.empty():
            while not inS.empty(): outS.push(inS.pop())   // reverse order once
        return outS.pop()
    function front():
        if outS.empty():
            while not inS.empty(): outS.push(inS.pop())
        return outS.top()

// stack using two queues — push O(n), pop O(1) (this variant)
function StackFromQueues:
    q1; q2
    function push(x):
        q2.push(x)
        while not q1.empty(): q2.push(q1.pop())   // new element ends up at the front
        swap(q1, q2)
    function pop(): return q1.pop()
    function top(): return q1.front()
```
**Complexity:** queue-from-stacks amortized **O(1)** all ops (each element pushed onto `out` once); stack-from-queues here is O(n) push / O(1) pop (the symmetric variant flips this).
**Traps:** • Queue-from-stacks: **only** transfer when `out` is empty — transferring eagerly destroys the amortized bound and the order. • Don't peek into the middle of `in`; `front()` must trigger the same lazy transfer as `pop()`. • Stack-from-queues forces O(n) on one side — decide whether push or pop should be the slow one and rotate accordingly.

### Monotonic stack vs monotonic deque

| | Monotonic **stack** | Monotonic **deque** |
|---|---|---|
| Question shape | nearest greater/smaller element; spans; histogram | min/max over a **sliding window** of fixed size k |
| Pop ends | back only (LIFO) | **both** ends — back to maintain monotonicity, front to evict expired |
| Eviction trigger | a dominating new element | element falls outside the window (`front <= i-k`) |
| Output timing | when an element is popped (it found its answer) | the front, once the window is full |
| Typical problems | daily temps, largest rectangle, trapping water, subarray mins, stock span | sliding-window max/min, shortest subarray ≥ K |

Both run in **amortized O(n)** for the same reason — *each index is pushed once and popped once*. Reach for the **stack** when the answer is "the next/previous element with property X"; reach for the **deque** when you need an extreme over a *moving window* and must also throw away elements that have aged out.

## 9. Linked lists

Linked-list problems are pure **pointer choreography** — no clever data structure, just careful reassignment under null and length edge cases. Two habits carry the whole section: use a **dummy head** so the real head needs no special case, and **draw the three or four pointers on paper before you code**. The node definitions used throughout:

```
Node  { val, next }          // singly linked
DNode { val, prev, next }    // doubly linked
```

> Say this in the room: "I'll add a dummy head so I never special-case the head node, and I'll save `next` before I rewire any pointer."

### Reverse a linked list
**LeetCode:** [Reverse Linked List](https://leetcode.com/problems/reverse-linked-list/) · [Reverse Linked List II](https://leetcode.com/problems/reverse-linked-list-ii/)
**When:** "reverse the list", or any problem that needs a reversed segment (palindrome check, reorder, k-group) as a sub-step.
Walk forward carrying three pointers — `prev`, `cur`, and a saved `nxt` — flipping each `next` backward. Recursion does the same by reversing the tail then pointing the tail's head back at you.

```
// iterative: prev/cur/next
function reverse(head):
    prev = null; cur = head
    while cur != null:
        nxt = cur.next        // SAVE before overwriting
        cur.next = prev       // flip
        prev = cur            // advance
        cur = nxt
    return prev               // new head (old tail)

// recursive
function reverseRec(head):
    if head == null or head.next == null: return head
    newHead = reverseRec(head.next)
    head.next.next = head     // tail points back to me
    head.next = null          // I become the new tail
    return newHead

// reverse only positions m..n (1-indexed, inclusive) — dummy head shines here
function reverseBetween(head, m, n):
    dummy = Node(0); dummy.next = head
    before = dummy
    for _ in 1..m-1: before = before.next     // node just before position m
    cur = before.next                          // first node of the segment
    for _ in 1..n-m:                           // splice each following node to the front
        moved = cur.next
        cur.next = moved.next
        moved.next = before.next
        before.next = moved
    return dummy.next

// reverse in groups of k (leave a trailing partial group as-is)
function reverseKGroup(head, k):
    dummy = Node(0); dummy.next = head
    groupPrev = dummy
    while true:
        kth = groupPrev
        for _ in 1..k:                         // find the k-th node ahead
            kth = kth.next
            if kth == null: return dummy.next  // fewer than k left -> done
        groupNext = kth.next
        // reverse [groupPrev.next .. kth] in place
        prev = groupNext; cur = groupPrev.next
        while cur != groupNext:
            nxt = cur.next; cur.next = prev; prev = cur; cur = nxt
        tmp = groupPrev.next                   // old first becomes group tail
        groupPrev.next = kth                   // wire prev block to new head
        groupPrev = tmp
    return dummy.next
```
**Complexity:** time O(n) / space O(1) iterative, O(n) recursive (call stack).
**Traps:** • **Save `cur.next` before overwriting it** — the #1 linked-list bug. • Iterative reverse returns `prev` (the *old tail*), not `head`. • Recursive reverse blows the stack on long lists — prefer iterative in production. • `reverseBetween`: the "splice to front" loop runs `n-m` times, not `n-m+1`. • k-group: only reverse *full* groups; verify k nodes exist *before* touching them.

### Fast & slow pointers (Floyd)
**LeetCode:** [Linked List Cycle](https://leetcode.com/problems/linked-list-cycle/) · [Middle of the Linked List](https://leetcode.com/problems/middle-of-the-linked-list/) · [Linked List Cycle II](https://leetcode.com/problems/linked-list-cycle-ii/) · [Find the Duplicate Number](https://leetcode.com/problems/find-the-duplicate-number/)
**When:** find the **middle**, detect a **cycle**, find the **cycle entry/length**, or any "tortoise and hare" structure (including the happy-number digit cycle).
Move `slow` one step and `fast` two. If they meet, there's a cycle; if `fast` falls off the end, there isn't. After a meeting, resetting one pointer to the head and stepping both by one lands them at the cycle's start (a number-theory identity, below).

```
// middle node (for even length, returns the SECOND middle)
function middle(head):
    slow = head; fast = head
    while fast != null and fast.next != null:
        slow = slow.next; fast = fast.next.next
    return slow

// detect cycle
function hasCycle(head):
    slow = head; fast = head
    while fast != null and fast.next != null:
        slow = slow.next; fast = fast.next.next
        if slow == fast: return true
    return false

// find cycle ENTRY node (null if no cycle)
// Math: let a = head->entry, b = entry->meet (along cycle), c = cycle length.
// Hare went 2(a+b) = a+b + n*c  =>  a = n*c - b  =>  a ≡ (c-b) mod c.
// So a steps from head == (remaining) steps from meet -> they collide at the entry.
function cycleEntry(head):
    slow = head; fast = head
    while fast != null and fast.next != null:
        slow = slow.next; fast = fast.next.next
        if slow == fast:                      // meeting point found
            p = head
            while p != slow:
                p = p.next; slow = slow.next
            return p                          // the entry node
    return null

// cycle length: after a meeting, loop slow until it returns
function cycleLength(meet):
    len = 1; p = meet.next
    while p != meet: p = p.next; len += 1
    return len

// happy number: same idea on the "sum of squared digits" successor function
function isHappy(n):
    slow = n; fast = n
    repeat:
        slow = squareDigitSum(slow)
        fast = squareDigitSum(squareDigitSum(fast))
        if fast == 1: return true
    until slow == fast
    return false                              // cycled without hitting 1
```
**Complexity:** time O(n) / space O(1).
**Traps:** • Loop guard is `fast != null and fast.next != null` — checking only `fast` dereferences null on the two-step. • For **even** length, this returns the *second* middle; to get the *first*, start `fast = head.next`. • Cycle-entry proof relies on `slow` having travelled exactly `a+b` and `fast` `2(a+b)`; don't try it without resetting *one* pointer to head. • Happy number is Floyd in disguise — the successor function eventually cycles, and `1` is its own fixed point.

### Merge sorted lists
**LeetCode:** [Merge Two Sorted Lists](https://leetcode.com/problems/merge-two-sorted-lists/) · [Merge k Sorted Lists](https://leetcode.com/problems/merge-k-sorted-lists/)
**When:** combine two (or k) already-sorted lists into one sorted list — a building block for merge sort on lists and the "merge k" classic.
For two lists, splice nodes onto a dummy-headed tail, always taking the smaller head. For k lists, either keep a **min-heap of the current heads** (see §11) or **divide & conquer** by pairwise-merging halves (see §12).

```
// merge two sorted lists
function mergeTwo(a, b):
    dummy = Node(0); tail = dummy
    while a != null and b != null:
        if a.val <= b.val: tail.next = a; a = a.next
        else:              tail.next = b; b = b.next
        tail = tail.next
    tail.next = a if a != null else b        // attach the non-empty remainder
    return dummy.next

// merge k sorted lists — min-heap of heads, O(N log k)  (see §11)
function mergeK(lists):
    h = min-heap keyed by node.val
    for each node in lists:
        if node != null: h.push(node)
    dummy = Node(0); tail = dummy
    while len(h) > 0:
        node = h.pop()                       // smallest current head
        tail.next = node; tail = node
        if node.next != null: h.push(node.next)
    tail.next = null
    return dummy.next

// merge k sorted lists — divide & conquer pairwise, O(N log k)  (see §12)
function mergeKDC(lists, lo, hi):
    if lo == hi: return lists[lo]
    if lo > hi:  return null
    mid = (lo + hi) / 2
    return mergeTwo(mergeKDC(lists, lo, mid), mergeKDC(lists, mid+1, hi))
```
**Complexity:** two-list merge O(n+m) / O(1). Merge-k: O(N log k) time (N = total nodes) for both heap and divide-and-conquer; heap uses O(k) extra space, D&C O(log k) stack.
**Traps:** • Use `<=` (not `<`) to keep the merge **stable**. • Don't forget to **attach the remaining tail** after one list empties. • Naively merging k lists one-by-one is O(N·k); the heap / D&C versions are O(N log k) — say which and why. • Heap must compare by `val`; if duplicate vals can't be ordered, add a tie-breaker so the comparator is total.

### Remove nodes
**LeetCode:** [Remove Linked List Elements](https://leetcode.com/problems/remove-linked-list-elements/) · [Remove Duplicates from Sorted List](https://leetcode.com/problems/remove-duplicates-from-sorted-list/) · [Remove Nth Node From End of List](https://leetcode.com/problems/remove-nth-node-from-end-of-list/) · [Remove Duplicates from Sorted List II](https://leetcode.com/problems/remove-duplicates-from-sorted-list-ii/)
**When:** delete the n-th node from the end, strip duplicates, or delete a node you only have a pointer to.
"n-th from end" is a two-pointer gap: advance one pointer n steps, then move both until it hits the end. Use a dummy head so deleting the actual head is uniform.

```
// remove n-th node from the END (1-indexed)
function removeNthFromEnd(head, n):
    dummy = Node(0); dummy.next = head
    fast = dummy; slow = dummy
    for _ in 1..n: fast = fast.next          // open a gap of n
    while fast.next != null:                  // move both; slow stops BEFORE target
        fast = fast.next; slow = slow.next
    slow.next = slow.next.next                // unlink
    return dummy.next

// remove duplicates from a SORTED list (keep one of each)
function dedupSorted(head):
    cur = head
    while cur != null and cur.next != null:
        if cur.next.val == cur.val: cur.next = cur.next.next   // skip the dup
        else: cur = cur.next
    return head

// remove ALL nodes that have duplicates from a SORTED list (keep only uniques)
function removeAllDupsSorted(head):
    dummy = Node(0); dummy.next = head
    prev = dummy; cur = head
    while cur != null:
        if cur.next != null and cur.next.val == cur.val:
            v = cur.val
            while cur != null and cur.val == v: cur = cur.next   // skip the whole run
            prev.next = cur
        else:
            prev = cur; cur = cur.next
    return dummy.next

// remove duplicates from an UNSORTED list (hash set)
function dedupUnsorted(head):
    seen = {}; dummy = Node(0); dummy.next = head; prev = dummy; cur = head
    while cur != null:
        if cur.val in seen: prev.next = cur.next
        else: seen.add(cur.val); prev = cur
        cur = cur.next
    return dummy.next

// delete a node given ONLY that node (not the tail) — copy-then-skip
function deleteGivenNode(node):
    node.val = node.next.val                  // steal successor's value
    node.next = node.next.next                // and unlink the successor
```
**Complexity:** O(n) time; O(1) space (O(n) for the unsorted-dedup set).
**Traps:** • "n-th from end" needs the **dummy head** so removing the first node isn't special; `slow` must stop on the node *before* the target. • In `dedupSorted`, advance `cur` only when you *didn't* delete (otherwise you skip a node). • "delete given only the node" can't delete the **tail** (no successor to copy) — call that out. • Unsorted dedup needs O(n) extra space; sorted dedup is O(1).

### Reorder / restructure
**LeetCode:** [Palindrome Linked List](https://leetcode.com/problems/palindrome-linked-list/) · [Odd Even Linked List](https://leetcode.com/problems/odd-even-linked-list/) · [Swap Nodes in Pairs](https://leetcode.com/problems/swap-nodes-in-pairs/) · [Rotate List](https://leetcode.com/problems/rotate-list/) · [Reorder List](https://leetcode.com/problems/reorder-list/)
**When:** palindrome check, the "L0→Ln→L1→Ln-1…" weave, odd/even regrouping, rotation, partitioning around a value, or pairwise swaps — composite problems solved by combining *find-middle*, *reverse*, and *merge*.
Most reduce to: split at the middle, reverse one part, then interleave or compare.

```
// palindrome list: find mid, reverse second half, compare
function isPalindrome(head):
    slow = head; fast = head
    while fast != null and fast.next != null: slow = slow.next; fast = fast.next.next
    second = reverse(slow)                    // reverse from the middle to the end
    p = head; q = second
    while q != null:
        if p.val != q.val: return false
        p = p.next; q = q.next
    return true                               // (optionally re-reverse to restore)

// reorder list: L0->Ln->L1->Ln-1->...   (split, reverse second half, weave)
function reorderList(head):
    if head == null or head.next == null: return
    slow = head; fast = head
    while fast.next != null and fast.next.next != null: slow = slow.next; fast = fast.next.next
    second = reverse(slow.next); slow.next = null   // cut first half off
    first = head
    while second != null:                     // interleave
        n1 = first.next; n2 = second.next
        first.next = second; second.next = n1
        first = n1; second = n2

// odd-even list: group odd-indexed nodes, then even-indexed, preserve relative order
function oddEvenList(head):
    if head == null: return head
    odd = head; even = head.next; evenHead = even
    while even != null and even.next != null:
        odd.next = even.next; odd = odd.next
        even.next = odd.next; even = even.next
    odd.next = evenHead                       // stitch even chain after odd chain
    return head

// rotate list right by k places
function rotateRight(head, k):
    if head == null or head.next == null: return head
    n = 1; tail = head
    while tail.next != null: tail = tail.next; n += 1     // length + tail
    k = k % n
    if k == 0: return head
    tail.next = head                          // close into a ring
    stepsToNewTail = n - k
    newTail = head
    for _ in 1..stepsToNewTail-1: newTail = newTail.next
    newHead = newTail.next; newTail.next = null
    return newHead

// partition list: nodes < x before nodes >= x, preserving relative order
function partition(head, x):
    lessD = Node(0); less = lessD
    geD   = Node(0); ge = geD
    cur = head
    while cur != null:
        if cur.val < x: less.next = cur; less = cur
        else:           ge.next = cur;   ge = cur
        cur = cur.next
    ge.next = null                            // terminate the >= chain
    less.next = geD.next                      // splice the two chains
    return lessD.next

// swap nodes in pairs: (1,2)(3,4)...
function swapPairs(head):
    dummy = Node(0); dummy.next = head; prev = dummy
    while prev.next != null and prev.next.next != null:
        a = prev.next; b = a.next
        a.next = b.next; b.next = a; prev.next = b   // swap
        prev = a
    return dummy.next
```
**Complexity:** all O(n) time / O(1) space.
**Traps:** • Palindrome/reorder: **cut the first half** (`slow.next = null`) before reversing, or you create a cycle. • The midpoint split differs for even vs odd length — reorder uses `fast.next && fast.next.next` to stop the slow pointer at the end of the *first* half. • Rotate: `k %= n` first (k can exceed length); a ring + cut is cleaner than counting from the end. • Partition must **terminate the `>=` chain** (`ge.next = null`) or you leave a dangling tail / cycle. • Pairwise swap is easiest with a dummy head and a `prev` cursor.

### Intersection of two linked lists
**LeetCode:** [Intersection of Two Linked Lists](https://leetcode.com/problems/intersection-of-two-linked-lists/)
**When:** two lists share a common suffix (a Y-shape) and you must return the first shared node — by **reference**, not value.
Either align by the length difference and walk together, or use the two-pointer switch trick: each pointer, on reaching the end, jumps to the *other* list's head; they meet at the intersection after at most `lenA + lenB` steps (or both at null if disjoint).

```
// two-pointer switch trick (no length precompute)
function getIntersection(headA, headB):
    a = headA; b = headB
    while a != b:                             // reference comparison
        a = headB if a == null else a.next    // switch lists at the end
        b = headA if b == null else b.next
    return a                                  // the node, or null if no intersection

// length-difference variant
function getIntersectionLen(headA, headB):
    la = length(headA); lb = length(headB)
    while la > lb: headA = headA.next; la -= 1
    while lb > la: headB = headB.next; lb -= 1
    while headA != headB: headA = headA.next; headB = headB.next
    return headA
```
**Complexity:** time O(n+m) / space O(1).
**Traps:** • Compare nodes by **identity/reference**, never by value. • The switch trick naturally returns `null` when the lists don't intersect (both reach null simultaneously) — no extra check. • Intersection means a shared *node onward* (a common tail), not just equal values along the way.

### Copy list with random pointer
**LeetCode:** [Copy List with Random Pointer](https://leetcode.com/problems/copy-list-with-random-pointer/)
**When:** deep-copy a list where each node has a `next` *and* an arbitrary `random` pointer — you must clone the random links without knowing the order.
Two approaches: a hash map `original → clone` (two passes), or the O(1)-space **interleave-clone** trick — weave each clone right after its original, copy randoms via `orig.next.random = orig.random.next`, then unzip.

```
// hashmap approach (simple): node -> its clone
function copyRandomList(head):
    if head == null: return null
    clone = {}                               // original -> copy
    cur = head
    while cur != null: clone[cur] = Node(cur.val); cur = cur.next
    cur = head
    while cur != null:
        clone[cur].next   = clone.get(cur.next, null)
        clone[cur].random = clone.get(cur.random, null)
        cur = cur.next
    return clone[head]

// O(1)-space interleave-clone
function copyRandomListWeave(head):
    if head == null: return null
    // 1. insert clone after each original:  A -> A' -> B -> B' -> ...
    cur = head
    while cur != null:
        cp = Node(cur.val); cp.next = cur.next; cur.next = cp; cur = cp.next
    // 2. wire randoms: clone's random is original.random's clone
    cur = head
    while cur != null:
        if cur.random != null: cur.next.random = cur.random.next
        cur = cur.next.next
    // 3. unzip the two interleaved lists
    cur = head; cloneHead = head.next
    while cur != null:
        cp = cur.next; cur.next = cp.next
        cp.next = cp.next.next if cp.next != null else null
        cur = cur.next
    return cloneHead
```
**Complexity:** hashmap O(n) time / O(n) space; interleave O(n) time / **O(1)** extra space.
**Traps:** • A `random` may point to **null** or to *any* node (including itself) — guard before dereferencing. • Hashmap version: `clone.get(x, null)` handles null `next`/`random` uniformly. • Interleave version: in step 2 the clone of `cur.random` is `cur.random.next` (because clones sit *right after* originals); in step 3 you must **restore the original list's `next`** while extracting the clone, or you corrupt the input.

### Add two numbers (digits in lists)
**LeetCode:** [Add Two Numbers](https://leetcode.com/problems/add-two-numbers/) · [Add Two Numbers II](https://leetcode.com/problems/add-two-numbers-ii/)
**When:** two numbers stored as linked lists of digits — add them with carry, producing a new list. Digits are usually **least-significant-first** (forward add); if most-significant-first, reverse or use stacks.
Walk both lists, sum `d1 + d2 + carry`, push `sum % 10`, propagate `sum / 10`. A dummy head collects the result and a final carry may add one more node.

```
// digits least-significant-first (e.g. 2->4->3 is 342)
function addTwoNumbers(l1, l2):
    dummy = Node(0); tail = dummy; carry = 0
    while l1 != null or l2 != null or carry != 0:
        s = carry
        if l1 != null: s += l1.val; l1 = l1.next
        if l2 != null: s += l2.val; l2 = l2.next
        carry = s / 10                        // integer division
        tail.next = Node(s % 10); tail = tail.next
    return dummy.next

// digits MOST-significant-first: push to stacks, pop together, build result reversed
function addTwoNumbersMSB(l1, l2):
    s1 = stack of l1's vals; s2 = stack of l2's vals
    head = null; carry = 0
    while not s1.empty() or not s2.empty() or carry != 0:
        s = carry
        if not s1.empty(): s += s1.pop()
        if not s2.empty(): s += s2.pop()
        carry = s / 10
        node = Node(s % 10); node.next = head; head = node   // prepend
    return head
```
**Complexity:** time O(max(n,m)) / space O(max(n,m)) for the result.
**Traps:** • The loop condition includes **`carry != 0`** so a final carry (e.g. 99+1) produces a leading node. • Don't stop when one list ends — keep going with the longer one plus carry. • MSB-first: don't reverse the inputs unless allowed; the two-stack approach is non-destructive. • Strip any leading zero only if the problem says so (usually it doesn't, since the result is built clean).

### Core traps (apply to every problem here)
- **Save the `next` pointer before you overwrite it** — reversing/reordering loses the rest of the list otherwise.
- **Null-check at head and tail** — `head == null`, `head.next == null`, and the `fast`/`fast.next` guards in two-pointer walks.
- **Use a dummy head** whenever the *first* node might be inserted, deleted, or moved — it removes the head special case entirely.
- **Even vs odd length** changes which node "the middle" is; decide first-vs-second middle up front and set the pointer start accordingly.
- **Terminate dangling tails** (`...next = null`) after splitting or partitioning, or you accidentally create a cycle.
- **Reference vs value:** intersection, cycle detection, and copy-with-random all hinge on comparing *nodes*, not their `val`.

## 10. Trees

Binary-tree problems on a node `TreeNode { val, left, right }`. Everything here is some flavor of DFS (recursion or explicit stack), BFS (queue), or BST structure exploited. The single decision that unlocks 90% of tree problems:

> **Say this in the room:** "What does each recursive call *return*, and what do I *combine* from `left` and `right`?" Decide the return contract first — height, a boolean, a sum, a `(rob, skip)` pair — then the body writes itself: recurse on children, combine, return.

Two accumulator styles, pick deliberately (a recurring trap):
- **Returned value** — `dfs` returns the answer for its subtree; parent combines. Pure, no shared state. Default.
- **Global / outer accumulator** — `dfs` returns one thing (e.g. height) but *updates* an outer `best` as a side effect. Needed when the quantity you report up (height) differs from the quantity you maximize (a bending path). Diameter, max-path-sum, longest-univalue-path all use this split.

`null` is the base case in nearly every template — handle it first, every time. N-ary trees swap `left/right` for a `children` list (note at end). Tries are a tree but specialized — see §18. Tree DP overlaps heavily with §14.

### DFS traversals — recursive (preorder / inorder / postorder)
**LeetCode:** [Maximum Depth of Binary Tree](https://leetcode.com/problems/maximum-depth-of-binary-tree/) · [Binary Tree Inorder Traversal](https://leetcode.com/problems/binary-tree-inorder-traversal/) · [Binary Tree Preorder Traversal](https://leetcode.com/problems/binary-tree-preorder-traversal/) · [Binary Tree Postorder Traversal](https://leetcode.com/problems/binary-tree-postorder-traversal/) · [Invert Binary Tree](https://leetcode.com/problems/invert-binary-tree/) · [Symmetric Tree](https://leetcode.com/problems/symmetric-tree/)
**When:** visit every node; the *position* of the visit relative to children is the whole point.
Pre = node before children; In = left, node, right (sorted order for a BST); Post = children before node (use when a node needs results from its subtrees, i.e. tree DP).
```
function preorder(node):
    if node == null: return
    visit(node.val)          // node first
    preorder(node.left)
    preorder(node.right)

function inorder(node):
    if node == null: return
    inorder(node.left)
    visit(node.val)          // node between
    inorder(node.right)

function postorder(node):
    if node == null: return
    postorder(node.left)
    postorder(node.right)
    visit(node.val)          // node last
```
**Complexity:** time O(n) / space O(h) recursion stack, h = height (O(n) skewed, O(log n) balanced).
**Traps:** • base case `null` must be first. • Recursion depth ≈ height — a degenerate (linked-list-shaped) tree can blow the stack; iterative or Morris dodges it. • "Inorder = sorted" holds **only for a BST**.

### DFS traversals — iterative with explicit stack
**LeetCode:** [Binary Tree Preorder Traversal](https://leetcode.com/problems/binary-tree-preorder-traversal/) · [Binary Tree Inorder Traversal](https://leetcode.com/problems/binary-tree-inorder-traversal/) · [Binary Tree Postorder Traversal](https://leetcode.com/problems/binary-tree-postorder-traversal/) · [Flatten Binary Tree to Linked List](https://leetcode.com/problems/flatten-binary-tree-to-linked-list/) · [Binary Search Tree Iterator](https://leetcode.com/problems/binary-search-tree-iterator/)
**When:** you must avoid recursion (deep tree / stack-overflow risk), or an interviewer asks "do it iteratively."
Simulate the call stack. Preorder is the easy one (push right then left so left pops first). Inorder needs a "go left as far as possible" loop. Postorder: either two stacks, or do a modified preorder (node, right, left) and reverse.
```
// PREORDER iterative
function preorderIter(root):
    if root == null: return
    st = []
    st.push(root)
    while not st.empty():
        node = st.pop()
        visit(node.val)
        if node.right != null: st.push(node.right)   // right first
        if node.left  != null: st.push(node.left)    // so left pops first

// INORDER iterative — the classic
function inorderIter(root):
    st = []
    cur = root
    while cur != null or not st.empty():
        while cur != null:           // dive left
            st.push(cur)
            cur = cur.left
        cur = st.pop()               // leftmost unvisited
        visit(cur.val)
        cur = cur.right              // then its right subtree

// POSTORDER iterative — modified preorder, then reverse
function postorderIter(root):
    if root == null: return
    st = []; out = []
    st.push(root)
    while not st.empty():
        node = st.pop()
        out.append(node.val)                          // root, right, left order
        if node.left  != null: st.push(node.left)
        if node.right != null: st.push(node.right)
    reverse(out)                                      // -> left, right, root
    for each v in out: visit(v)
```
**Complexity:** time O(n) / space O(h) for the stack (O(n) worst).
**Traps:** • Preorder pushes **right before left**. • Inorder: don't pop until you've exhausted the left spine. • Postorder one-stack-without-reversal exists but needs a "last visited" pointer and is error-prone under pressure — memorize the reverse trick instead. • `out` collects in reverse; reverse it (or push to a deque front).

### Morris inorder traversal (O(1) extra space)
**LeetCode:** [Binary Tree Inorder Traversal](https://leetcode.com/problems/binary-tree-inorder-traversal/) · [Recover Binary Search Tree](https://leetcode.com/problems/recover-binary-search-tree/)
**When:** strict O(1) auxiliary space (no recursion stack, no explicit stack) and you can mutate pointers temporarily.
Thread the tree: for each node with a left child, find its inorder predecessor (rightmost node of left subtree) and point that predecessor's right at the current node. The thread lets you climb back without a stack; on the second visit you undo it.
```
function morrisInorder(root):
    cur = root
    while cur != null:
        if cur.left == null:
            visit(cur.val)              // no left subtree: emit, go right
            cur = cur.right
        else:
            pred = cur.left             // find inorder predecessor
            while pred.right != null and pred.right != cur:
                pred = pred.right
            if pred.right == null:
                pred.right = cur        // create thread, then descend left
                cur = cur.left
            else:
                pred.right = null       // thread already there: unthread,
                visit(cur.val)          // emit, go right
                cur = cur.right
```
**Complexity:** time O(n) (each edge traversed ≤ 3×) / space **O(1)**.
**Traps:** • Mutates the tree mid-traversal — must restore (the `else` branch nulls the thread). Don't bail early or you leave the tree corrupted. • For preorder Morris, move the `visit` into the thread-creation branch. • Rarely required; mention it as the O(1)-space answer. Not thread-safe (it edits pointers).

### BFS / level order
**LeetCode:** [Binary Tree Level Order Traversal](https://leetcode.com/problems/binary-tree-level-order-traversal/) · [Average of Levels in Binary Tree](https://leetcode.com/problems/average-of-levels-in-binary-tree/) · [Binary Tree Level Order Traversal II](https://leetcode.com/problems/binary-tree-level-order-traversal-ii/) · [Binary Tree Zigzag Level Order Traversal](https://leetcode.com/problems/binary-tree-zigzag-level-order-traversal/) · [Binary Tree Right Side View](https://leetcode.com/problems/binary-tree-right-side-view/)
**When:** anything "per level," shortest path in an unweighted tree, or processing top-down breadth-first: level grouping, zigzag, right-side view, per-level average, connect next-right pointers.
Standard pattern: snapshot the queue size at the start of each level so you process exactly one level per outer iteration.
```
function levelOrder(root):
    levels = []
    if root == null: return levels
    q
    q.push(root)
    while not q.empty():
        sz = len(q)                     // freeze count for THIS level
        level = []
        for i in 0..sz-1:
            node = q.pop()
            level.append(node.val)
            if node.left  != null: q.push(node.left)
            if node.right != null: q.push(node.right)
        levels.append(level)            // for zigzag: reverse level on odd depth
    return levels
```
Variants on the same skeleton:
| Task | Tweak inside the `for` / after building `level` |
|---|---|
| Zigzag | reverse `level` when depth is odd before appending |
| Right-side view | take `level[last]` (or push right child first and grab first popped) |
| Per-level average | `sum(level) / sz` |
| Level sums / min / max | reduce `level` |
| Connect next-right (`next` ptr) | within the `for`, link previous popped node's `next` to current |
| Bottom-up level order | build top-down, then reverse `levels` |
**Complexity:** time O(n) / space O(w), w = max width (up to ~n/2 for the last level of a full tree).
**Traps:** • You MUST snapshot `sz = len(q)` before the inner loop — the queue grows as you enqueue children. • Don't enqueue `null` children (or guard on pop). • For "connect next-right" with O(1) space, use the established `next` pointers of the level above instead of a queue.

### BST operations
**LeetCode:** [Search in a Binary Search Tree](https://leetcode.com/problems/search-in-a-binary-search-tree/) · [Insert into a Binary Search Tree](https://leetcode.com/problems/insert-into-a-binary-search-tree/) · [Validate Binary Search Tree](https://leetcode.com/problems/validate-binary-search-tree/) · [Kth Smallest Element in a BST](https://leetcode.com/problems/kth-smallest-element-in-a-bst/) · [Delete Node in a BST](https://leetcode.com/problems/delete-node-in-a-bst/)
**When:** the tree is a **binary search tree** (left subtree < node < right subtree, by the problem's duplicate policy). Exploit ordering to get O(h) search/insert/delete and free sorted order via inorder.
```
function searchBST(node, key):
    while node != null and node.val != key:
        node = node.left if key < node.val else node.right
    return node                          // null if absent

function insertBST(node, key):
    if node == null: return new TreeNode(key)
    if key < node.val: node.left  = insertBST(node.left,  key)
    else:              node.right = insertBST(node.right, key)
    return node                          // re-link on the way up

function deleteBST(node, key):
    if node == null: return null
    if key < node.val:      node.left  = deleteBST(node.left,  key)
    elif key > node.val:    node.right = deleteBST(node.right, key)
    else:                                       // found it — 3 cases
        if node.left  == null: return node.right     // 0 or 1 child
        if node.right == null: return node.left
        succ = node.right                            // 2 children:
        while succ.left != null: succ = succ.left    // inorder successor
        node.val = succ.val                          // copy successor value
        node.right = deleteBST(node.right, succ.val) // delete successor
    return node
```
Validate BST — **bounds method** (the safe one; comparing only parent-child is the classic bug):
```
function isValidBST(node, lo = -INF, hi = +INF):
    if node == null: return true
    if node.val <= lo or node.val >= hi: return false       // strict if no dups
    return isValidBST(node.left,  lo, node.val)
       and isValidBST(node.right, node.val, hi)
```
kth smallest — inorder with a counter (stop early):
```
function kthSmallest(root, k):
    st = []; cur = root
    while cur != null or not st.empty():
        while cur != null: st.push(cur); cur = cur.left
        cur = st.pop()
        k = k - 1
        if k == 0: return cur.val
        cur = cur.right
    // for many repeated queries, augment each node with subtree size -> O(h) per query
```
LCA in a BST — walk down (no recursion into both sides needed):
```
function lcaBST(node, p, q):
    while node != null:
        if p < node.val and q < node.val:   node = node.left
        elif p > node.val and q > node.val: node = node.right
        else: return node                    // split point (or equals p/q)
```
Range sum of BST — prune branches outside `[lo, hi]`:
```
function rangeSumBST(node, lo, hi):
    if node == null: return 0
    if node.val < lo: return rangeSumBST(node.right, lo, hi)  // whole left < lo
    if node.val > hi: return rangeSumBST(node.left,  lo, hi)  // whole right > hi
    return node.val + rangeSumBST(node.left, lo, hi) + rangeSumBST(node.right, lo, hi)
```
Sorted array → height-balanced BST — pick the middle as root, recurse on halves:
```
function sortedArrayToBST(A, lo, hi):          // inclusive [lo, hi]
    if lo > hi: return null
    mid = (lo + hi) / 2                         // floor
    root = new TreeNode(A[mid])
    root.left  = sortedArrayToBST(A, lo, mid - 1)
    root.right = sortedArrayToBST(A, mid + 1, hi)
    return root
```
**Complexity:** search/insert/delete/LCA O(h) — O(log n) balanced, **O(n) if the BST degenerates** into a chain. Inorder/validate/range O(n). Array→BST O(n).
**Traps:** • **Decide the duplicate policy** (left ≤ vs strict <) and make validate match it. • Delete's two-child case: copy the *inorder successor* (smallest in right subtree) — or predecessor — then delete that node; forgetting the recursive re-delete leaves a dangling duplicate. • Validate with parent-child comparison only is WRONG (a deep node can violate a distant ancestor's bound) — pass `(lo, hi)`. • `mid = (lo+hi)/2` can overflow for huge indices; use `lo + (hi-lo)/2`.

### "Return info up" tree DP
**LeetCode:** [Balanced Binary Tree](https://leetcode.com/problems/balanced-binary-tree/) · [Diameter of Binary Tree](https://leetcode.com/problems/diameter-of-binary-tree/) · [House Robber III](https://leetcode.com/problems/house-robber-iii/) · [Longest Univalue Path](https://leetcode.com/problems/longest-univalue-path/) · [Binary Tree Maximum Path Sum](https://leetcode.com/problems/binary-tree-maximum-path-sum/)
**When:** the answer for a node is a small function of its children's answers, but the value you *report upward* may differ from the value you *optimize globally* (path bends, subtree must stay a single chain, etc.). Postorder by nature. Return a tiny tuple; keep the global best outside.
General shape:
```
best = -INF
function dfs(node):
    if node == null: return BASE          // e.g. 0 for height, (0,0) for rob/skip
    L = dfs(node.left)
    R = dfs(node.right)
    best = combine_for_answer(best, L, R, node)   // global: path may bend here
    return value_to_report_up(L, R, node)         // a SINGLE chain / one number
```
Concrete instances (all O(n) time, O(h) space):

**Height** — report up max child height + 1:
```
function height(node):
    if node == null: return 0
    return 1 + max(height(node.left), height(node.right))
```
**Diameter** (longest path in edges through any node):
```
diam = 0
function ddfs(node):
    if node == null: return 0
    L = ddfs(node.left); R = ddfs(node.right)
    diam = max(diam, L + R)            // path bends at node (count edges)
    return 1 + max(L, R)              // report a straight chain upward
```
**Is-balanced** — return height, or -1 as a "fail" sentinel that propagates:
```
function bdfs(node):
    if node == null: return 0
    L = bdfs(node.left);  if L == -1: return -1
    R = bdfs(node.right); if R == -1: return -1
    if abs(L - R) > 1: return -1
    return 1 + max(L, R)
// balanced iff bdfs(root) != -1
```
**Max path sum** (path may bend; node values can be negative):
```
best = -INF
function mps(node):
    if node == null: return 0
    L = max(0, mps(node.left))        // drop negative branches
    R = max(0, mps(node.right))
    best = max(best, node.val + L + R)   // best path bending at node
    return node.val + max(L, R)          // upward: pick one branch
```
**Count good nodes** (a node is "good" if no ancestor on its root-path is greater) — pass max-so-far down, count on the way:
```
count = 0
function good(node, maxSoFar):
    if node == null: return
    if node.val >= maxSoFar: count = count + 1
    m = max(maxSoFar, node.val)
    good(node.left, m); good(node.right, m)
```
**Longest univalue path** (longest edge-path of equal values):
```
best = 0
function uni(node):
    if node == null: return 0
    L = uni(node.left); R = uni(node.right)
    lArm = L + 1 if node.left  != null and node.left.val  == node.val else 0
    rArm = R + 1 if node.right != null and node.right.val == node.val else 0
    best = max(best, lArm + rArm)     // bend at node
    return max(lArm, rArm)            // one arm upward
```
**House Robber III** — return `(rob, skip)` = best if you rob this node vs skip it:
```
function rob(node):
    if node == null: return (0, 0)
    lr, ls = rob(node.left)
    rr, rs = rob(node.right)
    robHere  = node.val + ls + rs     // robbed -> children must be skipped
    skipHere = max(lr, ls) + max(rr, rs)  // skipped -> children free to choose
    return (robHere, skipHere)
// answer = max(rob(root))
```
**LCA of a general binary tree** — return the node if found in this subtree; first node whose two sides each return non-null is the LCA:
```
function lca(node, p, q):
    if node == null or node == p or node == q: return node
    L = lca(node.left,  p, q)
    R = lca(node.right, p, q)
    if L != null and R != null: return node   // p and q split here
    return L if L != null else R               // both on one side (or neither)
```
**Complexity:** all O(n) time / O(h) recursion space.
**Traps:** • Separate "report up" (one chain / single number) from "global best" (may bend) — conflating them is *the* classic bug. • Max-path-sum: clamp child contributions at 0 to drop negative branches, but the **bending** value still adds `node.val`. • Balanced via `-1` sentinel must short-circuit (`if L == -1: return -1`) — otherwise you recompute heights and lose the O(n). • Rob-III: a node "robbed" forces children skipped; a node "skipped" lets each child pick its own best (`max(childRob, childSkip)`).

### Construct a tree from traversals
**LeetCode:** [Convert Sorted Array to Binary Search Tree](https://leetcode.com/problems/convert-sorted-array-to-binary-search-tree/) · [Construct Binary Tree from Preorder and Inorder Traversal](https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/) · [Construct Binary Tree from Inorder and Postorder Traversal](https://leetcode.com/problems/construct-binary-tree-from-inorder-and-postorder-traversal/) · [Construct Binary Tree from Preorder and Postorder Traversal](https://leetcode.com/problems/construct-binary-tree-from-preorder-and-postorder-traversal/)
**When:** rebuild the unique tree from preorder+inorder, postorder+inorder, or a serialized form. Works because inorder splits left/right and pre/post identifies the root.
Preorder gives the root first; locate it in inorder to size the left subtree. Use a hash map `idx[val] -> index` for O(1) lookup, and a moving preorder pointer.
```
function buildFromPreIn(preorder, inorder):
    idx = {}                                  // value -> index in inorder
    for i in 0..len(inorder)-1: idx[inorder[i]] = i
    pre = 0                                    // pointer into preorder (outer)
    function build(inLo, inHi):               // inclusive bounds in inorder
        if inLo > inHi: return null
        rootVal = preorder[pre]; pre = pre + 1
        root = new TreeNode(rootVal)
        mid = idx[rootVal]
        root.left  = build(inLo, mid - 1)     // LEFT first (preorder order)
        root.right = build(mid + 1, inHi)
        return root
    return build(0, len(inorder) - 1)
```
Postorder+inorder: postorder gives the root *last*, so consume from the end and build **right before left**:
```
function buildFromPostIn(inorder, postorder):
    idx = {}; for i in 0..len(inorder)-1: idx[inorder[i]] = i
    post = len(postorder) - 1
    function build(inLo, inHi):
        if inLo > inHi: return null
        rootVal = postorder[post]; post = post - 1
        root = new TreeNode(rootVal)
        mid = idx[rootVal]
        root.right = build(mid + 1, inHi)     // RIGHT first (postorder reversed)
        root.left  = build(inLo, mid - 1)
        return root
    return build(0, len(inorder) - 1)
```
**Complexity:** time O(n) with the index map (O(n²) without it, from repeated linear searches) / space O(n).
**Traps:** • Pre/post pointer is a single shared cursor advancing in traversal order — pass it by reference / close over it, don't restart per call. • Postorder version recurses **right subtree first**. • Assumes **unique values** (the index map keys on value). • Preorder+postorder alone does **not** uniquely determine a tree (ambiguous for single-child nodes).

### Serialize / deserialize
**LeetCode:** [Subtree of Another Tree](https://leetcode.com/problems/subtree-of-another-tree/) · [Serialize and Deserialize BST](https://leetcode.com/problems/serialize-and-deserialize-bst/) · [Serialize and Deserialize Binary Tree](https://leetcode.com/problems/serialize-and-deserialize-binary-tree/)
**When:** persist or transmit a tree and rebuild it exactly. Preorder with explicit `null` markers is the cleanest; BFS works too.
Encode null as a sentinel so structure is recoverable. Preorder: emit node, then left, then right; rebuild by consuming the same stream.
```
NULL = "#"; SEP = ","

function serialize(root):                       // preorder
    out = []
    function dfs(node):
        if node == null: out.append(NULL); return
        out.append(str(node.val))
        dfs(node.left); dfs(node.right)
    dfs(root)
    return join(out, SEP)

function deserialize(data):
    tokens = split(data, SEP)
    pos = 0
    function build():
        tok = tokens[pos]; pos = pos + 1
        if tok == NULL: return null
        node = new TreeNode(parseInt(tok))
        node.left  = build()                    // same preorder order
        node.right = build()
        return node
    return build()
```
BFS variant (LeetCode "codec" format): enqueue real children, write `#` for null, rebuild level by level using a queue.
**Complexity:** time O(n) / space O(n) for the string and the recursion/queue.
**Traps:** • You MUST emit null markers — without them preorder alone is ambiguous (see construct trap). • Keep value parsing robust (negatives, multi-digit) — that's why a separator, not single chars. • Deserialize consumes tokens in the **same traversal order** as serialize produced them; a single shared `pos`/cursor. • BFS deserialize: don't enqueue the `#` placeholders as nodes.

### Path problems
**LeetCode:** [Path Sum](https://leetcode.com/problems/path-sum/) · [Binary Tree Paths](https://leetcode.com/problems/binary-tree-paths/) · [Sum Root to Leaf Numbers](https://leetcode.com/problems/sum-root-to-leaf-numbers/) · [Path Sum II](https://leetcode.com/problems/path-sum-ii/) · [Path Sum III](https://leetcode.com/problems/path-sum-iii/)
**When:** sum/collect along root-to-leaf paths, or count paths that may start and end anywhere. Root-to-leaf is plain DFS carrying a running sum; any-to-any uses prefix sums on the current path (a tree twist on the subarray-sum-equals-k trick, see §4/§7).
Root-to-leaf existence (`hasPathSum`):
```
function hasPathSum(node, target):
    if node == null: return false
    if node.left == null and node.right == null:   // leaf
        return node.val == target
    rem = target - node.val
    return hasPathSum(node.left, rem) or hasPathSum(node.right, rem)
```
All root-to-leaf paths summing to target (`pathSum II`) — backtrack the path list:
```
res = []
function dfs(node, rem, path):
    if node == null: return
    path.append(node.val)
    if node.left == null and node.right == null and rem == node.val:
        res.append(copy(path))                 // COPY — path is mutated after
    else:
        dfs(node.left,  rem - node.val, path)
        dfs(node.right, rem - node.val, path)
    path.pop()                                 // undo (backtrack)
```
Count paths (any node → any descendant) summing to target (`pathSum III`) — prefix sums along the current root-path:
```
function pathSumIII(root, target):
    prefix = {0: 1}                            // running-sum -> count, incl empty
    count = 0
    function dfs(node, cur):
        if node == null: return
        cur = cur + node.val
        count = count + prefix.get(cur - target, 0)   // earlier prefix that closes a path
        prefix[cur] = prefix.get(cur, 0) + 1
        dfs(node.left, cur); dfs(node.right, cur)
        prefix[cur] = prefix[cur] - 1          // REMOVE on exit — only count current path
    dfs(root, 0)
    return count
```
Binary tree paths (all root-to-leaf as strings) — same backtracking shape, join the path.
**Complexity:** hasPathSum O(n)/O(h). pathSum II O(n·h) worst (copying each path). pathSum III O(n) time / O(h) space (the prefix map is bounded by path depth).
**Traps:** • A **leaf** is `left == null AND right == null` — don't accept a half-empty node as a leaf, and don't subtract into a null child as if it were a valid path end. • pathSum II: **copy** `path` when you record it; you mutate it afterward. • Backtrack: every `path.append` / `prefix[cur]++` needs a matching pop / decrement on the way out — the prefix map must reflect *only the current root-path*, so decrement after recursing. • Seed `prefix = {0: 1}` so a path starting at the root counts. • Sums overflow on adversarial inputs — use wide integers.

### Comparison: recursive vs iterative vs Morris
| Approach | Aux space | Clarity | When to pick |
|---|---|---|---|
| Recursive DFS | O(h) call stack | highest — default | almost always; risk only on pathologically deep trees |
| Iterative + explicit stack | O(h) | medium (inorder/postorder fiddly) | recursion banned, or deep/skewed tree risking stack overflow |
| Morris | **O(1)** | lowest (mutates pointers) | hard O(1)-space requirement and mutation is acceptable |
| BFS (queue) | O(w) width | medium | level-by-level / shortest-unweighted / width-bound problems |

### N-ary trees (brief)
Node is `{ val, children[] }`. Traversals replace the two child calls with a loop `for each c in node.children: dfs(c)`. Preorder = visit then loop children; postorder = loop children then visit (no clean "inorder"). Level order is the same BFS with `for each c in node.children: q.push(c)`. Most binary-tree DP shapes (height, max-path) carry over by folding over `children` instead of combining exactly two. Tries (§18) are a specialized N-ary tree keyed by character.

### Section traps recap
- **`null` first** — every template's base case; forgetting it is the #1 tree bug.
- **Global vs returned accumulator** — separate "report up the chain" from "best that may bend"; conflating them silently breaks diameter / max-path-sum / univalue-path.
- **BST duplicate policy** — fix `<` vs `≤` once and make validate, insert, and delete agree.
- **Integer overflow in path sums** — adversarial trees overflow 32-bit; use wide ints, and clamp negative branches at 0 only for the *upward* return, not the bending total.
- **Validate-BST by parent-child only is wrong** — pass `(lo, hi)` bounds down.
- **Copy on record** — when collecting a mutable path, snapshot it; the live list is backtracked afterward.

## 11. Heaps & priority queues

A heap gives you the smallest (or largest) element in O(1) and pays O(log n) per insert/extract — use it whenever you repeatedly need the extreme of a *changing* multiset, or only the top-k of a stream you can't fully sort. Per the spec, **`h` is a min-heap by default**: `h.push(x)`, `h.pop()` removes & returns the min, `h.top()` peeks the min, `len(h)` is the size.

To get a **max-heap**: either your language's comparator/`max-heap` type, or push **negated keys** into the min-heap and negate on the way out (say "max-heap by negation" in the room). To order by a *field*, push tuples `(key, payload)` so the heap compares on `key` first.

> **Say this in the room:** "I don't need the whole thing sorted — I only need to repeatedly pull the extreme, so a heap beats a sort: O(n log k) for top-k instead of O(n log n), and it works on a stream."

### When heap vs sort vs balanced BST
**When:** deciding the right structure for "extremes / order statistics / top-k."
A heap gives *one* extreme cheaply but no ordered iteration and no arbitrary delete/search. A full sort gives everything ordered but costs O(n log n) and needs all data up front. A balanced BST (or order-statistics tree, see §19) gives ordered iteration, rank/select, and arbitrary delete — at higher constant factor.
| Need | Reach for | Cost |
|---|---|---|
| Repeated extract-min/max from a changing set | **heap** | O(log n) per op, O(1) peek |
| Top-k of a stream / k-largest, k unknown order among them | **size-k heap** | O(n log k) time, O(k) space |
| k-th order statistic, one shot, array in hand | **quickselect** (§12) | O(n) avg, O(n²) worst |
| Everything in sorted order | **sort** | O(n log n) |
| Ordered iteration + arbitrary insert/delete/search/rank | **balanced BST / order-stat tree** (§19) | O(log n) per op |
| Merge many sorted sources | **k-way merge heap** | O(N log k) |
| Running median / k-th in a sliding window | **two heaps** | O(log n) add |
**Traps:** • A heap is *not* sorted — popping all n elements to "sort" is heapsort at O(n log n), pointless if you could just `sort`. • Heap has no O(log n) search or arbitrary delete (see lazy-deletion trap). • If you need the *order among* the top-k, you still sort the k at the end (O(k log k)).

### Top-k largest / k smallest (size-k heap)
**LeetCode:** [Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/) · [Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/) · [Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/)
**When:** the k largest (or smallest) elements from a large or streaming sequence, and k ≪ n.
Counter-intuitive but key: for the **k largest**, keep a **min-heap of size k**. The heap's top is the *smallest of your current top-k* — the bouncer at the door. A new element only matters if it beats that smallest, so you compare against `h.top()` and conditionally swap.
```
function kLargest(stream, k):               // returns k largest, unordered
    h                                        // min-heap, capacity k
    for each x in stream:
        if len(h) < k:
            h.push(x)
        elif x > h.top():                    // x beats the current k-th largest
            h.pop()
            h.push(x)
    return h                                 // contents = k largest; sort if order needed
```
For **k smallest**, mirror it: a **max-heap of size k**, evict when `x < h.top()`.
**Complexity:** time O(n log k) / space O(k). vs full sort O(n log n); vs quickselect O(n) avg (but quickselect needs the whole array and mutates it — heap streams).
**Traps:** • **Min-heap for k-largest** (max-heap for k-smallest) — picking the wrong polarity keeps the wrong extreme and silently returns garbage. • Fill to size k first, *then* compare to top. • Result is unordered; sort the k if the caller wants ranked output. • Ties: with `>` you keep the first of equal values — fine for "any k largest," matters if the problem wants a specific tie-break.

### K closest points / kth largest in array / kth largest in a stream
**LeetCode:** [Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/) · [Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/) · [K Closest Points to Origin](https://leetcode.com/problems/k-closest-points-to-origin/)
**When:** order statistics — "the k closest to origin," "the k-th largest element," or maintaining the k-th largest as a stream grows.
All three are the size-k-heap pattern with the right key.
**K closest points to origin** — k-closest = k-smallest by distance → **max-heap of size k** keyed on squared distance (no `sqrt` needed; it's monotonic):
```
function kClosest(points, k):
    h                                        // MAX-heap of size k, key = dist²
    for each (x, y) in points:
        d = x*x + y*y
        if len(h) < k:
            h.push((d, x, y))
        elif d < h.top().key:                // closer than current farthest kept
            h.pop(); h.push((d, x, y))
    return h
```
**Kth largest in an array (one shot):** quickselect O(n) avg is optimal (§12); the heap answer is a size-k min-heap → its top is the k-th largest:
```
function kthLargestHeap(A, k):
    h                                        // min-heap size k
    for each x in A:
        h.push(x)
        if len(h) > k: h.pop()               // drop the smallest, keep k largest
    return h.top()                           // k-th largest
```
**Kth largest in a stream** (the LeetCode class): keep a **persistent** size-k min-heap across `add` calls; each `add` returns the current k-th largest = `h.top()`:
```
class KthLargest:
    init(k, initial):
        this.k = k; this.h                   // min-heap
        for each x in initial: this.add(x)
    add(val):
        this.h.push(val)
        if len(this.h) > this.k: this.h.pop()
        return this.h.top()                  // k-th largest so far
```
**Complexity:** k closest O(n log k); kth-largest array O(n) (quickselect) or O(n log k) (heap); stream O(log k) per `add`.
**Traps:** • Distances: compare **squared** distance — `sqrt` is wasteful and risks float error. • Kth-largest array: prefer quickselect when you have the full array and one query; use the heap when data streams or you want simplicity. • Stream: the heap is **kept between calls** — don't rebuild it. If `add` is called before k elements exist, `top()` is the smallest seen so far (define behavior per the prompt).

### K-way merge
**LeetCode:** [Merge k Sorted Lists](https://leetcode.com/problems/merge-k-sorted-lists/) · [Find K Pairs with Smallest Sums](https://leetcode.com/problems/find-k-pairs-with-smallest-sums/) · [Smallest Range Covering Elements from K Lists](https://leetcode.com/problems/smallest-range-covering-elements-from-k-lists/)
**When:** merge k already-sorted lists/arrays/streams into one sorted output; or any problem that's "advance the smallest frontier across k sources."
Min-heap holding one *current* element per list — `(value, listIdx, elemIdx)`. Pop the global min, emit it, push that list's next element. The heap never exceeds k entries.
```
function mergeKSorted(lists):               // lists[i] sorted ascending
    out = []
    h                                        // min-heap of (value, listIdx, elemIdx)
    for i in 0..len(lists)-1:
        if len(lists[i]) > 0:
            h.push((lists[i][0], i, 0))
    while not h.empty():
        val, li, ei = h.pop()
        out.append(val)
        if ei + 1 < len(lists[li]):
            h.push((lists[li][ei+1], li, ei+1))   // next from same list
    return out
```
For **merge k sorted linked lists**, push nodes `(node.val, idx, node)` and re-push `node.next`. Related order-statistics-over-k-sources problems use the same heap:
- **Smallest range covering an element from each of k lists:** heap of one element per list + track the current max across the heap; the range is `[h.top().value, curMax]`; pop the min, advance that list, update curMax, shrink the answer — stop when any list is exhausted.
- **K-th smallest in a sorted matrix** (rows & cols sorted): seed the heap with the first column (or first row), pop k-1 times pushing the right/down neighbor; or binary-search the value (see §5) for O(n log(range)).
- **Ugly number II / k-th smallest of a∙b structure:** min-heap + a `seen` set to dedupe generated candidates (e.g. multiply popped value by 2,3,5).
**Complexity:** time O(N log k) where N = total elements (each pushed/popped once) / space O(k). Beats repeated pairwise merge O(N·k).
**Traps:** • Carry `(listIdx, elemIdx)` (or the node) in the heap entry — you must know *which* list to advance after popping. • Guard empty lists at seeding and at the boundary (`ei+1 < len`). • Smallest-range: terminate as soon as **any** list runs out (you can no longer cover all k). • Ugly-number / matrix variants: **dedupe with a `seen` set** or you'll emit the same value via two parents.

### Two heaps — running median
**LeetCode:** [Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/) · [IPO](https://leetcode.com/problems/ipo/) · [Sliding Window Median](https://leetcode.com/problems/sliding-window-median/)
**When:** the median (or any near-middle order statistic) of a stream that keeps growing, with O(log n) inserts and O(1) median reads.
Split the data into a **max-heap `lo`** (smaller half) and a **min-heap `hi`** (larger half). Keep them size-balanced (`len(lo) == len(hi)` or `len(lo) == len(hi)+1`). The median is `lo.top()` (odd total) or the mean of the two tops (even). Each insert: push, cross-push to enforce ordering, then rebalance sizes.
```
lo                                           // MAX-heap (smaller half)
hi                                           // min-heap (larger half)

function addNum(x):
    if lo.empty() or x <= lo.top():
        lo.push(x)
    else:
        hi.push(x)
    // rebalance so |lo| - |hi| in {0, 1}
    if len(lo) > len(hi) + 1:
        hi.push(lo.pop())
    elif len(hi) > len(lo):
        lo.push(hi.pop())

function findMedian():
    if len(lo) > len(hi): return lo.top()                  // odd count
    return (lo.top() + hi.top()) / 2.0                     // even count
```
**Sliding-window median** (window of size w): same two heaps + **lazy deletion** — when an element leaves the window you can't delete it from the middle of a heap, so mark it removed in a `toDelete` count-map and purge it only when it surfaces at a heap top; rebalance counting *effective* sizes. (See lazy-deletion trap.)
**Complexity:** add O(log n) / median O(1) / space O(n). Sliding-window median O(n log w) with lazy deletion.
**Traps:** • `lo` is a **max-heap**, `hi` a **min-heap** — swapping them inverts the median. • Maintain the size invariant after *every* add (push to the bigger, then rebalance). • Even-count median averages the two tops — watch integer division and overflow (`lo.top() + hi.top()` can overflow; average as `lo.top() + (hi.top()-lo.top())/2` if needed). • Sliding window: heaps grow with stale entries; track *valid* sizes separately from raw `len(h)`.

### Greedy with a heap
**LeetCode:** [Last Stone Weight](https://leetcode.com/problems/last-stone-weight/) · [Reorganize String](https://leetcode.com/problems/reorganize-string/) · [Task Scheduler](https://leetcode.com/problems/task-scheduler/) · [Furthest Building You Can Reach](https://leetcode.com/problems/furthest-building-you-can-reach/) · [Single-Threaded CPU](https://leetcode.com/problems/single-threaded-cpu/)
**When:** a greedy that repeatedly needs "the current best/worst by some changing key" — schedule by remaining count, always merge the two cheapest, free the earliest-ending resource, etc.
Pattern: maintain a heap of candidates; each step pop the extreme, do work, push back the updated/new candidate.
**Task scheduler / reorganize string** — always emit the char with the **most remaining**, so use a **max-heap by count**; for the cooldown variant, hold popped items aside until their cooldown passes:
```
function reorganizeString(s):                // no two adjacent equal
    h                                         // MAX-heap of (count, char)
    for char c, count cnt in freq(s): h.push((cnt, c))
    res = []
    prev = null                               // last used (can't reuse immediately)
    while not h.empty():
        cnt, c = h.pop()                      // most frequent available
        res.append(c)
        if prev != null and prev.cnt > 0: h.push(prev)   // re-admit previous
        prev = (cnt - 1, c)                    // cools down one step
    return res if len(res) == len(s) else ""   // "" if impossible
```
**Minimum cost to connect ropes/sticks** — always combine the two cheapest (Huffman-style); min-heap, pop two, push their sum:
```
function connectRopes(sticks):
    h                                         // min-heap
    for each x in sticks: h.push(x)
    total = 0
    while len(h) > 1:
        a = h.pop(); b = h.pop()
        total = total + a + b
        h.push(a + b)
    return total
```
**Meeting rooms II** (min rooms needed) — sort by start; **min-heap of end times** = rooms in use; reuse a room if its end ≤ current start (cross-ref intervals, §6):
```
function minMeetingRooms(intervals):
    sort(intervals, key = start)
    h                                         // min-heap of end times (rooms busy)
    for each (s, e) in intervals:
        if not h.empty() and h.top() <= s:
            h.pop()                            // a room freed up — reuse it
        h.push(e)
    return len(h)                              // peak concurrency = rooms
```
**Single-threaded CPU** — sort tasks by available-time; min-heap of *available* tasks keyed by `(processingTime, index)`; advance time, pop the shortest available. **IPO** (maximize capital with k projects) — **two heaps**: a min-heap by capital to gate affordable projects, push their profits into a **max-heap**, take the most profitable affordable one, k times.
**Complexity:** generally O(n log n) (each item heap-pushed/popped once). Meeting rooms II O(n log n) dominated by the sort.
**Traps:** • Get the **comparator direction** right: max-heap for "emit the most frequent / most profitable," min-heap for "merge the cheapest / free the earliest." • Reorganize string: hold the just-used char aside one step (`prev`) before re-admitting — pushing it straight back lets it repeat adjacently. • Meeting rooms: compare `h.top() <= s` (a meeting ending exactly when another starts can share the room iff the problem treats end as exclusive). • IPO: gate by capital with one heap, *then* maximize profit with the other — a single heap can't do both keys.

### Heap in Dijkstra / Prim
**When:** shortest paths from a source on non-negative weights (Dijkstra) or minimum spanning tree (Prim) — both repeatedly extract the closest/cheapest frontier node. Full templates live in §15; the heap mechanics are identical to k-way merge: a min-heap keyed by tentative distance/edge weight, with the **stale-entry skip** below. Mentioned here because "Dijkstra with a priority queue" is the canonical heap-in-the-wild answer.

### Binary heap — implementation
**When:** you normally just **use** the standard-library heap (see §19) — but "implement a heap" / "build a priority queue" is a classic ask, so know the array-backed internals cold. *(stdlib normally; shown because it is a common implement-it ask.)*
A binary heap is a complete binary tree stored in an array, no pointers. For a **min-heap** with 0-indexed array `H`: `parent(i) = (i-1)/2` (floor), `left(i) = 2i+1`, `right(i) = 2i+2`. The invariant: every parent ≤ its children. **Push** appends then *sifts up*; **pop** swaps root with last, shrinks, then *sifts down*. **Build-heap** sifts down from the last internal node up — O(n), not O(n log n).
```
H = []                                        // backing array, 0-indexed

function parent(i): return (i - 1) / 2        // floor
function left(i):   return 2*i + 1
function right(i):  return 2*i + 2

function peek():                              // min, O(1)
    return H[0]

function push(x):                             // O(log n)
    H.append(x)
    siftUp(len(H) - 1)

function siftUp(i):
    while i > 0 and H[i] < H[parent(i)]:      // smaller than parent -> rise
        swap(H[i], H[parent(i)])
        i = parent(i)

function pop():                               // remove & return min, O(log n)
    n = len(H)
    swap(H[0], H[n-1])                         // move last to root
    minVal = H.pop()                           // remove old root from the back
    if len(H) > 0: siftDown(0)
    return minVal

function siftDown(i):
    n = len(H)
    while true:
        l = left(i); r = right(i); smallest = i
        if l < n and H[l] < H[smallest]: smallest = l
        if r < n and H[r] < H[smallest]: smallest = r
        if smallest == i: break                // heap property restored
        swap(H[i], H[smallest])
        i = smallest

function buildHeap(A):                         // heapify an array in O(n)
    H = A
    for i in (len(H)/2 - 1) .. 0:              // last internal node down to root
        siftDown(i)
```
For a **max-heap**, flip every comparison (`>` for `<`) or store negated keys. To support **decrease-key** (needed by a "clean" Dijkstra), keep a `pos[item] -> index` map updated on every swap and sift up from that index — otherwise use lazy deletion (below).
**Complexity:** push O(log n), pop O(log n), peek O(1), **build-heap O(n)** (the geometric series over level heights sums to O(n), not O(n log n)) / space O(n).
**Traps:** • **Index math** is the whole game — `parent=(i-1)/2`, `left=2i+1`, `right=2i+2`; off-by-one here corrupts everything. • Pop: swap root with **last**, pop the back, *then* sift down — don't sift down a hole. • Build-heap is O(n) only when you sift **down** from `n/2−1` to 0; sifting up from each leaf is O(n log n). • Bounds-check `l < n` / `r < n` before comparing (the last internal node may have only a left child). • No native arbitrary-delete or search — see lazy deletion.

### Section traps recap
- **Comparator direction** — the single most common heap bug. k-largest → **min**-heap; k-smallest → **max**-heap; "emit most X" → max-heap; "merge cheapest / free earliest" → min-heap. State your polarity out loud.
- **Stale / outdated entries (skip-if-outdated)** — Dijkstra and lazy-update heaps re-push improved keys without removing the old ones. On pop, **skip** entries that no longer match the current best (`if d > dist[u]: continue`). The heap may hold up to O(E) entries; correctness comes from skipping, not deleting.
- **No arbitrary delete → lazy deletion** — a binary heap can't remove a middle element in O(log n). Mark it deleted in a `toDelete` count-map; when a marked element bubbles to the top on `pop`/`peek`, discard it then. Track *effective* size separately from `len(h)`. This is how sliding-window median and "remove arbitrary task" work.
- **Stability is not guaranteed** — equal keys pop in arbitrary order. If ties must break deterministically, encode a tiebreaker (insertion index) into the key: push `(key, seq, payload)`.
- **Max-heap via negation** — pushing `-x` flips min↔max, but remember to negate back on pop, and beware overflow negating `INT_MIN`.

## 12. Recursion, divide & conquer, backtracking

Three faces of "solve the problem in terms of smaller versions of itself." Recursion is the mechanism; divide & conquer splits into independent subproblems and merges; backtracking explores a decision tree, undoing choices that fail. If subproblems overlap, this becomes DP — memoize (see §14).

### Recursion fundamentals
**When:** the problem has self-similar structure — a tree, a nested definition, "solve for n in terms of n−1," or a search over choices.
Write the **base case** (the smallest input you answer directly) and the **recursive case** (reduce toward the base, combine the sub-results). "Trust the recursion": assume the recursive call returns the correct answer for the smaller input and just use it — don't trace the whole tree in your head.
```
function solve(input):
    if is_base_case(input):
        return base_answer
    sub = solve(smaller(input))     // trust it
    return combine(input, sub)
```
The **recursion tree** has one node per call; total work = (work per node) × (number of nodes). **Stack depth** = height of the deepest call chain — each frame holds locals + return address. Convert to **iterative** (explicit stack, or a loop) when depth can exceed the call-stack limit (~10⁴–10⁵ frames in most languages; the JVM throws `StackOverflowError`) — e.g. a skewed tree or a linked list of length 10⁶. **Tail recursion** (the recursive call is the very last operation, nothing pending after it) *could* run in O(1) stack, but the **JVM does NOT do tail-call optimization** — so in Java, rewrite a tail-recursive function as a `while` loop yourself.
**Complexity:** time = nodes × work/node / space = O(max depth) call stack (+ any heap state).
**Traps:** • Missing or wrong base case → infinite recursion / `StackOverflowError`. • Not making progress toward the base each call. • Deep linear recursion (length-n list / skewed tree) overflows the stack — go iterative. • Recomputing the same subproblem exponentially (Fibonacci) — that's the DP signal, memoize. • Returning into a shared mutable accumulator without restoring it (see backtracking).

### Backtracking — the general template
**When:** "generate / count all valid configurations," "find a configuration satisfying constraints" — subsets, permutations, combinations, board placements, partitions. The search space is a tree of partial solutions.
Build a candidate incrementally: **choose** an option, **explore** (recurse) deeper, then **un-choose** (undo the choice) so the next option starts from a clean state. Prune branches that can't possibly succeed.
```
function backtrack(state, path):
    if is_complete(path):
        record(copy(path))          // copy! path is mutated in place
        return
    for each choice in candidates(state):
        if not feasible(choice, state): continue   // prune
        make(choice, state, path)          // choose
        backtrack(state, path)             // explore
        undo(choice, state, path)          // un-choose
```
Always **record a copy** of `path` — it is mutated in place, so the saved reference would otherwise reflect later states.
**Complexity:** time = O(branching^depth) worst case (× cost to copy a solution); space = O(depth) recursion + output size.
**Traps:** • Forgetting to undo the choice → state leaks across branches. • Saving `path` by reference instead of a copy. • Pruning incorrectly drops valid solutions. • Off-by-one in the `start` index for combinations.

### Subsets (power set)
**LeetCode:** [Subsets](https://leetcode.com/problems/subsets/)
**When:** enumerate all 2ⁿ subsets of a set.
Each element is independently **in or out** — that's a binary decision per element. Two equivalent formulations.
```
// Include/exclude recursion
function subsets(A):
    res = []
    function dfs(i, cur):
        if i == len(A):
            res.append(copy(cur)); return
        dfs(i + 1, cur)                 // exclude A[i]
        cur.append(A[i]); dfs(i + 1, cur); cur.pop()   // include A[i]
    dfs(0, [])
    return res

// Iterative bitmask (see §7): bit j of mask = "include A[j]"
function subsets_bitmask(A):
    n = len(A); res = []
    for mask in 0..(1 << n) - 1:
        cur = []
        for j in 0..n-1:
            if (mask >> j) & 1: cur.append(A[j])
        res.append(cur)
    return res
```
**Complexity:** O(n · 2ⁿ) time (2ⁿ subsets, O(n) to build each), O(n) recursion depth.
**Traps:** • `1 << n` overflows 32-bit `int` for n ≥ 31 — use `long` (see §7 bit tricks). • Bitmask order differs from DFS order; fine if order is unconstrained.

### Subsets with duplicates
**LeetCode:** [Subsets II](https://leetcode.com/problems/subsets-ii/)
**When:** the input array has repeats and you must not emit duplicate subsets.
**Sort first**, then at each tree level skip a choice equal to the previous one you already tried *at this level*.
```
function subsetsWithDup(A):
    sort(A); res = []
    function dfs(start, cur):
        res.append(copy(cur))
        for i in start..len(A)-1:
            if i > start and A[i] == A[i-1]: continue   // skip dup at this level
            cur.append(A[i]); dfs(i + 1, cur); cur.pop()
    dfs(0, [])
    return res
```
**Complexity:** O(n · 2ⁿ) worst case.
**Traps:** • The skip condition is `i > start` (not `i > 0`) — it must allow the duplicate when it's the *first* pick at this level. • Must sort, or equal elements aren't adjacent.

### Permutations
**LeetCode:** [Permutations](https://leetcode.com/problems/permutations/)
**When:** enumerate all n! orderings.
Track which elements are already placed. Two idioms: a `used[]` boolean array, or **swap-in-place** (swap the chosen element to the front of the remaining suffix).
```
// used[] array
function permute(A):
    n = len(A); res = []; used = [false]*n
    function dfs(cur):
        if len(cur) == n: res.append(copy(cur)); return
        for i in 0..n-1:
            if used[i]: continue
            used[i] = true; cur.append(A[i])
            dfs(cur)
            cur.pop(); used[i] = false
    dfs([])
    return res

// swap-in-place: A[0..start-1] is fixed prefix
function permute_swap(A, start, res):
    if start == len(A): res.append(copy(A)); return
    for i in start..len(A)-1:
        swap(A[start], A[i])
        permute_swap(A, start + 1, res)
        swap(A[start], A[i])        // swap back
```
**Complexity:** O(n · n!) time, O(n) depth.
**Traps:** • Forgetting `used[i] = false` / the swap-back leaks state. • The swap-in-place variant mutates the input — copy on record. • Swap-in-place does NOT produce lexicographic order.

### Permutations with duplicates
**LeetCode:** [Permutations II](https://leetcode.com/problems/permutations-ii/)
**When:** input has repeats; emit each distinct permutation once.
Sort, use `used[]`, and skip a duplicate value unless its identical predecessor is currently used (this fixes a canonical order among equal elements).
```
function permuteUnique(A):
    sort(A); n = len(A); res = []; used = [false]*n
    function dfs(cur):
        if len(cur) == n: res.append(copy(cur)); return
        for i in 0..n-1:
            if used[i]: continue
            if i > 0 and A[i] == A[i-1] and not used[i-1]: continue  // skip dup
            used[i] = true; cur.append(A[i]); dfs(cur); cur.pop(); used[i] = false
    dfs([])
    return res
```
**Complexity:** O(n · n!) worst case (fewer when many dups).
**Traps:** • The condition is `not used[i-1]` — using `used[i-1]` instead double-counts. • Must sort.

### Combinations & combination sum
**LeetCode:** [Combinations](https://leetcode.com/problems/combinations/) · [Combination Sum](https://leetcode.com/problems/combination-sum/) · [Combination Sum II](https://leetcode.com/problems/combination-sum-ii/) · [Combination Sum III](https://leetcode.com/problems/combination-sum-iii/) · [Combination Sum IV](https://leetcode.com/problems/combination-sum-iv/)
**When:** choose k of n (combinations), or pick numbers summing to a target. Order doesn't matter → use a **start index** so you never go backward (kills permutation-duplicates for free).
```
// C(n, k): all k-subsets of 1..n
function combine(n, k):
    res = []
    function dfs(start, cur):
        if len(cur) == k: res.append(copy(cur)); return
        if len(cur) + (n - start + 1) < k: return   // prune: not enough left
        for i in start..n:
            cur.append(i); dfs(i + 1, cur); cur.pop()
    dfs(1, [])
    return res

// Combination Sum I: each number reusable, target sum
function combinationSum(C, target):
    sort(C); res = []
    function dfs(start, remain, cur):
        if remain == 0: res.append(copy(cur)); return
        for i in start..len(C)-1:
            if C[i] > remain: break          // sorted → no later one fits either
            cur.append(C[i])
            dfs(i, remain - C[i], cur)        // i (not i+1): reuse allowed
            cur.pop()
    dfs(0, target, [])
    return res

// Combination Sum II: each number once, input may have dups
function combinationSum2(C, target):
    sort(C); res = []
    function dfs(start, remain, cur):
        if remain == 0: res.append(copy(cur)); return
        for i in start..len(C)-1:
            if i > start and C[i] == C[i-1]: continue   // skip dup at this level
            if C[i] > remain: break
            cur.append(C[i]); dfs(i + 1, remain - C[i], cur); cur.pop()   // i+1: once
    dfs(0, target, [])
    return res
```
**Complexity:** combinations O(k · C(n,k)); combination-sum exponential, heavily pruned.
**Traps:** • Reuse-allowed recurses on `i`, each-once on `i + 1`. • Dedup skip is `i > start`, not `i > 0`. • Sorting enables the `break` prune; without it use `continue`. • Negative numbers break the `break`-on-`C[i] > remain` prune.

### N-Queens
**LeetCode:** [N-Queens](https://leetcode.com/problems/n-queens/) · [N-Queens II](https://leetcode.com/problems/n-queens-ii/)
**When:** place n non-attacking queens (or count arrangements). Classic constraint backtracking.
Place one queen per **row**; track occupied **columns** and both diagonals. For cell (r, c): the `↘` diagonal is constant on `r − c`, the `↗` on `r + c`. Use sets (or bitmasks) for O(1) conflict checks.
```
function solveNQueens(n):
    res = []; cols = {}; diag1 = {}; diag2 = {}; placement = []
    function dfs(r):
        if r == n: res.append(copy(placement)); return
        for c in 0..n-1:
            if c in cols or (r - c) in diag1 or (r + c) in diag2: continue
            cols.add(c); diag1.add(r - c); diag2.add(r + c); placement.append(c)
            dfs(r + 1)
            cols.remove(c); diag1.remove(r - c); diag2.remove(r + c); placement.pop()
    dfs(0)
    return res
```
**Complexity:** ~O(n!) with constraint pruning (far below n^n).
**Traps:** • `r − c` can be negative — a hash set is fine; for an array index shift by `+ (n−1)`. • Don't forget to undo all three sets. • For pure counting, just increment a counter at `r == n` and skip `placement`.

### Sudoku solver
**LeetCode:** [Valid Sudoku](https://leetcode.com/problems/valid-sudoku/) · [Sudoku Solver](https://leetcode.com/problems/sudoku-solver/)
**When:** fill a 9×9 grid obeying row/column/box constraints. Backtracking with **constraint propagation**.
Find the next empty cell, try digits 1–9 that don't violate a constraint, recurse, undo on failure. Strongest speedup: pick the empty cell with the **fewest legal candidates** (MRV heuristic) instead of scanning left-to-right.
```
function solveSudoku(board):
    function valid(r, c, d):
        for i in 0..8:
            if board[r][i] == d or board[i][c] == d: return false
            br = 3*(r/3) + i/3; bc = 3*(c/3) + i%3
            if board[br][bc] == d: return false
        return true
    function dfs():
        for r in 0..8:
            for c in 0..8:
                if board[r][c] == '.':
                    for d in '1'..'9':
                        if valid(r, c, d):
                            board[r][c] = d
                            if dfs(): return true
                            board[r][c] = '.'         // backtrack
                    return false           // no digit fits → dead end
        return true                        // no empty cell → solved
    dfs()
```
**Complexity:** exponential worst case; trivial in practice with propagation.
**Traps:** • Return `false` immediately when an empty cell has no valid digit (prune). • Box index math `3*(r/3)` uses integer division. • For speed keep per-row/col/box bitmasks instead of rescanning.

### Word search in a grid
**LeetCode:** [Word Search](https://leetcode.com/problems/word-search/) · [Word Search II](https://leetcode.com/problems/word-search-ii/)
**When:** does `word` exist as a path of orthogonally adjacent cells (no cell reused)? DFS with a **visited** backtrack.
Try each cell as a start; at each step match the current letter, mark visited, recurse to 4 neighbors, then **unmark** on the way back.
```
function exist(grid, word):
    R = len(grid); C = len(grid[0])
    function dfs(r, c, k):
        if k == len(word): return true
        if r < 0 or r >= R or c < 0 or c >= C: return false
        if grid[r][c] != word[k]: return false
        tmp = grid[r][c]; grid[r][c] = '#'     // mark visited in place
        found = dfs(r+1,c,k+1) or dfs(r-1,c,k+1) or dfs(r,c+1,k+1) or dfs(r,c-1,k+1)
        grid[r][c] = tmp                        // un-mark
        return found
    for r in 0..R-1:
        for c in 0..C-1:
            if dfs(r, c, 0): return true
    return false
```
**Complexity:** O(R·C · 4^L) worst, L = word length.
**Traps:** • Mark before recursing, restore after, or a path revisits a cell. • Check bounds and letter match *before* marking. • For many queries on one grid, prune with a letter-frequency check first.

### Palindrome partitioning
**LeetCode:** [Palindrome Partitioning](https://leetcode.com/problems/palindrome-partitioning/) · [Palindrome Partitioning II](https://leetcode.com/problems/palindrome-partitioning-ii/)
**When:** split a string into all possible lists of palindromic substrings.
At index `start`, try every prefix `s[start..i]`; if it's a palindrome, recurse on the rest. Precompute an `isPal[i][j]` DP table to make the check O(1).
```
function partition(s):
    n = len(s); res = []
    // isPal[i][j] = s[i..j] is a palindrome (inclusive)
    function dfs(start, cur):
        if start == n: res.append(copy(cur)); return
        for end in start..n-1:
            if isPal[start][end]:
                cur.append(s[start..end]); dfs(end + 1, cur); cur.pop()
    dfs(0, [])
    return res
```
**Complexity:** O(n · 2ⁿ) (up to 2^(n−1) partitions), + O(n²) precompute.
**Traps:** • Recompute palindromicity naively → O(n) per check blows up; precompute the table. • `end` is inclusive in the substring `s[start..end]`.

### Generate parentheses
**LeetCode:** [Generate Parentheses](https://leetcode.com/problems/generate-parentheses/)
**When:** all valid sequences of n pairs of balanced parentheses.
Track counts of `(` and `)` used. Add `(` while `open < n`; add `)` only while `close < open` (an invariant that guarantees balance — never close more than you've opened).
```
function generateParenthesis(n):
    res = []
    function dfs(s, open, close):
        if len(s) == 2*n: res.append(s); return
        if open < n: dfs(s + "(", open + 1, close)
        if close < open: dfs(s + ")", open, close + 1)
    dfs("", 0, 0)
    return res
```
**Complexity:** O(4ⁿ / √n) — the n-th Catalan number (see §13) of valid strings.
**Traps:** • The invariant `close < open` is what enforces validity — don't generate then filter. • Count is Catalan(n), useful to state in the room.

### Letter combinations of a phone number
**LeetCode:** [Letter Combinations of a Phone Number](https://leetcode.com/problems/letter-combinations-of-a-phone-number/)
**When:** map a digit string to all letter strings (telephone keypad). Cartesian product via backtracking.
Recurse digit by digit; for each, append every mapped letter.
```
PAD = {'2':"abc",'3':"def",'4':"ghi",'5':"jkl",'6':"mno",'7':"pqrs",'8':"tuv",'9':"wxyz"}
function letterCombinations(digits):
    if len(digits) == 0: return []
    res = []
    function dfs(i, cur):
        if i == len(digits): res.append(cur); return
        for ch in PAD[digits[i]]:
            dfs(i + 1, cur + ch)
    dfs(0, "")
    return res
```
**Complexity:** O(4ⁿ · n) (7/9 have 4 letters), n = number of digits.
**Traps:** • Empty input returns `[]`, not `[""]`. • Digits `0`/`1` have no letters — handle or assume absent.

### Restore IP addresses
**LeetCode:** [Restore IP Addresses](https://leetcode.com/problems/restore-ip-addresses/)
**When:** insert 3 dots into a digit string to form all valid IPv4 addresses.
Place exactly 4 segments; each segment is 1–3 digits, value 0–255, **no leading zero** unless the segment is exactly "0".
```
function restoreIpAddresses(s):
    res = []
    function dfs(start, seg, parts):
        if seg == 4:
            if start == len(s): res.append(join(parts, "."))
            return
        for L in 1..3:
            if start + L > len(s): break
            piece = s[start .. start+L-1]
            if (L > 1 and piece[0] == '0') or int(piece) > 255: continue
            parts.append(piece); dfs(start + L, seg + 1, parts); parts.pop()
    dfs(0, 0, [])
    return res
```
**Complexity:** O(1) — bounded search (≤ 3³ branches), constant output.
**Traps:** • Leading-zero rule: "0" valid, "00"/"01" invalid. • Must consume the *entire* string at exactly 4 segments. • `> 255` cut.

### Pruning — turning exponential into tractable
**When:** the naive search tree is too big; cut branches that cannot lead to a (better) solution.
Four reusable cuts: (1) **sort + skip duplicates** at each level (`if i > start and A[i] == A[i-1]: continue`); (2) **feasibility/bound cuts** — stop when the partial already violates a constraint (`remain < 0`, `cur.length + remaining < target`); (3) **early return** the moment a single answer is found (decision problems like Sudoku/Word Search); (4) **constraint propagation** — narrow remaining choices after each decision (N-Queens diagonal sets, Sudoku candidate masks). Good pruning changes the *effective* branching factor — e.g. N-Queens drops from n^n to ~n!, Combination Sum's `break`-when-`C[i] > remain` discards whole subtrees.
> Say this in the room: "Backtracking is DFS over a decision tree; the interview difficulty is almost always in the pruning, not the recursion. I sort to dedupe, bound on the partial cost, and propagate constraints."
**Traps:** • Over-aggressive pruning silently drops valid answers — prove the cut is sound. • Bound cuts need a monotone partial cost. • Sort-skip only works after sorting and with `i > start`.

### Divide & conquer — merge sort
**LeetCode:** [Merge Sorted Array](https://leetcode.com/problems/merge-sorted-array/) · [Sort an Array](https://leetcode.com/problems/sort-an-array/) · [Sort List](https://leetcode.com/problems/sort-list/)
**When:** stable O(n log n) sort, or any "split in half, solve each, merge" problem; the workhorse for counting inversions and external/linked-list sorting.
Split the array in half, recursively sort each, then **merge** two sorted halves in linear time.
```
function mergeSort(A, lo, hi):           // sorts A[lo..hi] inclusive
    if lo >= hi: return
    mid = (lo + hi) / 2
    mergeSort(A, lo, mid); mergeSort(A, mid + 1, hi)
    merge(A, lo, mid, hi)
function merge(A, lo, mid, hi):
    L = A[lo..mid]; R = A[mid+1..hi]; i = 0; j = 0; k = lo
    while i < len(L) and j < len(R):
        if L[i] <= R[j]: A[k] = L[i]; i++       // <= keeps it STABLE
        else: A[k] = R[j]; j++
        k++
    while i < len(L): A[k] = L[i]; i++; k++
    while j < len(R): A[k] = R[j]; j++; k++
```
**Complexity:** O(n log n) time always; O(n) auxiliary space (or O(log n) for a linked-list merge sort).
**Traps:** • `<=` in the compare keeps equal elements in order (stability). • `mid = (lo + hi) / 2` can overflow for huge indices — `lo + (hi - lo) / 2`. • Allocating new arrays each merge is fine; reusing one scratch buffer is faster.

### Divide & conquer — quicksort (Lomuto & Hoare)
**LeetCode:** [Sort Colors](https://leetcode.com/problems/sort-colors/) · [Sort an Array](https://leetcode.com/problems/sort-an-array/)
**When:** in-place, cache-friendly average-O(n log n) sort; the basis of quickselect. Use **randomized** pivots to avoid the O(n²) adversarial case.
Pick a pivot, **partition** so smaller elements go left and larger go right, recurse on both sides. Two partition schemes:
```
// Lomuto: pivot = last element; simpler, returns final pivot index
function lomuto(A, lo, hi):
    p = A[hi]; i = lo
    for j in lo..hi-1:
        if A[j] < p: swap(A[i], A[j]); i++
    swap(A[i], A[hi]); return i

// Hoare: pivot = A[lo]; fewer swaps, returns a split point j (not pivot's final pos)
function hoare(A, lo, hi):
    p = A[lo]; i = lo - 1; j = hi + 1
    while true:
        repeat i++ while A[i] < p
        repeat j-- while A[j] > p
        if i >= j: return j
        swap(A[i], A[j])

function quicksort(A, lo, hi):
    if lo >= hi: return
    swap(A[lo + rand(hi - lo + 1)], A[hi])   // randomize, then Lomuto on A[hi]
    p = lomuto(A, lo, hi)
    quicksort(A, lo, p - 1); quicksort(A, p + 1, hi)
```
**Complexity:** O(n log n) average, O(n²) worst (sorted input + fixed pivot), O(log n) stack with recursion on the smaller half.
**Traps:** • Lomuto returns the pivot's final index; Hoare returns a boundary — recurse `[lo,j]`/`[j+1,hi]` for Hoare, never `p+1` past a Hoare split. • Quicksort is **not stable**. • Always randomize (or median-of-three) the pivot. • Recurse on the smaller partition first / tail-loop the larger to bound stack depth.

### Quickselect — kth element in O(n) average
**LeetCode:** [Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/) · [Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/) · [K Closest Points to Origin](https://leetcode.com/problems/k-closest-points-to-origin/) · [Wiggle Sort II](https://leetcode.com/problems/wiggle-sort-ii/)
**When:** find the k-th smallest/largest (or the median) *without* fully sorting. Average O(n), beats a heap's O(n log k) when you need one rank.
Partition like quicksort, but recurse into **only the side** containing rank k.
```
function quickselect(A, k):              // k is 0-indexed: k-th smallest
    lo = 0; hi = len(A) - 1
    while lo < hi:
        swap(A[lo + rand(hi - lo + 1)], A[hi])
        p = lomuto(A, lo, hi)
        if p == k: return A[k]
        elif p < k: lo = p + 1
        else: hi = p - 1
    return A[lo]
```
**Complexity:** O(n) average, O(n²) worst (mitigated by randomization; median-of-medians guarantees O(n) but is rarely worth coding).
**Traps:** • Mixing 0-indexed k with "k-th largest" (largest = index `n − k`). • Without random pivots an adversary forces O(n²). • Mutates the array (partial reorder).

### Count inversions via merge sort
**LeetCode:** [Count of Smaller Numbers After Self](https://leetcode.com/problems/count-of-smaller-numbers-after-self/) · [Reverse Pairs](https://leetcode.com/problems/reverse-pairs/)
**When:** count pairs `i < j` with `A[i] > A[j]` (a "sortedness" measure). Piggyback on merge sort.
During the merge, when you take an element from the **right** half, every remaining element in the left half forms an inversion with it.
```
function countInv(A, lo, hi):
    if lo >= hi: return 0
    mid = (lo + hi) / 2
    inv = countInv(A, lo, mid) + countInv(A, mid + 1, hi)
    // merge, counting:
    i = lo; j = mid + 1; ...
    while i <= mid and j <= hi:
        if A[i] <= A[j]: take A[i]; i++
        else: take A[j]; j++; inv += (mid - i + 1)   // all of A[i..mid] > A[j]
    // drain remainders
    return inv
```
**Complexity:** O(n log n) time, O(n) space.
**Traps:** • The inversion count is added when consuming from the *right*, and it's `mid − i + 1` (the whole remaining left run). • Use `long` for the count — it can reach n(n−1)/2, ~5·10⁹ already at n = 10⁵, past 32-bit. • A BIT/Fenwick approach (see §18) also works.

### Maximum subarray (divide & conquer variant)
**LeetCode:** [Maximum Subarray](https://leetcode.com/problems/maximum-subarray/)
**When:** asked specifically for the D&C solution (interviewers love comparing it to Kadane's O(n)).
The best subarray is entirely in the left half, entirely in the right half, or **crosses the midpoint**. Compute all three and take the max; the crossing one is the best suffix of the left plus best prefix of the right.
```
function maxSub(A, lo, hi):
    if lo == hi: return A[lo]
    mid = (lo + hi) / 2
    left = maxSub(A, lo, mid)
    right = maxSub(A, mid + 1, hi)
    // best suffix ending at mid + best prefix starting at mid+1
    s = -INF; cur = 0
    for i in mid..lo step -1: cur += A[i]; s = max(s, cur)
    p = -INF; cur = 0
    for i in mid+1..hi: cur += A[i]; p = max(p, cur)
    return max(left, right, s + p)
```
**Complexity:** O(n log n) — strictly worse than Kadane's O(n); know both.
**Traps:** • All-negative arrays: the answer is the largest single element, so seed with `-INF`, never 0. • The cross sum must use a *contiguous* suffix+prefix through the midpoint.

### Reverse pairs
**LeetCode:** [Reverse Pairs](https://leetcode.com/problems/reverse-pairs/) · [Count of Smaller Numbers After Self](https://leetcode.com/problems/count-of-smaller-numbers-after-self/)
**When:** count pairs `i < j` with `A[i] > 2·A[j]` (the harder inversion variant). Modified merge sort.
Before the normal merge step, run a two-pointer count over the two sorted halves for the `A[i] > 2·A[j]` condition; then merge as usual.
```
function reversePairs(A, lo, hi):
    if lo >= hi: return 0
    mid = (lo + hi) / 2
    cnt = reversePairs(A, lo, mid) + reversePairs(A, mid + 1, hi)
    j = mid + 1
    for i in lo..mid:
        while j <= hi and A[i] > 2 * A[j]: j++
        cnt += (j - (mid + 1))
    merge(A, lo, mid, hi)        // standard merge to keep halves sorted
    return cnt
```
**Complexity:** O(n log n) time, O(n) space.
**Traps:** • `2 * A[j]` overflows 32-bit `int` — cast to `long` (e.g. `A[i] > 2L * A[j]`). • Both halves must be sorted before the count pass. • The count loop is monotone — `j` never resets.

### Closest pair of points
**When:** minimum Euclidean distance among n 2-D points in O(n log n) (the textbook D&C, occasionally asked at the hard tier).
Sort by x; split at the median; recursively get the best distance `d` in each half; then check only points within a **strip of width 2d** around the split line, sorted by y, where each point compares to at most ~7 neighbors.
**Complexity:** O(n log n).
**Traps:** • The "≤7 neighbors in the strip" bound is what keeps the merge linear; without it you regress to O(n²). • In an interview, stating the strip idea is usually enough — full code is rarely required.

### Master theorem — recurrence → complexity
**When:** you have `T(n) = a·T(n/b) + f(n)` (a ≥ 1 subproblems, each size n/b, plus f(n) to split/merge) and need its big-O fast.
Compare `f(n)` against `n^(log_b a)` (the "watershed"):

| Case | Condition | Result | Example |
|---|---|---|---|
| 1 (leaves dominate) | f(n) = O(n^(log_b a − ε)) | T(n) = Θ(n^(log_b a)) | binary tree traversal: a=2,b=2,f=O(1) → Θ(n) |
| 2 (balanced) | f(n) = Θ(n^(log_b a)) | T(n) = Θ(n^(log_b a) · log n) | merge sort: a=2,b=2,f=Θ(n) → Θ(n log n) |
| 3 (root dominates) | f(n) = Ω(n^(log_b a + ε)) (+ regularity) | T(n) = Θ(f(n)) | a=2,b=2,f=Θ(n²) → Θ(n²) |

Memorize the watershed exponent `log_b a` (a=b → 1; a=2,b=2 → 1; a=1,b=2 → 0). **T(n)=T(n/2)+O(1) → O(log n)** (binary search). **T(n)=2T(n/2)+O(n) → O(n log n)**. **T(n)=2T(n/2)+O(1) → O(n)**. **T(n)=T(n−1)+O(n) → O(n²)** (not master-form; expand the sum).
**Traps:** • Master theorem needs the `n/b` shape — `T(n)=T(n−1)+…` (decrement, not divide) does NOT apply; sum it directly. • Case 3 requires the extra regularity condition (`a·f(n/b) ≤ c·f(n)`), almost always satisfied for polynomial f. • Mind log vs polynomial: `f(n)=n·log n` falls in a gap none of the three cases covers (use the extended/Akra–Bazzi form).

### Backtracking vs DP — when to switch
**When:** your backtracking is timing out and you suspect repeated work.
If the recursion explores **independent** configurations (each distinct), it's genuinely exponential — backtracking is the answer and you prune. If it **revisits the same subproblem** (same arguments → same result), the subproblems *overlap* — memoize on those arguments and it collapses to DP (see §14). The tell: the recursive answer depends only on a small **state** (e.g. `(index, remaining)`), not on the full path taken to reach it.

| | Backtracking | DP / memoized recursion |
|---|---|---|
| Subproblems | distinct, must enumerate | overlapping, reusable |
| Goal | list / count *all* configs | one optimal value / a count |
| State | the whole partial path matters | a few parameters fully capture it |
| Move | choose → explore → **undo** | compute → **cache** by state |
| Cost | O(branching^depth) | O(states × transitions) |

**Traps:** • You can only memoize when the answer is a pure function of the state — if you must report the actual configuration, you still backtrack (optionally using a memoized feasibility check to prune). • Generation problems ("output every subset") are inherently exponential; memoization won't shrink the output.

## 13. Math & number theory

The recurring number-theory toolkit: GCD/LCM, modular arithmetic under `MOD = 1e9+7`, sieves and factorization, fast exponentiation (scalar and matrix), and combinatorics with modular inverses. Almost every "answer modulo 1e9+7" or "count the number of ways" problem leans on these. The two perennial traps: **integer overflow before you take the mod**, and **division under a modulus** (you need a modular inverse, not `/`).

### GCD (Euclidean) & LCM
**LeetCode:** [Find Greatest Common Divisor of Array](https://leetcode.com/problems/find-greatest-common-divisor-of-array/) · [Greatest Common Divisor of Strings](https://leetcode.com/problems/greatest-common-divisor-of-strings/) · [Water and Jug Problem](https://leetcode.com/problems/water-and-jug-problem/)
**When:** reduce fractions, find common periods, simplify ratios, or as a subroutine for modular inverse and CRT.
`gcd(a, b) = gcd(b, a mod b)`, terminating when the second argument hits 0. LCM derives from it: `lcm(a,b) = a / gcd(a,b) * b` (divide *before* multiply to avoid overflow).
```
function gcd(a, b):
    while b != 0:
        a, b = b, a mod b
    return a              // gcd(a,0)=a; works for a,b >= 0

function lcm(a, b):
    return a / gcd(a, b) * b      // divide first → smaller intermediate

function gcdArray(A):
    g = A[0]
    for i in 1..len(A)-1:
        g = gcd(g, A[i])
        if g == 1: break          // 1 is absorbing — can stop early
    return g
```
**Complexity:** O(log(min(a,b))) per gcd; array gcd O(n · log max).
**Traps:** • `gcd(0, 0)` is conventionally 0 — guard if that's invalid input. • For negatives, take `abs` first (sign of `mod` is language-dependent). • LCM overflows fast — divide by the gcd before multiplying, and use `long`. • Once the running gcd is 1, it stays 1 — early-exit.

### Extended Euclid & modular inverse
**When:** you need `a⁻¹ mod m` — i.e. to *divide* under a modulus, solve `a·x ≡ 1 (mod m)`, or solve linear Diophantine `a·x + b·y = gcd(a,b)`.
Extended Euclid returns `(g, x, y)` with `a·x + b·y = g`. The inverse exists **iff** `gcd(a, m) = 1`; then `x mod m` is the inverse. **Shortcut: if `m` is prime, use Fermat's little theorem** — `a⁻¹ ≡ a^(m−2) (mod m)` via fast power (simpler, no extended Euclid).
```
function extgcd(a, b):
    if b == 0: return (a, 1, 0)
    g, x1, y1 = extgcd(b, a mod b)
    return (g, y1, x1 - (a / b) * y1)

function modInverse(a, m):           // general m, needs gcd(a,m)=1
    g, x, _ = extgcd(a mod m, m)
    if g != 1: return null           // no inverse
    return ((x mod m) + m) mod m     // normalize to [0, m)

function modInversePrime(a, p):      // p prime — Fermat
    return modpow(a, p - 2, p)
```
**Complexity:** O(log m) either way.
**Traps:** • Fermat needs `p` **prime** and `a` not a multiple of `p`. • Normalize the result into `[0, m)` — extended Euclid can return a negative `x`. • No inverse exists when `gcd(a,m) ≠ 1` — never silently use `/`. • `MOD = 1e9+7` is prime, so prefer the Fermat one-liner in contests.

### Sieve of Eratosthenes (+ linear / SPF sieve)
**LeetCode:** [Count Primes](https://leetcode.com/problems/count-primes/) · [Closest Prime Numbers in Range](https://leetcode.com/problems/closest-prime-numbers-in-range/) · [Distinct Prime Factors of Product of Array](https://leetcode.com/problems/distinct-prime-factors-of-product-of-array/)
**When:** you need all primes up to N, or to factorize many numbers fast (precompute smallest prime factor).
Mark composites by crossing off multiples of each prime starting at `p²`. The **linear sieve** additionally records each number's **smallest prime factor (SPF)**, enabling O(log n) factorization afterward.
```
function sieve(n):                   // boolean primality up to n
    isPrime = [true]*(n+1); isPrime[0] = isPrime[1] = false
    for p in 2..floor(sqrt(n)):
        if isPrime[p]:
            for m in p*p..n step p:  // start at p*p; smaller multiples done
                isPrime[m] = false
    return isPrime

function spfSieve(n):                // smallest prime factor, linear time
    spf = [0]*(n+1); primes = []
    for i in 2..n:
        if spf[i] == 0: spf[i] = i; primes.append(i)
        for p in primes:
            if p > spf[i] or i*p > n: break
            spf[i*p] = p
    return spf                       // factorize x: divide by spf[x] repeatedly
```
**Complexity:** classic sieve O(n log log n); linear sieve O(n). Space O(n).
**Traps:** • Inner loop starts at `p*p` (smaller multiples already crossed) and `p` only runs to `√n`. • `p*p` overflows `int` near n≈46341 — use `long` or cap the outer loop. • Memory: a `boolean[N]` for N=10⁸ is ~100 MB — use a bitset or segmented sieve for huge N. • SPF sieve lets you factorize in O(log x), far faster than trial division per query.

### Primality test & factorization O(√n)
**LeetCode:** [Count Primes](https://leetcode.com/problems/count-primes/) · [Smallest Value After Replacing With Sum of Prime Factors](https://leetcode.com/problems/smallest-value-after-replacing-with-sum-of-prime-factors/)
**When:** test or factor a *single* number too large to sieve, or only a handful of queries.
Trial-divide by 2, then odd numbers up to `√n`; any leftover `> 1` is a prime factor itself.
```
function isPrime(n):
    if n < 2: return false
    if n % 2 == 0: return n == 2
    i = 3
    while i * i <= n:                // i*i <= n avoids float sqrt
        if n % i == 0: return false
        i += 2
    return true

function factorize(n):               // returns list of (prime, exponent)
    factors = []; d = 2
    while d * d <= n:
        if n % d == 0:
            e = 0
            while n % d == 0: n /= d; e++
            factors.append((d, e))
        d += 1
    if n > 1: factors.append((n, 1))   // leftover prime
    return factors
```
**Complexity:** O(√n) per call.
**Traps:** • Use `i*i <= n`, not `i <= sqrt(n)` (float rounding bugs). • Don't forget the trailing `n > 1` factor. • `i*i` can overflow — use `long`. • For n up to ~10¹⁸ use deterministic **Miller–Rabin** (mention it; √n is too slow). • `1` is neither prime nor composite.

### Divisor count & sum
**LeetCode:** [The k-th Factor of n](https://leetcode.com/problems/the-kth-factor-of-n/) · [Four Divisors](https://leetcode.com/problems/four-divisors/) · [Closest Divisors](https://leetcode.com/problems/closest-divisors/)
**When:** "how many divisors does n have," or sum of divisors — directly from the prime factorization.
If `n = Π pᵢ^eᵢ`, then **number of divisors** = `Π (eᵢ + 1)` and **sum of divisors** = `Π (pᵢ^(eᵢ+1) − 1)/(pᵢ − 1)`.
```
function numDivisors(n):
    cnt = 1
    for (p, e) in factorize(n):
        cnt *= (e + 1)
    return cnt
// sum: for each (p,e), term = (p^(e+1) - 1) / (p - 1); multiply terms.
```
For "divisor count of *every* i up to N," use a sieve-style accumulation: `for d in 1..N: for m in d..N step d: div[m]++` in O(N log N).
**Complexity:** O(√n) via factorization, or O(N log N) for all values up to N.
**Traps:** • These are multiplicative formulas over the factorization — don't enumerate divisors when you only need the count. • Sum-of-divisors needs modular inverse if computed under a modulus (the `/(p−1)`).

### Fast exponentiation (binary / modular pow)
**LeetCode:** [Pow(x, n)](https://leetcode.com/problems/powx-n/) · [Super Pow](https://leetcode.com/problems/super-pow/)
**When:** compute `base^exp` (or `base^exp mod m`) for large `exp` in O(log exp) — ubiquitous in modular combinatorics and as the engine for Fermat inverses.
Square the base and halve the exponent; multiply the answer in whenever the current bit is set.
```
function modpow(base, exp, mod):     // iterative
    result = 1; base = base mod mod
    while exp > 0:
        if exp & 1: result = (result * base) mod mod
        base = (base * base) mod mod
        exp >>= 1
    return result

function powRec(base, exp):           // recursive, no mod
    if exp == 0: return 1
    half = powRec(base, exp / 2)
    h2 = half * half
    return h2 * base if (exp & 1) else h2
```
**Complexity:** O(log exp) multiplications.
**Traps:** • `result * base` and `base * base` can each overflow before the mod — use 64-bit, and for `mod` near 10¹⁸ even `long*long` overflows (use 128-bit or mulmod). • Reduce `base mod mod` up front. • `exp = 0 → 1` (including `0^0 = 1` by convention here). • Negative `exp` only makes sense modularly via an inverse (see pow(x,n) below for the floating-point version).

### Matrix exponentiation (linear recurrences)
**When:** an n-th term of a linear recurrence with huge n (e.g. Fibonacci at n=10¹⁸) — anything expressible as `state_{k} = M · state_{k−1}`.
Encode the recurrence as a transition matrix `M`; then `state_n = M^n · state_0`, and `M^n` is computed by binary exponentiation on matrices (multiply matrices instead of scalars).
```
// Fibonacci: [F(n+1), F(n)] = [[1,1],[1,0]]^n · [F(1), F(0)]
function matmul(A, B, mod):          // k×k matrices
    C = zeros(k, k)
    for i in 0..k-1:
      for j in 0..k-1:
        s = 0
        for t in 0..k-1: s = (s + A[i][t] * B[t][j]) mod mod
        C[i][j] = s
    return C
function matpow(M, p, mod):
    R = identity(k)
    while p > 0:
        if p & 1: R = matmul(R, M, mod)
        M = matmul(M, M, mod); p >>= 1
    return R                          // fib(n) = matpow([[1,1],[1,0]], n)[0][1]
```
**Complexity:** O(k³ · log n) for a k×k transition matrix.
**Traps:** • Only works for **linear** recurrences with constant coefficients. • Index carefully — `M^n[0][1]` vs `[0][0]` depends on your state vector layout; verify against small n. • Accumulate products under the modulus inside `matmul` to avoid overflow.

### Modular arithmetic rules
**When:** any problem says "return the answer modulo 1e9+7" (the standard prime, fits in `int`, and `MOD² ≈ 10¹⁸` fits in `long`).
Add/sub/mul distribute over `mod`; **division does NOT** — replace `a / b` with `a · b⁻¹`. Fix negatives with `((x mod m) + m) mod m`.
```
MOD = 1000000007
function addm(a, b): return (a + b) mod MOD
function subm(a, b): return ((a - b) mod MOD + MOD) mod MOD   // keep non-negative
function mulm(a, b): return (a * b) mod MOD                   // use 64-bit product!
function divm(a, b): return mulm(a, modInversePrime(b, MOD))  // b ⁻¹ since MOD prime
```
**Identities:** `(a+b) mod m = ((a mod m)+(b mod m)) mod m`; same for `−` and `·`. `(a^b) mod m` via fast power. `(a/b) mod m = a·b⁻¹ mod m` only when `gcd(b,m)=1`.
**Traps:** • In Java, `a * b` is `int*int` → overflow at ~46341²; cast one operand to `long` (`1L * a * b`). • `a − b` can go negative → add `MOD` back before the final `mod`. • Never use `/` under a modulus — always the inverse. • `MOD = 1e9+7` is **prime** (and so is `998244353`, the NTT-friendly one); Fermat applies.

### Combinatorics — nCr with modular inverse
**LeetCode:** [Pascal's Triangle](https://leetcode.com/problems/pascals-triangle/) · [Unique Paths](https://leetcode.com/problems/unique-paths/)
**When:** count combinations/arrangements modulo a prime, often many queries → **precompute factorials** and their inverses once, then each nCr is O(1).
`C(n, r) = n! / (r! · (n−r)!)`; under a modulus that division becomes multiplication by modular inverses of the factorials.
```
function precompute(N):
    fact[0] = 1
    for i in 1..N: fact[i] = mulm(fact[i-1], i)
    invFact[N] = modInversePrime(fact[N], MOD)          // one inverse
    for i in N..1 step -1: invFact[i-1] = mulm(invFact[i], i)  // backward fill
function nCr(n, r):
    if r < 0 or r > n: return 0
    return mulm(fact[n], mulm(invFact[r], invFact[n-r]))
```
**Complexity:** O(N) precompute (a single inverse, then a backward sweep), O(1) per query.
**Traps:** • Boundary: `C(n,r)=0` for `r<0` or `r>n`. • Compute `invFact[N]` once and fill the rest with `invFact[i-1] = invFact[i]·i` — don't take N separate inverses. • For `n` huge but `MOD` small/prime use **Lucas' theorem** instead. • `0! = 1`.

### Pascal's triangle (DP)
**LeetCode:** [Pascal's Triangle](https://leetcode.com/problems/pascals-triangle/) · [Pascal's Triangle II](https://leetcode.com/problems/pascals-triangle-ii/)
**When:** you need all `C(n, r)` for small n (no modulus needed, or n ≤ ~60 so values fit), or want the additive recurrence.
`C(n, r) = C(n−1, r−1) + C(n−1, r)`, with `C(n, 0) = C(n, n) = 1` — pure addition, no inverses.
```
function pascal(N):
    C = zeros(N+1, N+1)
    for n in 0..N:
        C[n][0] = 1
        for r in 1..n:
            C[n][r] = C[n-1][r-1] + C[n-1][r]   // add MOD-wise if modular
    return C
```
**Complexity:** O(N²) time and space (O(N) if you only keep one row).
**Traps:** • Without a modulus, `C(n,r)` overflows 64-bit around n≈67 — use the factorial/modular method past that. • Roll a single 1-D row right-to-left to drop to O(N) space. • Symmetry `C(n,r)=C(n,n−r)`.

### Catalan numbers
**LeetCode:** [Unique Binary Search Trees](https://leetcode.com/problems/unique-binary-search-trees/) · [Different Ways to Add Parentheses](https://leetcode.com/problems/different-ways-to-add-parentheses/)
**When:** counting structures with a balanced/nested constraint: valid parenthesizations of n pairs (see §12 generate-parentheses), binary trees with n nodes, monotonic lattice paths under the diagonal, ways to triangulate a polygon, stack-sortable permutations.
Closed form `Cₙ = C(2n, n) / (n+1)`; DP recurrence `Cₙ = Σ_{i=0}^{n−1} Cᵢ · C_{n−1−i}`. First few: 1, 1, 2, 5, 14, 42, 132.
```
function catalanDP(n):
    C = [0]*(n+1); C[0] = 1
    for m in 1..n:
        for i in 0..m-1:
            C[m] = C[m] + C[i] * C[m-1-i]      // (mod if needed)
    return C[n]
// Modular closed form: Cn = nCr(2n, n) * modInversePrime(n+1, MOD)  (see nCr above)
```
**Complexity:** DP O(n²); closed form O(1) after factorial precompute.
**Traps:** • The closed form's `/(n+1)` needs a **modular inverse** under a modulus — don't integer-divide. • DP convolution index is `C[i]·C[m−1−i]` — off-by-one here is the classic bug. • Grows ~4ⁿ; overflows fast without a modulus.

### Misc identities & integer hygiene
**When:** quick closed forms, and avoiding the overflow / sign / float bugs that sink otherwise-correct math code.
Memorize: `Σ_{1..n} i = n(n+1)/2`; `Σ i² = n(n+1)(2n+1)/6`; arithmetic series `Σ = count·(first+last)/2`; geometric `Σ aᵏ (k=0..n−1) = a·(rⁿ−1)/(r−1)`.
```
function sumTo(n): return n * (n + 1) / 2          // use long: n=10^9 overflows int
function sumSquares(n): return n*(n+1)*(2*n+1) / 6
function floorDivNeg(a, b):                         // floor toward -INF, not toward 0
    q = a / b
    if (a mod b != 0) and ((a < 0) != (b < 0)): q -= 1
    return q
```
**Integer overflow:** in Java `int` caps at ~2.1·10⁹ — `n(n+1)/2` overflows `int` by n≈65536; use `long` and divide last. **Floor vs truncation:** language `/` truncates toward zero, so `−7 / 2 = −3`, but mathematical floor is `−4` — adjust for negatives (above). **Don't mix int and float** for exact arithmetic; floating point loses precision past 2⁵³.
**Traps:** • `n(n+1)/2` — multiply in `long`, divide by 2 at the end (the product is always even). • Negative `%`: Java's `−7 % 3 = −1` (sign of dividend) — use `((x % m) + m) % m` for a non-negative residue. • Comparing floats with `==` is unsafe; use an epsilon.

### Base conversion
**LeetCode:** [Add Binary](https://leetcode.com/problems/add-binary/) · [Base 7](https://leetcode.com/problems/base-7/) · [Excel Sheet Column Title](https://leetcode.com/problems/excel-sheet-column-title/) · [Excel Sheet Column Number](https://leetcode.com/problems/excel-sheet-column-number/) · [Convert to Base -2](https://leetcode.com/problems/convert-to-base-2/)
**When:** read/write a number in base b (binary, hex, base-26 "Excel columns," arbitrary radix).
To-base: repeatedly take `n mod b` (least-significant digit first), then reverse. From-base: Horner — `value = value · b + digit`.
```
function toBase(n, b):               // n >= 0
    if n == 0: return "0"
    digits = []
    while n > 0: digits.append(n mod b); n /= b
    reverse(digits); return digits   // map to chars as needed

function fromBase(s, b):
    val = 0
    for ch in s: val = val * b + digitValue(ch)
    return val
```
**Complexity:** O(number of digits) = O(log_b n).
**Traps:** • Handle `n == 0` explicitly (loop produces nothing). • Excel-column / "1-indexed" bases (A=1) need a `n -= 1` adjustment each step. • Negative numbers / signs are problem-specific.

### Digit operations (digit sum, reverse integer)
**LeetCode:** [Plus One](https://leetcode.com/problems/plus-one/) · [Palindrome Number](https://leetcode.com/problems/palindrome-number/) · [Reverse Integer](https://leetcode.com/problems/reverse-integer/) · [Add Digits](https://leetcode.com/problems/add-digits/) · [Happy Number](https://leetcode.com/problems/happy-number/)
**When:** sum/extract digits, reverse an integer, palindrome-number checks — with the signature **overflow-on-reverse** trap.
Peel digits with `% 10` and `/ 10`. Reversing a 32-bit int can overflow → check *before* the final multiply-add.
```
function digitSum(n):
    n = abs(n); s = 0
    while n > 0: s += n mod 10; n /= 10
    return s

function reverseInt(x):              // 32-bit signed; return 0 on overflow
    INT_MAX = 2147483647; INT_MIN = -2147483648
    rev = 0
    while x != 0:
        d = x mod 10                 // language %: keep sign consistent
        x /= 10
        if rev > INT_MAX / 10 or (rev == INT_MAX / 10 and d > 7): return 0
        if rev < INT_MIN / 10 or (rev == INT_MIN / 10 and d < -8): return 0
        rev = rev * 10 + d
    return rev
```
**Complexity:** O(log n).
**Traps:** • **Overflow on reverse** is the whole point — check against `INT_MAX/10` before `rev*10+d`. • Negative numbers: be consistent about how `%` and `/` treat sign. • `INT_MIN` has no positive counterpart — don't `abs` it blindly (it overflows).

### pow(x, n) — floating point with negative exponent
**LeetCode:** [Pow(x, n)](https://leetcode.com/problems/powx-n/)
**When:** `x^n` for real `x` and possibly negative integer `n` (the float cousin of fast exponentiation).
Binary exponentiation as before; for `n < 0` compute `1 / pow(x, −n)`. Guard the `−INT_MIN` overflow by promoting `n` to 64-bit first.
```
function myPow(x, n):
    N = long(n)
    if N < 0: x = 1.0 / x; N = -N    // safe: widened to long before negating
    result = 1.0
    while N > 0:
        if N & 1: result *= x
        x *= x; N >>= 1
    return result
```
**Complexity:** O(log |n|).
**Traps:** • `n = INT_MIN` → `−n` overflows `int`; widen to `long` *before* negating. • Floating-point error accumulates over many squarings — acceptable for the LeetCode tolerance, not for exact integer powers (use `modpow`). • `x = 0, n ≤ 0` is undefined.

### Integer sqrt via binary search
**LeetCode:** [Sqrt(x)](https://leetcode.com/problems/sqrtx/) · [Valid Perfect Square](https://leetcode.com/problems/valid-perfect-square/)
**When:** `floor(sqrt(n))` for integers without float rounding error, or as the canonical "binary search on the answer" warm-up (see §5).
Binary-search the largest `m` with `m·m ≤ n`.
```
function isqrt(n):                    // floor of sqrt, n >= 0
    if n < 2: return n
    lo = 1; hi = n; ans = 0
    while lo <= hi:
        mid = lo + (hi - lo) / 2
        if mid <= n / mid:           // mid*mid <= n, written to avoid overflow
            ans = mid; lo = mid + 1
        else: hi = mid - 1
    return ans
```
**Complexity:** O(log n).
**Traps:** • Write `mid <= n / mid` (or use `long`/128-bit) — `mid*mid` overflows for large n. • Newton's method converges faster but needs careful termination; binary search is the safe interview answer. • `n = 0, 1` are edge cases.

### Number-theory traps (summary)
- **Overflow before the mod:** `a * b` in 32-bit overflows at ~46341²; promote to 64-bit (`1L * a * b`) *before* taking `mod`. For `MOD ≈ 10¹⁸`, even `long·long` overflows — use 128-bit or a `mulmod`.
- **Negative modulo:** language `%` follows the dividend's sign in Java/C++ (`−7 % 3 = −1`). Normalize with `((x % m) + m) % m` whenever you need a residue in `[0, m)`.
- **Mixing int and float:** floats are exact only up to 2⁵³; never use `double` for big-integer arithmetic or `==` comparisons — keep modular math in integers.
- **Dividing under a modulus:** `(a / b) mod m ≠ (a mod m) / (b mod m)`. Multiply by `b⁻¹ mod m` (Fermat `b^(m−2)` when `m` prime); the inverse exists only if `gcd(b, m) = 1`.
- **Floor vs truncation with negatives:** `/` truncates toward zero; mathematical floor/ceil differ for negatives — adjust explicitly.

## 14. Dynamic programming

DP solves a problem by combining answers to **overlapping subproblems** under **optimal substructure**. This is the largest section: it covers the mindset, a problem-solving checklist, top-down vs bottom-up templates, a signature-to-family lookup, then every family you'll meet in a loop. If a problem says "count the ways", "min/max cost", "is it reachable", "best over two sequences", "choose a subset to hit a target", or has tiny `N` with an exponential brute force — reach here first.

### The mindset (read once, internalize)
Two conditions must both hold or DP doesn't apply:
- **Optimal substructure** — an optimal solution is built from optimal solutions to subproblems. (Greedy needs this too; DP needs the second condition as well.)
- **Overlapping subproblems** — the same subproblem is solved many times in the naive recursion. (If subproblems are all distinct, it's plain divide & conquer — see §12 — not DP; memoization buys nothing.)

The entire skill is **defining the state**: the *minimal* set of variables that uniquely identifies a subproblem, such that knowing the answers to "smaller" states lets you compute this one. Everything else (recurrence, base case, order) follows mechanically once the state is right. The five moving parts:

| Part | Question it answers |
|---|---|
| **State** | What is the *minimal* info that identifies one subproblem? `dp[i]`, `dp[i][j]`, `dp[i][cap]`, `dp[mask]`… |
| **Transition** | How does `dp[state]` combine *already-solved* smaller states? This is the recurrence. |
| **Base case** | The smallest states whose answer is known directly (`dp[0]`, empty string, `cap == 0`). |
| **Order** | Evaluation order so every state's dependencies are ready: memo recursion (any order, lazy) or a loop in dependency order. |
| **Answer** | Which state holds the final answer (`dp[n]`, `dp[n][m]`, `max(dp)`, `dp[full_mask]`)? |

> Say this in the room: **always state the `dp[...]` meaning out loud in plain English before writing a single line of the recurrence.** "Let `dp[i][j]` be the length of the longest common subsequence of `A[0..i-1]` and `B[0..j-1]`." If you can't say it cleanly, your state is wrong and the recurrence will be too. This is the #1 thing interviewers grade on DP problems.

### The 5-step DP checklist (run this on any problem)
1. **Define `dp[...]` meaning in ENGLISH first.** Nail down exactly what one cell *is* (a count? a min cost? a boolean? the best ending *at* `i` vs *up to* `i`?). Pin the indexing convention (does `dp[i]` use the first `i` items, or items `0..i`?). 90% of DP bugs are a fuzzy definition.
2. **Write the recurrence/transition.** Express `dp[state]` using strictly smaller states. Identify the "last decision" (take/skip item `i`, cut at `k`, match/skip a char) — DP is "try every last decision, combine with the optimal rest."
3. **Base cases.** The states the recurrence can't reach (`dp[0]`, empty prefix, `i >= n`). Initialize the array to the identity of your combine op (`0` for sum/count, `INF`/`-INF` for min/max, `false`/`true` for reachability).
4. **Order / memo vs tabulation.** Top-down memo: write the recursion, cache by state — order handles itself. Bottom-up: iterate states so all dependencies are computed first (often increasing `i`; sometimes *decreasing*, e.g. interval DP by length, knapsack-1D by descending weight).
5. **Answer location + space optimization.** Know which cell to return. Then, if `dp[i]` only reads `dp[i-1]` (or a fixed window), drop a dimension with a rolling array.

### Top-down (memoization) vs bottom-up (tabulation)

```
// TOP-DOWN: write the natural recursion, add a cache. Lazy: only reachable states computed.
memo = {}                          // or an array filled with a "not-computed" sentinel
function solve(state):
    if state is a base case: return base_value
    if state in memo: return memo[state]
    ans = combine over each choice c: f(c, solve(smaller_state(state, c)))
    memo[state] = ans
    return ans
// call: solve(initial_state)
```

```
// BOTTOM-UP: allocate the table, seed base cases, fill in dependency order, read the answer cell.
dp = array sized over the state space, init to identity / base
set base cases explicitly
for state in dependency_order:      // e.g. for i in 1..n:  (or by increasing length, etc.)
    dp[state] = combine over each choice c: f(c, dp[smaller_state(state, c)])
return dp[answer_state]
```

**Converting top-down → bottom-up:** the memo's recursion *defines* the dependency graph. List which states `solve(s)` reads, then loop so those are filled first (usually: the variable that *decreases* in the recursive calls becomes the *increasing* loop variable). **Bottom-up → top-down:** read the recurrence, make the loop body a recursive call guarded by a cache.

| | Top-down (memo) | Bottom-up (tab) |
|---|---|---|
| Write speed in interview | Faster — mirrors brute force; add a cache | Slower — must reason about order |
| Computes | Only *reachable* states (can be far fewer) | *All* states in the table |
| Risk | Recursion depth / stack overflow on big `n` | Wasted cells; harder order bugs |
| Space optimization | Awkward | Natural (rolling array) |
| **Default advice** | **Start here to get correctness fast** | Switch when you need O(1)-dim space or recursion is too deep |

### Signature → family lookup
Match the problem's phrasing/shape to the family, then jump to that `###`.

| Signature in the prompt | Family / state shape |
|---|---|
| "How many ways…", "number of distinct…" | Counting DP — sum over transitions (init `0`); 1D/2D/knapsack-2 |
| "Min cost / max value / longest / shortest …" | Optimization DP — min/max over transitions (init `INF`/`-INF`) |
| "Can you reach / is it possible / partition into…" | Reachability DP — boolean OR over transitions (init `false`) |
| One sequence, answer about prefixes/suffixes | **1D linear** (`dp[i]`) |
| Two strings/arrays, align/match/transform | **Two-sequence** (`dp[i][j]`) — LCS, edit distance |
| Grid, move right/down (or any direction) | **2D grid** (`dp[r][c]`) |
| "Pick items, each take-or-skip, hit a capacity/target" | **0/1 knapsack** (`dp[i][cap]` → 1D) |
| "Unlimited copies of each item, reach an amount" | **Unbounded knapsack** (coin change) |
| "Optimal over a contiguous range, split at a point `k`" | **Interval DP** (`dp[i][j]`, loop by length) |
| Tree, answer combines children's answers | **Tree DP** (DFS, return a tuple up) — see §10 |
| `N ≤ ~20`, subsets/permutations of a small set | **Bitmask DP** (`dp[mask]`) — TSP, assignment |
| "Count numbers in `[0, N]` with a digit property" | **Digit DP** (`(pos, tight, started, extra)`) |
| "Buy/sell stock", limited transactions/states | **State-machine DP** (`hold` / `cash`) |
| Tiny `N` (≤ 20–22) + "optimal/count" + 2^N brute force | DP over subsets, or **meet-in-the-middle** (see §20) |

---

### 1D linear DP
**LeetCode:** [Climbing Stairs](https://leetcode.com/problems/climbing-stairs/) · [Min Cost Climbing Stairs](https://leetcode.com/problems/min-cost-climbing-stairs/) · [House Robber](https://leetcode.com/problems/house-robber/) · [House Robber II](https://leetcode.com/problems/house-robber-ii/) · [Decode Ways](https://leetcode.com/problems/decode-ways/) · [Maximum Subarray](https://leetcode.com/problems/maximum-subarray/)
**When:** one sequence; `dp[i]` depends on a constant number of earlier indices (`dp[i-1]`, `dp[i-2]`, or a "best so far"). The single most common family.

**State (English):** `dp[i]` = the answer considering the prefix ending at / up to index `i`. Be precise about *ending at* `i` (subarray/subseq problems — answer is `max(dp)`) vs *first `i` items* (answer is `dp[n]`).

```
// Generic 1D: try the last decision, combine with the best smaller prefix.
dp[0] = base
for i in 1..n-1:
    dp[i] = combine( dp[i-1], dp[i-2], ..., value(i) )
return dp[n-1]   // or max(dp), depending on the definition
```

**Complexity:** time O(n) (× O(transitions)) / space O(n), usually reducible to O(1).
**Traps:** • Confusing "best ending at i" with "best up to i". • Off-by-one in base cases (`dp[0]` vs `dp[1]`). • For O(1) space, keep only the last 1–2 values in scalars and update in the right order.

**Examples & recurrences:**

- **Fibonacci / Climbing Stairs** — `dp[i] = dp[i-1] + dp[i-2]`, `dp[0]=1, dp[1]=1`. Answer `dp[n]`. Space O(1):
```
a = 1; b = 1                  // ways to reach step 0, step 1
for i in 2..n:
    a, b = b, a + b
return b
```
- **House Robber I** — can't rob adjacent. `dp[i] = max(dp[i-1], dp[i-2] + nums[i])`. O(1) space with two rolling vars (`skip`, `take`).
- **House Robber II** — houses in a **circle**, so first and last are adjacent. Run the linear robber twice: once on `nums[0..n-2]`, once on `nums[1..n-1]`; answer = max of the two. (Single-element edge case: return `nums[0]`.)
- **Min Cost Climbing Stairs** — pay `cost[i]` to step off `i`. `dp[i] = cost[i] + min(dp[i-1], dp[i-2])`; answer `min(dp[n-1], dp[n-2])` (can start at 0 or 1, can step past the top).
- **Decode Ways** — digits → letters (A=1…Z=26). `dp[i] = (s[i] != '0' ? dp[i-1] : 0) + (10 ≤ int(s[i-1..i]) ≤ 26 ? dp[i-2] : 0)`. Trap: leading/standalone `'0'` is invalid (no letter is 0). Base `dp[0]=1`.
- **Word Break** — can `s` be cut into dictionary words? `dp[i]` = `s[0..i-1]` is segmentable. `dp[i] = OR over j<i of (dp[j] AND s[j..i-1] in dict)`. `dp[0]=true`. O(n²) (× substring/hash). Reachability flavor.
- **Maximum Subarray (Kadane)** — `dp[i]` = max subarray sum *ending at* `i`. `dp[i] = max(nums[i], dp[i-1] + nums[i])`; answer `max(dp)`. The canonical "ending at i" DP; O(1) space.
- **Jump Game (DP vs greedy)** — reachable? DP: `reach[i] = OR(reach[j] for j with j+nums[j] ≥ i)` is O(n²). **Greedy is strictly better** (§16): track the farthest reachable index in one pass, O(n) — recognize this and *say so*; Jump Game II (min jumps) is greedy BFS-by-levels, O(n). State the DP exists but you'd ship greedy.
- **Paint House** — `n` houses, 3 colors, no two adjacent same, minimize cost. `dp[i][c] = cost[i][c] + min(dp[i-1][c'] for c' != c)`. (Paint House II with `k` colors → track the min and 2nd-min of the previous row to keep it O(nk).)
- **Delete and Earn** — pick value `v`, gain `v × count(v)`, deleting all `v-1` and `v+1`. Bucket gains by value into `points[v]`, then it's **House Robber on the value axis** (`v` and `v±1` conflict): `dp[v] = max(dp[v-1], dp[v-2] + points[v])`.

---

### Longest Increasing Subsequence (LIS)
**LeetCode:** [Longest Increasing Subsequence](https://leetcode.com/problems/longest-increasing-subsequence/) · [Number of Longest Increasing Subsequence](https://leetcode.com/problems/number-of-longest-increasing-subsequence/) · [Maximum Length of Pair Chain](https://leetcode.com/problems/maximum-length-of-pair-chain/) · [Russian Doll Envelopes](https://leetcode.com/problems/russian-doll-envelopes/) · [Longest Increasing Subsequence II](https://leetcode.com/problems/longest-increasing-subsequence-ii/)
**When:** longest/widest subsequence (not subarray) satisfying a monotone/chain condition; or any problem reducible to "longest chain under a partial order."

**State (English):** `dp[i]` = length of the longest strictly increasing subsequence **ending at** index `i`.

```
// O(n^2) DP — clear, and needed if you must reconstruct or count.
for i in 0..n-1:
    dp[i] = 1
    for j in 0..i-1:
        if A[j] < A[i]:
            dp[i] = max(dp[i], dp[j] + 1)
return max(dp)
```

```
// O(n log n) patience / tails (see §5): tails[k] = smallest possible tail of an increasing subseq of length k+1.
tails = []
for x in A:
    p = lower_bound(tails, x)      // first index with tails[index] >= x  (use upper_bound for non-strict / longest non-decreasing)
    if p == len(tails): tails.append(x)
    else: tails[p] = x
return len(tails)
```

**Complexity:** O(n²)/O(n) for the DP; **O(n log n)/O(n)** for the tails+binary-search version (cross-ref §5 binary search). Note: `tails` is *not* an actual LIS, only its length is correct.
**Traps:** • `tails` array gives only the *length* — reconstructing the actual LIS needs parent pointers + index of insertion. • Strictly increasing → `lower_bound`; non-decreasing → `upper_bound`. • Counting LIS needs the O(n²) DP with a count array, not the tails trick.

**Examples:**
- **Number of LIS** — alongside `dp[i]` (length), keep `cnt[i]` (# of LIS ending at `i`). When `dp[j]+1 > dp[i]`: set `dp[i]=dp[j]+1, cnt[i]=cnt[j]`. When `==`: `cnt[i] += cnt[j]`. Answer = sum of `cnt[i]` over all `i` with `dp[i] == max`.
- **Russian Doll Envelopes** — sort by width **ascending**, and for equal widths by height **descending** (so equal widths can't chain), then run LIS on heights → O(n log n). The descending tie-break is the whole trick.
- **Longest Chain of Pairs / Maximum Length of Pair Chain** — sort by second element, greedily/DP chain. (Greedy by end works; or LIS-style DP.)
- **Longest String Chain** — words form a chain if one is a predecessor (delete one char). Sort by length; `dp[w] = max over predecessors p of dp[p] + 1`; O(L²·n).

---

### 2D / grid DP
**LeetCode:** [Unique Paths](https://leetcode.com/problems/unique-paths/) · [Minimum Path Sum](https://leetcode.com/problems/minimum-path-sum/) · [Triangle](https://leetcode.com/problems/triangle/) · [Maximal Square](https://leetcode.com/problems/maximal-square/) · [Dungeon Game](https://leetcode.com/problems/dungeon-game/)
**When:** a grid where each cell's answer depends on neighbors (typically up/left for right/down movement); or any naturally 2-indexed state.

**State (English):** `dp[r][c]` = the answer for reaching / the sub-grid at cell `(r, c)`.

```
// Count paths / min path — fill row-major; each cell reads top and left.
for r in 0..R-1:
    for c in 0..C-1:
        if (r,c) is the start: dp[r][c] = base
        else: dp[r][c] = combine( dp[r-1][c], dp[r][c-1], grid[r][c] )
return dp[R-1][C-1]
```

**Complexity:** time O(R·C) / space O(R·C) → usually O(min(R,C)) with one rolling row.
**Traps:** • Initialize the first row/column (only one incoming direction). • Obstacles force `dp=0` (paths) or `INF` (cost). • Watch direction: some problems fill **bottom-up/reverse** (dungeon game). • Rolling-row space reductions must update left-to-right or right-to-left consistently.

**Examples:**
- **Unique Paths** — only right/down. `dp[r][c] = dp[r-1][c] + dp[r][c-1]`; first row/col = 1. (Closed form `C(R+C-2, R-1)` exists, but DP is the safe answer.)
- **Unique Paths II (obstacles)** — obstacle cell ⇒ `dp[r][c]=0` (no path through it); seed first row/col with 0 after the first obstacle.
- **Minimum Path Sum** — `dp[r][c] = grid[r][c] + min(dp[r-1][c], dp[r][c-1])`.
- **Triangle (min path top→bottom)** — `dp[c] = triangle[r][c] + min(dp[c], dp[c+1])` filled **bottom row up**; answer `dp[0]`. O(n) extra space.
- **Maximal Square** — largest all-1 square. `dp[r][c]` = side of the largest square with bottom-right at `(r,c)`; if `grid[r][c]==1`: `dp[r][c] = 1 + min(dp[r-1][c], dp[r][c-1], dp[r-1][c-1])`. Answer = `max(dp)²`. (The 3-way `min` is the trick; Maximal Rectangle instead uses a monotonic stack per row, see §8.)
- **Dungeon Game (reverse DP)** — knight goes top-left→bottom-right, HP must stay ≥ 1. Fill **from bottom-right backward**: `dp[r][c] = max(1, min(dp[r+1][c], dp[r][c+1]) - dungeon[r][c])`. You can't go forward because the *future* requirement determines the present minimum — classic "DP must run backward" example.
- **Cherry Pickup** (hard, 3D / two agents) — two passes down-and-back is equivalent to **two walkers** descending simultaneously: state `dp[r1][c1][r2]` (`c2 = r1 + c1 − r2`, derived since both took the same number of steps). O(n³). Flag it as advanced; the insight is "two simultaneous walkers, don't double-count a shared cell."

---

### Two-sequence DP
**LeetCode:** [Longest Common Subsequence](https://leetcode.com/problems/longest-common-subsequence/) · [Edit Distance](https://leetcode.com/problems/edit-distance/) · [Distinct Subsequences](https://leetcode.com/problems/distinct-subsequences/) · [Interleaving String](https://leetcode.com/problems/interleaving-string/) · [Regular Expression Matching](https://leetcode.com/problems/regular-expression-matching/)
**When:** two strings/arrays to align, match, transform, or interleave. The `dp[i][j]` grid where `i` indexes one sequence's prefix and `j` the other's.

**State (English):** `dp[i][j]` = the answer for the first `i` chars of `A` and the first `j` chars of `B`.

```
// LCS skeleton — match advances both; mismatch drops one side.
for i in 0..n:
    for j in 0..m:
        if i==0 or j==0: dp[i][j] = base          // empty prefix
        elif A[i-1] == B[j-1]: dp[i][j] = dp[i-1][j-1] + 1
        else: dp[i][j] = max(dp[i-1][j], dp[i][j-1])
return dp[n][m]
```

**Complexity:** time O(n·m) / space O(n·m) → O(min(n,m)) with two rolling rows.
**Traps:** • Index convention: `dp[i][j]` uses `A[0..i-1]` (1-indexed dp over 0-indexed strings) — the most common off-by-one. • Empty-prefix base cases (`dp[i][0]`, `dp[0][j]`) carry real values in edit distance / distinct subsequences, not always 0. • Reconstruction needs to walk back through the choices.

**Examples & recurrences:**
- **Longest Common Subsequence (LCS)** — the skeleton above. `dp[n][m]` = LCS length.
- **Edit Distance (Levenshtein)** — min insert/delete/replace to turn `A`→`B`. Match: `dp[i-1][j-1]`. Else: `1 + min(dp[i-1][j] delete, dp[i][j-1] insert, dp[i-1][j-1] replace)`. Base: `dp[i][0]=i`, `dp[0][j]=j`.
- **Distinct Subsequences** — # of distinct subsequences of `A` equal to `B`. `dp[i][j]` = ways using `A[0..i-1]` to form `B[0..j-1]`. If `A[i-1]==B[j-1]`: `dp[i][j] = dp[i-1][j-1] + dp[i-1][j]` (use it or skip it); else `dp[i-1][j]`. Base `dp[i][0]=1` (empty target: one way).
- **Longest Common Substring (contiguous!)** — `dp[i][j]` = length of common suffix ending at `A[i-1],B[j-1]`. Match: `dp[i-1][j-1]+1`; mismatch: **reset to 0**. Answer = `max` over all cells (not `dp[n][m]`). This reset/`max(all)` is what distinguishes substring from subsequence.
- **Shortest Common Supersequence** — length = `n + m − LCS(A,B)`; reconstruct by walking the LCS table and emitting unmatched chars from both sides.
- **Interleaving String** — is `C` an interleaving of `A` and `B`? `dp[i][j]` = `C[0..i+j-1]` is an interleaving of `A[0..i-1]` and `B[0..j-1]`. `dp[i][j] = (A[i-1]==C[i+j-1] AND dp[i-1][j]) OR (B[j-1]==C[i+j-1] AND dp[i][j-1])`. Requires `len(A)+len(B)==len(C)`.
- **Regular Expression Matching (`.` and `*`)** — `dp[i][j]` = `A[0..i-1]` matches pattern `P[0..j-1]`. If `P[j-1]` is a normal char or `.`: `dp[i][j] = dp[i-1][j-1] AND match(A[i-1], P[j-1])`. If `P[j-1]=='*'`: `dp[i][j] = dp[i][j-2]` (zero of the preceding) OR (`match(A[i-1], P[j-2]) AND dp[i-1][j]`) (one more). The `*` consumes the char *before* it — that's the whole subtlety.
- **Wildcard Matching (`?` and `*`)** — `*` matches any sequence (simpler than regex's `*`): `dp[i][j] = dp[i-1][j]` (`*` eats one more char) OR `dp[i][j-1]` (`*` matches empty); `?`/exact char: `dp[i-1][j-1]`.

---

### Knapsack family (0/1 and unbounded)
**LeetCode:** [Partition Equal Subset Sum](https://leetcode.com/problems/partition-equal-subset-sum/) · [Coin Change](https://leetcode.com/problems/coin-change/) · [Coin Change II](https://leetcode.com/problems/coin-change-ii/) · [Target Sum](https://leetcode.com/problems/target-sum/) · [Combination Sum IV](https://leetcode.com/problems/combination-sum-iv/)
**When:** choose items to optimize/count a value while respecting a capacity/target. The single richest DP family — master the *loop order*, it encodes whether items are reusable and whether order matters.

**State (English):** `dp[cap]` = best value / count / reachability achievable using a total weight exactly/at-most `cap`.

```
// 0/1 KNAPSACK, 2D (clearest): each item taken at most once.
// dp[i][w] = max value using items 0..i-1 with capacity w.
for i in 1..n:
    for w in 0..W:
        dp[i][w] = dp[i-1][w]                                  // skip item i-1
        if weight[i-1] <= w:
            dp[i][w] = max(dp[i][w], dp[i-1][w - weight[i-1]] + value[i-1])  // take it
return dp[n][W]
```

```
// 0/1 KNAPSACK, 1D space-optimized: iterate capacity DESCENDING.
for i in 0..n-1:
    for w in W..weight[i]:          // DESCENDING  (w from W down to weight[i])
        dp[w] = max(dp[w], dp[w - weight[i]] + value[i])
return dp[W]
```
**Why descending for 0/1:** going **down**, `dp[w - weight[i]]` still refers to the *previous* item's row (item `i` not yet used at that smaller capacity) → each item used **at most once**. Iterate **ascending** and `dp[w - weight[i]]` already includes item `i` → you'd reuse it (that's exactly unbounded knapsack). **The loop direction is the only difference between 0/1 and unbounded** in the 1D form — memorize this.

```
// UNBOUNDED KNAPSACK / coin change: iterate capacity ASCENDING (items reusable).
for i in 0..n-1:
    for w in weight[i]..W:          // ASCENDING
        dp[w] = max(dp[w], dp[w - weight[i]] + value[i])
return dp[W]
```

**Complexity:** O(n·W) time, O(W) space (pseudo-polynomial — `W` is the *numeric value*, not input length; large `W` kills this).
**Traps:** • 1D 0/1 **must** go descending; reversing silently makes it unbounded. • "At most capacity" vs "exactly capacity" changes init: at-most → all 0; exactly → `dp[0]=0`, rest `-INF` (optimization) or `false` (reachability). • Pseudo-polynomial: if `W` is huge but `n` tiny, switch to meet-in-the-middle (§20). • Counting **combinations vs permutations** hinges on loop nesting (below).

**0/1 examples:**
- **0/1 Knapsack** — the templates above.
- **Subset Sum / Partition Equal Subset Sum** — can a subset sum to `target` (= `total/2`)? Boolean knapsack: `dp[s] = dp[s] OR dp[s - num]`, `s` descending, `dp[0]=true`. Partition: feasible only if `total` is even.
- **Target Sum (assign +/−)** — count ways to assign signs so the sum is `S`. Let `P` = positives subset: `P − (total − P) = S` ⇒ `P = (total + S)/2`. Reduces to **counting subsets summing to `P`** (`dp[s] += dp[s - num]`, descending). Needs `total+S` even and `≥ 0`.
- **Last Stone Weight II** — smash stones; minimize the final residue. Split into two groups as balanced as possible: maximize a subset sum `≤ total/2`; answer = `total − 2·best`. Pure subset-sum knapsack.

**Unbounded examples (loop order matters):**
- **Coin Change (min coins)** — fewest coins to make `amount`. `dp[a] = min over coins of dp[a - coin] + 1`; `dp[0]=0`, rest `INF`. Either loop order works for *min*, since each amount considers all coins. Answer `INF` ⇒ impossible.
- **Coin Change 2 (count COMBINATIONS)** — # of *combinations* (order-insensitive). **Coins outer, amount inner (ascending):**
```
dp[0] = 1
for c in coins:                 // outer = coin  → each coin "introduced" once → combinations
    for a in c..amount:         // inner = amount, ascending
        dp[a] += dp[a - c]
return dp[amount]
```
- **Combination Sum IV (count PERMUTATIONS)** — # of *ordered* sequences. **Amount outer, coins inner:**
```
dp[0] = 1
for a in 1..target:             // outer = amount
    for x in nums:              // inner = nums  → order counts → permutations
        if x <= a: dp[a] += dp[a - x]
return dp[target]
```

> Say this in the room: **"Coins outer = combinations; target outer = permutations."** Swapping the two loops in coin-change-2 is the single most common DP bug interviewers probe. With *coin* on the outside, you never revisit an earlier coin, so `{1,2}` and `{2,1}` collapse to one combination; with *amount* on the outside, both are counted.

- **Rod Cutting** — max revenue cutting a rod of length `n`, piece of length `i` sells for `price[i]`. Unbounded knapsack with `weight[i]=i`, `value[i]=price[i]`: `dp[L] = max over i of price[i] + dp[L-i]`.
- **Perfect Squares** — fewest perfect squares summing to `n`. Coin change (min) with coins `{1,4,9,…}`: `dp[x] = 1 + min over k² ≤ x of dp[x - k²]`. (Number theory shortcut exists — Lagrange/Legendre — but DP is the safe answer.)

---

### Interval DP
**LeetCode:** [Longest Palindromic Subsequence](https://leetcode.com/problems/longest-palindromic-subsequence/) · [Stone Game](https://leetcode.com/problems/stone-game/) · [Palindrome Partitioning II](https://leetcode.com/problems/palindrome-partitioning-ii/) · [Burst Balloons](https://leetcode.com/problems/burst-balloons/) · [Minimum Cost to Cut a Stick](https://leetcode.com/problems/minimum-cost-to-cut-a-stick/)
**When:** the answer for a range `[i..j]` is built by choosing a **split/last point `k`** inside it and combining the two sub-ranges. Hallmark: "merge", "burst", "cut", "matrix chain", "palindrome partition." **Loop by increasing interval length.**

**State (English):** `dp[i][j]` = best answer for the **inclusive** subarray `A[i..j]`.

```
// INTERVAL DP skeleton: enumerate length, then left endpoint, then the split k.
for length in 2..n:                       // start from small ranges
    for i in 0..n-length:
        j = i + length - 1
        dp[i][j] = INF                     // or -INF / 0 depending on objective
        for k in i..j-1:                   // try every split / last operation
            dp[i][j] = min(dp[i][j], dp[i][k] + dp[k+1][j] + cost(i, k, j))
return dp[0][n-1]
```

**Complexity:** O(n³) time / O(n²) space (Θ(n²) states × Θ(n) splits). Knuth's optimization cuts some to O(n²) but rarely needed in a loop.
**Traps:** • The loop **must** go by increasing length so both sub-ranges are already computed. • `cost(i,k,j)` is the gluing cost (e.g. `p[i-1]·p[k]·p[j]` for matrix chain). • Base case = length-1 (or length-0) intervals. • For burst balloons, the split point is the **last** balloon to pop, not the first — invert your intuition.

**Examples:**
- **Matrix Chain Multiplication** — min scalar multiplications to multiply `A_i…A_j` (dims in `p[]`). `dp[i][j] = min over k of dp[i][k] + dp[k+1][j] + p[i-1]·p[k]·p[j]`.
- **Burst Balloons** — pop all balloons for max coins; popping `i` (with neighbors `l,r`) yields `nums[l]·nums[i]·nums[r]`. Add virtual `1`s at both ends. State: `dp[i][j]` over the **open** interval; `k` = the **last** balloon popped in `(i,j)`: `dp[i][j] = max over k of dp[i][k] + dp[k][j] + nums[i]·nums[k]·nums[j]`. "Last to pop" makes the neighbors fixed (`i` and `j` survive) — that reframing is the whole problem.
- **Palindrome Partitioning II (min cuts)** — fewest cuts so every piece is a palindrome. Precompute `isPal[i][j]` (itself an interval DP), then 1D: `cuts[i] = min over j≤i of (isPal[j..i] ? cuts[j-1]+1 : ∞)`, `cuts[-1] = -1`.
- **Longest Palindromic Subsequence** — `dp[i][j]` = LPS of `s[i..j]`. If `s[i]==s[j]`: `dp[i][j] = dp[i+1][j-1] + 2`; else `max(dp[i+1][j], dp[i][j-1])`. (Equivalently `LCS(s, reverse(s))`.) Note indices go *inward* → fill by increasing length or decreasing `i` / increasing `j`.
- **Minimum Cost to Merge Stones** — merge exactly `k` consecutive piles at a time, cost = their sum; merge all into one. Add a `step` dimension or only allow splits where `(j-i) % (k-1) == 0`; classic hard interval DP. Feasible only if `(n-1) % (k-1) == 0`.
- **Stone Game family** — two players, take from ends, both optimal. `dp[i][j]` = best score *difference* (current player minus opponent) on `A[i..j]`: `dp[i][j] = max(A[i] - dp[i+1][j], A[j] - dp[i][j-1])`. Player 1 wins iff `dp[0][n-1] > 0`. (Tracking the *difference* collapses two players into one DP.)

---

### Tree DP
**LeetCode:** [Diameter of Binary Tree](https://leetcode.com/problems/diameter-of-binary-tree/) · [House Robber III](https://leetcode.com/problems/house-robber-iii/) · [Longest Univalue Path](https://leetcode.com/problems/longest-univalue-path/) · [Binary Tree Maximum Path Sum](https://leetcode.com/problems/binary-tree-maximum-path-sum/) · [Binary Tree Cameras](https://leetcode.com/problems/binary-tree-cameras/)
**When:** the answer at a node is a function of its children's answers; you need *one DFS* where each call returns a small bundle of values. Cross-ref §10 (Trees) for traversal mechanics.

**State (English):** `dfs(node)` returns a tuple summarizing the subtree rooted at `node` (e.g. `(robInclusive, robExclusive)`), while a side variable tracks the global best.

```
// TREE DP: post-order DFS; combine children, return a tuple up; update a global if the answer is "anywhere in the tree".
best = -INF
function dfs(node):
    if node == null: return identity_tuple        // e.g. (0, 0) or 0
    L = dfs(node.left)
    R = dfs(node.right)
    // update global best using L, R, node  (for "best path through node" style)
    best = max(best, combine_through(L, R, node))
    // return what the PARENT can legally use (often: best single downward branch)
    return combine_upward(L, R, node)
return best   // or dfs(root) if the answer is at the root
```

**Complexity:** O(n) time, O(h) space (recursion stack; `h` = height).
**Traps:** • Distinguish "value returned to parent" (often a single branch — you can't use both children in a *path* through the parent) from "global best" (may bend through the node using both children). • Null/leaf base case. • Use a wrapper/global (or pass a mutable holder) for the answer when it lives at an arbitrary node, not the root.

**Examples:**
- **House Robber III** (tree) — can't rob a node and its child. `dfs(node)` returns `(rob, skip)`: `rob = node.val + L.skip + R.skip`; `skip = max(L) + max(R)`. Answer = `max(dfs(root))`. (No global needed — answer is at the root.)
- **Binary Tree Maximum Path Sum** — best path (may start/end anywhere, bends at one node). `dfs` returns the best *downward* branch `node.val + max(0, max(L, R))`; **global** = `max(global, node.val + max(0,L) + max(0,R))` (bend using both sides). Clamp negatives to 0.
- **Diameter of Binary Tree** — `dfs` returns subtree **height**; global = `max(global, heightL + heightR)` (edges through this node). Same "return one thing up, update global with both" shape.

> Say this in the room: for tree DP, *"the function returns the best single branch the parent can extend; a separate global captures answers that bend through the current node using both children."* That sentence covers diameter, max-path-sum, and most tree-DP variants at once.

---

### Bitmask DP
**LeetCode:** [Partition to K Equal Sum Subsets](https://leetcode.com/problems/partition-to-k-equal-sum-subsets/) · [Shortest Path Visiting All Nodes](https://leetcode.com/problems/shortest-path-visiting-all-nodes/) · [Maximum Students Taking Exam](https://leetcode.com/problems/maximum-students-taking-exam/) · [Number of Ways to Wear Different Hats to Each Other](https://leetcode.com/problems/number-of-ways-to-wear-different-hats-to-each-other/)
**When:** `N ≤ ~20–22` and you must track *which subset* of a small set is used/visited/assigned. The mask (an integer) IS the state. Cross-ref §7 for submask iteration.

**State (English):** `dp[mask]` (often `dp[mask][last]`) = best answer having processed exactly the set of elements in `mask`. Bit `b` set ⇒ element `b` is used.

```
// HELD–KARP / shortest Hamiltonian path: dp[mask][i] = min cost to visit set `mask`, ending at city i.
dp[1<<start][start] = 0
for mask in 0..(1<<n)-1:
    for i in 0..n-1:
        if (mask >> i) & 1 == 0 or dp[mask][i] == INF: continue
        for j in 0..n-1:
            if (mask >> j) & 1: continue                 // j already visited
            nmask = mask | (1<<j)
            dp[nmask][j] = min(dp[nmask][j], dp[mask][i] + cost[i][j])
// TSP cycle: min over i of dp[FULL][i] + cost[i][start];  path: min over i of dp[FULL][i]
```

**Complexity:** Held–Karp O(2ⁿ·n²) time, O(2ⁿ·n) space. Subset-only DP O(2ⁿ·n) or O(3ⁿ) if iterating submasks of every mask.
**Traps:** • Feasible only for tiny `N` (2²⁰ ≈ 1M masks; `n²·2ⁿ` blows up past ~20). • Iterate masks in increasing order so subsets are ready. • `dp[mask][last]` needed when the next cost depends on where you currently are (TSP); plain `dp[mask]` when it doesn't (assignment by count). • Submask enumeration `sub = (sub-1) & mask` (see §7) gives O(3ⁿ) over all (mask, submask) pairs — use for partition-into-groups DP.

**Examples:**
- **Traveling Salesman / Shortest Hamiltonian Path** — Held–Karp above. The canonical bitmask DP.
- **Assignment Problem** — assign `n` workers to `n` jobs minimizing cost. `dp[mask]` = min cost having assigned jobs in `mask` to the first `popcount(mask)` workers: `dp[mask | (1<<j)] = min(..., dp[mask] + cost[popcount(mask)][j])`. O(2ⁿ·n). (Hungarian algorithm is the polynomial alternative for larger `n`.)
- **Partition to K Equal-Sum Subsets** — `dp[mask]` over which numbers are used; track running bucket sum mod target; fill greedily. Bitmask + memo on `mask`. Prune: sort descending, fail fast.
- **Can I Win** — players alternately pick from `1..maxChoosable` (no reuse) trying to push a running total over `target`. `dp[mask]` = can the player to move force a win given used set `mask`? Memoize over `mask`; classic minimax-as-bitmask.
- **Minimum Number of Work Sessions** — pack tasks into sessions of capacity `T`. `dp[mask]` = min sessions to finish set `mask`; iterate submasks that fit in one session. Cross-ref §7 submask iteration.
- **Counting / DP over subsets** — sum-over-subsets (SOS) DP: `for b in bits: for mask: if mask>>b & 1: dp[mask] += dp[mask ^ (1<<b)]`. O(2ⁿ·n) — the standard way to aggregate over all subsets.

---

### Digit DP
**LeetCode:** [Count Numbers with Unique Digits](https://leetcode.com/problems/count-numbers-with-unique-digits/) · [Non-negative Integers without Consecutive Ones](https://leetcode.com/problems/non-negative-integers-without-consecutive-ones/) · [Numbers At Most N Given Digit Set](https://leetcode.com/problems/numbers-at-most-n-given-digit-set/) · [Numbers With Repeated Digits](https://leetcode.com/problems/numbers-with-repeated-digits/)
**When:** "count integers in `[0, N]` (or `[L, R]`) whose **digits** satisfy a property" (no repeated digit, digit-sum divisible by k, no '4', etc.). `N` can be astronomically large — you DP over its decimal digits, not its value.

**State (English):** `solve(pos, tight, started, extra)` = count of valid ways to fill digit positions `pos..end`, where `tight` = still bounded by `N`'s prefix, `started` = have we placed a non-leading-zero digit yet, `extra` = whatever the property needs (running remainder, last digit, used-digit mask…).

```
digits = decimal digits of N (most-significant first), length L
memo = {}                                     // keyed by (pos, started, extra) — only when NOT tight
function solve(pos, tight, started, extra):
    if pos == L: return 1 if isValid(started, extra) else 0
    if not tight and (pos, started, extra) in memo: return memo[...]
    hi = digits[pos] if tight else 9
    total = 0
    for d in 0..hi:
        ntight   = tight and (d == hi)
        nstarted = started or (d != 0)
        nextra   = update(extra, d, nstarted)
        total += solve(pos+1, ntight, nstarted, nextra)
    if not tight: memo[(pos, started, extra)] = total
    return total
// answer for [0, N] = solve(0, true, false, init_extra);  [L, R] = f(R) - f(L-1)
```

**Complexity:** O(L · states_of_extra · 10) — `L` ≈ number of digits (≤ ~18 for 64-bit). Tiny.
**Traps:** • **Only memoize when `tight == false`** — tight states are unique to this `N`'s prefix and pollute the cache. • `started` (leading-zero) handling: a leading 0 usually shouldn't count as "using digit 0". • `[L, R]` via `f(R) − f(L−1)` (inclusive). • Define `isValid` to allow/disallow the all-zero / empty number per the problem.

**Examples:** Count numbers ≤ N with no repeated digit (`extra` = used-digit bitmask); count with digit sum ≡ 0 mod k (`extra` = running sum mod k); count without a forbidden digit; "Numbers At Most N Given Digit Set"; "Count of Integers" with digit-sum bounds.

---

### State-machine DP
**LeetCode:** [Best Time to Buy and Sell Stock with Cooldown](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-cooldown/) · [Best Time to Buy and Sell Stock with Transaction Fee](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-transaction-fee/) · [Best Time to Buy and Sell Stock III](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-iii/) · [Best Time to Buy and Sell Stock IV](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-iv/)
**When:** at each step you're in one of a few discrete **states** and transitions between them carry costs/rewards. Unifies the entire **buy/sell stock** family — model it as a tiny automaton instead of memorizing six separate solutions.

**State (English):** track, per day, the best cash when **holding** a share vs **not holding** (and, if limited, the transaction count / cooldown).

```
// Stock with unlimited transactions, model as two states:
hold = -INF        // best balance while currently HOLDING a share
cash = 0           // best balance while NOT holding
for price in prices:
    prev_cash = cash
    cash = max(cash, hold + price)            // sell today (or stay in cash)
    hold = max(hold, prev_cash - price)       // buy today (or keep holding)
return cash
```

```
// k transactions: dp over (transactions used, holding?). buy[t]/sell[t] rolling.
buy = array of size k+1 filled with -INF
sell = array of size k+1 filled with 0
for price in prices:
    for t in 1..k:
        buy[t]  = max(buy[t],  sell[t-1] - price)   // open the t-th position
        sell[t] = max(sell[t], buy[t]  + price)     // close the t-th position
return sell[k]
```

**Complexity:** unlimited/cooldown/fee: O(n) time, O(1) space. k-transactions: O(n·k); if `k ≥ n/2` it's effectively unlimited → fall back to the greedy O(n) version.
**Traps:** • `hold` starts at `-INF` (can't be holding before buying). • **Cooldown**: after selling you must skip a day → add a `cooldown`/`rest` state and route `buy` from `prev_prev_cash`. • **Transaction fee**: subtract it once per round trip (on sell: `hold + price - fee`). • **k ≥ n/2** shortcut prevents O(n·k) blowup. • Off-by-one: with `k` transactions you need `k+1` slots (index 0 = "no transaction yet").

**The unified family:**

| Variant | Modification to the two-state machine |
|---|---|
| Best Time II (unlimited) | The base template above. |
| Best Time I (one transaction) | `cash = max(cash, hold + price)`; `hold = max(hold, -price)` (buy from 0, not from cash). |
| With **cooldown** | Add `rest`; `buy = max(buy, rest - price)`, `rest = max(rest, cash)`, one-day lag. |
| With **fee** | `cash = max(cash, hold + price - fee)`. |
| At most **k** transactions | The `buy[t]/sell[t]` table above. |

> Say this in the room: *"I'll model this as a state machine with `hold` and `cash` states; every variant — cooldown, fee, k-limit — is just an extra state or an extra term, not a new algorithm."* Demonstrating this unification reads as senior.

---

### DP optimization toolkit (name-drop, don't over-engineer)
Reach for these only after the naive DP is correct and too slow:

- **Rolling array (space reduction)** — if `dp[i]` reads only `dp[i-1]` (or a fixed window), keep 1–2 rows / scalars instead of the full table. Turns O(n·m) space into O(m) or O(1). First optimization to mention.
- **Prefix sums to speed transitions** — when a transition sums/maxes over a *range* of previous states, precompute prefix sums (or a sliding-window aggregate) so each transition is O(1) instead of O(range). Turns O(n²) into O(n) for "sum over all `j < i`" recurrences.
- **Monotonic deque** — when the transition is `dp[i] = min/max over a sliding window of dp[j] + f(i,j)` and the window moves monotonically, a monotonic deque (§8) keeps the window optimum in amortized O(1) → O(n) total.
- **Convex Hull Trick / Li Chao tree** — for `dp[i] = min over j of (m_j · x_i + b_j)` (lines), maintain a lower hull. Advanced; mention it exists for "O(n²) → O(n log n) DP" but you'd rarely code it in a 45-minute loop.
- **Knuth / divide-and-conquer optimization** — interval/partition DPs whose optimal split is monotone can drop an O(n) factor. Name-only.
- **When DP is the WRONG tool:** (1) a **greedy** with an exchange-argument proof works in O(n log n) — prefer it (jump game, interval scheduling, see §16); (2) the **state space is too large** to enumerate (e.g. `2^n` with `n=40`) → **meet-in-the-middle** (split, enumerate halves, combine — see §20); (3) subproblems **don't overlap** → it's plain divide & conquer (§12), memoization is pointless.

### Recognizing DP from constraints (cross-ref §1)
The constraints leak the intended solution — read them before designing:

| Constraint signal | Likely DP shape |
|---|---|
| `n ≤ ~20–22`, "count/optimal", obvious 2^n brute force | **Bitmask DP** over subsets (or meet-in-the-middle, §20) |
| `n ≤ ~500`, two indices / a range, O(n²)–O(n³) budget | **2D / interval DP** (`dp[i][j]`) |
| `n ≤ 10^5`, "longest/optimal subsequence/subarray" | **1D DP O(n)** (or O(n log n) like LIS) |
| Capacity / target `W ≤ ~10^4`, "subset to hit target" | **Knapsack** O(n·W) (pseudo-polynomial) |
| `N` astronomically large but it's about its **digits** | **Digit DP** over O(log N) positions |
| "min cost / # ways" + small N + exponential naive recursion | **DP** — find the overlapping subproblems |

A clean tell: if the brute force is **exponential** but the number of *distinct subproblems* is **polynomial** (or `2^n` with `n` tiny), it's DP. State the brute-force recursion first, spot the repeated states, then add the cache — that derivation, said aloud, is exactly what the interviewer wants to hear.

## 15. Graphs

Traversal and shortest-path on explicit or implicit (grid/state) graphs. The whole section is "pick the right algorithm for the graph's properties, then run a templated traversal." Build the adjacency once, mark visited correctly, and choose by the table below.

### Decision table — which algorithm
Read the graph's *edge weights* and *what you're asked* off the prompt, then jump straight to the row.

| Graph property / question | Algorithm | Complexity |
|---|---|---|
| Reach / connectivity / components | BFS or DFS / Union-Find | O(V+E) |
| Shortest path, **unweighted** (or all edges = 1) | BFS (level = distance) | O(V+E) |
| Shortest path, weights **0 or 1** | 0-1 BFS (deque) | O(V+E) |
| Shortest path, **non-negative** weights | Dijkstra (min-heap) | O(E log V) |
| Shortest path, **negative** edges (no neg cycle) | Bellman–Ford | O(V·E) |
| Shortest path with **≤ k edges/stops** | Bellman–Ford, k rounds | O(k·E) |
| **All-pairs** shortest path, small V (≤ ~500) | Floyd–Warshall | O(V³) |
| DAG: longest/shortest path, count paths | Topo order + DP | O(V+E) |
| Ordering with prerequisites / cycle in **directed** | Topological sort (Kahn / DFS colors) | O(V+E) |
| Cycle in **undirected** | Union-Find or DFS-with-parent | O(V+E) α |
| 2-colorable? (no odd cycle) | Bipartite check (BFS/DFS) | O(V+E) |
| Min total edge weight to connect all | MST: Kruskal / Prim | O(E log E) |
| Strongly connected / bridges / articulation | Tarjan or Kosaraju | O(V+E) |
| Use every edge exactly once | Eulerian path (Hierholzer) | O(E) |

> Say this in the room: "First question I ask: weighted? negative? I pick BFS for unweighted, Dijkstra for non-negative, Bellman–Ford if there can be negative edges, Floyd–Warshall only when V is tiny and I need all pairs."

### Representations & building adjacency
**When:** before any traversal — decide the structure from V, E, and density.
Adjacency **list/map** is the default (`adj[u] = [v, …]`). Adjacency **matrix** for dense graphs or O(1) edge lookups / Floyd–Warshall. **Edge list** for Kruskal/Bellman–Ford. **Implicit/grid**: never materialize edges — neighbors are computed from coordinates or state transitions.

```
// from edge list to adjacency (n nodes, edges list of (u, v[, w]))
function buildAdj(n, edges, directed):
  adj = array of n empty lists
  for u, v, w in edges:
    adj[u].append((v, w))
    if not directed:
      adj[v].append((u, w))         // undirected → BOTH directions
  return adj
```

| Rep | Space | Edge query | Iterate neighbors | Use when |
|---|---|---|---|---|
| Adjacency list/map | O(V+E) | O(deg) | O(deg) | default, sparse |
| Adjacency matrix | O(V²) | O(1) | O(V) | dense, Floyd–Warshall, small V |
| Edge list | O(E) | — | — | Kruskal, Bellman–Ford |
| Implicit/grid | O(1) | computed | computed | grids, state-space search |

**Traps:** • **undirected = insert both directions** — forgetting one is the most common graph bug. • parallel edges/self-loops: a map-of-sets dedups but loses multiplicity (bad for Euler/multigraph). • node ids may be strings/coords → map them to `0..n-1` or use a hash map adjacency. • directed vs undirected changes *every* downstream algorithm (cycle detection especially).

### BFS — shortest path in unweighted graph
**LeetCode:** [Minimum Genetic Mutation](https://leetcode.com/problems/minimum-genetic-mutation/) · [Open the Lock](https://leetcode.com/problems/open-the-lock/) · [Shortest Path in Binary Matrix](https://leetcode.com/problems/shortest-path-in-binary-matrix/) · [Word Ladder](https://leetcode.com/problems/word-ladder/)
**When:** fewest edges / minimum steps in an unweighted graph; level-order anything.
Queue + visited. Distance = BFS level. **Mark visited on ENQUEUE, not on dequeue** — otherwise a node can be pushed by several neighbors before it's first popped, blowing up the queue and breaking the shortest-path invariant.

```
function bfs(adj, src):
  dist = array of -1 size n           // -1 = unvisited
  q = queue; q.push(src); dist[src] = 0
  while not q.empty():
    u = q.front(); q.pop()
    for v in adj[u]:
      if dist[v] == -1:               // unvisited → first time = shortest
        dist[v] = dist[u] + 1
        q.push(v)                      // visited-on-enqueue (dist set here)
  return dist
```

Level-by-level variant: snapshot `sz = len(q)` and pop exactly `sz` nodes per level (needed for "minimum number of levels / per-level work").
**Complexity:** time O(V+E) / space O(V).
**Traps:** • visiting on dequeue lets duplicates pile up and a longer path overwrite a shorter one. • BFS gives shortest path *only* when all edge weights are equal; weighted ⇒ Dijkstra. • setting `dist` at enqueue is what marks visited — don't keep a separate set that updates at dequeue.

### Multi-source BFS
**LeetCode:** [01 Matrix](https://leetcode.com/problems/01-matrix/) · [Rotting Oranges](https://leetcode.com/problems/rotting-oranges/) · [As Far from Land as Possible](https://leetcode.com/problems/as-far-from-land-as-possible/) · [Map of Highest Peak](https://leetcode.com/problems/map-of-highest-peak/)
**When:** shortest distance from the **nearest** of many sources (rotting oranges, 01-matrix, walls-and-gates, nearest exit).
Seed the queue with **all** sources at distance 0 simultaneously; one BFS computes every cell's distance to its closest source. Equivalent to a super-source connected to all sources.

```
function multiSourceBFS(grid, sources):
  q = queue
  for s in sources:
    dist[s] = 0; q.push(s)            // all sources enqueued up front
  while not q.empty():
    u = q.front(); q.pop()
    for v in neighbors(u):
      if dist[v] == -1:
        dist[v] = dist[u] + 1
        q.push(v)
  return dist
```

**Complexity:** time O(V+E) (grid: O(rows·cols)) / space O(V).
**Traps:** • seed *all* sources before the loop — don't BFS from each source separately (that's O(sources·V)). • rotting oranges: a fresh orange never reachable ⇒ stays `-1` ⇒ return `-1`. • the answer is often `maxDist` reached (time for the last cell), tracked as `dist[u]` or a level counter.

### DFS — recursive and iterative
**LeetCode:** [Flood Fill](https://leetcode.com/problems/flood-fill/) · [Number of Islands](https://leetcode.com/problems/number-of-islands/) · [Max Area of Island](https://leetcode.com/problems/max-area-of-island/) · [Pacific Atlantic Water Flow](https://leetcode.com/problems/pacific-atlantic-water-flow/)
**When:** explore fully (components, path existence, flood fill, tree-shaped recursion on graphs). Use **iterative** when recursion depth could overflow (huge grids/chains).
Visited set; recurse into unvisited neighbors. Iterative form mimics with an explicit stack.

```
// recursive
function dfs(u):
  seen.add(u)
  for v in adj[u]:
    if v not in seen:
      dfs(v)

// iterative (explicit stack) — for deep graphs
function dfsIter(src):
  st = [src]
  while not st.empty():
    u = st.pop()
    if u in seen: continue           // may be pushed multiple times
    seen.add(u)                       // mark on POP here (see trap)
    for v in adj[u]:
      if v not in seen: st.push(v)
```

**Complexity:** time O(V+E) / space O(V) (+ recursion stack O(V)).
**Traps:** • recursion depth: a 200×200 grid snake is 40k deep → stack overflow; go iterative or BFS. • iterative DFS marks on pop and must `continue` if already seen (a node can sit on the stack multiple times). • **DFS does NOT give shortest paths** — for that use BFS.

### Connected components / number of islands / flood fill / clone graph
**LeetCode:** [Flood Fill](https://leetcode.com/problems/flood-fill/) · [Number of Islands](https://leetcode.com/problems/number-of-islands/) · [Number of Provinces](https://leetcode.com/problems/number-of-provinces/) · [Clone Graph](https://leetcode.com/problems/clone-graph/) · [Surrounded Regions](https://leetcode.com/problems/surrounded-regions/)
**When:** count or label maximal connected regions; copy a graph.
Loop all nodes; each time you hit an unvisited one, run a full DFS/BFS from it (one traversal = one component) and increment the count. Flood fill = DFS that overwrites color. Clone graph = DFS/BFS carrying a `old→new` map.

```
function countComponents(n, adj):
  comp = 0
  for s in 0..n-1:
    if s not in seen:
      comp += 1
      dfs(s)                          // marks the whole component seen
  return comp

// clone graph: map original node → its copy
function clone(node):
  if node in oldToNew: return oldToNew[node]
  copy = newNode(node.val)
  oldToNew[node] = copy               // register BEFORE recursing (cycles!)
  for nb in node.neighbors:
    copy.neighbors.append(clone(nb))
  return copy
```

**Complexity:** time O(V+E) / space O(V).
**Traps:** • clone-graph: insert into `oldToNew` *before* recursing into neighbors, or a cycle recurses forever. • number-of-islands = connected components on a grid where edges are 4-adjacency between land cells. • flood fill: if newColor == oldColor, return immediately (else infinite loop). • Union-Find is an alternative for static component counting (see §18).

### Grid as graph — directions, bounds, visited
**LeetCode:** [Surrounded Regions](https://leetcode.com/problems/surrounded-regions/) · [Word Search](https://leetcode.com/problems/word-search/) · [Pacific Atlantic Water Flow](https://leetcode.com/problems/pacific-atlantic-water-flow/) · [Shortest Path in Binary Matrix](https://leetcode.com/problems/shortest-path-in-binary-matrix/)
**When:** any 2-D grid where you move to orthogonal (sometimes diagonal) neighbors: islands, regions, area, word search, escape problems.
Neighbors via a directions array; guard each with bounds + cell condition + visited. This skeleton powers number-of-islands, max-area-of-island, surrounded-regions, pacific-atlantic, word-search.

```
DIRS = [(0,1), (1,0), (0,-1), (-1,0)]    // R, D, L, U (add diagonals if needed)

function dfsGrid(r, c):
  if r < 0 or r >= R or c < 0 or c >= C: return   // bounds
  if grid[r][c] != LAND or (r,c) in seen: return  // cell + visited
  seen.add((r, c))
  for dr, dc in DIRS:
    dfsGrid(r + dr, c + dc)
```

Patterns: **max-area** returns `1 + sum(recurse)`; **surrounded-regions** runs DFS from border `O`s to mark the unflippable ones, then flips the rest; **pacific-atlantic** runs reverse-BFS from each ocean's border (uphill reachability) and intersects the two reachable sets. **Word-search** is grid-DFS + backtracking (mark cell used, recurse, unmark — see §12).
**Complexity:** time O(R·C) per traversal / space O(R·C).
**Traps:** • bounds check **before** indexing `grid[r][c]`. • can mutate the grid in place (`'1'→'0'`) instead of a visited set — but that destroys input; ask if allowed. • diagonals: 8 directions if moves are king-like. • surrounded-regions/pacific-atlantic invert the search (start from the *border/ocean*), a classic trick.

### BFS over state `(cell, keys-bitmask)` — shortest path with keys
**LeetCode:** [Shortest Path Visiting All Nodes](https://leetcode.com/problems/shortest-path-visiting-all-nodes/) · [Shortest Path to Get All Keys](https://leetcode.com/problems/shortest-path-to-get-all-keys/)
**When:** grid shortest path where the *state* is richer than position (collected keys, remaining fuel, steps-mod-k). LC 864 Shortest Path to Get All Keys.
The graph node is `(position, mask)` where `mask` bits = keys held. BFS over this expanded state space; picking up a key changes the mask, so the same cell with a different mask is a *different* node.

```
function shortestPathAllKeys(grid):
  allKeys = bitmask of all key bits
  start state = (startCell, mask=0)
  q = queue; q.push((start, 0))         // (state, steps)
  seen = {start state}
  while not q.empty():
    (pos, mask), d = q.front(); q.pop()
    if mask == allKeys: return d         // got every key
    for npos in neighbors(pos):
      if wall(npos): continue
      nmask = mask
      if isLock(npos) and bit(npos) not in mask: continue   // locked
      if isKey(npos): nmask = mask | bit(npos)               // pick up
      if (npos, nmask) not in seen:
        seen.add((npos, nmask)); q.push(((npos, nmask), d+1))
  return -1
```

**Complexity:** time O(R·C·2^k) / space O(R·C·2^k), k = number of keys.
**Traps:** • visited must key on `(cell, mask)`, not cell alone — same cell with new keys must be re-explorable. • BFS (not DFS) because all moves cost 1 and we want the shortest. • `2^k` blows up past ~10 keys; that's the intended bound.

### Topological sort — Kahn (BFS) and DFS post-order
**LeetCode:** [Course Schedule](https://leetcode.com/problems/course-schedule/) · [Course Schedule II](https://leetcode.com/problems/course-schedule-ii/) · [Minimum Height Trees](https://leetcode.com/problems/minimum-height-trees/) · [Sort Items by Groups Respecting Dependencies](https://leetcode.com/problems/sort-items-by-groups-respecting-dependencies/)
**When:** linearize a DAG respecting dependencies (build order, course schedule, task ordering); also detects cycles.
**Kahn:** repeatedly emit zero-indegree nodes, decrement neighbors' indegree. If you emit fewer than V nodes, there's a cycle. **DFS:** post-order push, then reverse — a node is finished only after all its descendants.

```
// Kahn (indegree + queue)
function topoKahn(n, adj):
  indeg = array of 0 size n
  for u in 0..n-1:
    for v in adj[u]: indeg[v] += 1
  q = queue
  for u in 0..n-1:
    if indeg[u] == 0: q.push(u)
  order = []
  while not q.empty():
    u = q.front(); q.pop(); order.append(u)
    for v in adj[u]:
      indeg[v] -= 1
      if indeg[v] == 0: q.push(v)
  if len(order) != n: return null        // cycle → no valid order
  return order

// DFS post-order
function topoDFS(n, adj):
  order = []
  function dfs(u):
    seen.add(u)
    for v in adj[u]:
      if v not in seen: dfs(v)
    order.append(u)                       // post-order
  for u in 0..n-1:
    if u not in seen: dfs(u)
  reverse(order)
  return order
```

**Complexity:** time O(V+E) / space O(V).
**Traps:** • a valid topo order exists **iff** the directed graph is acyclic — `len(order) != n` (Kahn) signals a cycle. • Kahn naturally yields *lexicographically smallest* order if you use a min-heap instead of a plain queue (course-schedule-II variants). • DFS form needs the **reverse** of post-order. • applies to course schedule I (feasible?), II (an order), alien dictionary (build edges from adjacent word pairs then topo), parallel courses (count BFS levels = semesters).

### Cycle detection in a DIRECTED graph (colors)
**LeetCode:** [Course Schedule](https://leetcode.com/problems/course-schedule/) · [Find Eventual Safe States](https://leetcode.com/problems/find-eventual-safe-states/) · [Course Schedule II](https://leetcode.com/problems/course-schedule-ii/)
**When:** "is there a cycle / can all tasks finish" in a directed graph, when you don't want to compute indegrees.
Three colors: white (unvisited), gray (on current DFS path), black (done). A gray→gray edge = **back edge** = cycle. (Indegree-leftover from Kahn is the BFS equivalent.)

```
WHITE = 0; GRAY = 1; BLACK = 2
function hasCycleDirected(n, adj):
  color = array of WHITE size n
  function dfs(u):
    color[u] = GRAY                       // entering recursion path
    for v in adj[u]:
      if color[v] == GRAY: return true    // back edge → cycle
      if color[v] == WHITE and dfs(v): return true
    color[u] = BLACK                      // fully explored
    return false
  for u in 0..n-1:
    if color[u] == WHITE and dfs(u): return true
  return false
```

**Complexity:** time O(V+E) / space O(V).
**Traps:** • gray = "in the current recursion stack," not merely "visited" — a black (finished) neighbor is *not* a cycle. • set GRAY on entry and BLACK on exit; mixing them up gives false positives. • for *undirected* graphs this color trick misfires (the edge back to your parent looks like a cycle) — use the parent-skip method below.

### Cycle detection in an UNDIRECTED graph
**LeetCode:** [Find if Path Exists in Graph](https://leetcode.com/problems/find-if-path-exists-in-graph/) · [Redundant Connection](https://leetcode.com/problems/redundant-connection/)
**When:** detect a cycle / "redundant edge" / "is this a tree" in an undirected graph.
Two clean options. **Union-Find** (see §18): process edges; if both endpoints already share a root, that edge closes a cycle. **DFS-with-parent**: a visited neighbor that isn't the node you came from is a cycle.

```
// Union-Find: first edge whose endpoints are already connected = cycle
function hasCycleUF(n, edges):
  for u, v in edges:
    if find(u) == find(v): return true    // already connected → cycle
    union(u, v)
  return false

// DFS-with-parent
function hasCycleUndirected(u, parent):
  seen.add(u)
  for v in adj[u]:
    if v not in seen:
      if hasCycleUndirected(v, u): return true
    elif v != parent:                      // visited & not where we came from
      return true
  return false
```

**Complexity:** UF O(E·α(V)); DFS O(V+E) / space O(V).
**Traps:** • DFS **must** skip the immediate parent or every edge looks like a 2-cycle. • parallel edges between u,v *are* a real cycle even though they look like parent edges — UF handles this, naive parent-skip may not (track edge id, not just parent node). • "graph is a tree" = connected **and** exactly `V−1` edges **and** acyclic (UF: no cycle + ends with one component).

### Bipartite check / 2-coloring
**LeetCode:** [Is Graph Bipartite?](https://leetcode.com/problems/is-graph-bipartite/) · [Possible Bipartition](https://leetcode.com/problems/possible-bipartition/)
**When:** "can nodes be split into two groups with no edge inside a group?" (possible-bipartition, is-graph-bipartite). Bipartite ⇔ no odd-length cycle.
Color graph with two colors via BFS/DFS; an edge between same-colored nodes ⇒ not bipartite. Run from every component (graph may be disconnected).

```
function isBipartite(n, adj):
  color = array of -1 size n             // -1 = uncolored
  for s in 0..n-1:
    if color[s] != -1: continue
    q = queue; q.push(s); color[s] = 0
    while not q.empty():
      u = q.front(); q.pop()
      for v in adj[u]:
        if color[v] == -1:
          color[v] = 1 - color[u]        // opposite color
          q.push(v)
        elif color[v] == color[u]:
          return false                   // same color across edge → odd cycle
  return true
```

**Complexity:** time O(V+E) / space O(V).
**Traps:** • iterate over **all** components — a single BFS misses disconnected parts. • possible-bipartition gives "dislikes" pairs as edges; build adjacency first, then 2-color. • Union-Find with two "groups" per node is an alternative. • bipartite check ≠ matching — that's Hopcroft–Karp (rarely asked).

### Dijkstra — non-negative weighted shortest path
**LeetCode:** [Network Delay Time](https://leetcode.com/problems/network-delay-time/) · [Path with Maximum Probability](https://leetcode.com/problems/path-with-maximum-probability/) · [Path with Minimum Effort](https://leetcode.com/problems/path-with-minimum-effort/) · [Swim in Rising Water](https://leetcode.com/problems/swim-in-rising-water/)
**When:** shortest path / min cost with **non-negative** weights and you want single-source distances.
Min-heap of `(dist, node)`. Pop the closest unsettled node, relax its edges. Use **lazy deletion**: keep a `dist[]` array; when you pop a `(d, u)` with `d > dist[u]`, it's stale — skip it (no decrease-key needed).

```
function dijkstra(adj, src):
  dist = array of INF size n; dist[src] = 0
  h = []; h.push((0, src))               // (distance, node) min-heap
  while len(h) > 0:
    d, u = h.pop()
    if d > dist[u]: continue             // stale entry → skip (lazy delete)
    for v, w in adj[u]:
      nd = d + w
      if nd < dist[v]:                   // relax
        dist[v] = nd
        h.push((nd, v))                  // push improved; old stays as stale
  return dist
```

Path reconstruction: keep `parent[v] = u` whenever you relax, then walk `parent` from target back to src and reverse.
**Complexity:** time O(E log V) (binary heap) / space O(V+E).
**Traps:** • **fails with negative edges** — a node settled early may later get a cheaper path; use Bellman–Ford. • the `if d > dist[u]: continue` skip is mandatory with a lazy heap, else you reprocess nodes. • push duplicates freely; correctness comes from the staleness check, not from deleting old entries. • applies to network-delay-time (answer = max over `dist`, or `-1` if any unreachable), path-with-max-probability (max-heap, multiply, relax on *larger*), swim-in-rising-water (Dijkstra where edge "cost" = max elevation so far).

### 0-1 BFS — weights are 0 or 1
**LeetCode:** [Minimum Obstacle Removal to Reach Corner](https://leetcode.com/problems/minimum-obstacle-removal-to-reach-corner/) · [Minimum Cost to Make at Least One Valid Path in a Grid](https://leetcode.com/problems/minimum-cost-to-make-at-least-one-valid-path-in-a-grid/)
**When:** shortest path where every edge costs **0 or 1** (e.g., min sign-flips, min obstacle removal, min moves where some are free).
A deque replaces Dijkstra's heap: relax a **0-edge → pushFront**, a **1-edge → pushBack**. This keeps the deque sorted by distance with O(1) ops.

```
function zeroOneBFS(adj, src):
  dist = array of INF size n; dist[src] = 0
  dq = deque; dq.pushFront((0, src))
  while not dq.empty():
    d, u = dq.popFront()
    if d > dist[u]: continue
    for v, w in adj[u]:                   // w is 0 or 1
      nd = d + w
      if nd < dist[v]:
        dist[v] = nd
        if w == 0: dq.pushFront((nd, v))  // free move → front
        else: dq.pushBack((nd, v))        // costly move → back
  return dist
```

**Complexity:** time O(V+E) / space O(V) — beats Dijkstra's log factor.
**Traps:** • only valid when weights ∈ {0,1}; anything else needs Dijkstra. • keep the staleness skip. • grid problems like "min cost where moving in the arrow direction is free (0) and turning costs 1."

### Bellman–Ford — negative edges & ≤ k stops
**LeetCode:** [Network Delay Time](https://leetcode.com/problems/network-delay-time/) · [Cheapest Flights Within K Stops](https://leetcode.com/problems/cheapest-flights-within-k-stops/)
**When:** shortest path that may have **negative edges**, or detect a negative cycle, or shortest path with **at most k edges**.
Relax **all** edges V−1 times (after `i` rounds, all shortest paths using ≤ i edges are correct). A V-th round that still relaxes ⇒ a negative cycle. For "≤ k stops," run exactly k+1 rounds **over a snapshot** of last round's distances.

```
function bellmanFord(n, edges, src):
  dist = array of INF size n; dist[src] = 0
  for i in 0..n-2:                        // V-1 rounds
    for u, v, w in edges:
      if dist[u] != INF and dist[u] + w < dist[v]:
        dist[v] = dist[u] + w
  // optional: one more round relaxes ⇒ negative cycle reachable
  return dist

// Cheapest flights within K stops (LC 787): snapshot prevents extra-hop bleed
function cheapestKStops(n, edges, src, dst, K):
  dist = array of INF size n; dist[src] = 0
  for i in 0..K:                          // K stops = K+1 edges
    snap = copy(dist)                     // relax off the PREVIOUS round only
    for u, v, w in edges:
      if snap[u] != INF and snap[u] + w < dist[v]:
        dist[v] = snap[u] + w
  return dist[dst] if dist[dst] != INF else -1
```

**Complexity:** time O(V·E) (k-stops: O(k·E)) / space O(V).
**Traps:** • **k-stops needs the `snap` copy** — relaxing in place lets one round propagate through multiple edges, overcounting hops. • guard `dist[u] != INF` before adding `w` or INF+negative underflows to a fake short path. • "within K stops" means K intermediate nodes = K+1 edges = K+1 rounds. • SPFA (queue-based Bellman–Ford) is a constant-factor speedup, same worst case.

### Floyd–Warshall — all-pairs shortest path
**LeetCode:** [Find the City With the Smallest Number of Neighbors at a Threshold Distance](https://leetcode.com/problems/find-the-city-with-the-smallest-number-of-neighbors-at-a-threshold-distance/) · [Course Schedule IV](https://leetcode.com/problems/course-schedule-iv/)
**When:** shortest path between **every** pair, V small (≤ ~400–500), or transitive closure / reachability matrix.
Triple loop with **k (intermediate node) outermost**: `dist[i][j] = min(dist[i][j], dist[i][k] + dist[k][j])`. The k-outermost order is the DP correctness condition.

```
function floydWarshall(n, w):             // w[i][j] = edge or INF, w[i][i]=0
  dist = copy(w)
  for k in 0..n-1:                        // intermediate — MUST be outermost
    for i in 0..n-1:
      for j in 0..n-1:
        if dist[i][k] != INF and dist[k][j] != INF:
          if dist[i][k] + dist[k][j] < dist[i][j]:
            dist[i][j] = dist[i][k] + dist[k][j]
  return dist
```

**Complexity:** time O(V³) / space O(V²).
**Traps:** • **k must be the outer loop** — i/j outer gives wrong results. • initialize `dist[i][i] = 0`, missing edges = INF, and guard against INF+INF overflow. • negative cycle iff any `dist[i][i] < 0` after running. • for transitive closure, replace `min/+` with `or/and` (reachability). • O(V³) means V≈500 is the ceiling; bigger ⇒ run Dijkstra from each node.

### Minimum spanning tree — Kruskal & Prim
**LeetCode:** [Min Cost to Connect All Points](https://leetcode.com/problems/min-cost-to-connect-all-points/)
**When:** minimum total edge weight to connect all nodes (min-cost-to-connect-points/cities, network wiring). Undirected, connected (or count components if not).
**Kruskal:** sort edges ascending, add an edge iff its endpoints aren't already united (Union-Find, §18) — greedy + cycle avoidance. **Prim:** grow a tree from one node, always pulling the cheapest edge crossing the frontier via a min-heap.

```
// Kruskal — best when edges given / sparse
function kruskal(n, edges):              // edges: (w, u, v)
  edges.sort()                            // ascending weight
  total = 0; used = 0
  for w, u, v in edges:
    if find(u) != find(v):                // doesn't form a cycle
      union(u, v); total += w; used += 1
      if used == n - 1: break             // tree complete
  return total

// Prim — best when dense / adjacency given
function prim(adj, n):
  inMST = array of false size n
  h = []; h.push((0, 0))                  // (weight, node)
  total = 0; count = 0
  while len(h) > 0 and count < n:
    w, u = h.pop()
    if inMST[u]: continue                 // lazy skip
    inMST[u] = true; total += w; count += 1
    for v, wt in adj[u]:
      if not inMST[v]: h.push((wt, v))
  return total
```

**Complexity:** Kruskal O(E log E); Prim O(E log V) / space O(V+E).
**Traps:** • MST is **undirected** — building directed adjacency breaks Prim. • a complete graph on points (min-cost-to-connect-points) has E = O(V²); Prim with adjacency is often better there. • disconnected graph has no spanning tree — Kruskal ends with `used < n−1` (count components). • Prim needs the lazy `if inMST[u]: continue` skip just like Dijkstra's staleness check. • for max-spanning-tree, sort descending / use a max-heap.

### Strongly connected components — Tarjan / Kosaraju
**LeetCode:** [Critical Connections in a Network](https://leetcode.com/problems/critical-connections-in-a-network/)
**When:** find maximal subsets where every node reaches every other (directed), or condense a digraph into a DAG. Related: **bridges & articulation points** (Tarjan low-link).
**Kosaraju (two passes):** DFS to record finish order; DFS the **transposed** graph in reverse finish order — each tree is one SCC. **Tarjan (one pass):** DFS tracking `disc` (discovery time) and `low` (lowest reachable disc); a node with `low == disc` roots an SCC popped off a stack. Critical-connections/**bridges** use the same low-link in one pass: edge `(u,v)` is a bridge iff `low[v] > disc[u]`.

```
// Tarjan bridges (critical connections, LC 1192) — one DFS, low-link
timer = 0
function dfs(u, parent):
  disc[u] = low[u] = timer; timer += 1
  for v in adj[u]:
    if v == parent: continue
    if disc[v] == -1:                      // tree edge
      dfs(v, u)
      low[u] = min(low[u], low[v])
      if low[v] > disc[u]:                 // nothing below v reaches u or above
        bridges.append((u, v))
    else:                                  // back edge
      low[u] = min(low[u], disc[v])
```

**Complexity:** time O(V+E) / space O(V).
**Traps:** • Tarjan's `low` updates differ for tree edges (use child's `low`) vs back edges (use neighbor's `disc`). • **bridge** condition is strict `>` (`low[v] > disc[u]`); **articulation point** condition is `>=` plus a special root-has-≥2-children rule. • Kosaraju needs the **transpose** (all edges reversed) for pass 2. • SCC is a *directed* concept; for undirected "components" just use BFS/DFS or Union-Find.

### Eulerian path — Hierholzer
**LeetCode:** [Reconstruct Itinerary](https://leetcode.com/problems/reconstruct-itinerary/) · [Valid Arrangement of Pairs](https://leetcode.com/problems/valid-arrangement-of-pairs/)
**When:** use **every edge exactly once** (reconstruct-itinerary, valid-arrangement-of-pairs). Exists in a directed graph iff connected and indegree==outdegree for all (Euler circuit) or one node has out−in=1 (start) and one has in−out=1 (end).
Hierholzer: DFS consuming edges; **post-order append** to the path, then reverse. A node is added only after all its outgoing edges are walked.

```
function hierholzer(adj, start):          // adj[u] = stack/sorted of destinations
  route = []
  st = [start]
  while not st.empty():
    u = st.top()
    if len(adj[u]) > 0:
      v = adj[u].pop()                     // consume an edge
      st.push(v)
    else:
      route.append(st.pop())               // post-order: dead end
  reverse(route)
  return route
```

**Complexity:** time O(E) (O(E log E) if you sort destinations for lexicographic order) / space O(E).
**Traps:** • **post-order + reverse** is the whole trick — appending in pre-order gives garbage when you backtrack out of a dead end. • reconstruct-itinerary wants lexicographically smallest → consume destinations in sorted order (min-heap / sorted list popped from the front). • each edge consumed exactly once — remove it from adjacency as you traverse.

### Graph traps (cross-cutting)
**Traps:** • **visited timing**: BFS marks on enqueue (else duplicates + wrong distance); iterative DFS marks on pop + `continue` if seen. • **directed vs undirected**: insert both directions for undirected; cycle detection, SCC, and parent-skip all hinge on this. • **Dijkstra/Prim staleness**: with a lazy heap you must skip popped entries that are out of date (`d > dist[u]` / `inMST[u]`). • **self-loops & parallel edges**: break naive parent-skip cycle detection and Euler; track edge ids. • **disconnected graphs**: loop over *all* start nodes for components/bipartite/topo; a single traversal misses parts. • **recursion depth**: deep grids/chains overflow the call stack — use BFS or an explicit stack. • **negative edges** silently break BFS and Dijkstra — switch to Bellman–Ford; **negative cycles** make "shortest path" undefined.

## 16. Greedy

Make the locally optimal choice and never reconsider it. Fast and simple **when correct** — but greedy is wrong far more often than DP is, so the senior move is to *prove it* (exchange argument) or kill it with a counterexample before coding. Most greedy problems reduce to "sort by the right key, then one pass."

### When greedy is provably correct
**When:** you suspect a sort-then-scan solution, but must justify it before trusting it.
Two proof tools. **Exchange argument:** assume an optimal solution differs from the greedy one; swap in the greedy choice and show the result is no worse — so a greedy-matching optimum exists. **Greedy-choice + optimal-substructure:** the first greedy pick is part of *some* optimum, and what remains is the same problem on a smaller input. **Matroid intuition (brief):** if the problem's feasible sets form a matroid (independence preserved under subsets + exchange), greedy by weight is optimal — that's why MST (Kruskal) and interval scheduling work.

> Say this in the room: "Greedy is a hypothesis, not a strategy. I'll state the choice, give a one-line exchange argument, then look for a counterexample. If I can't prove it, I fall back to DP."

**The discipline:** before coding greedy, ask — *"does the locally optimal choice provably stay in some global optimum?"* If yes, prove it. If you can construct any input where the greedy pick forecloses a better total, greedy is dead → DP/search.
**Traps:** • "it passed the examples" is not a proof — examples rarely include the adversarial case. • the hard part is *which* greedy key (earliest finish? largest ratio? highest first?), not the loop. • when in doubt, DP is the safe (slower) fallback.

### Interval scheduling / activity selection
**LeetCode:** [Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/) · [Maximum Length of Pair Chain](https://leetcode.com/problems/maximum-length-of-pair-chain/) · [Maximum Number of Events That Can Be Attended](https://leetcode.com/problems/maximum-number-of-events-that-can-be-attended/)
**When:** pick the **maximum number** of mutually non-overlapping intervals (max activities, non-overlapping-intervals min-removals — see §6).
Sort by **end time**; greedily take an interval whenever it starts after the last taken one ends. *Exchange-argument proof:* the earliest-finishing activity leaves the most room for the rest, so some optimum includes it.

```
function maxActivities(A):
  A.sort(key = lambda iv: iv[1])         // by END (not start!)
  count = 0; lastEnd = -INF
  for s, e in A:
    if s >= lastEnd:                      // compatible with last chosen
      count += 1; lastEnd = e
  return count
```

**Complexity:** time O(n log n) / space O(1).
**Traps:** • sort by **finish** time, not start — sorting by start is the classic wrong greedy (one long early job blocks many). • min-removals = `n − maxActivities` (LC 435, see §6). • `>=` vs `>` decides whether touching endpoints conflict — match the problem.

### Jump Game — reachability (furthest reach)
**LeetCode:** [Jump Game](https://leetcode.com/problems/jump-game/) · [Jump Game III](https://leetcode.com/problems/jump-game-iii/)
**When:** can you reach the last index, given each `A[i]` = max jump length?
Track the **furthest reachable** index in one pass; if you ever stand on an index beyond the current reach, you're stuck.

```
function canJump(A):
  reach = 0
  for i in 0..len(A)-1:
    if i > reach: return false            // can't even get here
    reach = max(reach, i + A[i])
    if reach >= len(A)-1: return true
  return true
```

**Complexity:** time O(n) / space O(1).
**Traps:** • check `i > reach` *before* updating reach. • greedy beats the O(n²) DP cleanly here. • `reach` is the furthest index, not a count of jumps.

### Jump Game II — minimum jumps (BFS-like levels)
**LeetCode:** [Jump Game II](https://leetcode.com/problems/jump-game-ii/)
**When:** *fewest* jumps to reach the end. Greedy = implicit BFS over "jump levels."
Maintain the current jump's `curEnd` (furthest reachable with `jumps` jumps) and the `farthest` reachable seen while scanning it. When `i` hits `curEnd`, you must jump: increment, set `curEnd = farthest`.

```
function jump(A):
  jumps = 0; curEnd = 0; farthest = 0
  for i in 0..len(A)-2:                   // no need to act on last index
    farthest = max(farthest, i + A[i])
    if i == curEnd:                        // end of current jump's range
      jumps += 1
      curEnd = farthest
  return jumps
```

**Complexity:** time O(n) / space O(1).
**Traps:** • loop to `n−2`: landing *on* the last index needs no further jump. • each "level" is the set of indices reachable with `jumps` jumps — this is BFS without a queue. • assumes the end is reachable (Jump Game I question).

### Gas station — circular tank
**LeetCode:** [Gas Station](https://leetcode.com/problems/gas-station/)
**When:** can you complete a circular route; if so from which start, given `gas[]` and `cost[]`?
If `sum(gas) < sum(cost)` it's impossible. Otherwise a unique start exists: scan keeping a running `tank`; whenever it drops below 0, **no station in `[start..i]` can be the answer**, so reset `start = i+1`, `tank = 0`.

```
function canCompleteCircuit(gas, cost):
  if sum(gas) < sum(cost): return -1      // globally infeasible
  start = 0; tank = 0
  for i in 0..len(gas)-1:
    tank += gas[i] - cost[i]
    if tank < 0:                          // can't reach i+1 from start
      start = i + 1                       // skip the whole failed segment
      tank = 0
  return start
```

**Complexity:** time O(n) / space O(1).
**Traps:** • the total check is essential — it both proves feasibility and guarantees the found `start` works (no need to re-verify). • reset `start` to `i+1`, not `i`. • the key insight: if the prefix sum from `start` goes negative at `i`, *every* start in that prefix also fails, so jump past it.

### Candy — two passes
**LeetCode:** [Candy](https://leetcode.com/problems/candy/)
**When:** assign candies so each child gets ≥1 and any child with a higher rating than a neighbor gets more; minimize total.
A single greedy pass can't satisfy both directions. **Two passes:** left→right enforce the left-neighbor rule, right→left enforce the right-neighbor rule with `max` so neither constraint is lost.

```
function candy(ratings):
  n = len(ratings)
  c = array of 1 size n
  for i in 1..n-1:                         // left → right
    if ratings[i] > ratings[i-1]: c[i] = c[i-1] + 1
  for i in n-2..0:                         // right → left (descending)
    if ratings[i] > ratings[i+1]:
      c[i] = max(c[i], c[i+1] + 1)         // keep the larger requirement
  return sum(c)
```

**Complexity:** time O(n) / space O(n).
**Traps:** • the second pass must `max` against the first, not overwrite — both neighbor constraints must hold simultaneously. • equal ratings impose no constraint (strict `>`). • one pass is insufficient; this is the canonical "two sweeps" greedy.

### Partition labels — last-index map
**LeetCode:** [Partition Labels](https://leetcode.com/problems/partition-labels/)
**When:** cut a string into the fewest parts so each letter appears in only one part.
Precompute each char's **last** index. Sweep; extend the current partition's `end` to the max last-index of any char seen; when `i == end`, the partition is closed.

```
function partitionLabels(s):
  last = {}                                // char → last index
  for i in 0..len(s)-1: last[s[i]] = i
  res = []; start = 0; end = 0
  for i in 0..len(s)-1:
    end = max(end, last[s[i]])             // partition must reach this char's last seen
    if i == end:                           // every char so far ends by here
      res.append(end - start + 1)
      start = i + 1
  return res
```

**Complexity:** time O(n) / space O(1) (alphabet-bounded map).
**Traps:** • close on `i == end`, then reset `start = i+1`. • it's the same furthest-reach trick as Jump Game II, applied to last-occurrence. • build the last-index map fully before the sweep.

### Task scheduler — most-frequent-first
**LeetCode:** [Task Scheduler](https://leetcode.com/problems/task-scheduler/) · [Reorganize String](https://leetcode.com/problems/reorganize-string/)
**When:** schedule tasks with a cooldown `n` between identical tasks; minimize total time slots (LC 621). Greedy by frequency (heap-based form in §11).
Place the **most frequent** task as the skeleton: `(maxCount − 1)` full frames of length `(n+1)`, plus the number of tasks tying that max in the last frame. Answer = `max(len(tasks), (maxCount−1)*(n+1) + tiesAtMax)`.

```
function leastInterval(tasks, n):
  freq = count of each task
  maxCount = max(freq.values())
  ties = number of tasks with freq == maxCount
  frame = (maxCount - 1) * (n + 1) + ties
  return max(len(tasks), frame)            // can't be shorter than #tasks
```

**Complexity:** time O(T) counting / space O(1) (26 letters).
**Traps:** • `max(len(tasks), frame)`: when there are many distinct tasks, no idle time is needed and the answer is just the task count. • `ties` counts *all* tasks sharing the max frequency (they fill the final partial frame). • the equivalent max-heap simulation (pop up to n+1 tasks per round, decrement, re-push) is in §11 — same answer.

### Assign cookies / lemonade change / boats (simple greedies)
**LeetCode:** [Assign Cookies](https://leetcode.com/problems/assign-cookies/) · [Lemonade Change](https://leetcode.com/problems/lemonade-change/) · [Boats to Save People](https://leetcode.com/problems/boats-to-save-people/)
**When:** small "match or make change" greedies that fall out of sorting or a fixed denomination order.
**Assign cookies** (LC 455): sort children's greed and cookie sizes; two pointers, give the smallest sufficient cookie to the least greedy child. **Lemonade change** (LC 860): greedily give change preferring \$10s over \$5s (keep \$5s flexible). **Boats to save people** (LC 881, two-pointer greedy): sort; pair heaviest with lightest if they fit, else heaviest goes alone.

```
// assign cookies
function findContentChildren(g, s):
  g.sort(); s.sort()
  i = 0; j = 0                              // i child, j cookie
  while i < len(g) and j < len(s):
    if s[j] >= g[i]: i += 1                 // cookie satisfies child
    j += 1
  return i

// boats to save people (limit per boat)
function numRescueBoats(people, limit):
  people.sort()
  i = 0; j = len(people) - 1; boats = 0
  while i <= j:
    if people[i] + people[j] <= limit: i += 1   // lightest also fits
    j -= 1                                        // heaviest always boards
    boats += 1
  return boats
```

**Complexity:** assign/boats O(n log n); lemonade O(n) / space O(1).
**Traps:** • boats: heaviest person *always* takes a seat each iteration (`j -= 1` unconditional); only the lightest may join. • lemonade: prefer giving back a \$10 first to preserve scarce \$5s. • assign cookies advances the cookie pointer every step but the child pointer only on a match.

### Minimum arrows to burst balloons
**LeetCode:** [Minimum Number of Arrows to Burst Balloons](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/)
**When:** fewest points to "stab" all intervals (LC 452) — interval-stabbing greedy (see §6).
Sort by **end**; fire an arrow at the current interval's end; it bursts every later interval starting ≤ that position. New arrow only when an interval starts strictly after the last arrow.

```
function findMinArrowShots(points):
  points.sort(key = lambda iv: iv[1])      // by END
  arrows = 1; arrowAt = points[0][1]
  for s, e in points[1:]:
    if s > arrowAt:                         // not covered → new arrow
      arrows += 1; arrowAt = e
  return arrows
```

**Complexity:** time O(n log n) / space O(1).
**Traps:** • touching balloons *are* burst → guard is strict `s > arrowAt`. • equals `n − maxNonOverlapping`; same machinery as activity selection, opposite framing. • watch coordinate overflow if you ever subtract near INT bounds.

### Queue reconstruction by height
**LeetCode:** [Queue Reconstruction by Height](https://leetcode.com/problems/queue-reconstruction-by-height/)
**When:** rebuild a queue from `(height, k)` where `k` = number of taller-or-equal people in front (LC 406).
Sort by **height descending, then k ascending**; insert each person at index `k`. Since taller people are placed first, inserting at `k` is exact — shorter later insertions don't disturb the count of taller ones already placed.

```
function reconstructQueue(people):
  people.sort(key = lambda p: (-p.height, p.k))   // tall first, then small k
  res = []
  for p in people:
    res.insert(p.k, p)                      // index k among already-placed (taller)
  return res
```

**Complexity:** time O(n²) (list inserts) / space O(n).
**Traps:** • the sort key is the whole trick: tallest first so "k taller in front" is satisfiable by raw index. • among equal heights, smaller k first. • inserting at index `k` works only because everyone already in `res` is ≥ this person's height.

### Hand of straights / divide into consecutive groups
**LeetCode:** [Hand of Straights](https://leetcode.com/problems/hand-of-straights/) · [Divide Array in Sets of K Consecutive Numbers](https://leetcode.com/problems/divide-array-in-sets-of-k-consecutive-numbers/)
**When:** can you partition cards into groups of `k` consecutive values (LC 846 / 1296)?
Count values; repeatedly start a group at the **smallest** remaining value and consume `value, value+1, …, value+k−1`, decrementing counts; fail if any required value is missing.

```
function isNStraightHand(hand, k):
  if len(hand) % k != 0: return false
  cnt = count of each card
  for v in sorted(cnt.keys()):
    if cnt[v] > 0:
      need = cnt[v]                          // must start this many groups here
      for x in v..v+k-1:
        if cnt.get(x, 0) < need: return false
        cnt[x] -= need
  return true
```

**Complexity:** time O(n log n) (or O(n + U log U)) / space O(U).
**Traps:** • always start the next group at the smallest leftover value (a min-heap or sorted keys). • the smallest value's count dictates how many groups must begin there — consume that many at once. • length divisible by k is a necessary precheck.

### Fractional knapsack — greedy by value/weight ratio
**LeetCode:** [Maximum Units on a Truck](https://leetcode.com/problems/maximum-units-on-a-truck/) · [Maximum Bags With Full Capacity of Rocks](https://leetcode.com/problems/maximum-bags-with-full-capacity-of-rocks/)
**When:** maximize value within a weight budget when items are **divisible** (you can take fractions).
Sort by **value/weight ratio** descending; take whole items greedily, then a fraction of the next to fill the remaining capacity. Greedy is *provably optimal here* (exchange argument: swapping any weight to a higher-ratio item never lowers value).

```
function fractionalKnapsack(items, cap):  // items: (value, weight)
  items.sort(key = lambda it: -it.value / it.weight)   // best ratio first
  total = 0
  for v, w in items:
    if cap >= w:
      total += v; cap -= w                  // take whole item
    else:
      total += v * (cap / w)                // take fraction, budget full
      break
  return total
```

**Complexity:** time O(n log n) / space O(1).
**Traps:** • greedy works *only because items are divisible*. • for **0/1 knapsack (indivisible)** greedy-by-ratio FAILS — needs DP (see §14). • compare ratios, not raw values.

### Greedy vs DP — the canonical contrast (0/1 knapsack)
**When:** deciding whether your "sort and pick best ratio" idea is legitimate.
The split: **fractional** knapsack → greedy by ratio is optimal; **0/1** knapsack → greedy by ratio is *wrong*, you need DP. Counterexample for 0/1: capacity 4, items `(v=3,w=2,ratio 1.5)`, `(v=5,w=4,ratio 1.25)`, `(v=3,w=2,ratio 1.5)` — greedy takes the two ratio-1.5 items (value 6), which happens to win here; but flip to capacity 50 with items `(60,10),(100,20),(120,30)`: greedy-by-ratio takes `(60,10)+(100,20)`=160 leaving cap 20, total **160**, while the optimal `(100,20)+(120,30)`=**220**. Locally best ratio forecloses the better whole-item combo.

| | Greedy | DP |
|---|---|---|
| Decision | locally optimal, never revisited | tries/combines subproblem optima |
| Correctness needs | exchange argument / matroid | optimal substructure + overlapping subproblems |
| Cost | usually O(n log n) | often O(n·capacity) / O(n²) |
| Fractional knapsack | ✅ optimal (by ratio) | overkill |
| 0/1 knapsack | ❌ wrong | ✅ required |
| Coin change (min coins, arbitrary denoms) | ❌ wrong | ✅ required |
| Activity selection / MST | ✅ optimal | overkill |

> Say this in the room: "Fractional knapsack is greedy by ratio; 0/1 knapsack is DP. The moment a *locally* best pick can block a better whole-item combination, greedy dies and I switch to DP."

**Traps:** • the existence of a clean ratio/sort key tempts greedy even when items are indivisible — check divisibility. • coin change with non-canonical denominations is the other classic "greedy looks right, is wrong" (e.g. coins {1,3,4} for 6: greedy 4+1+1=3 coins, optimal 3+3=2).

### Huffman coding / connect ropes — repeated min-merge
**LeetCode:** [Last Stone Weight](https://leetcode.com/problems/last-stone-weight/)
**When:** build an optimal prefix code, or minimize total cost of merging items where each merge costs the sum of the two (connect-ropes LC 1167, optimal merge pattern). Min-heap greedy (see §11).
Repeatedly pop the **two smallest**, merge them (cost = their sum, added to the running total), push the merged weight back. Greedy is optimal: least-frequent symbols sit deepest, so frequent ones get shorter codes.

```
function minCostConnectRopes(ropes):
  h = []
  for r in ropes: h.push(r)               // min-heap of lengths
  total = 0
  while len(h) > 1:
    a = h.pop(); b = h.pop()              // two smallest
    cost = a + b
    total += cost
    h.push(cost)                           // merged rope back into the heap
  return total
```

**Complexity:** time O(n log n) / space O(n).
**Traps:** • always merge the **two smallest** — merging large ropes early inflates the total (they get re-counted each later merge). • Huffman builds the tree by the same merges, with frequencies as weights and the merged node as an internal node. • a plain sort once is *not* enough — the merged weight re-enters the ordering, so you need the heap.

### Two-pointer greedy — container with most water
**LeetCode:** [Container With Most Water](https://leetcode.com/problems/container-with-most-water/) · [Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/)
**When:** maximize/optimize a quantity over a pair of indices where moving a pointer is a *greedy* choice (container-with-most-water LC 11; see §4).
Two pointers from the ends; area = `min(height[l], height[r]) * (r − l)`. Always move the **shorter** wall inward — moving the taller one can only shrink the width without raising the limiting height.

```
function maxArea(height):
  l = 0; r = len(height) - 1; best = 0
  while l < r:
    best = max(best, min(height[l], height[r]) * (r - l))
    if height[l] < height[r]: l += 1        // move the shorter wall
    else: r -= 1
  return best
```

**Complexity:** time O(n) / space O(1).
**Traps:** • move the **shorter** wall — that's the exchange argument: the short wall caps the area, so keeping it can never help. • on ties (`==`) move either. • this is greedy, not sliding window — the width only shrinks.

### Greedy traps (cross-cutting)
**Traps:** • **unsorted input** — most greedies *require* sorting first; the wrong sort key (or none) silently yields a wrong answer, not a crash. • **ties** — equal end-times, equal heights, equal ratios often need a secondary sort key or a strict/non-strict (`<` vs `<=`) decision tied to the problem's endpoint semantics. • **proving correctness** — always have a one-line exchange argument; if you can't produce one, suspect DP. • **counterexamples** — coin change (non-canonical denominations) and 0/1 knapsack are the textbook "greedy looks right but isn't"; keep them as litmus tests. • **off-by-one in the scan** — Jump Game II loops to `n−2`, gas-station resets to `i+1`, partition-labels closes at `i == end`: each has a specific boundary. • **don't reconsider** — greedy commits; if the problem needs to *undo* a choice, it's backtracking/DP, not greedy.

## 17. String algorithms

Reach here when naive O(nm) substring matching is too slow — large text, many queries, or a "longest palindrome / longest repeated substring" twist that needs a linear or near-linear trick. For everyday `indexOf`, naive (or the stdlib) is fine; pull out KMP/Z/Rabin–Karp/Manacher when n·m blows the budget or the structure of the failure/prefix function buys you something extra (period detection, palindrome counting).

### Naive matching
**LeetCode:** [Find the Index of the First Occurrence in a String](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/) · [Repeated Substring Pattern](https://leetcode.com/problems/repeated-substring-pattern/)
**When:** small text, one-off search, or you just need it correct and readable. Honestly the right call in most interviews unless the constraints scream otherwise.
Slide the pattern over every start position and compare char-by-char; bail on first mismatch.
```
function naiveSearch(text, pat):
    n = len(text); m = len(pat)
    for i in 0..n-m:
        j = 0
        while j < m and text[i+j] == pat[j]:
            j += 1
        if j == m:
            return i          // first match index
    return -1
```
**Complexity:** time O(n·m) worst (e.g. `aaaa…a` vs `aaab`) / space O(1).
**Traps:** • guard the loop bound `i in 0..n-m`, not `0..n-1`. • empty pattern conventionally matches at 0. • worst case is real on pathological repeats — that's the cue to upgrade.

> Say this in the room: "I'd start with naive O(nm); if n·m is ~10^10 or there are many queries I'd switch to KMP or rolling-hash."

### Expand-around-center (palindromes)
**LeetCode:** [Longest Palindromic Substring](https://leetcode.com/problems/longest-palindromic-substring/) · [Palindromic Substrings](https://leetcode.com/problems/palindromic-substrings/)
**When:** longest palindromic substring or count-palindromic-substrings, and O(n²) is acceptable (n ≤ ~10^3–10^4). This is the *good-enough* answer — reach for Manacher only if O(n) is demanded.
Every palindrome has a center: n single-char centers + (n−1) between-char centers. Expand outward from each while characters mirror.
```
function longestPalindrome(s):
    if len(s) == 0: return ""
    start = 0; end = 0
    for c in 0..len(s)-1:
        l1 = expand(s, c, c)       // odd-length center
        l2 = expand(s, c, c+1)     // even-length center
        best = max(l1, l2)
        if best > end - start + 1:
            start = c - (best - 1) / 2     // integer division
            end   = c + best / 2
    return s[start..end]           // inclusive

function expand(s, l, r):
    while l >= 0 and r < len(s) and s[l] == s[r]:
        l -= 1; r += 1
    return r - l - 1               // length of the palindrome found

// Count palindromic substrings: sum of expansions
function countSubstrings(s):
    count = 0
    for c in 0..len(s)-1:
        count += countFrom(s, c, c) + countFrom(s, c, c+1)
    return count

function countFrom(s, l, r):
    cnt = 0
    while l >= 0 and r < len(s) and s[l] == s[r]:
        cnt += 1; l -= 1; r += 1
    return cnt
```
**Complexity:** time O(n²) / space O(1).
**Traps:** • do BOTH odd and even centers — forgetting even centers misses `"abba"`. • the `start`/`end` recovery from a center+length is the off-by-one everyone botches: with even/odd length `best`, `start = c - (best-1)/2`. • `expand` returns `r - l - 1` because the loop overshoots by one on each side.

### KMP (Knuth–Morris–Pratt)
**LeetCode:** [Find the Index of the First Occurrence in a String](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/) · [Repeated Substring Pattern](https://leetcode.com/problems/repeated-substring-pattern/) · [Shortest Palindrome](https://leetcode.com/problems/shortest-palindrome/)
**When:** single-pattern exact match in guaranteed O(n+m), or any problem that secretly needs the **prefix function** — string period, shortest-palindrome prefix, repeated-substring-pattern.
Precompute `lps` ("longest proper prefix that is also a suffix"). `lps[i]` = length of the longest border of `pat[0..i]` (inclusive). On a mismatch at pattern index `j`, don't restart — jump `j = lps[j-1]`, because that prefix is already matched.
```
// Build failure / LPS array for the pattern.
function buildLPS(pat):
    m = len(pat)
    lps = array(m, 0)
    length = 0                  // length of current border
    i = 1
    while i < m:
        if pat[i] == pat[length]:
            length += 1
            lps[i] = length
            i += 1
        elif length > 0:
            length = lps[length-1]   // fall back, do NOT advance i
        else:
            lps[i] = 0
            i += 1
    return lps

// Search using the LPS array.
function kmpSearch(text, pat):
    n = len(text); m = len(pat)
    if m == 0: return 0
    lps = buildLPS(pat)
    i = 0; j = 0               // i over text, j over pattern
    while i < n:
        if text[i] == pat[j]:
            i += 1; j += 1
            if j == m:
                return i - j           // match start; or record & j = lps[j-1] to find all
        elif j > 0:
            j = lps[j-1]               // reuse the border, don't move i
        else:
            i += 1
    return -1
```
**Applications.** `strStr`/`indexOf` (above). **Shortest palindrome** (prepend fewest chars to make `s` a palindrome): build `combined = s + "#" + reverse(s)`, take `lps[-1]` = longest palindromic prefix of `s`; answer = `reverse(s[lps[-1]..]) + s`. **Repeated substring pattern** (is `s` a repeat of a block?): let `L = lps[n-1]`; `s` is periodic iff `L > 0 and n % (n - L) == 0`. **String period:** smallest period = `n - lps[n-1]`.
**Complexity:** time O(n+m) (build O(m), search O(n)) / space O(m).
**Traps:** • LPS is *proper* prefix/suffix — never the whole string. • in `buildLPS`, on mismatch with `length>0` you fall back via `length = lps[length-1]` and do **not** increment `i`. • the `#` separator in shortest-palindrome must be a char absent from the alphabet, else borders leak across the seam. • to find *all* matches, after `j==m` set `j = lps[j-1]` instead of returning.

### Rabin–Karp (rolling hash)
**LeetCode:** [Repeated Substring Pattern](https://leetcode.com/problems/repeated-substring-pattern/) · [Repeated DNA Sequences](https://leetcode.com/problems/repeated-dna-sequences/) · [Distinct Echo Substrings](https://leetcode.com/problems/distinct-echo-substrings/)
**When:** multiple patterns of the same length, "does substring X repeat?", longest duplicate / longest repeating substring (binary-search the length + hash all windows), or you want average O(n) with simple code. Also the go-to when you need to compare many substrings for equality fast.
Hash the pattern and each text window as a base-B polynomial mod a large prime. Slide the window with an O(1) update: drop the leading char's contribution, shift, add the new char. **Double-hash** (two independent (base, mod) pairs) to make collisions astronomically unlikely — single hash is forgeable and collision-prone.
```
// Rolling polynomial hash, single pattern; verify on hash hit.
function rabinKarp(text, pat):
    n = len(text); m = len(pat)
    if m > n: return -1
    B = 256; MOD = 1_000_000_007
    powTop = 1                          // B^(m-1) % MOD
    for k in 0..m-2: powTop = (powTop * B) % MOD
    hPat = 0; hWin = 0
    for k in 0..m-1:
        hPat = (hPat * B + code(pat[k])) % MOD
        hWin = (hWin * B + code(text[k])) % MOD
    for i in 0..n-m:
        if hWin == hPat and text[i..i+m-1] == pat:   // verify! hash can collide
            return i
        if i < n - m:                                 // roll to next window
            // drop text[i], shift, add text[i+m]
            hWin = (hWin - code(text[i]) * powTop) % MOD
            hWin = (hWin * B + code(text[i+m])) % MOD
            hWin = (hWin + MOD) % MOD                  // keep non-negative
    return -1
```
**Rolling-update formula:** `newHash = ((oldHash − code(out)·B^(m−1)) · B + code(in)) mod MOD`. **Double hashing:** carry `(h1, h2)` with `(B1,MOD1)`,`(B2,MOD2)`; two windows are "equal" only if both match — then you can usually skip the char-by-char verify.
**Applications.** **Multiple-pattern same length:** put pattern hashes in a set, slide one window, check membership. **Longest duplicate substring** (`Longest Duplicate Substring`): binary-search length L; for each L, hash all n−L+1 windows into a set and look for a repeat → `O(n log n)` expected. **Longest repeating substring**, **distinct substrings of length k**, **find repeated DNA sequences** all follow the same hash-the-windows pattern.
**Complexity:** time O(n+m) average, O(n·m) worst when collisions force repeated verification (adversarial input / weak modulus); the binary-search-the-length variant is O(n log n) expected; space O(1) (or O(n) for the window-hash set).
**Traps:** • integer overflow — take `mod` after every multiply; in Java use `long` and re-add `MOD` to kill negatives. • **always verify** on a hash hit unless you're double-hashing and accept the tiny risk. • precompute `B^(m−1) mod MOD` once, not per step. • a small/round modulus invites collisions and anti-hash test cases — use a big prime (and randomize the base if adversarial).

### Z-algorithm
**LeetCode:** [Find the Index of the First Occurrence in a String](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/) · [Shortest Palindrome](https://leetcode.com/problems/shortest-palindrome/) · [Sum of Scores of Built Strings](https://leetcode.com/problems/sum-of-scores-of-built-strings/)
**When:** pattern matching, counting occurrences, or any "longest common prefix between the string and each of its suffixes" question. Often the cleaner alternative to KMP and just as fast.
`Z[i]` = length of the longest substring starting at `i` that is also a prefix of the whole string (`Z[0]` undefined/0). Maintain the rightmost match window `[L, R]`; inside it, reuse the mirror `Z[i-L]`, then extend past `R` by brute force.
```
function zArray(s):
    n = len(s)
    Z = array(n, 0)
    L = 0; R = 0                 // current [L,R] z-box (inclusive R)
    for i in 1..n-1:
        if i <= R:
            Z[i] = min(R - i + 1, Z[i - L])   // mirror, clamped to the box
        while i + Z[i] < n and s[Z[i]] == s[i + Z[i]]:
            Z[i] += 1                          // extend
        if i + Z[i] - 1 > R:
            L = i; R = i + Z[i] - 1            // grow the box
    return Z

// Pattern matching: build Z of  pat + sep + text  (sep absent from alphabet).
function zSearch(text, pat):
    s = pat + "#" + text
    Z = zArray(s)
    m = len(pat)
    res = []
    for i in m+1..len(s)-1:
        if Z[i] >= m:
            res.append(i - m - 1)    // match start in text
    return res
```
**Complexity:** time O(n) to build, O(n+m) to search / space O(n+m).
**Traps:** • the clamp `min(R - i + 1, Z[i-L])` is the heart of linearity — drop it and you re-scan. • `R` is inclusive; box update is `R = i + Z[i] - 1`. • the separator must not appear in `pat` or `text`, or matches bleed across the seam. • `Z[0]` is conventionally 0 (or n) — don't rely on it.

### Manacher's algorithm
**LeetCode:** [Longest Palindromic Substring](https://leetcode.com/problems/longest-palindromic-substring/) · [Palindromic Substrings](https://leetcode.com/problems/palindromic-substrings/)
**When:** longest palindromic substring (or count all palindromes) and you genuinely need **O(n)** — expand-around-center's O(n²) won't pass. Niche; know it exists and have the template.
Transform `s` into `t = ^#a#b#a#$` (separators between every char + sentinels at the ends) so every palindrome is odd-length and centers align. `P[i]` = radius of the palindrome centered at `t[i]`; mirror around the current center `C` (rightmost reach `R`) to seed `P[i]`, then expand.
```
function manacher(s):
    // transform: ^ # s0 # s1 # ... # $   (sentinels prevent bounds checks)
    t = "^"
    for c in s: t += "#" + c
    t += "#$"
    n = len(t)
    P = array(n, 0)
    C = 0; R = 0                          // center & right edge of current rightmost palindrome
    for i in 1..n-2:
        mirror = 2*C - i
        if i < R:
            P[i] = min(R - i, P[mirror])  // mirror trick, clamped
        while t[i + P[i] + 1] == t[i - P[i] - 1]:   // sentinels guarantee this halts
            P[i] += 1
        if i + P[i] > R:
            C = i; R = i + P[i]
    // longest: max P[i]; center back in s
    bestLen = 0; bestCenter = 0
    for i in 1..n-2:
        if P[i] > bestLen:
            bestLen = P[i]; bestCenter = i
    start = (bestCenter - bestLen) / 2     // index in original s
    return s[start .. start + bestLen - 1] // inclusive
```
**Complexity:** time O(n) / space O(n).
**Traps:** • the sentinels `^` and `$` (distinct, absent from `s`) let the inner `while` skip bounds checks — don't omit them. • `P[i]` in `t` equals the palindrome *length* in the original `s`; map back with `start = (i - P[i]) / 2`. • count-all-palindromes = `sum( (P[i] + 1) / 2 )`. • easy to mis-derive — if shaky, fall back to expand-around-center and say so.

### Name-only — beyond typical interview scope
Know these exist; you almost never implement them in a coding round, but naming them signals depth.

| Structure | One-liner | When it'd come up |
|---|---|---|
| **Suffix array** (+ LCP via Kasai) | sorted array of all suffixes; O(n log n) build | many substring queries, distinct-substring counts, bioinformatics |
| **Suffix automaton** | minimal DFA accepting all substrings; O(n) | count distinct substrings, longest common substring of two strings |
| **Aho–Corasick** | KMP generalized to a trie of many patterns + failure links | multi-pattern search / dictionary matching (e.g. content filters) |
| **Generalized suffix tree** | suffix tree over multiple strings | longest common substring, fuzzy matching — theoretically O(n), hairy to code |

> Say this in the room: "Past KMP/Z/Manacher I'd reach for a suffix array with Kasai's LCP for offline substring queries, or Aho–Corasick for many patterns at once — but I wouldn't hand-roll a suffix automaton in 45 minutes."

### Comparison table

| Algorithm | Use when | Cost (build / search) | Gotcha |
|---|---|---|---|
| **Naive** | small / one-off; correctness first | — / O(n·m) | pathological repeats → O(nm) |
| **KMP** | single pattern, O(n+m) guaranteed; period / shortest-palindrome / repeated-substring | O(m) / O(n) | LPS is *proper* border; fallback `lps[length-1]` doesn't advance `i` |
| **Z-algorithm** | pattern match + occurrence counting; "LCP with each suffix" | O(n) / O(n+m) | clamp `min(R-i+1, Z[i-L])`; separator must be unique |
| **Rabin–Karp** | many same-length patterns; longest duplicate/repeating substring | O(m) / O(n) avg, O(nm) worst | overflow → mod every step; verify or double-hash; big prime |
| **Manacher** | longest palindromic substring in true O(n) | O(n) / O(n) | sentinels + `#` separators; map `t`-radius back to `s` index |

## 18. Implement-on-the-spot data structures

These aren't in the standard library — be ready to code them cold, from memory, in 10 minutes. This is the section that separates "uses collections" from "builds collections": the full working internals below are the templates worth drilling until you can write them blind.

| Structure | What it's for | Query / Update | Build it when you see… |
|---|---|---|---|
| **Trie** | prefix queries over many strings | O(L) per op (L = word length) | "prefix", "starts with", dictionary, autocomplete, word search II |
| **Binary trie** | max XOR pair, XOR range queries | O(32) per op | "maximum XOR of two numbers", "count pairs with XOR < k" |
| **Union-Find (DSU)** | dynamic connectivity, MST | ~O(α(n)) amortized | "connected components", "redundant edge", "merge accounts", "are u,v connected" added online |
| **Fenwick / BIT** | prefix sums with point updates | O(log n) both | "count smaller after self", "reverse pairs", mutable range-sum |
| **Segment tree** | range query + range update, any associative op | O(log n) both | min/max over range + updates, "range assign", non-invertible aggregate |
| **Sparse table** | idempotent range query, immutable data | O(n log n) build, **O(1)** query | "range min/max/gcd", data never changes, many queries |
| **LRU cache** | O(1) get/put with eviction by recency | O(1) both | "design LRU", "least recently used" |
| **LFU cache** | O(1) get/put with eviction by frequency | O(1) both | "design LFU", "least frequently used" |
| **RandomizedSet** | O(1) insert/remove/getRandom | O(1) all | "insert delete getRandom O(1)" |
| **Skip list** | ordered set without a BST | O(log n) expected | "design skiplist", ordered ops, no TreeMap allowed |
| **Custom HashMap** | the map itself (rarely asked) | O(1) amortized | "design HashMap/HashSet" explicitly |

> Min-stack / max-stack and monotonic stack/deque live in **§8** — cross-reference, don't reimplement here.

### Trie (prefix tree)
**LeetCode:** [Implement Trie (Prefix Tree)](https://leetcode.com/problems/implement-trie-prefix-tree/) · [Design Add and Search Words Data Structure](https://leetcode.com/problems/design-add-and-search-words-data-structure/) · [Replace Words](https://leetcode.com/problems/replace-words/) · [Map Sum Pairs](https://leetcode.com/problems/map-sum-pairs/) · [Word Search II](https://leetcode.com/problems/word-search-ii/)
**When:** any problem keyed on **string prefixes** — autocomplete, "word dictionary", word search on a grid, replace-words, longest-word-built-from-others. The instant "starts with" or "all words sharing prefix" appears, reach for a trie.
A tree where each edge is a character; a node marks `isEnd` if a word terminates there. Insert/search walk one node per character, so cost is the word length, independent of how many words are stored.
```
class TrieNode:
    children = {}        // map<char, TrieNode>
    isEnd    = false

class Trie:
    function init():
        root = new TrieNode()

    function insert(word):
        node = root
        for c in word:
            if c not in node.children:
                node.children[c] = new TrieNode()
            node = node.children[c]
        node.isEnd = true

    function search(word):          // exact word present?
        node = walk(word)
        return node != null and node.isEnd

    function startsWith(prefix):    // any word with this prefix?
        return walk(prefix) != null

    function walk(s):               // node at end of s, or null
        node = root
        for c in s:
            if c not in node.children: return null
            node = node.children[c]
        return node
```
**Wildcard search** (`WordDictionary`, `.` matches any char) needs DFS at `.`:
```
function searchWild(word):
    return dfs(root, 0, word)

function dfs(node, i, word):
    if i == len(word): return node.isEnd
    c = word[i]
    if c == '.':
        for ch, child in node.children:
            if dfs(child, i+1, word): return true
        return false
    if c not in node.children: return false
    return dfs(node.children[c], i+1, word)
```
**Applications.** Autocomplete (DFS-collect from the prefix node); word search II (build a trie of the dictionary, DFS the grid pruning by trie edges — far faster than searching each word); replace-words (insert roots, on each word walk until first `isEnd`); longest-word (BFS/DFS the trie, extend only through nodes that are themselves `isEnd`).
**Complexity:** insert/search/startsWith O(L); space O(total chars) — up to O(N·L·Σ) with array-children, less with maps.
**Traps:** • `startsWith` checks node existence only; `search` *also* checks `isEnd`. • use a **map** for children when the alphabet is large/unknown; a 26-slot array is faster for lowercase-only. • for word-search-II, after finding a word **prune** dead trie branches (or de-dup results) to avoid re-emitting. • don't forget to set `isEnd` — silent bug.

### Binary trie (maximum XOR)
**LeetCode:** [Maximum XOR of Two Numbers in an Array](https://leetcode.com/problems/maximum-xor-of-two-numbers-in-an-array/) · [Maximum XOR With an Element From Array](https://leetcode.com/problems/maximum-xor-with-an-element-from-array/)
**When:** "maximum XOR of two numbers in an array", "max XOR with an element from query", "count pairs with XOR ≤ k". Bits, XOR, and "maximize/minimize over pairs" together ⇒ binary trie.
Insert each number's bits MSB→LSB (fixed width, e.g. 32) as a path in a 2-child trie. To maximize XOR for a query `x`, greedily walk toward the **opposite** bit at each level (a differing bit sets that position to 1); fall back to the same bit if the opposite child is absent.
```
class BitTrieNode:
    child = [null, null]      // child[0], child[1]

class BitTrie:
    function init():
        root = new BitTrieNode()
        BITS = 31             // enough for non-negative 32-bit ints

    function insert(num):
        node = root
        for b in BITS..0:                 // MSB → LSB
            bit = (num >> b) & 1
            if node.child[bit] == null:
                node.child[bit] = new BitTrieNode()
            node = node.child[bit]

    function maxXor(num):                  // best XOR of num with any inserted number
        node = root; res = 0
        for b in BITS..0:
            bit  = (num >> b) & 1
            want = 1 - bit                 // prefer the opposite bit
            if node.child[want] != null:
                res |= (1 << b)
                node = node.child[want]
            else:
                node = node.child[bit]
        return res
```
**Usage** (`Maximum XOR of Two Numbers`): insert all numbers, then `answer = max over x of maxXor(x)`. For **count pairs with XOR < k**, walk both the prefix bits of `k` and the trie, adding subtree sizes when a branch is forced below `k` (store a `count` per node).
**Complexity:** insert/query O(BITS) = O(32) ≈ O(1); space O(N·BITS).
**Traps:** • iterate bits **high → low**; reversing the order gives wrong greedy choices. • pick `BITS` from the value range (31 for non-negative 32-bit; 63 for longs) — too few drops high bits. • for counting variants, maintain a per-node `count` and decrement on deletes if the multiset shrinks.

### Union-Find / Disjoint Set Union (DSU)
**LeetCode:** [Number of Provinces](https://leetcode.com/problems/number-of-provinces/) · [Redundant Connection](https://leetcode.com/problems/redundant-connection/) · [Number of Operations to Make Network Connected](https://leetcode.com/problems/number-of-operations-to-make-network-connected/) · [Accounts Merge](https://leetcode.com/problems/accounts-merge/) · [Most Stones Removed with Same Row or Column](https://leetcode.com/problems/most-stones-removed-with-same-row-or-column/)
**When:** **dynamic connectivity** — "are u and v in the same group?", "how many components?", merging sets online, cycle detection in an undirected graph, Kruskal's MST. If edges arrive over time and you need fast connectivity, it's DSU, not BFS-per-query.
Each element points at a parent; the root is the set id. `find` walks to the root with **path compression** (re-point nodes straight at the root); `union` attaches the smaller tree under the larger (**by rank/size**). Together these give near-constant amortized cost.
```
class DSU:
    function init(n):
        parent = array(n); for i in 0..n-1: parent[i] = i
        rank   = array(n, 0)          // upper bound on tree height
        count  = n                    // number of components

    function find(x):                 // root of x, with path compression
        while parent[x] != x:
            parent[x] = parent[parent[x]]   // path halving
            x = parent[x]
        return x

    function union(a, b):             // returns false if already joined
        ra = find(a); rb = find(b)
        if ra == rb: return false
        if rank[ra] < rank[rb]: swap(ra, rb)   // attach smaller under larger
        parent[rb] = ra
        if rank[ra] == rank[rb]: rank[ra] += 1
        count -= 1
        return true

    function connected(a, b):
        return find(a) == find(b)
```
Use **size[]** instead of rank when you need component sizes (most-stones, "largest component"): attach smaller under larger by `size`, and `size[ra] += size[rb]`.
**Applications.** Kruskal's MST (sort edges, `union` if it doesn't cycle — see §15); **redundant connection** (first edge whose `union` returns false closes a cycle); **number of connected components** (`count` after unioning all edges); **accounts merge** / **number of islands II** (dynamic — add land cells and union neighbors, track `count`); **most stones removed** (answer = n − components). **DSU on strings:** make `parent` a `map<string,string>` initialized lazily (`parent.get(s, s)`), same logic. **Weighted DSU / DSU-with-relations** (`Evaluate Division`): store a `weight[x]` = ratio from `x` to its parent, multiply ratios during `find`'s path compression and on `union`; lets you answer `a/b` if connected.
**Complexity:** find/union ~O(α(n)) amortized (α = inverse Ackermann, ≤ 4 for any realistic n) → effectively O(1); space O(n).
**Traps:** • **must** use both path compression and union-by-rank/size for the α bound; one alone is O(log n). • always `union(find(a), find(b))` — never `parent[a] = b` on raw nodes. • returning `false` from `union` (already connected) is the clean cycle-detector. • for component sizes use `size[]`, not `rank[]` (rank is a height bound, not a count). • weighted DSU: update `weight` *during* compression, easy to get the ratio direction backwards.

### Fenwick tree / Binary Indexed Tree (BIT)
**LeetCode:** [Range Sum Query - Mutable](https://leetcode.com/problems/range-sum-query-mutable/) · [Count of Smaller Numbers After Self](https://leetcode.com/problems/count-of-smaller-numbers-after-self/) · [Reverse Pairs](https://leetcode.com/problems/reverse-pairs/)
**When:** **prefix sums with point updates** in O(log n) each, or "count of smaller/greater elements seen so far" (count-smaller-after-self, reverse pairs, count-of-range-sum). If you'd otherwise recompute a prefix sum after every mutation, use a BIT.
A 1-indexed array where index `i` covers the range `[i − (i&−i) + 1, i]`. `i & -i` isolates the lowest set bit = the length of that covered block. Update climbs by **adding** the low bit; prefix query descends by **subtracting** it.
```
class BIT:
    function init(n):
        tree = array(n+1, 0)        // 1-INDEXED; index 0 unused
        N = n

    function update(i, delta):       // add delta at 1-based position i
        while i <= N:
            tree[i] += delta
            i += i & (-i)            // jump to next responsible node
    function prefix(i):              // sum of A[1..i]
        s = 0
        while i > 0:
            s += tree[i]
            i -= i & (-i)            // jump to parent prefix block
        return s
    function rangeSum(l, r):          // inclusive A[l..r]
        return prefix(r) - prefix(l-1)
```
**Range-update / point-query** variant (add `v` to `A[l..r]`, then read a single `A[i]`): maintain a BIT over the **difference array** — `update(l, +v)`, `update(r+1, −v)`; then `A[i] = prefix(i)`.
**Applications.** **Count smaller numbers after self / reverse pairs / count of range sum:** coordinate-compress the values, sweep right→left, `prefix(rank−1)` gives how many smaller already seen, then `update(rank, 1)`. **Mutable range-sum** (`Range Sum Query - Mutable`): point `update` on change, `rangeSum` on query.
**2D BIT** (point update + rectangle-sum on a grid): nest the trick on both axes.
```
function update2D(x, y, delta):
    i = x
    while i <= R:
        j = y
        while j <= C:
            tree[i][j] += delta
            j += j & (-j)
        i += i & (-i)
function prefix2D(x, y):              // sum of rectangle [1..x][1..y]
    s = 0; i = x
    while i > 0:
        j = y
        while j > 0:
            s += tree[i][j]; j -= j & (-j)
        i -= i & (-i)
    return s
// rectangle (x1,y1)..(x2,y2) = prefix2D(x2,y2) - prefix2D(x1-1,y2) - prefix2D(x2,y1-1) + prefix2D(x1-1,y1-1)
```
**Complexity:** update/query O(log n) (O(log²n) for 2D); space O(n) (O(R·C) for 2D).
**Traps:** • **1-indexed** — off-by-one here is the classic BIT bug; if values can be 0, shift by +1. • `i & -i` relies on two's-complement; fine in Java/C++, but in Python use `i & (-i)` (works) and watch unbounded ints. • BIT does prefix sums of an **invertible** op (sum, xor); it can't do min/max for arbitrary range — that's a job for a segment tree (range-min has no inverse). • coordinate-compress before indexing by value, or the tree is huge.

### Segment tree
**LeetCode:** [Range Sum Query - Mutable](https://leetcode.com/problems/range-sum-query-mutable/) · [Falling Squares](https://leetcode.com/problems/falling-squares/) · [Count of Smaller Numbers After Self](https://leetcode.com/problems/count-of-smaller-numbers-after-self/) · [The Skyline Problem](https://leetcode.com/problems/the-skyline-problem/)
**When:** **range query + range update** together, or any **non-invertible** aggregate over a range (min, max, gcd, range-assign) that a BIT can't undo. Reach for it when you need both "what's the min of [l,r]?" and "add 7 to all of [l,r]" efficiently.
A binary tree over array intervals: each node stores the aggregate of its segment, parent = `merge(left, right)`. Point update walks one root-to-leaf path; range query splits the interval across O(log n) nodes. **Lazy propagation** defers range updates: a node holds a pending delta pushed down only when its children are visited.
```
// Array-backed segment tree, parameterized by an associative merge (here: sum).
class SegTree:
    function init(A):
        n = len(A)
        tree = array(4*n, 0)
        build(A, 1, 0, n-1)            // node 1 covers [0, n-1]

    function merge(a, b): return a + b         // swap for min/max/gcd
    function neutral():   return 0             // 0 for sum, +INF for min, -INF for max

    function build(A, node, lo, hi):
        if lo == hi:
            tree[node] = A[lo]; return
        mid = (lo + hi) / 2
        build(A, 2*node,   lo,   mid)
        build(A, 2*node+1, mid+1, hi)
        tree[node] = merge(tree[2*node], tree[2*node+1])

    function update(node, lo, hi, idx, val):   // point set A[idx]=val
        if lo == hi:
            tree[node] = val; return
        mid = (lo + hi) / 2
        if idx <= mid: update(2*node,   lo,   mid, idx, val)
        else:          update(2*node+1, mid+1, hi,  idx, val)
        tree[node] = merge(tree[2*node], tree[2*node+1])

    function query(node, lo, hi, l, r):        // aggregate of [l, r]
        if r < lo or hi < l: return neutral()  // disjoint
        if l <= lo and hi <= r: return tree[node]   // fully covered
        mid = (lo + hi) / 2
        return merge(query(2*node, lo, mid, l, r),
                     query(2*node+1, mid+1, hi, l, r))
```
**Lazy propagation** — range-add + range-sum (the canonical "range update, range query"):
```
class LazySeg:
    tree = array(4*n, 0)
    lazy = array(4*n, 0)            // pending add for this node's whole segment

    function push(node, lo, hi):    // apply pending, then propagate to children
        if lazy[node] != 0:
            tree[node] += lazy[node] * (hi - lo + 1)    // sum scales by length
            if lo != hi:                                 // not a leaf
                lazy[2*node]   += lazy[node]
                lazy[2*node+1] += lazy[node]
            lazy[node] = 0

    function updateRange(node, lo, hi, l, r, val):       // add val to [l,r]
        push(node, lo, hi)
        if r < lo or hi < l: return
        if l <= lo and hi <= r:
            lazy[node] += val
            push(node, lo, hi)
            return
        mid = (lo + hi) / 2
        updateRange(2*node, lo, mid, l, r, val)
        updateRange(2*node+1, mid+1, hi, l, r, val)
        tree[node] = tree[2*node] + tree[2*node+1]

    function queryRange(node, lo, hi, l, r):
        push(node, lo, hi)                                // resolve lazy before reading
        if r < lo or hi < l: return 0
        if l <= lo and hi <= r: return tree[node]
        mid = (lo + hi) / 2
        return queryRange(2*node, lo, mid, l, r) +
               queryRange(2*node+1, mid+1, hi, l, r)
```
For **range-assign** (set all of `[l,r]` to v), store a separate "assign" lazy with a "has-assignment" flag and let assign override pending adds when pushed. An **iterative segment tree** exists (bottom-up, half the constant factor) — mention it; recursive is what you write under pressure.
**State when segment tree beats BIT:** non-invertible ops (min/max/gcd over a range), range-assign, range-add+range-query, or storing richer node state (e.g. max-subarray) — anything you can't reconstruct by subtracting two prefixes.
**Complexity:** build O(n); query/update O(log n); space O(4n). Lazy adds O(log n) per range op.
**Traps:** • size `4*n` (a tight `2*next_pow2(n)` also works) — `2*n` overflows for non-power-of-two `n`. • **always `push` before reading or recursing** in the lazy version, or stale aggregates leak. • `merge` must be associative; the neutral element must match the op (`0`/`+INF`/`-INF`). • range-add lazy scales by segment length `(hi-lo+1)` for sums but **not** for min/max. • disjoint check `r < lo or hi < l` first, then full-cover — order matters.

### Sparse table
**When:** **idempotent** range queries (min, max, gcd, AND, OR) on **immutable** data with many queries — O(1) per query after preprocessing. If the array never changes and you only need min/max/gcd, this beats a segment tree on query speed and simplicity.
Precompute `sparse[k][i]` = answer for the interval `[i, i + 2^k − 1]` (length 2^k). Any range `[l, r]` is covered by **two overlapping** power-of-two blocks; overlap is harmless because the op is idempotent (`min(x,x)=x`), so no need to align them.
```
function build(A):
    n = len(A)
    K = floor(log2(n)) + 1
    sparse = 2D array [K][n]
    for i in 0..n-1: sparse[0][i] = A[i]          // length-1 blocks
    for k in 1..K-1:
        for i in 0 .. n - (1<<k):
            sparse[k][i] = min(sparse[k-1][i], sparse[k-1][i + (1<<(k-1))])
    // optional: precompute logTable[len] for O(1) k lookup

function query(l, r):                              // min over inclusive [l, r]
    k = floor(log2(r - l + 1))
    return min(sparse[k][l], sparse[k][r - (1<<k) + 1])
```
**Complexity:** build O(n log n) time & space; query **O(1)**.
**Traps:** • only for **idempotent** ops — sum is NOT idempotent (overlap double-counts), so sparse table can't do range-sum (use prefix sums or a BIT). • **immutable only** — no updates; one change forces an O(n log n) rebuild. • precompute `floor(log2)` per length into a table to keep query truly O(1) and avoid float `log` pitfalls. • the two blocks **overlap** by design — that's correct, don't try to make them disjoint.

| | Sparse table | Segment tree | BIT |
|---|---|---|---|
| Range query | O(1) | O(log n) | O(log n) |
| Update | rebuild | O(log n) | O(log n) |
| Ops | idempotent (min/max/gcd) | any associative | invertible (sum/xor) |
| Space | O(n log n) | O(4n) | O(n) |
| Pick when | static + many queries | dynamic + min/max/assign | dynamic + prefix sums |

### LRU cache
**LeetCode:** [LRU Cache](https://leetcode.com/problems/lru-cache/)
**When:** "design an LRU cache" — `get`/`put` in **O(1)** with eviction of the least-recently-used key at capacity. One of the most-asked design questions; have it cold.
**Hash map + doubly linked list.** The map gives O(1) lookup (key → node); the list orders nodes by recency (front = most recent). Every access moves the node to the front; eviction removes the tail. Dummy head/tail sentinels kill all the null-edge-case branching.
```
class DNode:
    key; val; prev; next

class LRUCache:
    function init(capacity):
        cap = capacity
        map = {}                       // key -> DNode
        head = new DNode(); tail = new DNode()   // dummy sentinels
        head.next = tail; tail.prev = head       // empty list: head <=> tail

    function remove(node):              // unlink from list
        node.prev.next = node.next
        node.next.prev = node.prev

    function addFront(node):            // insert right after head (most recent)
        node.next = head.next
        node.prev = head
        head.next.prev = node
        head.next = node

    function get(key):
        if key not in map: return -1
        node = map[key]
        remove(node); addFront(node)   // touch → most recent
        return node.val

    function put(key, value):
        if key in map:
            node = map[key]
            node.val = value
            remove(node); addFront(node)
            return
        if len(map) == cap:            // evict LRU = node before tail
            lru = tail.prev
            remove(lru)
            map.remove(lru.key)
        node = new DNode(key, value)
        map[key] = node
        addFront(node)
```
**Complexity:** get/put O(1); space O(capacity).
**Traps:** • **store `key` inside the node** — eviction must delete from the map by key, and you only have the tail node. • use **dummy head and tail** so `remove`/`addFront` never touch null. • on `put` of an existing key, update value **and** move to front. • a `LinkedHashMap` (Java, `accessOrder=true`) or `OrderedDict` (Python) does this in the stdlib — mention it, but interviewers usually want the hand-rolled list.

### LFU cache
**LeetCode:** [LFU Cache](https://leetcode.com/problems/lfu-cache/)
**When:** "design an LFU cache" — `get`/`put` O(1) evicting the **least-frequently-used** key, breaking ties by **least-recently-used** within that frequency. Harder cousin of LRU.
Three parts: `map` key → node (with a `freq` field); `freqMap` frequency → a doubly linked list (an LRU list) of all keys at that frequency; and a `minFreq` pointer to the smallest non-empty bucket. On access, bump a node's freq by moving it from bucket `f` to bucket `f+1`; evict from the front/back of the `minFreq` bucket.
```
class Node: key; val; freq

class LFUCache:
    function init(capacity):
        cap = capacity
        map = {}                       // key -> Node
        freqMap = {}                   // freq -> ordered list (LRU) of nodes
        minFreq = 0

    function touch(node):              // promote node from freq f to f+1
        f = node.freq
        freqMap[f].remove(node)
        if freqMap[f].empty() and minFreq == f:
            minFreq += 1
        node.freq = f + 1
        freqMap.get(f+1, newList()).addFront(node)   // front = most recent

    function get(key):
        if key not in map: return -1
        node = map[key]
        touch(node)
        return node.val

    function put(key, value):
        if cap == 0: return
        if key in map:
            node = map[key]
            node.val = value
            touch(node)
            return
        if len(map) == cap:            // evict LRU within the minFreq bucket
            victim = freqMap[minFreq].popBack()       // back = least recent
            map.remove(victim.key)
        node = new Node(key, value, freq = 1)
        map[key] = node
        freqMap.get(1, newList()).addFront(node)
        minFreq = 1                    // new item has freq 1
```
**Complexity:** get/put O(1); space O(capacity).
**Traps:** • **reset `minFreq = 1`** on every insert (the new key has freq 1). • update `minFreq` when promoting empties the current min bucket. • within a frequency bucket, order by recency (front = newest, evict from the back) so LFU ties break by LRU. • each freq bucket is itself an LRU doubly linked list — reuse the LRU node/list machinery.

### RandomizedSet
**LeetCode:** [Insert Delete GetRandom O(1)](https://leetcode.com/problems/insert-delete-getrandom-o1/) · [Insert Delete GetRandom O(1) - Duplicates allowed](https://leetcode.com/problems/insert-delete-getrandom-o1-duplicates-allowed/) · [Random Pick with Weight](https://leetcode.com/problems/random-pick-with-weight/)
**When:** "insert, remove, and getRandom all in **O(1)** average." The combination of O(1) random access *and* O(1) delete is the tell.
**Dynamic array + value→index map.** The array gives O(1) uniform `getRandom`; the map gives O(1) lookup. The trick for O(1) `remove`: swap the target with the **last** element, pop the last, and fix the moved element's index in the map (no shifting).
```
class RandomizedSet:
    function init():
        arr = []                       // dense storage of values
        idx = {}                       // value -> position in arr

    function insert(val):              // true if newly added
        if val in idx: return false
        idx[val] = len(arr)
        arr.append(val)
        return true

    function remove(val):              // true if was present
        if val not in idx: return false
        i = idx[val]
        last = arr[len(arr)-1]
        arr[i] = last                  // move last into the hole
        idx[last] = i
        arr.pop()                      // drop the duplicate at the back
        idx.remove(val)
        return true

    function getRandom():
        return arr[randomInt(0, len(arr)-1)]   // uniform
```
**Duplicates-allowed variant** (`Insert Delete GetRandom O(1) - Duplicates allowed`): make `idx` a `map<value, set<index>>`; on remove pull any one index from the set, swap-with-last, and update both the moved value's index set and the removed one. `getRandom`'s probability is then proportional to multiplicity.
**Complexity:** insert/remove/getRandom O(1) average; space O(n).
**Traps:** • the swap-with-last **must** update `idx[last]` before popping, or the map points at a freed slot. • removing the last element is a self-swap — make sure that path still works. • `getRandom` must be uniform over current size; sample `[0, len-1]`. • duplicates variant: store a *set* of indices per value, not a single index.

### Skip list
**LeetCode:** [Design Skiplist](https://leetcode.com/problems/design-skiplist/)
**When:** "design a skiplist" — ordered `add`/`erase`/`search` in **O(log n)** expected, when a balanced BST is banned or you want simpler probabilistic balancing. A randomized stand-in for TreeMap/TreeSet.
A stack of sorted linked lists: level 0 holds every node; each higher level is an express lane, a node promoted with probability ~½ per level (coin flips). Search descends from the top, moving right while the next key is smaller, dropping a level when it overshoots — skipping large gaps like binary search over a list.
```
class SkipNode:
    val; next = []                     // next[i] = forward pointer at level i

class Skiplist:
    function init():
        MAX = 16; P = 0.5
        head = new SkipNode(-INF, MAX) // sentinel, full height
        level = 1                      // current highest level in use

    function randomLevel():            // coin-flip height
        lvl = 1
        while random() < P and lvl < MAX: lvl += 1
        return lvl

    function search(target):
        node = head
        for i in level-1 .. 0:                       // top → bottom
            while node.next[i] != null and node.next[i].val < target:
                node = node.next[i]
        node = node.next[0]
        return node != null and node.val == target

    function add(num):
        update = array(MAX, head)                    // predecessors per level
        node = head
        for i in level-1 .. 0:
            while node.next[i] != null and node.next[i].val < num:
                node = node.next[i]
            update[i] = node
        lvl = randomLevel()
        if lvl > level:
            for i in level .. lvl-1: update[i] = head
            level = lvl
        newNode = new SkipNode(num, lvl)
        for i in 0 .. lvl-1:                          // splice in at each level
            newNode.next[i] = update[i].next[i]
            update[i].next[i] = newNode

    function erase(num):               // remove one occurrence
        update = array(MAX, head)
        node = head; found = false
        for i in level-1 .. 0:
            while node.next[i] != null and node.next[i].val < num:
                node = node.next[i]
            update[i] = node
        target = node.next[0]
        if target == null or target.val != num: return false
        for i in 0 .. level-1:
            if update[i].next[i] == target:
                update[i].next[i] = target.next[i]
        return true
```
**Complexity:** search/add/erase O(log n) **expected** (worst case O(n) if the coin flips conspire); space O(n) expected.
**Traps:** • the `update[]` array of predecessors-per-level is what makes splice/unsplice O(level) — capture it during the descent. • `add` allows duplicates (skiplist is a multiset by default); `erase` removes exactly one. • a balanced-BST (red-black / AVL) is the deterministic alternative — say "I'd reach for a skiplist over hand-rolling a red-black tree because the rotations are error-prone under time pressure." • promote with probability ½ per level; `randomLevel` should cap at `MAX`.

### Custom HashMap / HashSet
**LeetCode:** [Design HashSet](https://leetcode.com/problems/design-hashset/) · [Design HashMap](https://leetcode.com/problems/design-hashmap/)
**When:** *explicitly* asked to "design a HashMap" without using the language's built-in. Otherwise this is §19's job — never reimplement the map when you can just use one.
**Array of buckets + chaining.** Hash the key to a bucket index; each bucket is a list of (key, value) pairs; collisions append to the bucket. (Open addressing — probe to the next free slot — is the alternative; chaining is simpler to write.)
```
class MyHashMap:
    function init():
        SIZE = 1009                    // prime reduces clustering
        buckets = array(SIZE, emptyList())   // each is a list of (key,val)

    function hash(key): return key % SIZE

    function put(key, value):
        b = buckets[hash(key)]
        for pair in b:
            if pair.key == key:
                pair.val = value; return    // overwrite existing
        b.append((key, value))

    function get(key):
        for pair in buckets[hash(key)]:
            if pair.key == key: return pair.val
        return -1                      // absent

    function remove(key):
        b = buckets[hash(key)]
        for i in 0..len(b)-1:
            if b[i].key == key:
                b.removeAt(i); return
```
`MyHashSet` is the same with no value (store keys, or values-as-presence). **Resizing** (rehash when load factor > ~0.75) and treeifying long buckets are how the real `HashMap` stays O(1) — mention them; an interview rarely needs them coded.
**Complexity:** O(1) average per op, O(n) worst (all keys in one bucket); space O(capacity + entries).
**Traps:** • a **prime** table size spreads `% SIZE` better than a power of two with weak hashes. • on `put`, scan the bucket to overwrite an existing key — don't blindly append duplicates. • return a clear sentinel (`-1` or null) for absent keys; document it. • this is normally **§19** territory — only hand-roll when the problem forbids the stdlib map.

## 19. Standard-library data structures

Know these cold as **APIs and complexities**; never reimplement them in an interview (unless explicitly asked) — that's §18's job. This section is deliberately a thin reference: which type to reach for, its core ops with amortized cost, and the concrete Java / Python names. No internals here — red-black trees, hash probing, and heap sift-down are out of scope by design (the §18/§19 boundary *is* the point).

| Structure | Core ops & amortized complexity | Reach for it when | Java type | Python type |
|---|---|---|---|---|
| **Dynamic array** | index O(1); append O(1)*; insert/remove mid O(n) | default sequence, random access, stack-by-index | `ArrayList<>` | `list` |
| **Hash map** | get/put/remove O(1) avg | key→value lookup, counting, memo | `HashMap<>` | `dict` |
| **Hash set** | add/contains/remove O(1) avg | membership, dedup, "seen" | `HashSet<>` | `set` |
| **Ordered map** | get/put/remove O(log n); floor/ceil/first/last O(log n) | keys in sorted order, range/neighbor queries | `TreeMap<>` | `SortedDict` (sortedcontainers) |
| **Ordered set** | add/remove/contains O(log n); floor/ceil O(log n) | sorted unique keys, "next greater present" | `TreeSet<>` | `SortedList` (sortedcontainers) |
| **Stack (LIFO)** | push/pop/top O(1) | DFS, backtracking, monotonic stack, undo | `ArrayDeque<>` | `list` (`append`/`pop`) |
| **Queue (FIFO)** | push/pop/front O(1) | BFS, level-order, sliding window | `ArrayDeque<>` | `collections.deque` |
| **Deque** | push/pop/peek both ends O(1) | monotonic deque, sliding-window max, work-stealing | `ArrayDeque<>` | `collections.deque` |
| **Min-heap / PQ** | push/pop O(log n); peek O(1) | k-th smallest, Dijkstra, merge-k, scheduling | `PriorityQueue<>` | `heapq` (on a `list`) |
| **Linked list** | O(1) insert/remove given node; O(n) index | rarely chosen directly; use deque instead | `LinkedList<>` | — (use `deque`) |
| **String builder** | append O(1)*; build O(n) | accumulate strings (never `+=` in a loop) | `StringBuilder` | `list` of chars + `"".join` |

`*` amortized — array/`StringBuilder` growth is O(1) amortized but O(n) on the occasional resize.

Below: only the spots where an interview gotcha lurks. Everything else is just "use the API."

### TreeMap/TreeSet give floor/ceil/higher/lower — the ordered-map superpower
The reason to pick `TreeMap` over `HashMap`: O(log n) **neighbor queries** on sorted keys. `floorKey(k)` = largest ≤ k, `ceilingKey(k)` = smallest ≥ k, `lowerKey`/`higherKey` = strict versions, plus `firstKey`/`lastKey` and `subMap`/`headMap`/`tailMap` for range scans. This is the clean answer to "find the closest booking", "calendar / interval overlap" (`My Calendar`), "contains nearby almost-duplicate", and stream-median-ish problems. In Python there is **no built-in** equivalent — use `sortedcontainers.SortedDict`/`SortedList` (`.bisect_left` / `.irange`) or hand-roll with `bisect` on a kept-sorted list.

### Java PriorityQueue is a min-heap; no decrease-key — use lazy deletion
`PriorityQueue` pops the **smallest** by default; for a max-heap pass `Comparator.reverseOrder()` (or negate keys). Critically, there is **no `decreaseKey`** and `remove(Object)` is O(n). For Dijkstra/Prim, don't try to update a key in place — **push the new (better) entry and skip stale ones on pop** (lazy deletion: when you pop `(dist, node)`, ignore it if `dist > best[node]`). `peek()`/`poll()` are O(log n) for poll, O(1) for peek; building from a collection via the constructor is O(n) (heapify) vs O(n log n) for repeated `offer`.

### heapq is min-only — push negatives for a max-heap
Python's `heapq` operates **in place on a plain `list`** and is **min-heap only**. For a max-heap, push `-x` (or `(-key, item)` tuples) and negate on pop. Use `heapq.heapify(lst)` for O(n) build, `heappush`/`heappop` for O(log n), and `heapq.nlargest(k, ...)`/`nsmallest` for one-shot top-k. Tuples break ties by the next field — wrap items so an un-orderable payload never gets compared (`(priority, counter, item)`).

### Python has no built-in balanced BST → reach for sortedcontainers
Java has `TreeMap`/`TreeSet`; Python's stdlib has **nothing** sorted-by-key with O(log n) insert. The interview-accepted answer is the third-party **`sortedcontainers`** (`SortedList`, `SortedDict`, `SortedSet`) — O(log n) add/remove, `.bisect_left`, `.irange`, indexable. If a third-party import is off-limits, keep a sorted `list` and use the stdlib **`bisect`** module (`bisect_left`/`insort`), accepting O(n) inserts. Say which you'd use and why.

### ArrayDeque over Stack/LinkedList in modern Java
Use **`ArrayDeque`** for both stacks and queues. `java.util.Stack` is a legacy synchronized `Vector` (slower, locks on every op) — avoid it. `LinkedList` works as a `Deque` but has terrible cache locality and per-node object overhead. `ArrayDeque` is the array-backed, allocation-light default: `push`/`pop`/`peek` for stack semantics, `offer`/`poll`/`peek` for queue semantics. (It forbids `null` elements — use a sentinel if you truly need one.) In Python, `list` is the stack (`append`/`pop`), `collections.deque` is the queue/deque (`appendleft`/`popleft` are O(1), whereas `list.pop(0)` is O(n)).

### Build the string with a builder — never `+=` in a loop
Repeated string concatenation is **O(n²)** because strings are immutable (each `+=` copies). In Java use `StringBuilder` (`append`, then `toString`); in Python accumulate into a `list` and `"".join(parts)` at the end (or write to `io.StringIO`). This is a silent TLE on large inputs — the interviewer notices.

## 20. Approaches & tricks grab-bag

Senior tricks that turn a hard problem into a known one. Each is a reframing: spot the signal, map to a primitive you already own. Format below is deliberately tight — trick → when → 2–4 line how → example.

### Meet in the middle
**LeetCode:** [Partition to K Equal Sum Subsets](https://leetcode.com/problems/partition-to-k-equal-sum-subsets/) · [Closest Subsequence Sum](https://leetcode.com/problems/closest-subsequence-sum/)
**When:** subset/knapsack-style search with n ≤ ~40 — too big for 2ⁿ, too small/structureless for DP. "Choose a subset summing to / closest to target," "count subsets with sum = S" at n=40.
Split the n items into two halves of ~n/2. Enumerate all 2^(n/2) subset-sums of each half (~10⁶ each). Sort one half's sums; for each sum in the other half, **binary-search** (or two-pointer) the complement that hits/approaches the target. Turns 2ⁿ into 2^(n/2)·n.
**Example:** *Partition to k equal subsets* (small n), *closest subset sum to target*, *count subsets summing to S* (n=40), *4SUM on huge values* (split into two pairs).
**Complexity:** O(2^(n/2) · n/2). **Trap:** memory for 2^(n/2) sums; dedupe if counting distinct.

### Coordinate compression
**LeetCode:** [Count of Smaller Numbers After Self](https://leetcode.com/problems/count-of-smaller-numbers-after-self/) · [Falling Squares](https://leetcode.com/problems/falling-squares/) · [The Skyline Problem](https://leetcode.com/problems/the-skyline-problem/)
**When:** values are huge or sparse (up to 10⁹) but you only have ≤ m of them, and an algorithm (BIT/segment tree/bucket) needs indices in `0..m−1`.
Collect all values, sort, dedupe; replace each value by its **rank** (position in the sorted-unique list). Order is preserved, so any rank-based structure works. Binary-search to look a value's rank back up.
```
function compress(values):
    sorted_unique = dedupe(sort(copy(values)))
    rank = {}
    for i in 0..len(sorted_unique)-1: rank[sorted_unique[i]] = i
    return rank        // map original value -> 0..m-1
```
**Example:** *Count of smaller numbers after self* (BIT over compressed values), *skyline / interval problems with 10⁹ coords*, *range queries on sparse keys*.
**Trap:** when relative order is all that matters, compress; if exact gaps matter (distances), don't.

### Offline query processing
**When:** many queries answerable in *any* order, and a convenient order makes each cheap — "answer Q range/threshold queries," updates and queries interleaved, you control timing.
Read all queries first, **sort** them (by right endpoint, by threshold, by time), then sweep once, maintaining a structure (DSU, BIT, sorted set) that grows monotonically as you advance — each query reads the structure at its moment.
**Example:** *Number of connected components after adding edges in increasing weight* (sort queries by weight + DSU), *offline range-distinct via BIT sorted by right end*, *Kruskal-reconstruction queries*. (See Mo's algorithm below for the array-range variant.)
**Trap:** only valid when queries are independent of each other's *answers*; if a later query depends on an earlier answer, you must go online.

### Sweep line / event framing
**LeetCode:** [The Skyline Problem](https://leetcode.com/problems/the-skyline-problem/) · [Falling Squares](https://leetcode.com/problems/falling-squares/)
**When:** intervals, geometry, or "max overlap / busiest moment" problems (see §6). Anything where state changes only at discrete x-coordinates.
Turn each interval/object into **+1 start** and **−1 end** events; sort events by coordinate (ties: process ends before starts, or starts before ends, per the inclusivity rule); sweep left→right maintaining a running counter or an active set.
```
// max concurrent intervals
events = []
for [s, e] in intervals: events.append((s, +1)); events.append((e, -1))
sort(events)                  // tie rule: -1 before +1 if endpoints touch but don't overlap
cur = 0; best = 0
for (x, d) in events: cur += d; best = max(best, cur)
```
**Example:** *Meeting rooms II*, *skyline*, *max overlapping intervals*, *rectangle area union*.
**Trap:** the tie-break between same-coordinate start/end events is the whole game — decide whether `[1,2]` and `[2,3]` overlap.

### Binary lifting (kth ancestor, LCA)
**LeetCode:** [Kth Ancestor of a Tree Node](https://leetcode.com/problems/kth-ancestor-of-a-tree-node/)
**When:** a rooted tree with many "k-th ancestor of u" or "LCA(u, v)" queries — answer each in O(log n) after O(n log n) preprocessing.
Precompute `up[k][v]` = the `2^k`-th ancestor of `v` (a sparse table): `up[0][v] = parent[v]`, `up[k][v] = up[k−1][ up[k−1][v] ]`. Jump by powers of two: decompose k into set bits. For LCA, lift the deeper node to equal depth, then lift both together while their ancestors differ.
```
function kthAncestor(v, k):
    for j in 0..LOG-1:
        if (k >> j) & 1: v = up[j][v]; if v == -1: return -1
    return v
```
**Example:** *Kth ancestor of a tree node* (LeetCode 1483), *LCA*, *path queries decomposed via LCA*.
**Complexity:** O(n log n) build, O(log n) query. **Trap:** size `LOG = ceil(log2(n))`; sentinel `−1` past the root.

### Sqrt decomposition / Mo's algorithm
**When:** range queries where a segment tree is overkill or the query (e.g. "number of distinct values in a range") isn't a clean monoid — and queries can be **offline**.
**Sqrt decomposition:** split the array into blocks of size √n; precompute a per-block aggregate; a range query touches O(√n) full blocks + 2 partial ones. **Mo's algorithm:** sort offline queries by `(block of left endpoint, then right endpoint)`; move two pointers `[L, R]` incrementally between consecutive queries — total pointer movement is O((n+q)·√n).
**Example:** *Range distinct-count*, *range mode*, *range "count of value v"* — all offline. Sqrt decomposition also gives an easy **updatable** range-sum when you don't want a BIT.
**Complexity:** Mo's O((n+q)√n). **Trap:** Mo's requires O(1) add/remove-one-element transitions and offline queries; if updates interleave, you need the harder "Mo's with modifications."

### Prefix/suffix decomposition
**LeetCode:** [Product of Array Except Self](https://leetcode.com/problems/product-of-array-except-self/) · [Candy](https://leetcode.com/problems/candy/) · [Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/)
**When:** answer for index i depends on "everything to the left" and "everything to the right" separately — and you're not allowed division, or division is unsafe (zeros).
Compute a left-to-right prefix array and a right-to-left suffix array, then combine them at each index in O(1). The canonical no-division trick.
```
// product of array except self (no division)
n = len(A); res = [1]*n
pre = 1
for i in 0..n-1: res[i] = pre; pre *= A[i]      // prefix product (excl. self)
suf = 1
for i in n-1..0 step -1: res[i] *= suf; suf *= A[i]   // multiply suffix in
```
**Example:** *Product of array except self*, *Trapping rain water* (prefix-max & suffix-max per column; alt to two-pointer/stack), *Candy* (left pass for left-neighbor rule, right pass for right-neighbor, take max).
**Complexity:** O(n) time, O(1) extra if the output array isn't counted. **Trap:** off-by-one in where "excluding self" lands — prefix excludes A[i] *before* multiplying it in.

### Reservoir sampling
**LeetCode:** [Linked List Random Node](https://leetcode.com/problems/linked-list-random-node/) · [Random Pick Index](https://leetcode.com/problems/random-pick-index/)
**When:** uniformly sample k items from a **stream of unknown length** (can't fit in memory, length not known in advance) — "random node from a linked list," "sample a line from a huge file."
Keep the first k items. For the i-th item (i > k, 1-indexed), keep it with probability k/i by replacing a uniformly random one of the current k. Every item ends up equally likely.
```
function reservoir(stream, k):
    res = first k items of stream
    i = k
    for each x in rest of stream:
        i += 1
        j = rand(i)               // uniform in 0..i-1
        if j < k: res[j] = x      // keep x, evict res[j]
    return res
```
**Example:** *Random pick index*, *linked-list random node*, *sample log lines*. **Complexity:** O(n) time, O(k) space, single pass. **Trap:** the probability is k/i at step i — getting the index math wrong breaks uniformity; verify with k=1.

### Fisher–Yates shuffle
**LeetCode:** [Shuffle an Array](https://leetcode.com/problems/shuffle-an-array/)
**When:** produce a **uniformly random permutation** in place — "shuffle an array," and the correctness baseline interviewers probe ("why not just swap each with a random index?").
Walk from the last index down; swap `A[i]` with a uniformly random index in `0..i` (inclusive of i). Each of the n! permutations is equally likely.
```
function shuffle(A):
    for i in len(A)-1 .. 1 step -1:
        j = rand(i + 1)           // 0..i inclusive — must include i
        swap(A[i], A[j])
```
**Example:** *Shuffle an array*, dealing cards, randomized test inputs. **Complexity:** O(n). **Trap:** the random range must be `0..i` (including i); the naive `swap each with rand(n)` is **biased** (produces nⁿ equally-likely sequences, not n! equally-likely permutations).

### Randomization (pivots, treaps)
**When:** an adversarial worst case kills a deterministic structure — sorted input vs fixed-pivot quicksort, or a degenerate BST.
**Randomized pivot** (quicksort/quickselect, see §12): pick the pivot uniformly at random so no input is reliably worst-case → expected O(n log n) / O(n). **Treap:** a BST keyed by value but heap-ordered by a *random* priority — the random priorities keep it balanced in expectation (O(log n) ops) with far simpler code than red-black trees; mention it when asked for a balanced BST you'd actually write.
**Example:** *Quickselect for kth largest*, *randomized quicksort*, *order-statistics tree via treap*. **Trap:** randomization gives *expected* bounds, not worst-case guarantees — say so if the interviewer needs hard guarantees (then reach for mergesort / median-of-medians / a true balanced tree).

### State-space BFS
**LeetCode:** [Open the Lock](https://leetcode.com/problems/open-the-lock/) · [Minimum Genetic Mutation](https://leetcode.com/problems/minimum-genetic-mutation/) · [Sliding Puzzle](https://leetcode.com/problems/sliding-puzzle/)
**When:** "minimum number of moves/transformations to reach a goal," where each configuration is a node and each legal move is an edge of weight 1 (see §15). Puzzles, word transforms, lock combinations.
Model each **state** as a graph node; generate neighbor states by applying one move; BFS from start gives the shortest move count. Use a `visited` set keyed by a hashable encoding of the state.
```
function bfsStates(start, isGoal, neighbors):
    q = [start]; dist = {start: 0}
    while not q.empty():
        s = q.pop()
        if isGoal(s): return dist[s]
        for t in neighbors(s):
            if t not in dist: dist[t] = dist[s] + 1; q.push(t)
    return -1
```
**Example:** *Word ladder* (change one letter), *Open the lock* (rotate one wheel), *Sliding puzzle / 8-puzzle*, *minimum genetic mutation*. **Bidirectional BFS:** search from start and goal simultaneously, expanding the smaller frontier each step; meet in the middle to cut the explored states from O(b^d) to O(b^(d/2)) — big win for *word ladder* with a known target. **Trap:** encode state canonically (so equal states hash equal); the branching factor can explode — prune impossible moves and dedupe aggressively.

### "Transform the problem" catalog
**When:** a problem looks novel but is a disguised standard one — the senior move is recognizing the reduction. Match the signal in the left column to the known technique.

| Signal / phrasing | Reduce to |
|---|---|
| Longest/shortest path in a **DAG** | DP over nodes in **topological order** (see §15) |
| **Max rectangle** in a binary matrix | "largest rectangle in histogram" per row (monotonic stack, §8) |
| **Count pairs** with a property (i<j, A[i]>A[j], etc.) | sort + two pointers, or BIT/merge-sort inversion count (§12, §18) |
| "k-th smallest/largest **value**" with a feasibility check | **binary search on the answer** + count-≤-x predicate (§5) |
| "**Exactly K** distinct/...": | `atMost(K) − atMost(K−1)` (sliding window, §4) |
| **Minimize the maximum** (or maximize the minimum) | binary search on the answer + a greedy/feasibility check (§5) |
| "Can we partition/schedule within capacity C" | binary search on C + greedy packing check |
| **Connectivity over a threshold** / merging groups | DSU / union-find (§18), often offline-sorted |
| **Cycle / ordering of dependencies** | topological sort; cycle ⇒ no valid order (§15) |
| "Next greater/smaller element" | monotonic stack (§8) |
| Range min/max/sum with updates | segment tree / BIT (§18) |
| Shortest transformation, unit-cost moves | state-space BFS (above, §15) |

**Trap:** the reduction must preserve the answer — prove "feasible(x) is monotone in x" before binary-searching the answer; prove the histogram rows actually correspond to maximal rectangles.

### Invariants & loop-invariant thinking
**When:** designing or debugging a loop/two-pointer/sliding-window algorithm — what is *always true* at the top of each iteration?
State an **invariant** (e.g. "`A[0..i]` is sorted," "the window `A[l..r]` contains ≤ K distinct," "`st` is strictly decreasing") and ensure every branch **preserves** it; the invariant at loop exit gives correctness. This is how you justify two-pointer correctness ("moving `l` can only help, never skips a valid answer") and prove a greedy is safe (exchange argument: any optimal solution can be transformed to the greedy one without getting worse).
**Example:** binary search's "answer ∈ `[lo, hi]`" invariant; Dutch-flag partition's three-region invariant; Dijkstra's "popped node has final distance" invariant. **Trap:** an algorithm that "passes the examples" but has no statable invariant is a guess — find the invariant and the edge cases reveal themselves.

### Amortized analysis intuition
**When:** explaining why a structure with occasionally-expensive operations is cheap *on average* — monotonic stack, two pointers, dynamic-array growth, union-find.
Bound the **total** work across all operations, then divide by the count. **Two pointers / monotonic stack are O(n) because each element is pushed and popped at most once** — even though one step may pop many elements, the *total* pops ≤ total pushes ≤ n. **Dynamic array append is amortized O(1)** because doubling makes the total copying across n appends ≤ 2n (geometric series). **Union-find with path compression + union by rank is ~O(α(n))** per op, effectively constant.
> Say this in the room: "Each element enters and leaves the stack once, so the inner `while` doesn't make it O(n²) — the total pop count is bounded by the total push count, n. That's the amortized O(n) argument."
**Trap:** amortized ≠ worst-case-per-op — a single op may be O(n); say "amortized" explicitly. The accounting (each element handled O(1) times *total*) is what the interviewer wants to hear, not just the big-O.

> Say this in the room: read the constraints to pick the approach before you code (see §1). n ≤ 20 → bitmask/meet-in-the-middle/exponential backtracking; n ≤ 40 → meet in the middle (2^(n/2)); n ≤ 500 → O(n³) Floyd/interval DP is fine; n ≤ 5000 → O(n²) DP; n ≤ 1e5–1e6 → you need O(n log n) or O(n), so think sort + sweep / two pointers / heap / binary-search-the-answer; n ≥ 1e7 → O(n) single pass, watch constant factors and I/O. "Answer mod 1e9+7" ⇒ combinatorics/DP (§13); "shortest/min moves, unit cost" ⇒ BFS; "k-th value" or "min the max" ⇒ binary search on the answer.

## 21. Cheat sheet

The night-before single-screen recap. Everything here is expanded in the section cited.

### Complexity of the operations you'll quote

| Operation | Cost | Notes |
|---|---|---|
| Hash map/set get/put | O(1) avg, O(n) worst | worst on adversarial hashing; amortized O(1) (§19) |
| Balanced BST / TreeMap op | O(log n) | gives ordered `floor`/`ceil`/`higher`/`lower` (§19) |
| Binary heap push/pop | O(log n) | peek O(1); build-heap O(n) (§11) |
| Sort | O(n log n) | counting/radix O(n) when values bounded (§7) |
| Binary search | O(log n) | array, or **on the answer** (§5) |
| Two pointers / sliding window | O(n) | each index advances monotonically (§4) |
| Monotonic stack/deque | O(n) amortized | each element pushed/popped once (§8) |
| BFS / DFS | O(V + E) | adjacency list (§15) |
| Dijkstra (binary heap) | O(E log V) | non-negative weights (§15) |
| Bellman–Ford | O(V·E) | negative edges, neg-cycle detection (§15) |
| Floyd–Warshall | O(V³) | all pairs (§15) |
| Union-Find op | ~O(α(n)) ≈ O(1) | path compression + union by rank (§18) |
| Fenwick / segment-tree query/update | O(log n) | segment tree + lazy = range update too (§18) |
| Sparse-table range min/max | O(1) query, O(n log n) build | immutable only (§18) |
| Trie insert/search | O(L) | L = key length (§18) |
| Backtracking | O(branch^depth) | output-bound; prune hard (§12) |

### Pattern index (symptom → §)

- Contiguous window / running aggregate → §4 · sorted/monotone search → §5 · overlaps → §6
- Complement/freq/bits → §7 · next-greater / brackets → §8 · pointer surgery → §9
- Aggregate-up tree / level-order → §10 · top-k / median / k-merge → §11
- Enumerate all / fill board → §12 · gcd/primes/mod/fastpow → §13
- Count-ways / min-cost / align / knapsack / interval / bitmask → §14
- Paths & connectivity → §15 · provable local choice → §16 · pattern match → §17
- Build-it-yourself structure → §18 · just-call-it structure → §19 · clever reductions → §20

### Templates everyone forgets the edge case of

- **Binary search:** use one template (half-open `[lo, hi)`, lower-bound) for *every*
  variant; `mid = lo + (hi - lo) / 2`. Decide the predicate, not the comparisons (§5).
- **Sliding window:** grow `right` every step; `while invariant broken: shrink left`.
  Update the answer at the right place (after shrinking for "valid", before for "at most") (§4).
- **DFS on a grid:** `DIRS` array + bounds check + mark visited; mark on enqueue in BFS (§15).
- **Dijkstra:** skip a popped entry if its distance > recorded (lazy deletion); never on
  negative edges (§15).
- **Backtracking:** choose → recurse → **un-choose**; `sort` then skip duplicates
  (`if i > start and a[i] == a[i-1]: continue`) (§12).
- **Linked list:** use a `dummy` head; save `next` before you rewire (§9).
- **Tree recursion:** decide *what each call returns* and *what you combine*; null is the
  base case (§10).
- **DP:** write `dp[i] = …` in English first; 0/1 knapsack iterates capacity **descending**,
  unbounded **ascending**; combinations loop items-outer, permutations target-outer (§14).
- **Modular:** take `mod` after every add/multiply; fix negatives `((x % m) + m) % m`;
  division needs the modular inverse (§13).

### Numbers worth memorizing

- ~10⁸ ops/sec budget. log₂(10⁹) ≈ 30, log₂(10¹⁸) ≈ 60.
- `MOD = 1_000_000_007` (prime → Fermat inverse `a^(MOD-2)`).
- 32-bit int max ≈ 2.1·10⁹; sums of 10⁵ values near 10⁹ overflow `int` → use 64-bit.

## 22. Write-blind drill

If you can reproduce these **from memory, on a blank page, in ~5 minutes each**, you're
ready for the coding round. Don't re-read — reconstruct, then diff against the cited
section. The list is ordered roughly by how often it shows up.

### Tier 1 — must be automatic

1. Binary search, lower-bound, half-open `[lo, hi)` template (§5).
2. Variable-size sliding window with the shrink loop (§4).
3. Two-pointer in-place overwrite (`read`/`write`) (§4).
4. BFS on a graph/grid with a visited set, marking on enqueue (§15).
5. DFS recursive + iterative-with-stack (§10, §15).
6. Reverse a singly linked list (iterative, with `dummy` where needed) (§9).
7. Floyd's fast/slow cycle detection **and** finding the cycle entry (§9).
8. Binary-tree traversals: pre/in/post recursive, and iterative inorder (§10).
9. Level-order BFS grouped by level (§10).
10. Backtracking skeleton (choose → recurse → un-choose) → subsets & permutations (§12).
11. Top-k with a size-k heap; kth largest (§11).
12. Quickselect partition for kth element (§12).
13. 0/1 knapsack and coin change (both top-down and 1D bottom-up) (§14).
14. LIS in O(n log n) with `bisect` (§14, §5).
15. Edit distance / LCS 2D table (§14).

### Tier 2 — strong-hire depth

16. Union-Find with path compression + union by rank (§18).
17. Trie: node, insert, search, startsWith (§18).
18. LRU cache: hash map + doubly linked list, O(1) (§18).
19. Dijkstra with a min-heap and stale-entry skip (§15).
20. Topological sort, both Kahn (indegree) and DFS, with cycle detection (§15).
21. Monotonic-stack "next greater element" template (§8).
22. Monotonic-deque sliding-window maximum (§8).
23. Merge intervals + the sweep-line "max concurrent" count (§6).
24. Two-heaps running median (§11).
25. Prefix-sum + hash map for "subarray sum = k" (§4, §7).
26. Fenwick (BIT): update and prefix query with `i & -i` (§18).
27. Kadane's maximum subarray (§4).
28. Dutch national flag 3-way partition (§4).

### Tier 3 — senior polish (know the shape, can derive)

29. Segment tree with lazy propagation (range update + range query) (§18).
30. KMP failure function + search (§17).
31. Rabin–Karp rolling hash (§17).
32. Bitmask DP (Held–Karp TSP shape) (§14).
33. Bellman–Ford and Floyd–Warshall (§15).
34. Binary heap internals: sift-up / sift-down / build-heap (§11).
35. Meet in the middle for n ≤ 40 subset problems (§20).

> **Say this in the room:** when you blank on a template, narrate the *invariant* instead
> — "left of `write` is the kept prefix", "`dp[i]` is the best ending at `i`", "the deque
> holds indices in decreasing value". Reconstructing from the invariant beats memorizing
> keystrokes, and it's what the interviewer actually wants to hear.

## 23. Tracing & explaining on a shared text editor

In a CoderPad / Google Doc / CodeSignal screen you can't draw — but a few ASCII diagrams
in a comment block do three jobs at once: they **communicate** your plan, they **catch your
own bugs** before you run, and they keep the interviewer nodding along. Everything below is
monospace and uses only the simplest symbols, so it survives copy-paste into any editor.

### The symbol set (keep it tiny)

| Symbol | Means |
|---|---|
| `->` `<-` `<->` | direction / pointer / edge |
| `^` `v` | "this position" caret, or up / down |
| `\|` `-` `+` | box edges and tree-branch lines |
| `/` `\` | tree branches (down-left / down-right) |
| `=>` | "leads to" / transition / therefore |
| `*` `#` | emit / current / visited / wall mark |
| `[ ]` | a cell, a window, or a list node |

Two layouts carry most of the load: **draw the structure** as ASCII art (for shape), and
**trace the execution** as an aligned step table (for movement — the "poor man's
debugger"). Reach for a table whenever pointers or variables move; reach for art to show a
tree, list, or grid once.

### Trace table — the workhorse

Don't redraw the array every step. Tabulate: one row per iteration, one column per piece of
state you care about.

```
two pointers, nums = [2, 7, 11, 15, 19], target = 26
 step | L | R | nums[L] + nums[R] | action
 -----+---+---+-------------------+------------------
   1  | 0 | 4 |    2 + 19 = 21     | 21 < 26  => L++
   2  | 1 | 4 |    7 + 19 = 26     | hit      => return (1, 4)
```

Same tool for a sliding window — track the window, the running state, and the answer:

```
longest substring without repeat, s = "abcabcbb"
 left | right | window | seen      | best
 -----+-------+--------+-----------+-----
   0  |   0   | a      | {a}       |  1
   0  |   1   | ab     | {a,b}     |  2
   0  |   2   | abc    | {a,b,c}   |  3
   0  |   3   | abc+a  | dup 'a'!  |  -    => shrink: drop s[0]
   1  |   3   | bca    | {b,c,a}   |  3
```

### Array with two pointers — caret markers

For a single snapshot, a caret line under the array beats a sentence of prose. Keep one tiny
example and walk it inward:

```
is "racecar" a palindrome?  two pointers walking from the ends
    r  a  c  e  c  a  r
    ^                 ^     'r' == 'r'  => L++, R--
       ^           ^        'a' == 'a'  => L++, R--
          ^     ^           'c' == 'c'  => L++, R--
             ^              L meets R   => palindrome
```

### Linked list — arrows and a reversal trace

A list is literally arrows; show the rewire one step at a time, and always note "save `next`
first":

```
reverse a list — keep prev / cur / nxt

 start:   prev = null    cur -> 1 -> 2 -> 3 -> null

 step 1:  nxt = 2;  set 1.next = prev:   1 -> null
          advance:  prev = 1    cur -> 2 -> 3 -> null

 step 2:  2 -> 1 -> null
          prev = 2    cur -> 3 -> null

 step 3:  3 -> 2 -> 1 -> null
          cur = null  => stop, return prev (= 3)
```

### Trees — branches with `/` and `\`

Draw the shape, then write the traversal orders beside it as the proof you understand them:

```
        8
       / \
      3   10
     / \    \
    1   6    14
       / \   /
      4   7 13

 inorder  => 1 3 4 6 7 8 10 13 14   (sorted => it's a valid BST)
 preorder => 8 3 1 6 4 7 10 14 13
 level    => 8 | 3 10 | 1 6 14 | 4 7 13      ('|' separates BFS levels)
```

### Recursion / backtracking — an indented call tree

Pure-ASCII branches (`+--` child, `\--` last child, `|` continues the parent); indentation
is depth, `*` marks an emitted answer:

```
subsets of [1,2,3] via include / exclude (decide one element per level)

 dfs(i=0, [])
 +-- take 1 -> dfs(1, [1])
 |   +-- take 2 -> dfs(2, [1,2])
 |   |   +-- take 3 -> [1,2,3] *
 |   |   \-- skip 3 -> [1,2]   *
 |   \-- skip 2 -> dfs(2, [1])
 |       +-- take 3 -> [1,3]   *
 |       \-- skip 3 -> [1]     *
 \-- skip 1 -> dfs(1, [])
     ... (mirror of the top half, all subsets without element 1) ...
```

### DP table — a filled grid plus the transition

Draw the grid, fill a few cells, and write the recurrence next to the cell it explains:

```
unique paths to the bottom-right of a 3x3 grid
dp[r][c] = dp[r-1][c] + dp[r][c-1]   (paths from above + paths from left)

         c0  c1  c2
   r0 |   1   1   1
   r1 |   1   2   3
   r2 |   1   3   6     dp[2][2] = dp[1][2] + dp[2][1] = 3 + 3 = 6

 answer = dp[2][2] = 6
```

### Grid / matrix — BFS distances and marks

Show input and result stacked; `#` is a wall, numbers are distance from the source:

```
BFS shortest distance from S (top-left); '#' = wall; +1 per layer

 grid           result
 S . .          0 1 2
 # # .          # # 3
 . . .          6 5 4      bottom-left reached at distance 6
```

### Monotonic stack — tabulate the stack contents

```
next greater element, arr = [2, 1, 2, 4, 3]   (stack holds indices, values decreasing)

 i | arr[i] | pop while arr[top] < arr[i] | stack after | result
 --+--------+-----------------------------+-------------+-------------------
 0 |   2    | -                           | [0]         |
 1 |   1    | -                           | [0,1]       |
 2 |   2    | pop 1                       | [0,2]       | ans[1] = 2
 3 |   4    | pop 2, pop 0                | [3]         | ans[2] = 4, ans[0] = 4
 4 |   3    | -                           | [3,4]       |
 end  (indices 3,4 never popped => no greater element)  | ans[3] = ans[4] = -1
```

### Graph — adjacency plus traversal order

```
 edges: 0-1, 0-2, 1-3, 2-3         adjacency list:
                                     0 -> [1, 2]
    0                                1 -> [0, 3]
   / \                               2 -> [0, 3]
  1   2                              3 -> [1, 2]
   \ /
    3       BFS from 0 => 0, 1, 2, 3        DFS from 0 => 0, 1, 3, 2
```

### Rules that keep it readable

- **Draw once, trace in a table.** Redrawing a 6-element array five times wastes the
  screen; a step table shows the same motion in five short rows.
- **Label every marker** (`lo` / `hi` / `slow` / `fast`) and keep columns aligned —
  misalignment is how *you* misread your own trace.
- **Always show before `->` after** for a mutation (pointer rewire, swap, cell update).
- **Pick one tiny example** (n = 4 or 5) that still hits the tricky case — a duplicate, an
  empty branch, a wrap-around. Trace *that*, not a generic input.
- **Don't over-draw.** A tree past ~7 nodes or a grid past ~4x4 costs more than it explains;
  switch to "…and the same pattern continues."

> **Say this in the room:** "Let me trace one small example to check my indices" — then
> narrate the table row by row. Interviewers read a clean trace as evidence you debug
> methodically, and it routinely surfaces the off-by-one *before* you've finished the loop.

## 24. Company problem bank (free problems, by frequency)

A frequency-ranked starter set of **non-premium** LeetCode problems with the companies that
most report them. Drill top-down: the first ~25 are the "if you do nothing else" core —
essentially Blind 75 reordered by how often it actually shows up. Rows **26–70** add medium
breadth; **71–140** extend it with more patterns and the **Yandex · T-Bank · Ozon · VK ·
Avito · Sber** circuit (see the note just under the table). Every technique section above
(§4–§18, §20) now also carries an inline **LeetCode:** line linking the free problems that
drill that exact pattern.

Caveats worth knowing:

- LeetCode's per-company tags are **Premium-gated and shift every quarter**, so read the
  company column as "commonly reported, 2026 season," not gospel. Within a band the order is
  approximate — don't over-index on exact rank.
- Famous **premium-locked** problems (Meeting Rooms / Meeting Rooms II, Alien Dictionary,
  Number of Connected Components, Graph Valid Tree, Encode and Decode Strings) are
  deliberately omitted — they aren't openly accessible. Their *patterns* are still in this
  file (intervals §6, topo sort §15, union-find §18).
- The **§** column points to the owning technique section here, so each problem doubles as a
  drill for a pattern.

Columns: **rank · problem (linked) · difficulty · pattern (§) · companies (most frequent first)**.

| # | Problem | Diff | § | Companies |
|---|---|---|---|---|
| 1 | [Two Sum](https://leetcode.com/problems/two-sum/) | Easy | §7 | Amazon · Google · Apple · Microsoft · Adobe · Bloomberg · Yandex · T-Bank · Sber |
| 2 | [Valid Parentheses](https://leetcode.com/problems/valid-parentheses/) | Easy | §8 | Amazon · Google · Microsoft · Meta · Bloomberg · Yandex · T-Bank · Ozon |
| 3 | [Best Time to Buy and Sell Stock](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/) | Easy | §14 | Amazon · Microsoft · Meta · Google · Bloomberg |
| 4 | [LRU Cache](https://leetcode.com/problems/lru-cache/) | Medium | §18 | Amazon · Microsoft · Google · Meta · Bloomberg · Uber |
| 5 | [Number of Islands](https://leetcode.com/problems/number-of-islands/) | Medium | §15 | Amazon · Google · Microsoft · Meta · Bloomberg |
| 6 | [Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/) | Medium | §4 | Amazon · Google · Meta · Microsoft · Adobe · Bloomberg · Yandex · T-Bank · VK |
| 7 | [Merge Intervals](https://leetcode.com/problems/merge-intervals/) | Medium | §6 | Google · Meta · Amazon · Microsoft · Bloomberg · Yandex · Ozon |
| 8 | [Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/) | Hard | §4 / §8 | Amazon · Google · Goldman Sachs · Bloomberg · Apple · Yandex · T-Bank |
| 9 | [Group Anagrams](https://leetcode.com/problems/group-anagrams/) | Medium | §7 | Amazon · Google · Microsoft · Uber · Meta · Yandex · Ozon · T-Bank |
| 10 | [3Sum](https://leetcode.com/problems/3sum/) | Medium | §4 | Amazon · Meta · Google · Adobe · Microsoft |
| 11 | [Product of Array Except Self](https://leetcode.com/problems/product-of-array-except-self/) | Medium | §4 / §20 | Amazon · Meta · Microsoft · Apple · Lyft |
| 12 | [Maximum Subarray](https://leetcode.com/problems/maximum-subarray/) | Medium | §4 / §14 | Amazon · Microsoft · LinkedIn · Google · Bloomberg |
| 13 | [Valid Anagram](https://leetcode.com/problems/valid-anagram/) | Easy | §7 | Amazon · Bloomberg · Google · Uber · Yandex · Sber |
| 14 | [Merge Two Sorted Lists](https://leetcode.com/problems/merge-two-sorted-lists/) | Easy | §9 | Amazon · Microsoft · Apple · Adobe |
| 15 | [Reverse Linked List](https://leetcode.com/problems/reverse-linked-list/) | Easy | §9 | Amazon · Microsoft · Meta · Apple · Google · Adobe · Yandex · T-Bank · Sber |
| 16 | [Container With Most Water](https://leetcode.com/problems/container-with-most-water/) | Medium | §4 | Amazon · Google · Meta · Bloomberg |
| 17 | [Course Schedule](https://leetcode.com/problems/course-schedule/) | Medium | §15 | Amazon · Google · Meta · Microsoft · ByteDance |
| 18 | [Coin Change](https://leetcode.com/problems/coin-change/) | Medium | §14 | Amazon · Google · Microsoft · Uber · Goldman Sachs |
| 19 | [Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/) | Medium | §11 / §12 | Amazon · Meta · Microsoft · Google |
| 20 | [Word Break](https://leetcode.com/problems/word-break/) | Medium | §14 | Amazon · Google · Meta · Uber · Bloomberg |
| 21 | [Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/) | Medium | §11 | Amazon · Meta · Google · Microsoft · Yelp · Yandex · Avito |
| 22 | [Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/) | Hard | §11 | Amazon · Google · Microsoft · Meta · Goldman Sachs |
| 23 | [Climbing Stairs](https://leetcode.com/problems/climbing-stairs/) | Easy | §14 | Amazon · Adobe · Apple · Google |
| 24 | [Validate Binary Search Tree](https://leetcode.com/problems/validate-binary-search-tree/) | Medium | §10 | Amazon · Meta · Microsoft · Google · Bloomberg |
| 25 | [Binary Tree Level Order Traversal](https://leetcode.com/problems/binary-tree-level-order-traversal/) | Medium | §10 | Amazon · Microsoft · Meta · Bloomberg · Yandex · Ozon |
| 26 | [Lowest Common Ancestor of a Binary Tree](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree/) | Medium | §10 | Amazon · Meta · Microsoft · LinkedIn |
| 27 | [Longest Palindromic Substring](https://leetcode.com/problems/longest-palindromic-substring/) | Medium | §17 | Amazon · Microsoft · Google · Adobe · Wayfair |
| 28 | [Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/) | Medium | §5 | Amazon · Meta · Microsoft · Google · Bloomberg |
| 29 | [Merge k Sorted Lists](https://leetcode.com/problems/merge-k-sorted-lists/) | Hard | §11 / §9 | Amazon · Google · Microsoft · Meta · Bloomberg · Yandex · T-Bank |
| 30 | [Subsets](https://leetcode.com/problems/subsets/) | Medium | §12 | Amazon · Meta · Google · Bloomberg |
| 31 | [Permutations](https://leetcode.com/problems/permutations/) | Medium | §12 | Amazon · Meta · Microsoft · LinkedIn |
| 32 | [Combination Sum](https://leetcode.com/problems/combination-sum/) | Medium | §12 | Amazon · Google · Uber · Bloomberg |
| 33 | [Word Search](https://leetcode.com/problems/word-search/) | Medium | §12 | Amazon · Microsoft · Bloomberg · Meta |
| 34 | [Clone Graph](https://leetcode.com/problems/clone-graph/) | Medium | §15 | Amazon · Google · Meta · Microsoft · Uber |
| 35 | [Course Schedule II](https://leetcode.com/problems/course-schedule-ii/) | Medium | §15 | Amazon · Google · Meta · ByteDance |
| 36 | [Rotting Oranges](https://leetcode.com/problems/rotting-oranges/) | Medium | §15 | Amazon · Google · Microsoft |
| 37 | [Add Two Numbers](https://leetcode.com/problems/add-two-numbers/) | Medium | §9 | Amazon · Microsoft · Meta · Bloomberg · Adobe · Yandex · Ozon |
| 38 | [Median of Two Sorted Arrays](https://leetcode.com/problems/median-of-two-sorted-arrays/) | Hard | §5 | Google · Amazon · Adobe · Microsoft · Apple |
| 39 | [Min Stack](https://leetcode.com/problems/min-stack/) | Medium | §8 / §18 | Amazon · Google · Microsoft · Bloomberg · Uber |
| 40 | [Daily Temperatures](https://leetcode.com/problems/daily-temperatures/) | Medium | §8 | Amazon · Google · Microsoft |
| 41 | [Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/) | Hard | §4 | Amazon · Meta · Google · Microsoft · Uber |
| 42 | [Implement Trie (Prefix Tree)](https://leetcode.com/problems/implement-trie-prefix-tree/) | Medium | §18 | Amazon · Google · Microsoft · Meta · Uber |
| 43 | [Serialize and Deserialize Binary Tree](https://leetcode.com/problems/serialize-and-deserialize-binary-tree/) | Hard | §10 | Amazon · Meta · Google · Microsoft · LinkedIn |
| 44 | [Binary Tree Maximum Path Sum](https://leetcode.com/problems/binary-tree-maximum-path-sum/) | Hard | §10 / §14 | Amazon · Meta · Microsoft · Google · DoorDash |
| 45 | [Pacific Atlantic Water Flow](https://leetcode.com/problems/pacific-atlantic-water-flow/) | Medium | §15 | Amazon · Google · Meta |
| 46 | [Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/) | Hard | §8 | Amazon · Google · Meta · ByteDance · Yandex · VK |
| 47 | [Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/) | Hard | §8 | Amazon · Google · Microsoft · Goldman Sachs |
| 48 | [Word Ladder](https://leetcode.com/problems/word-ladder/) | Hard | §15 / §20 | Amazon · Google · Meta · LinkedIn |
| 49 | [Copy List with Random Pointer](https://leetcode.com/problems/copy-list-with-random-pointer/) | Medium | §9 | Amazon · Meta · Microsoft · Bloomberg |
| 50 | [Linked List Cycle](https://leetcode.com/problems/linked-list-cycle/) | Easy | §9 | Amazon · Microsoft · Bloomberg · Meta · Yandex · T-Bank |
| 51 | [Insert Interval](https://leetcode.com/problems/insert-interval/) | Medium | §6 | Google · Meta · Amazon · LinkedIn · Yandex |
| 52 | [Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/) | Medium | §6 / §16 | Amazon · Google · Meta |
| 53 | [Jump Game](https://leetcode.com/problems/jump-game/) | Medium | §16 / §14 | Amazon · Google · Microsoft · DoorDash |
| 54 | [Unique Paths](https://leetcode.com/problems/unique-paths/) | Medium | §14 | Amazon · Google · Bloomberg · Goldman Sachs |
| 55 | [House Robber](https://leetcode.com/problems/house-robber/) | Medium | §14 | Amazon · Google · Microsoft · Cisco |
| 56 | [Decode Ways](https://leetcode.com/problems/decode-ways/) | Medium | §14 | Amazon · Meta · Google · Microsoft · Uber |
| 57 | [Longest Increasing Subsequence](https://leetcode.com/problems/longest-increasing-subsequence/) | Medium | §14 / §5 | Amazon · Microsoft · Google · Meta |
| 58 | [Edit Distance](https://leetcode.com/problems/edit-distance/) | Medium | §14 | Amazon · Google · Microsoft · ByteDance |
| 59 | [Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/) | Medium | §4 / §7 | Amazon · Meta · Google · Microsoft · Yandex · Ozon |
| 60 | [Spiral Matrix](https://leetcode.com/problems/spiral-matrix/) | Medium | §4 | Amazon · Microsoft · Google · Apple |
| 61 | [Rotate Image](https://leetcode.com/problems/rotate-image/) | Medium | §4 | Amazon · Microsoft · Apple · Google |
| 62 | [Find the Duplicate Number](https://leetcode.com/problems/find-the-duplicate-number/) | Medium | §4 / §9 | Amazon · Google · Microsoft · Bloomberg |
| 63 | [Kth Smallest Element in a BST](https://leetcode.com/problems/kth-smallest-element-in-a-bst/) | Medium | §10 | Amazon · Meta · Google |
| 64 | [Move Zeroes](https://leetcode.com/problems/move-zeroes/) | Easy | §4 | Amazon · Meta · Microsoft · Bloomberg · Yandex · Sber |
| 65 | [Maximum Depth of Binary Tree](https://leetcode.com/problems/maximum-depth-of-binary-tree/) | Easy | §10 | Amazon · Google · LinkedIn |
| 66 | [Majority Element](https://leetcode.com/problems/majority-element/) | Easy | §7 | Amazon · Google · Adobe |
| 67 | [Single Number](https://leetcode.com/problems/single-number/) | Easy | §7 | Amazon · Google · Adobe · Yandex · Sber |
| 68 | [Search a 2D Matrix](https://leetcode.com/problems/search-a-2d-matrix/) | Medium | §5 | Amazon · Microsoft · Google |
| 69 | [Longest Common Subsequence](https://leetcode.com/problems/longest-common-subsequence/) | Medium | §14 | Amazon · Google · Microsoft |
| 70 | [First Missing Positive](https://leetcode.com/problems/first-missing-positive/) | Hard | §4 | Amazon · Google · Microsoft · Goldman Sachs |
| 71 | [Find All Anagrams in a String](https://leetcode.com/problems/find-all-anagrams-in-a-string/) | Medium | §4 / §7 | Yandex · Amazon · Google · T-Bank · ByteDance |
| 72 | [Permutation in String](https://leetcode.com/problems/permutation-in-string/) | Medium | §4 / §7 | Yandex · Microsoft · Amazon · Ozon · Booking |
| 73 | [Max Consecutive Ones III](https://leetcode.com/problems/max-consecutive-ones-iii/) | Medium | §4 | Yandex · Amazon · Google · T-Bank · Facebook |
| 74 | [Longest Repeating Character Replacement](https://leetcode.com/problems/longest-repeating-character-replacement/) | Medium | §4 | Yandex · Google · Amazon · VK · Datadog |
| 75 | [Minimum Size Subarray Sum](https://leetcode.com/problems/minimum-size-subarray-sum/) | Medium | §4 | Yandex · Amazon · Google · Ozon · Facebook |
| 76 | [Subarrays with K Different Integers](https://leetcode.com/problems/subarrays-with-k-different-integers/) | Hard | §4 / §7 | Yandex · Google · Amazon · ByteDance |
| 77 | [Fruit Into Baskets](https://leetcode.com/problems/fruit-into-baskets/) | Medium | §4 | Yandex · Google · Amazon · Avito · T-Bank |
| 78 | [Maximum Average Subarray I](https://leetcode.com/problems/maximum-average-subarray-i/) | Easy | §4 | Yandex · Amazon · Google · Wildberries |
| 79 | [Max Consecutive Ones](https://leetcode.com/problems/max-consecutive-ones/) | Easy | §4 | Yandex · Amazon · Google · T-Bank · Sber |
| 80 | [Squares of a Sorted Array](https://leetcode.com/problems/squares-of-a-sorted-array/) | Easy | §4 | Yandex · Amazon · Google · Ozon · Bloomberg |
| 81 | [Two Sum II - Input Array Is Sorted](https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/) | Medium | §4 | Yandex · Amazon · Google · T-Bank · Apple |
| 82 | [Valid Palindrome](https://leetcode.com/problems/valid-palindrome/) | Easy | §4 / §17 | Yandex · Meta · Amazon · Microsoft · Ozon |
| 83 | [Sort Colors](https://leetcode.com/problems/sort-colors/) | Medium | §4 | Yandex · Amazon · Microsoft · T-Bank · Bloomberg |
| 84 | [Remove Duplicates from Sorted Array](https://leetcode.com/problems/remove-duplicates-from-sorted-array/) | Easy | §4 | Yandex · Amazon · Microsoft · Sber · Bloomberg |
| 85 | [Jewels and Stones](https://leetcode.com/problems/jewels-and-stones/) | Easy | §7 | Yandex · Amazon · Google · VK |
| 86 | [Contiguous Array](https://leetcode.com/problems/contiguous-array/) | Medium | §4 / §7 | Yandex · Amazon · Meta · Google · T-Bank |
| 87 | [Continuous Subarray Sum](https://leetcode.com/problems/continuous-subarray-sum/) | Medium | §4 / §7 | Yandex · Amazon · Google · Ozon · Facebook |
| 88 | [Generate Parentheses](https://leetcode.com/problems/generate-parentheses/) | Medium | §12 | Yandex · Amazon · Google · T-Bank · Uber |
| 89 | [Basic Calculator](https://leetcode.com/problems/basic-calculator/) | Hard | §8 | Yandex · Google · Amazon · Meta · Microsoft |
| 90 | [Basic Calculator II](https://leetcode.com/problems/basic-calculator-ii/) | Medium | §8 | Yandex · Google · Amazon · T-Bank · ByteDance |
| 91 | [Evaluate Reverse Polish Notation](https://leetcode.com/problems/evaluate-reverse-polish-notation/) | Medium | §8 | Yandex · Amazon · LinkedIn · Ozon · Sber |
| 92 | [Decode String](https://leetcode.com/problems/decode-string/) | Medium | §8 | Yandex · Google · Amazon · Microsoft · T-Bank |
| 93 | [Simplify Path](https://leetcode.com/problems/simplify-path/) | Medium | §8 | Yandex · Amazon · Microsoft · Facebook · Bloomberg |
| 94 | [Find the Index of the First Occurrence in a String](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/) | Easy | §17 | Yandex · Amazon · Google · Sber · Microsoft |
| 95 | [Repeated Substring Pattern](https://leetcode.com/problems/repeated-substring-pattern/) | Easy | §17 | Yandex · Amazon · Google · Avito |
| 96 | [Reverse Integer](https://leetcode.com/problems/reverse-integer/) | Medium | §13 | Yandex · Amazon · Bloomberg · T-Bank · Apple |
| 97 | [String to Integer (atoi)](https://leetcode.com/problems/string-to-integer-atoi/) | Medium | §13 / §17 | Yandex · Amazon · Microsoft · Ozon · Bloomberg |
| 98 | [My Calendar I](https://leetcode.com/problems/my-calendar-i/) | Medium | §6 / §18 | Yandex · Google · Amazon · Booking · Datadog |
| 99 | [Meeting Rooms III](https://leetcode.com/problems/meeting-rooms-iii/) | Hard | §6 / §11 | Yandex · Google · Amazon · ByteDance · Databricks |
| 100 | [Last Stone Weight](https://leetcode.com/problems/last-stone-weight/) | Easy | §11 | Yandex · Amazon · Google · T-Bank · Sber |
| 101 | [K Closest Points to Origin](https://leetcode.com/problems/k-closest-points-to-origin/) | Medium | §11 | Yandex · Amazon · Meta · Google · Ozon |
| 102 | [Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/) | Easy | §11 | Yandex · Amazon · Google · T-Bank · Wise |
| 103 | [Sort an Array](https://leetcode.com/problems/sort-an-array/) | Medium | §12 | Yandex · Amazon · Microsoft · Sber · JetBrains |
| 104 | [Gas Station](https://leetcode.com/problems/gas-station/) | Medium | §16 | Yandex · Amazon · Google · Microsoft · Stripe |
| 105 | [Contains Duplicate II](https://leetcode.com/problems/contains-duplicate-ii/) | Easy | §7 | Yandex · Amazon · Google · VK |
| 106 | [Partition Labels](https://leetcode.com/problems/partition-labels/) | Medium | §16 / §7 | Yandex · Amazon · Meta · Google · ByteDance |
| 107 | [Sliding Window Median](https://leetcode.com/problems/sliding-window-median/) | Hard | §11 | Yandex · Amazon · Google · Two Sigma |
| 108 | [Find Duplicate Subtrees](https://leetcode.com/problems/find-duplicate-subtrees/) | Medium | §10 | Yandex · Amazon · Google · ByteDance |
| 109 | [Same Tree](https://leetcode.com/problems/same-tree/) | Easy | §10 | Yandex · Amazon · Microsoft · T-Bank · Bloomberg |
| 110 | [Symmetric Tree](https://leetcode.com/problems/symmetric-tree/) | Easy | §10 | Yandex · Amazon · Microsoft · Sber · LinkedIn |
| 111 | [Balanced Binary Tree](https://leetcode.com/problems/balanced-binary-tree/) | Easy | §10 | Yandex · Amazon · Google · Ozon · Adobe |
| 112 | [Path Sum II](https://leetcode.com/problems/path-sum-ii/) | Medium | §10 / §12 | Yandex · Amazon · Microsoft · T-Bank · Bloomberg |
| 113 | [Remove Invalid Parentheses](https://leetcode.com/problems/remove-invalid-parentheses/) | Hard | §15 / §12 | Yandex · Meta · Amazon · Google |
| 114 | [Maximal Square](https://leetcode.com/problems/maximal-square/) | Medium | §14 | Yandex · Amazon · Google · Snowflake · Bloomberg |
| 115 | [Time Based Key-Value Store](https://leetcode.com/problems/time-based-key-value-store/) | Medium | §5 / §18 | Amazon · Google · Snowflake · Datadog · Stripe |
| 116 | [Design Add and Search Words Data Structure](https://leetcode.com/problems/design-add-and-search-words-data-structure/) | Medium | §18 | Amazon · Meta · Google · JetBrains · Microsoft |
| 117 | [Word Search II](https://leetcode.com/problems/word-search-ii/) | Hard | §12 / §18 | Amazon · Google · Microsoft · Snowflake · Palantir |
| 118 | [Course Schedule IV](https://leetcode.com/problems/course-schedule-iv/) | Medium | §15 | Amazon · Google · ByteDance · Databricks |
| 119 | [Network Delay Time](https://leetcode.com/problems/network-delay-time/) | Medium | §15 | Amazon · Google · Databricks · Datadog · Snowflake |
| 120 | [Cheapest Flights Within K Stops](https://leetcode.com/problems/cheapest-flights-within-k-stops/) | Medium | §15 / §14 | Amazon · Google · Booking · Grab · Uber |
| 121 | [Min Cost to Connect All Points](https://leetcode.com/problems/min-cost-to-connect-all-points/) | Medium | §15 | Amazon · Google · Microsoft · Stripe · Palantir |
| 122 | [Accounts Merge](https://leetcode.com/problems/accounts-merge/) | Medium | §18 / §15 | Amazon · Meta · Google · Atlassian · Revolut |
| 123 | [Evaluate Division](https://leetcode.com/problems/evaluate-division/) | Medium | §15 | Amazon · Google · Meta · Klarna · Bloomberg |
| 124 | [Maximum Product Subarray](https://leetcode.com/problems/maximum-product-subarray/) | Medium | §14 | Amazon · Microsoft · LinkedIn · T-Bank · Booking |
| 125 | [Partition Equal Subset Sum](https://leetcode.com/problems/partition-equal-subset-sum/) | Medium | §14 | Amazon · Google · Meta · Ozon · eBay |
| 126 | [Number of Provinces](https://leetcode.com/problems/number-of-provinces/) | Medium | §18 / §15 | Amazon · Google · Microsoft · Wildberries · Atlassian |
| 127 | [Surrounded Regions](https://leetcode.com/problems/surrounded-regions/) | Medium | §15 | Amazon · Google · Microsoft · Avito |
| 128 | [Letter Combinations of a Phone Number](https://leetcode.com/problems/letter-combinations-of-a-phone-number/) | Medium | §12 | Amazon · Google · Meta · Atlassian · Uber |
| 129 | [Flatten Binary Tree to Linked List](https://leetcode.com/problems/flatten-binary-tree-to-linked-list/) | Medium | §10 | Amazon · Microsoft · Google · JetBrains · Bloomberg |
| 130 | [Construct Binary Tree from Preorder and Inorder Traversal](https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/) | Medium | §10 | Amazon · Microsoft · Google · Bloomberg · Spotify |
| 131 | [Design Twitter](https://leetcode.com/problems/design-twitter/) | Medium | §11 / §18 | Amazon · Twitter · Meta · ByteDance · Grab |
| 132 | [Asteroid Collision](https://leetcode.com/problems/asteroid-collision/) | Medium | §8 | Amazon · Google · Microsoft · Spotify · Revolut |
| 133 | [Find Peak Element](https://leetcode.com/problems/find-peak-element/) | Medium | §5 | Amazon · Google · Microsoft · ByteDance · Wise |
| 134 | [Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/) | Medium | §5 | Amazon · Google · Meta · Databricks · Grab |
| 135 | [Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/) | Medium | §5 | Amazon · Microsoft · Google · T-Bank · Bloomberg |
| 136 | [Number of 1 Bits](https://leetcode.com/problems/number-of-1-bits/) | Easy | §13 | Amazon · Apple · Microsoft · Sber · JetBrains |
| 137 | [Pow(x, n)](https://leetcode.com/problems/powx-n/) | Medium | §13 | Amazon · Google · Meta · LinkedIn · Bloomberg |
| 138 | [Happy Number](https://leetcode.com/problems/happy-number/) | Easy | §13 / §9 | Amazon · Google · Airbnb · JetBrains |
| 139 | [Implement Queue using Stacks](https://leetcode.com/problems/implement-queue-using-stacks/) | Easy | §8 | Amazon · Microsoft · Bloomberg · T-Bank · Nebius |
| 140 | [Subsets II](https://leetcode.com/problems/subsets-ii/) | Medium | §12 | Amazon · Meta · Google · Ozon · Bloomberg |

**Less-common-company circuit (Yandex · T-Bank · Ozon · VK · Avito · Sber).** The 2026
RU/EE-adjacent loop over-indexes on **sliding window, two-pointer/sorted-array,
prefix-sum + hashmap, interval/sweep-line, anagram/hashing, balanced-bracket generation,
k-way merge,** and **expression parsing** — rows 71–113 here cluster on exactly those.
Yandex's live **AA** section is ~10–20-line, language-neutral problems "run in your head"
(no IDE/internet); the classic expression-evaluator wants a **recursive-descent / LL
parser building an AST**, not bracket-stripping (which is O(n²)) — see Basic Calculator
(#89–90). **T-Bank / Ozon / VK / Avito / Sber / Wildberries** draw from the same core
pool, so the **top-25 above transfer directly**. These attributions are pattern- and
candidate-report-based (Habr; see the fuller `yandex.md` bank), **not** LeetCode-Premium-tag-sourced.

> **How to use it.** Pass 1: solve #1–#25 until each is automatic (that's the 80% of the
> signal). Pass 2: #26–#50 for medium breadth. Pass 3: the Hards and the rest. If you're
> targeting one company, filter the column and drill its rows first — but the core 25 are
> common to all of them, so they're never wasted.
