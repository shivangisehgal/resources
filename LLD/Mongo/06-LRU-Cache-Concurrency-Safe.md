Here is the streamlined SDE-2 interview playbook for **Implement a Thread-Safe, High-Concurrency LRU Cache**.

In an SDE-1 interview, you just write a single-threaded `HashMap` + Doubly Linked List (LeetCode 146). In an **SDE-2 interview**, the core trap is that **even `get(key)` is a write operation** in an LRU cache (because reading a key moves its node to the front of the Doubly Linked List!).

If you just slap `synchronized` on `get()` and `put()`, 64 threads reading the cache will line up one by one on a single global lock. Here is how to build it from the clean baseline up to the high-concurrency **Segmented (Sharded) Lock** architecture used in real production caches like Guava Cache.

---

## Part 1: Problem Statement, Example & Clarifying Questions

### Problem Statement

Design and implement an in-memory **Least Recently Used (LRU) Cache** with a fixed `capacity` that supports concurrent access by multiple threads:

* `V get(K key)`: Returns the value associated with `key` if present (and marks it as most recently used), or `null` if absent.
* `void put(K key, V value)`: Inserts or updates `(key, value)` (marking it as most recently used). If the cache exceeds `capacity`, evicts the **least recently used** entry.
* Both `get` and `put` must run in **$\mathcal{O}(1)$ time** and remain **100% thread-safe** without race conditions, corrupted linked-list pointers, or deadlocks.

### Example (Capacity = 2)

```text
put("A", 1)   // Cache order (MRU -> LRU): [A=1]
put("B", 2)   // Cache order (MRU -> LRU): [B=2, A=1]
get("A")      // Returns 1. 'A' moves to front! Order: [A=1, B=2]
put("C", 3)   // Cache full! Evicts LRU ('B'). Order: [C=3, A=1]
get("B")      // Returns null (was evicted)

```

### 3 Quick Clarifying Questions (Ask Before Coding!)

1. *"In an LRU cache, `get(key)` mutates the Doubly Linked List by moving the node to the head. Does `ReadWriteLock` help here, or do reads also require exclusive pointer mutation?"*
* **Why this impresses the interviewer immediately:** It shows you know the #1 trap of concurrent LRU! A standard `ReadWriteLock`'s `readLock()` cannot be used if `get()` modifies Doubly Linked List pointers (`prev`/`next`), because two concurrent readers modifying pointers simultaneously will corrupt the linked list!


2. *"How many concurrent threads do we expect, and is a **Segmented (Sharded) LRU** acceptable so threads accessing different keys don't block each other?"* (Yes—sharding is the gold-standard SDE-2 optimization).
3. *"Can keys or values be `null`?"* (Standard concurrent Java collections like `ConcurrentHashMap` disallow `null` keys/values to avoid ambiguity—we will disallow `null`).

---

## Part 2: Two-Step Interview Progression

### Approach 1: Single-Segment Thread-Safe LRU (`HashMap` + Doubly Linked List + `ReentrantLock`)

#### Simple Java Concepts Used

1. **Why `HashMap` + Doubly Linked List?**
* `HashMap<K, Node<K, V>>` gives $\mathcal{O}(1)$ lookup to find a node anywhere in the list.
* **Doubly Linked List** (with dummy `head` and `tail` sentinel nodes) gives $\mathcal{O}(1)$ node removal and $\mathcal{O}(1)$ insertion at the front (Most Recently Used) and eviction from the back (Least Recently Used).
* Using **dummy `head` and `tail` nodes** eliminates all ugly `if (node.prev == null)` null-checks!


2. **Why `ReentrantLock` instead of `ReentrantReadWriteLock` for a single list?**
* Every `get(key)` detaches `node` from its current position and attaches it right after `head`. Because that mutates `prev` and `next` pointers on shared nodes, `get()` is actually a **state-mutating operation** and needs an exclusive lock on that linked list.



#### How to Narrate This in the Interview

> *"First, I'll build the core thread-safe LRU unit using a `HashMap` mapped to nodes in a Doubly Linked List with dummy `head` and `tail` sentinels. We keep the Most Recently Used (MRU) items right after `head` and evict the Least Recently Used (LRU) items right before `tail`.*
> *A common trap is trying to use a `ReadWriteLock` where `get()` takes a shared `readLock()`. That fails because `get()` mutates the linked list's `prev` and `next` pointers to move the node to the front—two threads doing that concurrently would corrupt the list!*
> *So for a single LRU unit, I'll guard the `HashMap` and Doubly Linked List with a `ReentrantLock` inside a `try-finally` block. Then, in Step 2, I'll remove the single-lock bottleneck by sharding the cache into $N$ independent segments."*

#### Stage 1 Java Code (Core Thread-Safe LRU Segment)

```java
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadSafeLRUCache<K, V> {

    // Doubly Linked List Node
    private static class Node<K, V> {
        final K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> map;
    // Dummy head and tail sentinel nodes so we never deal with null pointer checks!
    // Order: head (MRU side) <-> ...nodes... <-> tail (LRU side)
    private final Node<K, V> head;
    private final Node<K, V> tail;
    private final ReentrantLock lock = new ReentrantLock();

    public ThreadSafeLRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }
        this.capacity = capacity;
        this.map = new HashMap<>(capacity);

        // Initialize dummy sentinels pointing to each other
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        this.head.next = this.tail;
        this.tail.prev = this.head;
    }

    public V get(K key) {
        if (key == null) throw new NullPointerException("Key cannot be null");

        lock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null) {
                return null; // Cache miss
            }
            // Cache hit! Move node to the front (MRU position)
            moveToHead(node);
            return node.value;
        } finally {
            // CRITICAL: Always unlock in a finally block!
            lock.unlock();
        }
    }

    public void put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("Key and value cannot be null");
        }

        lock.lock();
        try {
            Node<K, V> existing = map.get(key);

            // Case 1: Key already exists -> update value and move to front (MRU)
            if (existing != null) {
                existing.value = value;
                moveToHead(existing);
                return;
            }

            // Case 2: New key -> create node, add to map and front of list
            Node<K, V> newNode = new Node<>(key, value);
            map.put(key, newNode);
            addAfterHead(newNode);

            // Case 3: If over capacity, evict the Least Recently Used node (right before tail)
            if (map.size() > capacity) {
                Node<K, V> lruNode = tail.prev;
                removeNode(lruNode);
                map.remove(lruNode.key);
            }
        } finally {
            lock.unlock();
        }
    }

    // --- O(1) Doubly Linked List Helper Methods (Called only while holding lock) ---

    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addAfterHead(node);
    }

    // Detaches a node from its current position in O(1)
    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    // Inserts a node right after the dummy head (Most Recently Used position) in O(1)
    private void addAfterHead(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }
}

```

---

### Approach 2: High-Concurrency Segmented (Sharded) LRU Cache (The SDE-2 Winner)

#### Why the Interviewer Pushes to Approach 2

> **Interviewer:** *"In Approach 1, every `get()` and `put()` acquires the same `lock`. If 32 threads call `get()` on 32 different keys at the same time, 31 threads are blocked waiting! How do we achieve high concurrency?"*

#### The Core Insight: Lock Striping / Sharding (How Production Caches Do It)

Instead of **1 giant LRU cache** with 1 global lock, we split the cache into **$S$ independent `ThreadSafeLRUCache` segments** (e.g., $S = 16$ shards), each with capacity $\lceil \text{totalCapacity} / S \rceil$ and its **own independent lock**!

```text
Incoming Request for key "user_42"
       |
       v
hash("user_42") % 16  --->  Routes to Segment #5 (Locks ONLY Segment #5!)
                            Meanwhile, 15 other threads can read/write Segments #0..#4, #6..#15 in parallel!

```

* **Zero Code Duplication:** We literally reuse the `ThreadSafeLRUCache<K, V>` class we just wrote in Stage 1 as our `Segment`!
* **16x Higher Throughput:** Threads accessing keys in different segments never block each other on `get()` or `put()`.

#### How to Narrate This in the Interview

> *"To eliminate the single global lock bottleneck on `get()` and `put()`, we can use **Lock Striping (Segmented LRU)**—the exact same technique used in Java 7's `ConcurrentHashMap` and Google's Guava Cache.*
> *Instead of one global linked list and one lock, we partition the cache into $S$ independent `ThreadSafeLRUCache` shards (say, 16 shards). Each shard has its own `HashMap`, its own Doubly Linked List, and its own `ReentrantLock`, and holds `totalCapacity / 16` items.*
> *When `get(key)` or `put(key, value)` is called, we hash the key (`Math.floorMod(key.hashCode(), numSegments)`) and route the call to that specific shard. Now up to 16 threads can mutate LRU pointers in parallel with zero lock contention."*

#### Stage 2 Java Code (High-Concurrency Sharded Wrapper — Only 25 Lines!)

```java
public class HighConcurrencyLRUCache<K, V> {

    private final int numSegments;
    private final ThreadSafeLRUCache<K, V>[] segments;

    @SuppressWarnings("unchecked")
    public HighConcurrencyLRUCache(int totalCapacity, int concurrencyLevel) {
        if (totalCapacity <= 0 || concurrencyLevel <= 0) {
            throw new IllegalArgumentException("Capacity and concurrencyLevel must be > 0");
        }
        // Cap numSegments at totalCapacity so no segment gets a capacity of 0
        this.numSegments = Math.min(totalCapacity, concurrencyLevel);
        this.segments = new ThreadSafeLRUCache[this.numSegments];

        // Distribute capacity evenly across segments (rounding up so total >= totalCapacity)
        int baseCap = totalCapacity / this.numSegments;
        int remainder = totalCapacity % this.numSegments;

        for (int i = 0; i < this.numSegments; i++) {
            int segmentCapacity = baseCap + (i < remainder ? 1 : 0);
            this.segments[i] = new ThreadSafeLRUCache<>(segmentCapacity);
        }
    }

    // Route each key deterministically to one of the N segments in O(1) time
    private ThreadSafeLRUCache<K, V> getSegment(K key) {
        if (key == null) throw new NullPointerException("Key cannot be null");
        // Spread hash bits (same bit-mixing trick as ConcurrentHashMap) and keep index positive
        int hash = key.hashCode();
        int spreadHash = (hash ^ (hash >>> 16)) & 0x7fffffff;
        return segments[spreadHash % numSegments];
    }

    public V get(K key) {
        // Only locks the specific shard for this key!
        return getSegment(key).get(key);
    }

    public void put(K key, V value) {
        // Only locks the specific shard for this key!
        getSegment(key).put(key, value);
    }
}

```

---

## Part 3: SDE-2 Cheat Sheet (Complexity & Advanced Interviewer Q&A)

### 1. Time & Space Complexity

Let $C$ = total cache capacity, and $S$ = number of segments (`concurrencyLevel`, e.g., 16).

| Operation / Metric | Approach 1 (Single Lock LRU) | Approach 2 (Segmented High-Concurrency LRU) |
| --- | --- | --- |
| **`get(key)` Time** | $\mathcal{O}(1)$ | **$\mathcal{O}(1)$** — 1 hash calculation + $\mathcal{O}(1)$ inside shard. |
| **`put(key, value)` Time** | $\mathcal{O}(1)$ | **$\mathcal{O}(1)$** — 1 hash calculation + $\mathcal{O}(1)$ inside shard. |
| **Lock Contention** | **High** — All threads compete for 1 lock. | **Low ($\frac{1}{S}$)** — Up to $S$ threads execute concurrently without blocking. |
| **Space Complexity** | $\mathcal{O}(C)$ | **$\mathcal{O}(C + S)$** — Stores at most $C$ nodes across $S$ segments. |

### 2. Two Classic SDE-2 Follow-Up Questions (And Winning Answers)

| Interviewer Question | Your Winning SDE-2 Answer |
| --- | --- |
| **"What is the trade-off of Segmented LRU vs. a Single Global LRU?"** | *"Segmented LRU trades **strict global LRU eviction** for **massive concurrency**. Instead of evicting the single oldest key across the entire cache, it evicts the oldest key *within that key's shard* (approximated LRU). With a good hash function, entries distribute evenly across shards, so in practice the hit rate is virtually identical to global LRU while throughput scales linearly with CPU cores."* |
| **"What if 1,000 threads all read the EXACT SAME 'hot key' (e.g., a celebrity profile)? Even with sharding, they all hit the same shard!"** | *"Great question. In ultra-high-scale libraries like **Caffeine** (modern Java cache), `get(key)` doesn't immediately lock the linked list! Instead, `get()` reads from a `ConcurrentHashMap` completely **lock-free**, and appends the key to a lightweight ring buffer (`ReadBuffer`). A background task drains the buffer in batches to update the LRU order asynchronously. Mentioning **lock-free reads + buffered async LRU updates** is how production systems solve hot-key contention."* |
