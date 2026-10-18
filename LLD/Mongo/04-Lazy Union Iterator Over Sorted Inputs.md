Here is the complete, unified SDE-2 interview playbook for the **Lazy Union Iterator Over Sorted Inputs** problem, combining the base question and both follow-ups into one structured progression.

---

## Part 1: Problem Statement, Examples & Constraints

### Problem Statement

Design a lazy iterator class `UnionIterator<T>` that takes two ascending iterators (`it1` and `it2`) in its constructor and implements the standard `Iterator<T>` interface (`hasNext()` and `next()`):

* **Lazy Evaluation:** Elements must be read on demand without loading all elements into memory up front ($\mathcal{O}(1)$ auxiliary space).
* **Idempotent `hasNext()`:** Calling `hasNext()` repeatedly without calling `next()` must not advance the iterators or skip elements.
* **Three Progressive Variants:**
1. **Part 1 (Merge Preserving Duplicates):** Emit all elements in ascending order, preserving duplicates and returning the left iterator's value (`it1`) first on ties.
2. **Follow-Up 1 (Deduplicating Union):** Modify the iterator so each unique element is emitted only once (strictly ascending output).
3. **Follow-Up 2 ($K$ Sorted Iterators):** Generalize the deduplicating union to accept a list of $K$ ascending iterators.



### Examples

**Input:**

* `Iterator 1:` `2, 4, 6, 8, 10`
* `Iterator 2:` `1, 2, 3, 5, 8, 13`

**Outputs by Variant:**

* **Part 1 (Preserving Duplicates):** `1, 2, 2, 3, 4, 5, 6, 8, 8, 10, 13`
* **Follow-Up 1 & 2 (Deduplicated Union):** `1, 2, 3, 4, 5, 6, 8, 10, 13`

### 3 Quick Clarifying Questions (Ask Before Coding)

1. *"Should the iterator support generic types like both `Integer` and `String` via `Comparable<T>`?"* (Yes, using `<T Comparable<T extends>>`).
2. *"Can either input iterator be empty or `null`?"* (Yes, handle empty or `null` iterators gracefully).
3. *"What should `next()` do if called when `hasNext()` is `false`?"* (Throw `NoSuchElementException`, per the standard Java `Iterator` contract).

---

## Part 2: Progressive Build-Up (From Base Case to $K$ Iterators)

### Core Concept: The 1-Element Lookahead Buffer (`peek1` & `peek2`)

A naive approach would dump both iterators into a `List` or `TreeSet` inside the constructor, but that uses $\mathcal{O}(N + M)$ memory and fails the lazy evaluation requirement.

Instead, we want a two-pointer merge in $\mathcal{O}(1)$ memory. Because Java's `Iterator` only has `next()` and `hasNext()` (no `peek()` method), we store the current head element of each iterator in two variables: **`peek1`** and **`peek2`**.

* **`hasNext()`** simply checks `return peek1 != null || peek2 != null;`. Because it only inspects the cached variables, calling `hasNext()` 100 times in a row never consumes an element.
* **`next()`** compares `peek1` and `peek2`, returns the smaller one, and pulls the next value from the underlying iterator into `peek1` or `peek2`.

---

### Stage 1: Lazy Merge Preserving Duplicates (Left First on Ties)

#### How to Narrate This in the Interview

> *"To avoid loading both iterators into memory, I'll maintain a 1-element lookahead buffer for each iterator: `peek1` and `peek2`. In the constructor, I prime `peek1` and `peek2` with the first element of each iterator.*
> *When `hasNext()` is called, I just check if either `peek1` or `peek2` is non-null. That runs in $\mathcal{O}(1)$ time and never consumes output on repeated calls.*
> *When `next()` is called, if both `peek1` and `peek2` exist, I compare them. Since we want to preserve duplicates and return the left value first on ties, whenever `peek1.compareTo(peek2) <= 0`, I return `peek1` and advance only `it1`. Otherwise, I return `peek2` and advance only `it2`."*

#### Stage 1 Java Code

```java
import java.util.*;

public class UnionIterator<T extends Comparable<T>> implements Iterator<T> {

    private final Iterator<T> it1;
    private final Iterator<T> it2;

    // 1-element lookahead buffers to simulate peek() in O(1) space
    private T peek1;
    private T peek2;

    public UnionIterator(Iterator<T> it1, Iterator<T> it2) {
        // Safely handle null iterators
        this.it1 = (it1 != null) ? it1 : Collections.emptyIterator();
        this.it2 = (it2 != null) ? it2 : Collections.emptyIterator();

        // Prime both lookahead buffers with the first element (if available)
        this.peek1 = this.it1.hasNext() ? this.it1.next() : null;
        this.peek2 = this.it2.hasNext() ? this.it2.next() : null;
    }

    @Override
    public boolean hasNext() {
        // Pure read check: calling hasNext() repeatedly never advances the iterators
        return peek1 != null || peek2 != null;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements");
        }

        // Pick peek1 (left iterator) if:
        // 1. peek1 is available AND
        // 2. either peek2 is exhausted (null) OR peek1 <= peek2 (left wins on ties!)
        if (peek1 != null && (peek2 == null || peek1.compareTo(peek2) <= 0)) {
            T result = peek1;
            peek1 = it1.hasNext() ? it1.next() : null; // Advance ONLY left iterator
            return result;
        } else {
            T result = peek2;
            peek2 = it2.hasNext() ? it2.next() : null; // Advance ONLY right iterator
            return result;
        }
    }
}

```

---

### Stage 2: Follow-Up 1 — Removing Duplicates (Strictly Sorted Union)

#### How to Narrate This in the Interview

> *"To remove duplicates so every value appears only once in the output, the constructor and `hasNext()` stay completely unchanged. Only `next()` changes.*
> *In `next()`, once we pick the smaller value as `result`, instead of advancing just one iterator by a single step, we run a `while` loop on `it1` to skip all elements equal to `result`, and a `while` loop on `it2` to skip all elements equal to `result`. This handles both cross-iterator duplicates and internal duplicates within the same iterator in $\mathcal{O}(1)$ extra space."*

#### Stage 2 Java Code (Only `next()` Changes)

```java
import java.util.*;

public class DeduplicatingUnionIterator<T extends Comparable<T>> implements Iterator<T> {

    private final Iterator<T> it1;
    private final Iterator<T> it2;
    private T peek1;
    private T peek2;

    public DeduplicatingUnionIterator(Iterator<T> it1, Iterator<T> it2) {
        this.it1 = (it1 != null) ? it1 : Collections.emptyIterator();
        this.it2 = (it2 != null) ? it2 : Collections.emptyIterator();
        this.peek1 = this.it1.hasNext() ? this.it1.next() : null;
        this.peek2 = this.it2.hasNext() ? this.it2.next() : null;
    }

    @Override
    public boolean hasNext() {
        return peek1 != null || peek2 != null;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements");
        }

        // 1. Identify the smaller available element between peek1 and peek2
        T result;
        if (peek1 != null && (peek2 == null || peek1.compareTo(peek2) <= 0)) {
            result = peek1;
        } else {
            result = peek2;
        }

        // 2. Advance it1 past ALL occurrences of 'result'
        while (peek1 != null && peek1.compareTo(result) == 0) {
            peek1 = it1.hasNext() ? it1.next() : null;
        }

        // 3. Advance it2 past ALL occurrences of 'result'
        while (peek2 != null && peek2.compareTo(result) == 0) {
            peek2 = it2.hasNext() ? it2.next() : null;
        }

        return result;
    }
}

```

---

### Stage 3: Follow-Up 2 — Generalizing to $K$ Iterators Using a Min-Heap

#### How to Narrate This in the Interview

> *"When we generalize from $2$ iterators to $K$ iterators, keeping $K$ separate peek variables and scanning them linearly on every `next()` call would take $\mathcal{O}(K)$ time per element.*
> *Instead, I can store a wrapper `PeekNode(peekValue, iterator)` inside a **Min-Heap (`PriorityQueue`)** of size at most $K$.*
> * *In the constructor, I push the first element of each non-empty iterator into the Min-Heap in $\mathcal{O}(K \log K)$ time.*
> * *`hasNext()` simply checks `!minHeap.isEmpty()` in $\mathcal{O}(1)$ time.*
> * *In `next()`, I look at the smallest value `result` at the top of the heap. Then, using a `while` loop, as long as the top of the heap equals `result`, I pop that node, advance its iterator, and push it back into the heap if it still has elements. This deduplicates across all $K$ iterators in $\mathcal{O}(\log K)$ amortized time and $\mathcal{O}(K)$ memory."*
> 
> 

#### Stage 3 Java Code ($K$-Iterator Solution)

```java
import java.util.*;

public class KUnionIterator<T extends Comparable<T>> implements Iterator<T> {

    // Wrapper class pairing each Iterator with its current 1-element lookahead ('peek')
    private static class PeekNode<T extends Comparable<T>> implements Comparable<PeekNode<T>> {
        T peek;
        final Iterator<T> iterator;

        PeekNode(T peek, Iterator<T> iterator) {
            this.peek = peek;
            this.iterator = iterator;
        }

        @Override
        public int compareTo(PeekNode<T> other) {
            // Orders the PriorityQueue (Min-Heap) by the current peek value
            return this.peek.compareTo(other.peek);
        }
    }

    private final PriorityQueue<PeekNode<T>> minHeap;

    public KUnionIterator(List<Iterator<T>> iterators) {
        this.minHeap = new PriorityQueue<>();

        if (iterators != null) {
            // Prime the Min-Heap with the first element from each non-empty iterator
            for (Iterator<T> it : iterators) {
                if (it != null && it.hasNext()) {
                    minHeap.offer(new PeekNode<>(it.next(), it));
                }
            }
        }
    }

    @Override
    public boolean hasNext() {
        // O(1) check, does not modify the heap on repeated calls
        return !minHeap.isEmpty();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements");
        }

        // 1. The smallest element across all K iterators is sitting at the top of the heap
        T result = minHeap.peek().peek;

        // 2. Pop and advance every iterator whose current peek equals 'result' (deduplication!)
        while (!minHeap.isEmpty() && minHeap.peek().peek.compareTo(result) == 0) {
            PeekNode<T> node = minHeap.poll();

            // If this iterator still has more items, pull the next one and re-insert into heap
            if (node.iterator.hasNext()) {
                node.peek = node.iterator.next();
                minHeap.offer(node);
            }
        }

        return result;
    }
}

```

---

## Part 3: Complexity Summary (How to Close the Interview)

Let $N$ be the total number of elements across all input iterators, and $K$ be the number of input iterators.

| Variant | Constructor Time | `hasNext()` Time | `next()` Time (Amortized) | Auxiliary Space |
| --- | --- | --- | --- | --- |
| **Stage 1: 2 Iterators (Keep Duplicates)** | $\mathcal{O}(1)$ | $\mathcal{O}(1)$ | $\mathcal{O}(1)$ | **$\mathcal{O}(1)$** (2 variables: `peek1`, `peek2`) |
| **Stage 2: 2 Iterators (Deduplicated)** | $\mathcal{O}(1)$ | $\mathcal{O}(1)$ | $\mathcal{O}(1)$ amortized across full traversal | **$\mathcal{O}(1)$** (2 variables: `peek1`, `peek2`) |
| **Stage 3: $K$ Iterators (Deduplicated)** | $\mathcal{O}(K \log K)$ | $\mathcal{O}(1)$ | $\mathcal{O}(\log K)$ amortized across full traversal | **$\mathcal{O}(K)$** (Heap holds at most $K$ nodes) |
