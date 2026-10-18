**In-Memory Versioned Datastore** problem (similar to *Time Based Key-Value Store*, LeetCode 981, but with **two critical twists**: timestamps arrive **out-of-order**, and `get` asks for **strictly less than** `<` rather than `<=`).

---

## Part 1: Problem Statement, Example & Constraints

### Problem Statement

Design an in-memory datastore that stores multiple timestamped versions of documents:

* `void put(int docId, String contents, int timestamp)`: Saves `contents` for `docId` at `timestamp`. Calls for the same `docId` **can arrive out of timestamp order**.
* `String get(int docId, int timestamp)`: Returns the `contents` of `docId` with the largest timestamp **strictly less than** (`<`) the given `timestamp`. Returns `""` if `docId` doesn't exist or no version has a timestamp `< timestamp`.

### Example

```text
put(1, "abc", 10)
put(1, "cde", 8)   // Out-of-order! 8 arrives after 10.

get(1, 5)  -> ""    // No version < 5
get(1, 8)  -> ""    // Strictly less than 8! Version 8 does NOT qualify.
get(1, 9)  -> "cde" // Version 8 < 9
get(1, 11) -> "abc" // Versions 8 and 10 are < 11; 10 is the latest.

```

### 3 Quick Clarifying Questions (Ask Before Coding)

1. *"Can `put` be called twice with the **exact same** `docId` and `timestamp`? If so, should the latest call overwrite the previous content?"* (Usually yes—overwriting is standard).
2. *"Are timestamps guaranteed to be positive integers, and can `put` timestamps arrive out of order?"* (Yes, out-of-order is explicitly stated—this rules out a simple append-only `ArrayList` with binary search unless we sort).
3. *"Do we need this to be thread-safe for concurrent reads and writes?"* (We will start single-threaded and add thread-safety as the SDE-2 follow-up).

---

## Part 2: Two-Step Interview Progression

### Approach 1: Brute Force (`HashMap` + Unsorted `ArrayList`) — *Mention Briefly, Don't Code Unless Asked*

#### How to Narrate This in the Interview

> *"As a quick baseline, I could store a `Map<Integer, List<Version>>` where each `docId` maps to an unsorted list of `(timestamp, contents)` pairs.*
> * **`put` would be $\mathcal{O}(1)$:** Just append the new version to the list.*
> * **`get` would be $\mathcal{O}(V)$ (where $V$ is the number of versions for that `docId`):** Scan the entire list linearly to find the version with the largest timestamp strictly less than the query `timestamp`.*
> 
> 
> *However, if a document has thousands of versions and `get` is called frequently, an $\mathcal{O}(V)$ linear scan is too slow. Because `put` timestamps arrive out of order, we can't just binary search an `ArrayList` without re-sorting it in $\mathcal{O}(V \log V)$. Instead, we should keep each document's versions dynamically sorted using a Balanced Binary Search Tree (`TreeMap`)."*

---

### Approach 2: Optimal Solution (`HashMap` + `TreeMap`)

#### Simple Java Concepts Used

* **`TreeMap<Integer, String>`:** In Java, a `TreeMap` is a Red-Black Tree (a self-balancing binary search tree) that keeps its keys (`timestamp`) in sorted order automatically, even when inserted out of order!Both inserting a key (`put`) and searching for a key take **$\mathcal{O}(\log V)$** time.
* **`treeMap.lowerEntry(timestamp)` (The Star Method!):**
Java's `TreeMap` has two built-in methods for range lookups that interviewers love:
* `floorEntry(t)` $\rightarrow$ Finds the greatest key **less than or equal to** `t` ($\le t$).
* **`lowerEntry(t)`** $\rightarrow$ Finds the greatest key **strictly less than** `t` ($< t$).
Since our problem asks for a version **strictly less than** `timestamp`, `treeMap.lowerEntry(timestamp)` does the exact binary search we need in a single line!



#### How to Narrate This in the Interview

> *"To optimize both out-of-order writes and fast reads, I'll use a nested map structure: `Map<Integer, String TreeMap<Integer,>>`.*
> * *The outer `HashMap` maps each `docId` to its version history in $\mathcal{O}(1)$ time.*
> * *The inner `TreeMap` maps `timestamp -> contents`, automatically keeping timestamps sorted via a Red-Black Tree even when `put` calls arrive out of order.*
> * *For `put`, we insert into the `TreeMap` in $\mathcal{O}(\log V)$ time.*
> * *For `get`, we call `treeMap.lowerEntry(timestamp)`, which walks the tree in $\mathcal{O}(\log V)$ time to find the largest timestamp strictly less than our query timestamp."*
> 
> 

#### Java Code (Optimal Solution)

```java
import java.util.*;

public class VersionedDatastore {

    // Outer Map: docId -> Inner TreeMap
    // Inner TreeMap: timestamp (sorted ascending) -> contents
    private final Map<Integer, TreeMap<Integer, String>> store;

    public VersionedDatastore() {
        this.store = new HashMap<>();
    }

    public void put(int docId, String contents, int timestamp) {
        // 1. Fetch the TreeMap for this docId, or create a new one if this is the first time seeing docId.
        // computeIfAbsent is clean and runs in O(1) average time.
        TreeMap<Integer, String> versions = store.computeIfAbsent(docId, id -> new TreeMap<>());

        // 2. Insert (or overwrite) the contents at this timestamp.
        // TreeMap automatically places it in sorted timestamp order in O(log V) time!
        versions.put(timestamp, contents);
    }

    public String get(int docId, int timestamp) {
        // 1. Lookup the version history for docId in O(1) time
        TreeMap<Integer, String> versions = store.get(docId);

        // If docId doesn't exist, return ""
        if (versions == null) {
            return "";
        }

        // 2. lowerEntry(timestamp) finds the entry with the greatest key STRICTLY LESS (<) than 'timestamp'
        // Runs in O(log V) time using binary search over the Red-Black Tree.
        Map.Entry<Integer, String> entry = versions.lowerEntry(timestamp);

        // If all existing versions have timestamps >= query timestamp, entry will be null
        return entry != null ? entry.getValue() : "";
    }
}

```

---

### Bonus SDE-2 Follow-Up: "How Do You Make This Thread-Safe?"

In almost every SDE-2 interview, once you write the `TreeMap` code above (which only takes ~5 minutes), the interviewer will ask:

> *"Great! Now what if multiple threads are calling `put` and `get` concurrently?"*

#### Simple Java Concepts for Concurrency

* `HashMap` and `TreeMap` are **not thread-safe**.
* For the outer map, we swap `HashMap` for `ConcurrentHashMap`.
* What about the inner map? You have **two great options** to mention:
1. **`ConcurrentSkipListMap<Integer, String>` (Simplest Code):** Java's built-in thread-safe equivalent of `TreeMap`! It uses a lock-free Skip List under the hood, keeps keys sorted, has the exact same `.lowerEntry(timestamp)` method, and gives $\mathcal{O}(\log V)$ thread-safe `put` and `get` without writing a single lock!
2. **Per-Document `ReentrantReadWriteLock`:** If you want fine-grained locking around a `TreeMap`, give each `docId` its own `ReadWriteLock` so multiple threads can `get()` the same document simultaneously, while `put()` acquires a write lock for that specific document only.



#### How to Narrate This in the Interview

> *"To make this thread-safe with high concurrency, I don't want a single global `synchronized` lock on the whole datastore because threads modifying `docId = 1` shouldn't block threads reading `docId = 2`.*
> *In Java, the cleanest drop-in upgrade is replacing the outer `HashMap` with `ConcurrentHashMap` and replacing the inner `TreeMap` with `ConcurrentSkipListMap`. `ConcurrentSkipListMap` is Java's lock-free, thread-safe sorted map—it supports the exact same `lowerEntry(timestamp)` API in $\mathcal{O}(\log V)$ time while allowing safe concurrent reads and writes across all documents."*

#### Java Code (Thread-Safe SDE-2 Upgrade)

```java
import java.util.*;
import java.util.concurrent.*;

public class ConcurrentVersionedDatastore {

    // ConcurrentHashMap + ConcurrentSkipListMap = 100% thread-safe and lock-free!
    private final ConcurrentMap<Integer, ConcurrentSkipListMap<Integer, String>> store;

    public ConcurrentVersionedDatastore() {
        this.store = new ConcurrentHashMap<>();
    }

    public void put(int docId, String contents, int timestamp) {
        // Atomic creation of the per-document SkipListMap if absent, then O(log V) thread-safe insert
        store.computeIfAbsent(docId, id -> new ConcurrentSkipListMap<>())
             .put(timestamp, contents);
    }

    public String get(int docId, int timestamp) {
        ConcurrentSkipListMap<Integer, String> versions = store.get(docId);
        if (versions == null) {
            return "";
        }

        // ConcurrentSkipListMap also implements NavigableMap, so lowerEntry() works identically!
        Map.Entry<Integer, String> entry = versions.lowerEntry(timestamp);
        return entry != null ? entry.getValue() : "";
    }
}

```

---

## Part 3: Complexity Summary (How to Close the Interview)

Let **$D$** = number of unique `docId`s, and **$V$** = maximum number of versions stored for a single `docId` (and **$N$** = total `put` calls across all documents).

| Operation / Metric | Approach 1 (Unsorted List) | Approach 2 (`HashMap` + `TreeMap` / `ConcurrentSkipListMap`) |
| --- | --- | --- |
| **`put(docId, contents, timestamp)`** | $\mathcal{O}(1)$ | **$\mathcal{O}(\log V)$** — $\mathcal{O}(1)$ to lookup `docId` + $\mathcal{O}(\log V)$ to insert into the tree/skip-list. |
| **`get(docId, timestamp)`** | $\mathcal{O}(V)$ linear scan | **$\mathcal{O}(\log V)$** — $\mathcal{O}(1)$ to lookup `docId` + $\mathcal{O}(\log V)$ binary search via `lowerEntry()`. |
| **Space Complexity** | $\mathcal{O}(N)$ | **$\mathcal{O}(N)$** to store all versions across all documents. |
