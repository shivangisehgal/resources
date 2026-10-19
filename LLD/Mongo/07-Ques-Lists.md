# MongoDB LLD — 6 LeetCode-Style Coding Problems

I've reviewed all six files in your [GitHub repository](https://github.com/shivangisehgal/resources/tree/main/LLD/Mongo?utm_source=chatgpt.com) and converted them into standalone Java practice problems.

Each problem includes a problem statement, examples, constraints, starter code, and interview follow-ups. The boilerplate deliberately contains no implementation, so you can practice from scratch.

## Practice order

| # | Problem                          | Difficulty  | Main concepts                              |
| - | -------------------------------- | ----------- | ------------------------------------------ |
| 1 | Recursive JSON Key Search        | Medium      | DFS, BFS, recursion                        |
| 2 | Versioned Datastore              | Medium      | HashMap, TreeMap, concurrency              |
| 3 | Lazy Union Iterator              | Medium–Hard | Iterators, two pointers, heap              |
| 4 | Nested Predicate Expression Tree | Hard        | Trees, recursion, short-circuiting         |
| 5 | Thread-Safe LRU Cache            | Hard        | HashMap, DLL, locks, sharding              |
| 6 | Concurrent Web Crawler           | Hard        | Thread pools, concurrency, synchronization |

These are practice difficulty estimates, not official LeetCode ratings.

## 1. Recursive Key Search in Nested JSON

Medium · Trees / DFS / BFS

### Problem statement

You are given a nested JSON-like structure `root` and a string `targetKey`.

The JSON structure may contain maps, lists, strings, numbers, booleans, and null values. Maps and lists may contain other nested maps and lists.

Implement a function that searches the entire structure for `targetKey` and returns its associated value.

The search follows depth-first traversal, preserving the iteration order of maps and lists. At each map, check its own keys before searching its children.

If the key does not exist, return `null`.

If the key exists with a `null` value, that counts as a successful match, and the search must stop.

### Example 1

Input

```

{
  "user": "alice",
  "roles": [
    {
      "team": "payments",
      "secretKey": "xyz-987"
    },
    {
      "team": "platform"
    }
  ]
}

```

`targetKey = "secretKey"`

Output

```

"xyz-987"

```

### Example 2

```

root = {
  "a": {
    "target": null
  },
  "b": {
    "target": 42
  }
}

targetKey = "target"

Output: null

```

Explanation: The first matching key exists and has an explicit `null` value.

### Constraints

- Total JSON nodes: 1 to 100,000.
- Maximum nesting depth: 10,000.
- Keys are case-sensitive.
- Maps use insertion order for deterministic traversal.
- JSON input contains no cyclic references.

### Java boilerplate

```

import java.util.*;

class JsonSearcher {

    public Object findKey(Object root, String targetKey) {
        // TODO: Implement search
        return null;
    }

}

```

### Follow-ups

1. Implement recursive DFS.
2. Implement iterative BFS without recursion.
3. Return the shallowest occurrence of a key.
4. Distinguish an absent key from a key whose value is `null`.

Source: [Original implementation](https://github.com/shivangisehgal/resources/blob/main/LLD/Mongo/02-Recursive-JSON-Key-search.md?utm_source=chatgpt.com)

## 2. Design an In-Memory Versioned Datastore

Medium · HashMap / TreeMap / Binary Search

### Problem statement

Design an in-memory datastore that maintains multiple versions of a document, each associated with an integer timestamp.

Implement the `VersionedDatastore` class:

- `VersionedDatastore()` initializes the datastore.
- `void put(int docId, String contents, int timestamp)` stores the content of a document at a particular timestamp.
- `String get(int docId, int timestamp)` returns the content associated with the largest stored timestamp strictly smaller than the given timestamp.

If no matching version exists, return an empty string.

Unlike a conventional time-based key-value store, timestamps are not guaranteed to arrive in increasing order.

If `put` is called again for an existing `(docId, timestamp)` pair, overwrite the previous content.

### Example 1

```

Input:
["VersionedDatastore", "put", "put", "get",
 "get", "get", "get"]

[[], [1, "abc", 10], [1, "cde", 8],
 [1, 5], [1, 8], [1, 9], [1, 11]]

Output:
[null, null, null, "", "", "cde", "abc"]

```

Explanation

| Operation           | Result          |
| ------------------- | --------------- |
| `put(1, "abc", 10)` | Save version 10 |
| `put(1, "cde", 8)`  | Save version 8  |
| `get(1, 5)`         | `""`            |
| `get(1, 8)`         | `""`            |
| `get(1, 9)`         | `"cde"`         |
| `get(1, 11)`        | `"abc"`         |

### Constraints

- `1 <= docId <= 10^6`
- `1 <= timestamp <= 10^9`
- `0 <= contents.length <= 10^4`
- At most `10^5` calls to `put` and `get`.
- Timestamps may arrive out of order.
- A document may have multiple versions.

### Java boilerplate

```

import java.util.*;

class VersionedDatastore {

    public VersionedDatastore() {
        // TODO: Initialize datastore
    }

    public void put(int docId, String contents, int timestamp) {
        // TODO: Store or overwrite a version
    }

    public String get(int docId, int timestamp) {
        // TODO: Find the latest version with timestamp < t
        return "";
    }
}

```

### Follow-ups

1. Implement with an unsorted list.
2. Optimize using an ordered data structure.
3. Implement your own balanced-search-tree-based approach instead of using `TreeMap`.
4. Make `put()` and `get()` thread-safe for concurrent readers and writers.

Source: [Original implementation](https://github.com/shivangisehgal/resources/blob/main/LLD/Mongo/03-Versioned-Datastore-With-Unordered-Timestamp.md?utm_source=chatgpt.com)

## 3. Design a Lazy Union Iterator Over Sorted Inputs

Medium–Hard · Iterator / Two Pointers / Min-Heap

### Problem statement

You are given two iterators `it1` and `it2` that produce elements in non-decreasing order.

Design a class `UnionIterator<T>` implementing Java's `Iterator<T>` interface.

The iterator must lazily merge the two sequences into one sorted sequence.

Your implementation must support:

- `boolean hasNext()` — Returns whether another element is available.
- `T next()` — Returns the next smallest element.
- Duplicates must be preserved.
- When elements are equal, consume from `it1` before `it2`.
- Repeated calls to `hasNext()` must not skip or duplicate output.
- Do not materialize the complete merged sequence in memory.

### Example 1

```

it1 = [2, 4, 6, 8, 10]
it2 = [1, 2, 3, 5, 8, 13]

Output:
[1, 2, 2, 3, 4, 5, 6, 8, 8, 10, 13]

```

### Example 2

```

it1 = []
it2 = [1, 3, 5]

hasNext() -> true
hasNext() -> true
next()    -> 1
next()    -> 3
next()    -> 5
hasNext() -> false
next()    -> throws NoSuchElementException

```

### Constraints

- `0 <= N, M <= 10^5`
- Each iterator produces sorted elements.
- Elements implement `Comparable<T>`.
- Neither input contains `null` elements.
- Auxiliary space must be `O(1)` for the two-iterator version.
- The total number of `next()` calls is at most `N + M`.

### Java boilerplate

```

import java.util.*;

class UnionIterator<T extends Comparable<T>>
        implements Iterator<T> {

    public UnionIterator(Iterator<T> it1, Iterator<T> it2) {
        // TODO
    }

    @Override
    public boolean hasNext() {
        // TODO
        return false;
    }

    @Override
    public T next() {
        // TODO
        throw new NoSuchElementException();
    }
}

```

### Follow-up 1: Deduplicating union

Modify the iterator to return each distinct element only once.

```

it1 = [1, 1, 2, 4, 4, 7]
it2 = [1, 3, 4, 5]

Output = [1, 2, 3, 4, 5, 7]

```

```

class DistinctUnionIterator<T extends Comparable<T>>
        implements Iterator<T> {

    public DistinctUnionIterator(Iterator<T> it1,
                                 Iterator<T> it2) {
        // TODO
    }

    @Override
    public boolean hasNext() {
        // TODO
        return false;
    }

    @Override
    public T next() {
        // TODO
        throw new NoSuchElementException();
    }
}

```

### Follow-up 2: Merge K sorted iterators

Generalize the deduplicating union to accept `K` sorted iterators.

```

it1 = [1, 3, 5]
it2 = [2, 3, 8]
it3 = [1, 4, 9]

Output = [1, 2, 3, 4, 5, 8, 9]

```

```

import java.util.*;

class KSortedUnionIterator<T extends Comparable<T>>
        implements Iterator<T> {

    public KSortedUnionIterator(List<Iterator<T>> iterators) {
        // TODO
    }

    @Override
    public boolean hasNext() {
        // TODO
        return false;
    }

    @Override
    public T next() {
        // TODO
        throw new NoSuchElementException();
    }
}

```

Additional constraint: Use `O(K)` auxiliary memory rather than loading all `N` elements.

Source: [Original implementation](https://github.com/shivangisehgal/resources/blob/main/LLD/Mongo/04-Lazy%20Union%20Iterator%20Over%20Sorted%20Inputs.md?utm_source=chatgpt.com)

## 4. Evaluate a Nested Predicate Expression Tree

Hard · Trees / DFS / Expression Evaluation

### Problem statement

You are given a document represented as `Map<String, Object>` and a boolean expression tree represented by `PredicateNode`.

Each node in the tree can be one of four types:

| Node type | Behaviour                                            |
| --------- | ---------------------------------------------------- |
| `AND`     | Returns true if all children evaluate to true        |
| `OR`      | Returns true if at least one child evaluates to true |
| `NOT`     | Negates its only child's result                      |
| `LEAF`    | Evaluates a field comparison against the document    |

A leaf node contains a field path, comparison operator, and expected value.

Supported comparison operators are `EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, and `LESS_THAN`.

Implement `boolean evaluate(PredicateNode root, Map<String, Object> document)`.

Requirements:

- Support dot-separated nested paths, such as `user.country`.
- Return false for missing fields.
- Support short-circuit evaluation.
- `AND` with no children evaluates to true.
- `OR` with no children evaluates to false.
- `NOT` has exactly one child.
- Comparisons between incompatible types evaluate to false.
- Equality with explicit `null` is allowed, but an absent field must not match even a `null` comparison.
- Ordering comparisons support compatible numbers and strings.

### Example 1

Document

```

{
  "role": "admin",
  "age": 28,
  "user": {
    "country": "IN",
    "verified": true
  }
}

```

Expression

Expression: (role == "admin") AND (age > 30 OR user.country == "IN")

Output: `true`

### Example 2

```

Document:
{ "age": 25, "active": false }

Expression:
AND(
    age > 18,
    NOT(active == true)
)

Output: true

```

### Constraints

- Number of nodes: 1 to 100,000.
- Maximum depth: 50,000.
- Leaf paths contain at most 100 segments.
- Field names are case-sensitive.
- No cycles exist in the expression tree.
- All predicate nodes are structurally valid.

### Java boilerplate

```

import java.util.*;

enum NodeType {
    AND, OR, NOT, LEAF
}

enum ComparisonOp {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN
}

class PredicateNode {

    NodeType type;
    List<PredicateNode> children;

    String fieldPath;
    ComparisonOp op;
    Object expectedValue;

    // Constructor for logical nodes
    PredicateNode(NodeType type,
                  List<PredicateNode> children) {
        this.type = type;
        this.children = children;
    }

    // Constructor for leaf nodes
    PredicateNode(String fieldPath,
                  ComparisonOp op,
                  Object expectedValue) {
        this.type = NodeType.LEAF;
        this.fieldPath = fieldPath;
        this.op = op;
        this.expectedValue = expectedValue;
        this.children = Collections.emptyList();
    }
}

class PredicateEvaluator {

    public boolean evaluate(PredicateNode root,
                            Map<String, Object> document) {
        // TODO
        return false;
    }

    private Object resolveField(Map<String, Object> document,
                                String path) {
        // TODO
        return null;
    }

    private boolean evaluateLeaf(PredicateNode node,
                                 Map<String, Object> document) {
        // TODO
        return false;
    }
}

```

### Follow-ups

1. Implement recursively with short-circuit evaluation.
2. Introduce a configurable maximum recursion depth.
3. Evaluate an expression tree of depth 50,000 without using recursion.
4. Preserve short-circuiting in the iterative implementation.

Source: [Original implementation](https://github.com/shivangisehgal/resources/blob/main/LLD/Mongo/05-Evaluate%20a%20Nested%20Predicate%20Expression%20Tree.md?utm_source=chatgpt.com)

## 5. Design a Thread-Safe LRU Cache

Hard · HashMap / Doubly Linked List / Locks

### Problem statement

Design and implement a generic, in-memory Least Recently Used (LRU) cache with a fixed capacity.

The cache must support concurrent access by multiple threads without corrupting internal state.

Implement `ThreadSafeLRUCache<K, V>`:

- `ThreadSafeLRUCache(int capacity)` initializes the cache.
- `V get(K key)` returns the cached value, or `null` if absent. A successful lookup makes the entry the most recently used.
- `void put(K key, V value)` inserts or updates an entry, marking it most recently used.

When inserting a new item would exceed capacity, evict the least recently used item.

Both operations should run in expected `O(1)` time, excluding time spent waiting for locks.

The implementation must be linearizable: each operation should behave as though it takes effect atomically at some instant between invocation and completion.

### Example

```

ThreadSafeLRUCache<String, Integer> cache
    = new ThreadSafeLRUCache<>(2);

cache.put("A", 1);
cache.put("B", 2);

cache.get("A");    // 1
cache.put("C", 3);

cache.get("B");    // null (evicted)
cache.get("A");    // 1
cache.get("C");    // 3

```

### Constraints

- `1 <= capacity <= 10^5`
- At most `10^5` operations per test.
- Multiple threads may call `get` and `put` concurrently.
- Null keys and null values are not permitted.
- A key must appear at most once.
- Eviction must follow the global LRU order for the base problem.

### Java boilerplate

```

import java.util.*;
import java.util.concurrent.locks.*;

class ThreadSafeLRUCache<K, V> {

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    public ThreadSafeLRUCache(int capacity) {
        // TODO
    }

    public V get(K key) {
        // TODO
        return null;
    }

    public void put(K key, V value) {
        // TODO
    }

    private void addToHead(Node<K, V> node) {
        // TODO
    }

    private void removeNode(Node<K, V> node) {
        // TODO
    }

    private void moveToHead(Node<K, V> node) {
        // TODO
    }
}

```

### Follow-up: Sharded LRU

Suppose your cache is accessed by 64 concurrent threads, and a single global lock is causing contention.

Design a segmented LRU cache in which each segment maintains an independent eviction order and lock.

For this follow-up, global LRU eviction is no longer required; eviction occurs within the relevant segment.

```

class SegmentedLRUCache<K, V> {

    public SegmentedLRUCache(int totalCapacity,
                             int concurrencyLevel) {
        // TODO
    }

    public V get(K key) {
        // TODO
        return null;
    }

    public void put(K key, V value) {
        // TODO
    }
}

```

Additional requirements: The total capacity across segments must equal `totalCapacity`, each segment has its own synchronization mechanism, and key-to-segment routing is deterministic.

Source: [Original implementation](https://github.com/shivangisehgal/resources/blob/main/LLD/Mongo/06-LRU-Cache-Concurrency-Safe.md?utm_source=chatgpt.com)

## 6. Design a Concurrent Web Crawler

Hard · Graphs / BFS / Thread Pools / Concurrent Collections

### Problem statement

You are given a starting URL `seedUrl` and an `HtmlParser` interface.

The interface provides a method `fetchAndParse(url)` that fetches a webpage and returns all URLs present on that page.

Implement a web crawler that visits every uniquely reachable URL belonging to the same hostname as `seedUrl`.

The crawler must avoid infinite loops, skip URLs belonging to other hostnames, and return all qualifying URLs in any order.

### Provided interface

```

interface HtmlParser {
    List<String> fetchAndParse(String url);
}

```

### Example

```

seedUrl = "http://news.yahoo.com/news"

Web graph:

"http://news.yahoo.com/news"
    -> [
        "http://news.yahoo.com/us",
        "http://news.yahoo.com/world",
        "http://news.google.com/"
       ]

"http://news.yahoo.com/us"
    -> ["http://news.yahoo.com/news"]

"http://news.yahoo.com/world"
    -> []

Output (any order):
[
    "http://news.yahoo.com/news",
    "http://news.yahoo.com/us",
    "http://news.yahoo.com/world"
]

```

Explanation: The Google URL belongs to a different hostname and is excluded. The cycle back to the seed URL must not cause repeated crawling.

### Constraints

- `1 <= number of reachable URLs <= 10^5`
- Each page contains at most 100 URLs.
- The URL graph can contain cycles.
- URLs are valid absolute HTTP/HTTPS URLs.
- URL uniqueness is based on exact string equality.
- `fetchAndParse` is a blocking I/O operation and can take 100–2,000 ms.
- Result ordering does not matter.
- Concurrent parser calls are supported.

### Java boilerplate

```

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

class Solution {

    public List<String> crawl(String seedUrl,
                              HtmlParser htmlParser) {
        // TODO: Crawl all unique same-host URLs
        return new ArrayList<>();
    }

    private String getHost(String url) {
        // TODO: Extract exact hostname
        return "";
    }
}

```

### Follow-up 1: Concurrent crawling

Your single-threaded implementation is too slow because each network request blocks.

Modify the crawler to fetch multiple pages concurrently.

You may use `ExecutorService`, `CompletableFuture`, or other Java concurrency primitives.

### Follow-up 2: Bounded concurrency and failures

Extend your crawler to meet the following requirements:

- At most `maxThreads` fetches may execute simultaneously.
- A failing fetch must not terminate the entire crawl.
- The crawler must handle timeouts; the parser may additionally support cancellation or network-level timeouts.
- All resources must be cleaned up after execution.

```

class BoundedCrawler {

    public List<String> crawl(String seedUrl,
                              HtmlParser parser,
                              int maxThreads) {
        // TODO
        return new ArrayList<>();
    }
}

```

Note: Timing out a `CompletableFuture` does not necessarily stop the underlying blocking network request.

### Follow-up 3: Remove the BFS-level barrier

Your current implementation waits for an entire BFS level to finish before starting work on newly discovered URLs.

Modify it so workers can schedule newly discovered URLs immediately.

Ensure the implementation:

- Does not crawl a URL twice.
- Safely coordinates concurrent task submission.
- Detects when all queued and running work has completed.
- Does not shut down the executor prematurely.
- Handles failures without leaving completion counters inconsistent.

```

class ConcurrentCrawler {

    public List<String> crawl(String seedUrl,
                              HtmlParser parser,
                              int maxThreads) {
        // TODO
        return new ArrayList<>();
    }

    private void submitTask(String url) {
        // TODO: Schedule a crawl task
    }
}

```

Source: [Original implementation](https://github.com/shivangisehgal/resources/blob/main/LLD/Mongo/01-ConcurentWebCrawler.md?utm_source=chatgpt.com)

## Track your practice

Your completion progress

0 / 6 solved

1\. Recursive JSON Key Search

Not started

2\. Versioned Datastore

Not started

3\. Lazy Union Iterator

Not started

4\. Nested Predicate Expression Tree

Not started

5\. Thread-Safe LRU Cache

Not started

6\. Concurrent Web Crawler

Not started

&#x20;Copy practice tracker

For interview practice, solve each base problem first and then attempt its follow-ups without checking the original implementations. Once you have code working, assess time complexity, space complexity, correctness under edge cases, and—for the concurrent problems—thread safety.
