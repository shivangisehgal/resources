# Java Collections Framework — 58 Interview Questions (Sequential)

Roadmap of the framework (interfaces in plain text, common implementations in brackets):

```
Iterable
 └── Collection
      ├── List      [ArrayList, LinkedList, Vector → Stack, CopyOnWriteArrayList]
      ├── Set       [HashSet → LinkedHashSet, EnumSet, CopyOnWriteArraySet]
      │    └── SortedSet → NavigableSet   [TreeSet, ConcurrentSkipListSet]
      └── Queue     [PriorityQueue, ConcurrentLinkedQueue]
           ├── Deque         [ArrayDeque, LinkedList, ConcurrentLinkedDeque]
           └── BlockingQueue [ArrayBlockingQueue, LinkedBlockingQueue, PriorityBlockingQueue, DelayQueue, SynchronousQueue]

Map (NOT a Collection)  [HashMap → LinkedHashMap, Hashtable, IdentityHashMap, WeakHashMap, EnumMap]
 └── SortedMap → NavigableMap   [TreeMap, ConcurrentSkipListMap]
 └── ConcurrentMap              [ConcurrentHashMap]

Java 21+: SequencedCollection / SequencedSet / SequencedMap sit above List, Deque, LinkedHashSet, LinkedHashMap, TreeSet, TreeMap.
```

## Part 1: Hierarchy & fundamentals (Q1–Q5)

### Q1. Which interface is the root of the Collection hierarchy?

A) Collection  B) Iterable  C) Object  D) Map

**Answer: B) Iterable.** `Collection extends Iterable`, which is why every collection works in a for-each loop.

- A) Collection is the root of the *collection* types, but it itself extends Iterable.
- C) Object is the root of all classes, not an interface in the hierarchy.
- D) Map is a separate hierarchy; it does not extend Collection or Iterable.

### Q2. Which of these does NOT extend the Collection interface?

A) List  B) Set  C) Queue  D) Map

**Answer: D) Map.** A Map stores key→value pairs, and `add(E e)` makes no sense for a pair, so Map is its own root. You view it as a collection through `keySet()`, `values()` and `entrySet()`.

- A, B, C all directly extend `Collection<E>`.

### Q3. What is the difference between Collection and Collections?

A) They are aliases  B) Collection is an interface; Collections is a utility class of static methods  C) Collections is the interface; Collection is the class  D) Collections is deprecated

**Answer: B.** `Collection` is the root interface for groups of elements. `Collections` is a final class with static helpers: `sort`, `reverse`, `shuffle`, `unmodifiableList`, `synchronizedMap`, `max`, `frequency`, `emptyList`, etc.

- A) They are different types in `java.util`.
- C) The roles are reversed.
- D) Collections is fully supported.

### Q4. What does this print?

```java
List<Integer> list = new ArrayList<>(List.of(10, 20, 30));
list.remove(1);
System.out.println(list);
```

A) \[20, 30\]  B) \[10, 30\]  C) \[10, 20, 30\]  D) Compile error

**Answer: B) \[10, 30\].** `remove(int index)` is picked over `remove(Object o)` because the literal `1` is an `int` and an exact primitive match beats autoboxing in overload resolution. Index 1 holds 20.

- A) That would be `remove(0)`.
- C) That would happen with `remove(Integer.valueOf(1))` — no element equals 1.
- D) Both overloads exist, so it compiles.

Interview trap: to remove the *value* 20, write `list.remove(Integer.valueOf(20))`.

### Q5. Why do collections not accept primitives such as `int`?

A) For speed  B) Generics work only with reference types, so primitives are autoboxed to wrapper objects  C) Primitives are allowed in Java 17+  D) Only arrays may hold objects

**Answer: B.** Type parameters are erased to `Object`, and a primitive is not an Object. `list.add(5)` compiles because the compiler autoboxes to `Integer.valueOf(5)`.

- A) Boxing actually costs speed and memory.
- C) No Java release allows `List<int>` (Project Valhalla is still in progress).
- D) Arrays can hold primitives (`int[]`) or objects; this is unrelated.

## Part 2: List — ArrayList, LinkedList, Vector, Stack (Q6–Q13)

### Q6. What data structure backs an ArrayList?

A) Doubly linked list  B) Resizable `Object[]` array  C) Hash table  D) Red-black tree

**Answer: B.** ArrayList wraps an `Object[] elementData`. That gives O(1) `get(i)` by index arithmetic and amortised O(1) `add` at the end.

- A) That is LinkedList.
- C) That is HashMap/HashSet.
- D) That is TreeMap/TreeSet.

### Q7. How does ArrayList grow when full?

A) Doubles in size  B) Grows to about 1.5× (`old + (old >> 1)`) and copies the array  C) Grows by 10 each time  D) It cannot grow; it throws

**Answer: B.** `grow()` computes `oldCapacity + (oldCapacity >> 1)` and calls `Arrays.copyOf`. `new ArrayList<>()` starts with an empty shared array and allocates capacity 10 on the first `add`.

- A) Doubling is Vector's default policy, not ArrayList's.
- C) Fixed increments would make appends O(n) amortised.
- D) Growth is automatic; only fixed-size lists like `Arrays.asList` refuse to grow.

Tip: if you know the size, `new ArrayList<>(n)` or `ensureCapacity(n)` avoids repeated copying.

### Q8. Which operation is O(1) on LinkedList but O(n) on ArrayList?

A) `get(index)`  B) `addFirst(e)` / removing the head  C) `contains(o)`  D) `size()`

**Answer: B.** LinkedList keeps `first` and `last` node pointers, so inserting or removing at either end is pointer rewiring. ArrayList must shift every element one slot right with `System.arraycopy`.

- A) `get(i)` is O(1) on ArrayList and O(n) on LinkedList (it walks from the nearer end) — the reverse.
- C) `contains` is O(n) on both.
- D) `size()` is a stored field on both, so O(1) on both.

Interview nuance: in practice ArrayList usually wins even for middle inserts, because arraycopy is fast and linked nodes scatter across memory (poor cache locality). Use LinkedList mainly as a Deque, and prefer ArrayDeque even then.

### Q9. Which interfaces does LinkedList implement?

A) Only List  B) List and Deque  C) List and RandomAccess  D) Set and Queue

**Answer: B.** LinkedList implements `List`, `Deque` (and therefore `Queue`), `Cloneable` and `Serializable`. That is why it offers `push`, `pop`, `offerFirst`, `pollLast`, `peek`.

- A) It is also a Deque.
- C) `RandomAccess` is a marker for O(1) indexed access; ArrayList has it, LinkedList does not.
- D) It allows duplicates and keeps index order, so it is not a Set.

### Q10. How does Vector differ from ArrayList?

A) Vector is unsynchronized  B) Vector's methods are `synchronized` and it doubles on growth  C) Vector cannot hold nulls  D) Vector is not a List

**Answer: B.** Vector is a legacy (JDK 1.0) class whose every public method is synchronized, which adds locking cost even in single-threaded code.

- A) The reverse: ArrayList is the unsynchronized one.
- C) Vector accepts null elements.
- D) Vector was retrofitted to implement List in JDK 1.2.

Modern choice: ArrayList for single-threaded code; `Collections.synchronizedList` or `CopyOnWriteArrayList` when shared.

### Q11. What does this print?

```java
Stack<Integer> s = new Stack<>();
s.push(1); s.push(2); s.push(3);
s.pop();
System.out.println(s.peek() + " " + s);
```

A) 3 \[1, 2\]  B) 2 \[1, 2\]  C) 1 \[2, 3\]  D) 2 \[2, 1\]

**Answer: B) 2 \[1, 2\].** LIFO: `pop()` removes 3; `peek()` returns the new top, 2, without removing it. `toString` comes from Vector and prints bottom to top.

- A) 3 was already popped.
- C) That would be FIFO (queue) behaviour.
- D) Stack prints in insertion order, not top-first.

Why interviewers ask: Stack extends Vector (synchronized, and exposes `get(i)`/`add(i, e)` that break LIFO). The recommended stack is `Deque<Integer> st = new ArrayDeque<>();` — note `ArrayDeque.toString()` prints top first.

### Q12. Which line throws an exception?

```java
List<String> a = Arrays.asList("x", "y");   // line 1
a.set(0, "z");                               // line 2
a.add("w");                                  // line 3
```

A) Line 1  B) Line 2  C) Line 3 — UnsupportedOperationException  D) None

**Answer: C.** `Arrays.asList` returns a fixed-size list backed by the array: `set` writes through to the array, but `add`/`remove` would change the size, so they throw `UnsupportedOperationException`.

- A) Creating the view is fine.
- B) `set` is allowed and also changes the original array.
- D) Line 3 always throws.

Compare: `List.of(...)` throws on `set` too (fully immutable), and `new ArrayList<>(Arrays.asList(...))` is fully mutable.

### Q13. Write code to remove duplicates from a List while keeping the first-seen order.

```java
List<Integer> in = List.of(3, 1, 3, 2, 1);
List<Integer> out = new ArrayList<>(new LinkedHashSet<>(in));   // [3, 1, 2]
// or with streams:
List<Integer> out2 = in.stream().distinct().toList();           // [3, 1, 2]
```

**Why this is correct:** a LinkedHashSet rejects duplicates via `equals`/`hashCode` and remembers insertion order; `distinct()` keeps the first occurrence of each element and preserves encounter order for ordered streams.

**Why not the alternatives:** a plain `HashSet` loses the order; a `TreeSet` sorts (giving \[1, 2, 3\]); nested loops with `contains` work but are O(n²).

## Part 3: Iteration, fail-fast, ConcurrentModificationException (Q14–Q18)

### Q14. What happens here?

```java
List<String> names = new ArrayList<>(List.of("a", "b", "c"));
for (String n : names) {
    if (n.equals("a")) names.remove(n);
}
```

A) Removes "a" fine  B) ConcurrentModificationException  C) Compile error  D) Infinite loop

**Answer: B.** For-each uses an Iterator. `names.remove` bumps the list's `modCount`; on the next `next()` the iterator sees `modCount != expectedModCount` and throws. This is **fail-fast** behaviour.

- A) Structural changes through the list (not the iterator) during iteration are detected.
- C) The code is legal Java.
- D) The iterator throws rather than loop.

Trap variant: removing the *second-to-last* element ("b") does NOT throw — `hasNext()` checks `cursor != size`, returns false, and the loop exits silently skipping "c". Fail-fast is best-effort, never guaranteed.

### Q15. Which is the correct way to remove elements while iterating?

A) `for (int i = 0; i < list.size(); i++) list.remove(i);`  B) `Iterator<String> it = list.iterator(); while (it.hasNext()) if (cond(it.next())) it.remove();`  C) Call `list.remove` inside for-each  D) It is impossible

**Answer: B.** `Iterator.remove()` removes the last returned element and resyncs `expectedModCount`. Since Java 8 the shortest form is `list.removeIf(s -> cond(s));`.

- A) No exception, but each removal shifts elements left, so the element after every removed one is skipped. (Iterating backwards from `size()-1` fixes this.)
- C) Throws ConcurrentModificationException (Q14).
- D) B and `removeIf` both do it.

### Q16. Fail-fast vs fail-safe iterators — which statement is true?

A) ArrayList's iterator is fail-safe  B) CopyOnWriteArrayList and ConcurrentHashMap iterators do not throw ConcurrentModificationException  C) Fail-safe iterators always see the latest data  D) HashMap's iterator is fail-safe

**Answer: B.** CopyOnWriteArrayList iterates over a snapshot array; ConcurrentHashMap's iterators are *weakly consistent* (they tolerate concurrent changes and may or may not reflect them). Neither throws CME.

- A, D) ArrayList, HashMap, HashSet, LinkedList and TreeMap iterators are fail-fast.
- C) A snapshot iterator never sees later changes; weakly consistent ones may miss them.

### Q17. What can ListIterator do that Iterator cannot?

A) Remove elements  B) Traverse backwards, get indexes, and `set`/`add` elements  C) Work on any Collection  D) Run in parallel

**Answer: B.** `ListIterator` adds `hasPrevious()`, `previous()`, `nextIndex()`, `previousIndex()`, `set(e)` and `add(e)`.

- A) Plain Iterator also has `remove()`.
- C) ListIterator is only available from a List (`list.listIterator()`).
- D) Parallel traversal is `Spliterator`'s job.

### Q18. Make a custom class usable in a for-each loop.

```java
class Range implements Iterable<Integer> {
    private final int start, end;
    Range(int s, int e) { start = s; end = e; }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            int cur = start;
            public boolean hasNext() { return cur < end; }
            public Integer next() {
                if (!hasNext()) throw new NoSuchElementException();
                return cur++;
            }
        };
    }
}
for (int x : new Range(1, 4)) System.out.print(x);   // 123
```

**Why this is correct:** for-each compiles to `iterator()` / `hasNext()` / `next()` calls, so implementing `Iterable` is the only requirement. Throwing `NoSuchElementException` past the end follows the Iterator contract.

**Why not the alternatives:** implementing `Iterator` directly on Range (instead of `Iterable`) does not work with for-each and allows only one pass; extending `Collection` is unnecessary work.

## Part 4: equals/hashCode and Set — HashSet, LinkedHashSet, TreeSet (Q19–Q25)

### Q19. Which statement about the equals/hashCode contract is correct?

A) Equal hashCodes imply equal objects  B) If `a.equals(b)` then `a.hashCode() == b.hashCode()` must hold  C) Overriding only `equals` is enough for HashSet  D) hashCode must be unique per object

**Answer: B.** Hash collections first use `hashCode` to choose a bucket, then `equals` inside it. If equal objects had different hashes they would land in different buckets and never be compared.

- A) The reverse is not required: different objects may collide (e.g. `"Aa"` and `"BB"` both hash to 2112).
- C) Without overriding `hashCode`, two equal objects get identity-based hashes and both get stored.
- D) Uniqueness is impossible (2³² ints, unlimited objects); only good distribution is desired.

### Q20. What does this print?

```java
class P { String n; P(String n){this.n=n;}
  @Override public boolean equals(Object o){ return o instanceof P p && p.n.equals(n); } }
Set<P> s = new HashSet<>();
s.add(new P("x")); s.add(new P("x"));
System.out.println(s.size());
```

A) 1  B) 2  C) 0  D) Compile error

**Answer: B) 2 (almost always).** `hashCode` was not overridden, so the two objects get different identity hashes, land in different buckets, and `equals` is never called.

- A) You get 1 only after adding `@Override public int hashCode(){ return Objects.hash(n); }`. A Java `record` generates both automatically.
- C) Both adds succeed.
- D) The code compiles (pattern-matching `instanceof` is Java 16+).

### Q21. How does HashSet work internally?

A) It is a sorted array  B) It wraps a HashMap; elements are keys, mapped to a shared dummy `PRESENT` value  C) It uses a linked list only  D) It uses a TreeMap

**Answer: B.** `add(e)` is `map.put(e, PRESENT) == null`, so it returns false for duplicates. All HashSet behaviour (O(1) average, one null allowed, no order) comes from HashMap.

- A) No sorting happens.
- C) Linked lists appear only inside buckets for collisions.
- D) TreeMap backs TreeSet.

### Q22. Iteration order of the three main Sets?

```java
List<String> in = List.of("banana", "apple", "cherry");
new HashSet<>(in); new LinkedHashSet<>(in); new TreeSet<>(in);
```

A) All insertion order  B) HashSet: unspecified; LinkedHashSet: insertion; TreeSet: sorted \[apple, banana, cherry\]  C) All sorted  D) HashSet sorted; TreeSet insertion

**Answer: B.** HashSet orders by bucket (hash), which is unpredictable and can change on resize. LinkedHashSet adds a doubly linked list through entries to remember insertion order. TreeSet is a red-black tree ordered by natural order or a Comparator.

- A, C, D) Each misattributes the order guarantee. Never rely on HashSet order, even if a small test looks sorted.

### Q23. What happens on `treeSet.add(null)`?

A) Stores null first  B) Throws NullPointerException (with natural ordering)  C) Ignored silently  D) Stores null last

**Answer: B.** TreeSet must call `compareTo` on the element, and `null.compareTo(...)` throws NPE. HashSet and LinkedHashSet allow one null.

- A, D) No position exists for null under natural ordering. (A custom Comparator such as `Comparator.nullsFirst(...)` can allow it.)
- C) Collections never silently drop elements; `add` either returns true/false or throws.

### Q24. What does this print?

```java
TreeSet<Integer> t = new TreeSet<>(List.of(10, 20, 30, 40));
System.out.println(t.floor(25) + " " + t.ceiling(25) + " " + t.higher(40) + " " + t.headSet(30));
```

A) 20 30 null \[10, 20\]  B) 30 20 40 \[10, 20, 30\]  C) 20 30 40 \[10, 20\]  D) Throws

**Answer: A.** NavigableSet methods: `floor` = greatest ≤ x (20); `ceiling` = least ≥ x (30); `higher` = least strictly > x (none, so null); `headSet(30)` = elements strictly less than 30.

- B) Swaps floor and ceiling, and `headSet` excludes its bound by default (use `headSet(30, true)` to include it).
- C) `higher(40)` cannot be 40; it is strictly greater.
- D) These methods return null instead of throwing (unlike `first()`/`last()` on an empty set, which throw `NoSuchElementException`).

Also know: `lower`, `tailSet(x)` (inclusive), `subSet(a, b)`, `pollFirst`, `descendingSet`.

### Q25. TreeSet uses a Comparator that compares only by `age`. You add Person("Ann", 30) and Person("Bob", 30). Size?

A) 2  B) 1  C) Throws  D) Depends on equals

**Answer: B) 1.** TreeSet decides duplicates by `compare(a, b) == 0`, not by `equals`. Both compare as 0, so Bob is rejected.

- A) Would need a tie-breaker: `Comparator.comparing(Person::age).thenComparing(Person::name)`.
- C) Rejecting a duplicate just returns false.
- D) TreeSet never calls `equals`, which is why a Comparator should be *consistent with equals*.

## Part 5: Comparable vs Comparator (Q26–Q28)

### Q26. Which statement is correct?

A) Comparable has `compare(a, b)`; Comparator has `compareTo(o)`  B) Comparable (`java.lang`) defines one natural order inside the class via `compareTo`; Comparator (`java.util`) defines any number of external orders via `compare`  C) Both live in `java.util`  D) A class can implement Comparable several times for different orders

**Answer: B.** `String`, `Integer` and `LocalDate` implement Comparable (their natural order). A Comparator is a separate object, so you can sort the same class by name, salary, date, etc., even classes you cannot modify.

- A) The method names are swapped.
- C) Comparable is in `java.lang`.
- D) A class can implement `Comparable<T>` only once, giving exactly one natural order.

### Q27. Sort employees by department ascending, then salary descending.

```java
record Emp(String dept, String name, double salary) {}
emps.sort(Comparator.comparing(Emp::dept)
                    .thenComparing(Emp::salary, Comparator.reverseOrder()));
```

**Why this is correct:** `comparing` builds the primary key, `thenComparing` applies only to ties, and passing `reverseOrder()` reverses just the salary key.

**Why not the alternatives:** `Comparator.comparing(Emp::dept).thenComparing(Emp::salary).reversed()` reverses *both* keys (department becomes descending too). Two separate `sort` calls work only if you sort by salary first and rely on stability — easy to get backwards.

### Q28. What is wrong with this comparator?

```java
Comparator<Integer> c = (a, b) -> a - b;
```

A) Nothing  B) Integer overflow gives wrong signs for large values  C) It sorts descending  D) It does not compile

**Answer: B.** `Integer.MIN_VALUE - 1` overflows to a large positive number, so MIN\_VALUE would sort *after* 1. Use `Integer.compare(a, b)` or `Comparator.naturalOrder()`.

- A) The bug appears with large magnitudes and causes "Comparison method violates its general contract!" errors in TimSort.
- C) `a - b` is ascending (when it does not overflow).
- D) It compiles fine.

Related fact: `Collections.sort`/`List.sort` use TimSort, which is **stable** (equal elements keep their order) and O(n log n); `Arrays.sort` on primitives uses dual-pivot quicksort, which is not stable.

## Part 6: Map — HashMap internals, LinkedHashMap, TreeMap, Hashtable, special maps (Q29–Q40)

### Q29. How does HashMap find the bucket for a key?

A) `key.hashCode() % 16`  B) `hash = h ^ (h >>> 16)`, then `index = (n - 1) & hash`  C) Binary search on keys  D) Sequential scan

**Answer: B.** HashMap holds a `Node<K,V>[] table` (each Node has hash, key, value, next). It spreads the high bits into the low bits (`h ^ (h >>> 16)`) because only the low bits pick the bucket, and because capacity is always a power of two, `(n - 1) & hash` equals `hash % n` but is faster and never negative.

- A) `%` can be negative for negative hashCodes and ignores the high-bit spreading.
- C) Binary search is TreeMap-style ordering, not hashing.
- D) That would be O(n), defeating the point of hashing.

### Q30. What happens when many keys collide into one bucket (Java 8+)?

A) Always a linked list  B) The list converts to a red-black tree once it holds 8 nodes and the table has at least 64 buckets; it reverts to a list at 6  C) The map throws  D) The key is rejected

**Answer: B.** `TREEIFY_THRESHOLD = 8`, `MIN_TREEIFY_CAPACITY = 64` (below 64 the table resizes instead), `UNTREEIFY_THRESHOLD = 6`. Worst-case lookup drops from O(n) to O(log n).

- A) True only before Java 8 or below the thresholds.
- C, D) Collisions are normal and handled.

### Q31. `new HashMap<>()` — when does the first resize happen?

A) After 16 entries  B) When size exceeds 12 (capacity 16 × load factor 0.75); capacity doubles to 32  C) After 10 entries  D) Never

**Answer: B.** threshold = capacity × loadFactor. Resizing doubles the table and redistributes nodes: each node either stays at index `i` or moves to `i + oldCap`, decided by one bit of its hash. The table itself is allocated lazily on the first `put`.

- A) Waiting until 100% full would mean long collision chains.
- C) 10 is ArrayList's default capacity, not a HashMap number.
- D) Resizing is automatic.

Tip: to hold n entries without resizing, use `HashMap.newHashMap(n)` (Java 19+) or `new HashMap<>((int)(n / 0.75f) + 1)`.

### Q32. What does this print?

```java
Map<String, Integer> m = new HashMap<>();
System.out.println(m.put("a", 1));
System.out.println(m.put("a", 2));
System.out.println(m.get("a") + " " + m.size() + " " + m.get("z"));
```

A) null / 1 / 2 1 null  B) 1 / 2 / 2 2 null  C) null / null / 1 1 0  D) true / false / 2 1 null

**Answer: A.** `put` returns the *previous* value (null for a new key). A duplicate key replaces the value; size stays 1. `get` of a missing key returns null.

- B) Keys are unique; there is never a second "a" entry.
- C) The second put replaced 1 with 2, and missing keys return null, not 0.
- D) That describes `Set.add`, which returns boolean.

Note: `get` returning null is ambiguous when null values are allowed; use `containsKey` or `getOrDefault`.

### Q33. Null handling — which is correct?

A) HashMap: one null key, many null values; Hashtable and ConcurrentHashMap: no null keys or values; TreeMap: no null key (natural order), null values OK  B) All maps allow null keys  C) No map allows null values  D) TreeMap allows one null key

**Answer: A.** HashMap special-cases the null key into bucket 0. Hashtable calls `key.hashCode()` directly and rejects null values. ConcurrentHashMap bans nulls because, under concurrency, `get` returning null could mean "absent" or "mapped to null" with no safe way to check. TreeMap must compare keys.

- B, C, D) Each contradicts one of the rules above.

### Q34. What does this print?

```java
List<Integer> key = new ArrayList<>(List.of(1));
Map<List<Integer>, String> m = new HashMap<>();
m.put(key, "v");
key.add(2);
System.out.println(m.get(key) + " " + m.containsKey(List.of(1)));
```

A) v true  B) null false  C) v false  D) Throws

**Answer: B.** The entry sits in the bucket for hash(\[1\]). After mutation, `get(key)` hashes \[1, 2\] and looks in a different bucket. Looking up `List.of(1)` reaches the right bucket but `equals` fails, since the stored key is now \[1, 2\]. The entry is stranded.

- A, C) The lookup by the mutated key cannot reach the original bucket (barring a coincidental collision).
- D) HashMap never detects this; it fails silently.

Lesson: keys should be immutable (String, Integer, records with immutable fields). This is also why String caches its hash.

### Q35. HashMap vs Hashtable — which is true?

A) Hashtable is faster  B) Hashtable is synchronized, legacy, allows no nulls, and uses Enumeration; HashMap is unsynchronized and allows nulls  C) HashMap is thread-safe  D) They are identical

**Answer: B.** Hashtable locks the whole table on every call. For concurrency use ConcurrentHashMap instead.

- A) Locking makes Hashtable slower in single-threaded use.
- C) HashMap is not thread-safe; concurrent puts can lose updates or corrupt the structure.
- D) See the differences above.

### Q36. What does this print?

```java
Map<String,Integer> m = new LinkedHashMap<>(16, 0.75f, true);  // accessOrder = true
m.put("a",1); m.put("b",2); m.put("c",3);
m.get("a");
System.out.println(m.keySet());
```

A) \[a, b, c\]  B) \[b, c, a\]  C) \[c, b, a\]  D) Unspecified

**Answer: B.** With `accessOrder = true`, every `get`/`put` moves that entry to the tail, so the head is the least recently used. This is the basis of an LRU cache (see Q57).

- A) That is the default insertion-order mode (`accessOrder = false`).
- C) Neither mode reverses.
- D) LinkedHashMap order is always defined.

### Q37. What does this print?

```java
TreeMap<Integer,String> t = new TreeMap<>(Map.of(1,"a", 5,"e", 9,"i"));
System.out.println(t.firstKey() + " " + t.floorKey(7) + " " + t.headMap(5) + " " + t.descendingMap());
```

A) 1 5 {1=a} {9=i, 5=e, 1=a}  B) 1 9 {1=a, 5=e} {1=a, 5=e, 9=i}  C) 9 5 {1=a} {9=i, 5=e, 1=a}  D) Throws

**Answer: A.** TreeMap (red-black tree, O(log n)) sorts keys. `floorKey(7)` = greatest key ≤ 7 = 5. `headMap(5)` excludes 5. `descendingMap()` is a reversed view.

- B) `floorKey` never returns a key above its argument, and `headMap` is exclusive.
- C) `firstKey` is the smallest key.
- D) All keys exist; nothing throws.

### Q38. What is the most efficient way to iterate keys and values of a HashMap?

A) `for (K k : map.keySet()) map.get(k)`  B) `for (Map.Entry<K,V> e : map.entrySet())` or `map.forEach((k, v) -> ...)`  C) Convert to an array first  D) Use an index loop

**Answer: B.** entrySet gives key and value together in one pass. You can also call `e.setValue(...)` safely during iteration.

- A) Works, but does a second hash lookup per key.
- C) Extra copying for nothing.
- D) Maps have no index.

### Q39. Which pairing of special map and purpose is correct?

A) IdentityHashMap compares keys with `==` instead of `equals`; WeakHashMap lets an entry be garbage-collected once its key is no longer strongly referenced  B) WeakHashMap is for thread safety  C) IdentityHashMap sorts keys  D) WeakHashMap holds weak references to values

**Answer: A.** IdentityHashMap suits object graph traversal, serialization and proxies (two equal-but-distinct objects stay separate). WeakHashMap suits metadata caches keyed by objects you do not own.

- B) WeakHashMap is not synchronized.
- C) IdentityHashMap uses identity hashes, so order is unspecified.
- D) Only keys are weak; a value that refers back to its own key prevents collection (a classic leak).

### Q40. Why prefer EnumMap/EnumSet for enum keys?

```java
enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }
EnumSet<Day> weekend = EnumSet.of(Day.SAT, Day.SUN);
EnumSet<Day> weekdays = EnumSet.complementOf(weekend);
EnumMap<Day, Integer> hours = new EnumMap<>(Day.class);
```

A) They allow null keys  B) EnumSet is a bit vector and EnumMap an array indexed by `ordinal()`, so they are faster and smaller than hash-based versions and iterate in declaration order  C) They are thread-safe  D) They sort alphabetically

**Answer: B.** No hashing or collisions: `contains` is a bit test. `EnumSet.range(MON, FRI)`, `allOf`, `noneOf` and `complementOf` are handy factory methods.

- A) Null keys throw NullPointerException.
- C) Neither is synchronized.
- D) Order follows the enum declaration, not names.

## Part 7: Queue & Deque — PriorityQueue, ArrayDeque (Q41–Q45)

### Q41. On an empty Queue, which pair throws versus returns a special value?

A) `add/remove/element` throw; `offer/poll/peek` return false/null  B) Both pairs throw  C) Both return null  D) `poll` throws, `remove` returns null

**Answer: A.** Each Queue operation comes in two styles:

| Operation | Throws exception | Returns special value |
| --- | --- | --- |
| Insert | `add(e)` → IllegalStateException if full | `offer(e)` → false |
| Remove head | `remove()` → NoSuchElementException | `poll()` → null |
| Examine head | `element()` → NoSuchElementException | `peek()` → null |

- B, C) Only one style per pair behaves that way.
- D) The roles are reversed.

### Q42. What does this print?

```java
PriorityQueue<Integer> pq = new PriorityQueue<>(List.of(5, 1, 4, 2, 3));
System.out.println(pq.peek());
StringBuilder sb = new StringBuilder();
while (!pq.isEmpty()) sb.append(pq.poll());
System.out.println(sb);
```

A) 5 / 51423  B) 1 / 12345  C) 1 / 51423  D) 5 / 54321

**Answer: B.** PriorityQueue is a binary **min-heap** in an array: the head is always the smallest, and repeated `poll` yields ascending order. `offer`/`poll` are O(log n), `peek` O(1).

- A, D) Default ordering is natural (smallest first), not insertion or largest first.
- C) Polling repeatedly gives sorted output.

Trap: `System.out.println(pq)` or a for-each loop shows *heap array order* (e.g. \[1, 2, 4, 5, 3\]), not sorted order. PriorityQueue also rejects null and is not thread-safe (use PriorityBlockingQueue).

### Q43. Find the k largest elements of an array efficiently.

```java
static List<Integer> topK(int[] nums, int k) {
    PriorityQueue<Integer> heap = new PriorityQueue<>();   // min-heap of size k
    for (int n : nums) {
        heap.offer(n);
        if (heap.size() > k) heap.poll();                 // drop the smallest
    }
    return new ArrayList<>(heap);
}
```

**Why this is correct:** the heap always holds the k largest seen so far, with the smallest of them on top ready to be evicted. Time O(n log k), space O(k).

**Why not the alternatives:** sorting the whole array is O(n log n). A max-heap (`new PriorityQueue<>(Comparator.reverseOrder())`) of all n elements polled k times is O(n + k log n) to build and poll but uses O(n) memory. For k *smallest*, flip it: keep a max-heap of size k.

### Q44. Why is ArrayDeque recommended over Stack and LinkedList?

A) It is synchronized  B) It is a resizable circular array: O(1) amortised at both ends, no per-node objects, better cache locality, and no synchronization overhead  C) It allows nulls  D) It supports indexed access

**Answer: B.** The JDK docs say ArrayDeque "is likely to be faster than Stack when used as a stack, and faster than LinkedList when used as a queue."

- A) It is not synchronized (that is part of why it is fast).
- C) It rejects null, because `poll()` uses null to signal "empty".
- D) It has no `get(i)`.

### Q45. What does this print?

```java
Deque<Integer> d = new ArrayDeque<>();
d.offerLast(1); d.offerLast(2); d.offerFirst(0);
d.push(9);
System.out.println(d.pollLast() + " " + d.pop() + " " + d);
```

A) 2 9 \[0, 1\]  B) 9 2 \[0, 1\]  C) 1 0 \[9, 2\]  D) 2 0 \[9, 1\]

**Answer: A.** After the offers the deque is \[0, 1, 2\]. `push` = `addFirst`, giving \[9, 0, 1, 2\]. `pollLast()` removes 2. `pop` = `removeFirst`, removing 9. What remains is \[0, 1\].

- B) The order of the two removals is swapped.
- C, D) They treat `push` as adding at the tail; for a Deque, the stack top is the head.

Memory aid: a Deque used as a stack uses `push/pop/peek` (head); as a queue it uses `offer/poll/peek` (offer at tail, poll from head).

## Part 8: Collections/Arrays utilities & immutability (Q46–Q50)

### Q46. What does this print?

```java
List<String> src = new ArrayList<>(List.of("a"));
List<String> view = Collections.unmodifiableList(src);
List<String> copy = List.copyOf(src);
src.add("b");
System.out.println(view + " " + copy);
```

A) \[a\] \[a\]  B) \[a, b\] \[a\]  C) \[a, b\] \[a, b\]  D) Throws

**Answer: B.** `unmodifiableList` is a read-only *view*: you cannot change it through `view`, but changes to `src` show through. `List.copyOf` makes an independent immutable copy.

- A) The view is not a snapshot.
- C) The copy was taken before "b" was added.
- D) Only writes through `view` or `copy` throw UnsupportedOperationException; writing to `src` is fine.

Key distinction: *unmodifiable* (view, backing data may still change) vs *immutable* (nobody can change it).

### Q47. Which line throws?

```java
List<String> a = List.of("x", "y");          // 1
boolean b = a.contains(null);                // 2
List<String> c = List.of("x", null);         // 3
```

A) Line 2 only  B) Line 3 only  C) Both lines 2 and 3 throw NullPointerException  D) Neither

**Answer: C.** `List.of`, `Set.of` and `Map.of` (Java 9+) reject null elements, and even reject null *queries* like `contains(null)`. Also: `Set.of("a", "a")` throws IllegalArgumentException for duplicates, and `Set.of`/`Map.of` iteration order is deliberately randomized per JVM run.

- A, B) Each misses one of the two null checks.
- D) Both are null-hostile by design.

Compare: `Collections.unmodifiableList(new ArrayList<>(...))` and `Arrays.asList` allow nulls.

### Q48. With `List<String> s = Collections.synchronizedList(new ArrayList<>());`, what must you do when iterating?

A) Nothing; it is fully thread-safe  B) Wrap the iteration in `synchronized (s) { for (String x : s) ... }`  C) Use `s.parallelStream()`  D) Iteration is forbidden

**Answer: B.** Each single method call is synchronized on the wrapper, but iteration is many calls (`hasNext`/`next`). Another thread can modify the list between them and trigger ConcurrentModificationException. The Javadoc requires manual locking on the list itself.

- A) Compound actions (iterate, check-then-act) are not atomic.
- C) A stream also iterates without the lock.
- D) It is allowed when locked.

Often better: CopyOnWriteArrayList (read-heavy) or a concurrent collection.

### Q49. What does this print?

```java
int[] arr = {3, 1, 2};
System.out.println(Arrays.asList(arr).size());
```

A) 3  B) 1  C) 0  D) Compile error

**Answer: B) 1.** Generics cannot be primitive, so `T` is inferred as `int[]` and you get a `List<int[]>` with one element (the array itself).

- A) You get 3 with `Integer[] arr` or `Arrays.stream(arr).boxed().toList()`.
- C) The list holds the array reference.
- D) It compiles; this silent behaviour is exactly the trap.

### Q50. What does this print?

```java
List<Integer> l = new ArrayList<>(List.of(4, 2, 2, 9, 1));
Collections.sort(l);
System.out.println(l + " " + Collections.binarySearch(l, 9) + " " + Collections.frequency(l, 2)
    + " " + Collections.max(l) + " " + Collections.nCopies(2, "z"));
Collections.reverse(l);
System.out.println(l);
```

A) \[1, 2, 2, 4, 9\] 4 2 9 \[z, z\] / \[9, 4, 2, 2, 1\]  B) \[1, 2, 2, 4, 9\] 5 2 9 \[z, z\] / \[9, 4, 2, 2, 1\]  C) \[4, 2, 2, 9, 1\] 3 2 9 zz / \[1, 9, 2, 2, 4\]  D) Throws

**Answer: A.** After sorting, 9 sits at index 4. `frequency` counts equals-matches (two 2s), `max` uses natural order, `nCopies` returns an immutable list of repeats, and `reverse` reverses in place.

- B) `binarySearch` returns a zero-based index (a missing key returns `-(insertionPoint) - 1`).
- C) Ignores that `sort` happens first; `nCopies` returns a List, not a string.
- D) Every call is valid. `binarySearch` on an *unsorted* list does not throw, but its result is undefined.

Other utilities worth knowing: `shuffle`, `swap`, `rotate`, `fill`, `addAll(c, ...)`, `disjoint`, `emptyList`, `singletonList`, `Arrays.fill`, `Arrays.copyOfRange`, `Arrays.equals`, `Arrays.deepToString`.

## Part 9: Concurrent collections (Q51–Q54)

### Q51. How does ConcurrentHashMap (Java 8+) achieve thread safety?

A) One lock on the whole map, like Hashtable  B) CAS to insert into an empty bucket, and `synchronized` on only the first node of a non-empty bucket; reads are lock-free via volatile fields  C) 16 fixed Segments, each with a ReentrantLock  D) It copies the table on every write

**Answer: B.** Locking is per bucket, so threads writing to different buckets never block each other, and `get` takes no lock at all. Resizing is done cooperatively by multiple threads. It also treeifies long bins like HashMap.

- A) That is Hashtable / `Collections.synchronizedMap` — one lock, poor scaling.
- C) That was the Java 7 design (concurrency level 16), replaced in Java 8.
- D) Copy-on-write is CopyOnWriteArrayList's strategy.

Also: `size()` is an estimate under concurrent updates (use `mappingCount()` for a long), and nulls are banned (Q33).

### Q52. Several threads count words in a shared ConcurrentHashMap. Which is correct?

```java
// Option 1
Integer c = map.get(w); map.put(w, c == null ? 1 : c + 1);
// Option 2
map.merge(w, 1, Integer::sum);
```

A) Both are thread-safe  B) Only Option 2 is atomic; Option 1 can lose updates  C) Only Option 1 is safe  D) Neither

**Answer: B.** Each call in Option 1 is thread-safe, but the get-then-put sequence is not: two threads can read 5 and both write 6. `merge`, `compute`, `computeIfAbsent` and `putIfAbsent` perform the whole read-modify-write atomically on ConcurrentHashMap.

- A) Thread-safe methods do not make a sequence of them atomic.
- C) The reverse.
- D) Option 2 is the standard idiom (or `map.computeIfAbsent(w, k -> new LongAdder()).increment()` for heavy contention).

### Q53. What does this print, and when should you use this class?

```java
List<Integer> l = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
for (Integer x : l) {
    if (x == 1) l.add(99);
}
System.out.println(l);
```

A) ConcurrentModificationException  B) \[1, 2, 3, 99\] — the loop iterates a snapshot, so it runs 3 times  C) Infinite loop  D) \[1, 2, 3\]

**Answer: B.** Every write copies the internal array; iterators keep the old array. So modifications during iteration are safe and invisible to the running loop.

- A) Its iterators never throw CME.
- C) The snapshot is fixed at 3 elements, so 99 is never visited.
- D) The write succeeded; it just was not visible to that iterator.

Use it when reads vastly outnumber writes (listener lists, config, routing tables). Writes are O(n) and memory-heavy, so avoid it for write-heavy lists. Its iterator's `remove()` throws UnsupportedOperationException.

### Q54. Producer-consumer: which BlockingQueue method blocks the consumer until an item arrives?

A) `poll()`  B) `take()`  C) `remove()`  D) `peek()`

**Answer: B.** `put(e)` blocks while full, `take()` blocks while empty; `offer(e, timeout, unit)` and `poll(timeout, unit)` wait up to a limit.

```java
BlockingQueue<Integer> q = new ArrayBlockingQueue<>(10);
new Thread(() -> { for (int i = 0; i < 5; i++) q.put(i); }).start();   // wrap put/take in try/catch InterruptedException
new Thread(() -> { while (true) System.out.println(q.take()); }).start();
```

- A) `poll()` returns null immediately when empty.
- C) `remove()` throws NoSuchElementException when empty.
- D) `peek()` returns null and does not remove.

Know the implementations: ArrayBlockingQueue (bounded, array, one lock), LinkedBlockingQueue (optionally bounded, separate put/take locks, higher throughput), PriorityBlockingQueue (unbounded heap), DelayQueue (elements available after a delay), SynchronousQueue (zero capacity: each put waits for a take; used by `Executors.newCachedThreadPool`). Non-blocking alternatives: ConcurrentLinkedQueue/Deque; sorted concurrent: ConcurrentSkipListMap/Set.

## Part 10: Java 8+ APIs and implementation questions (Q55–Q58)

### Q55. What does this print?

```java
Map<String, List<Integer>> m = new HashMap<>();
m.computeIfAbsent("a", k -> new ArrayList<>()).add(1);
m.computeIfAbsent("a", k -> new ArrayList<>()).add(2);
m.putIfAbsent("b", List.of(7));
m.putIfAbsent("b", List.of(8));
Map<String, Integer> cnt = new HashMap<>();
cnt.merge("x", 1, Integer::sum); cnt.merge("x", 1, Integer::sum);
System.out.println(m.get("a") + " " + m.get("b") + " " + cnt.get("x") + " " + cnt.getOrDefault("y", 0));
```

A) \[1, 2\] \[7\] 2 0  B) \[2\] \[8\] 1 null  C) \[1, 2\] \[8\] 2 0  D) \[1\] \[7\] 2 0

**Answer: A.** `computeIfAbsent` creates the list only the first time and returns the existing list afterwards (the "multimap" idiom). `putIfAbsent` keeps the first value. `merge` inserts 1, then applies `Integer::sum` to give 2. `getOrDefault` returns 0 for the missing key.

- B) Treats each call as replacing; none of these methods overwrite an existing value (except `merge` via its function).
- C) The second `putIfAbsent` does nothing because "b" is present.
- D) The second `computeIfAbsent` returned the same list, so 2 was appended.

Trap: `computeIfAbsent` with a function that returns null stores nothing; `merge`/`compute` returning null *removes* the entry.

### Q56. Word frequency and first non-repeating character.

```java
// 1) Word frequency, sorted by word
String text = "to be or not to be";
Map<String, Long> freq = Arrays.stream(text.split(" "))
    .collect(Collectors.groupingBy(w -> w, TreeMap::new, Collectors.counting()));
// {be=2, not=1, or=1, to=2}

// 2) First non-repeating character
static Character firstUnique(String s) {
    Map<Character, Integer> count = new LinkedHashMap<>();
    for (char c : s.toCharArray()) count.merge(c, 1, Integer::sum);
    for (var e : count.entrySet()) if (e.getValue() == 1) return e.getKey();
    return null;
}
// firstUnique("swiss") → 'w'
```

**Why this is correct:** `groupingBy(classifier, mapFactory, downstream)` lets you pick the map type (TreeMap gives sorted keys). In part 2, LinkedHashMap keeps first-seen order, so the first entry with count 1 is the answer, in O(n).

**Why not the alternatives:** a plain HashMap in part 2 loses order and may return 'i' or another unique char; nested loops are O(n²). Plain `groupingBy(w -> w, counting())` works but returns a HashMap with no defined order.

Also know: `Collectors.toMap(k, v, mergeFn)` throws IllegalStateException on duplicate keys unless you pass a merge function; `partitioningBy` splits into true/false lists.

### Q57. Implement an LRU cache with O(1) get and put.

```java
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    LRUCache(int capacity) {
        super(capacity, 0.75f, true);          // accessOrder = true
        this.capacity = capacity;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;              // evict least recently used
    }
}
// cache(2): put(1), put(2), get(1), put(3) → key 2 is evicted; keys = [1, 3]
```

**Why this is correct:** access-order LinkedHashMap moves each accessed entry to the tail (Q36), so the head is always least recently used. `put` calls `removeEldestEntry` after each insertion; returning true evicts the head. Both operations are O(1).

**Why not the alternatives:** a HashMap plus ArrayList of keys makes "move to most recent" O(n). A TreeMap keyed by timestamp is O(log n). If the interviewer forbids LinkedHashMap, build it yourself: a `HashMap<K, Node>` plus a hand-written doubly linked list with dummy head and tail, where `get` unlinks the node and re-adds it at the tail, and `put` evicts `head.next` when over capacity. For thread safety, wrap with `Collections.synchronizedMap` or use a library such as Caffeine.

### Q58. Implement a simplified HashMap (put, get, resize).

```java
class MyHashMap<K, V> {
    private static class Node<K, V> {
        final K key; V value; Node<K, V> next;
        Node(K k, V v, Node<K, V> n) { key = k; value = v; next = n; }
    }
    private Node<K, V>[] table = (Node<K, V>[]) new Node[16];
    private int size;

    private int index(Object key, int len) {
        int h = (key == null) ? 0 : key.hashCode();
        h ^= (h >>> 16);
        return h & (len - 1);                          // len is a power of two
    }

    public V put(K key, V value) {
        int i = index(key, table.length);
        for (Node<K, V> n = table[i]; n != null; n = n.next) {
            if (Objects.equals(n.key, key)) { V old = n.value; n.value = value; return old; }
        }
        table[i] = new Node<>(key, value, table[i]);   // insert at bucket head
        if (++size > table.length * 0.75) resize();
        return null;
    }

    public V get(K key) {
        for (Node<K, V> n = table[index(key, table.length)]; n != null; n = n.next) {
            if (Objects.equals(n.key, key)) return n.value;
        }
        return null;
    }

    private void resize() {
        Node<K, V>[] old = table;
        table = (Node<K, V>[]) new Node[old.length * 2];
        for (Node<K, V> head : old) {
            for (Node<K, V> n = head; n != null; ) {
                Node<K, V> next = n.next;
                int i = index(n.key, table.length);
                n.next = table[i]; table[i] = n;      // rehash into new table
                n = next;
            }
        }
    }
}
```

**Why this is correct:** it reproduces the real design: hash spreading, power-of-two masking, separate chaining for collisions, `equals` to find the key inside a bucket, `put` returning the old value, and doubling at load factor 0.75. `Objects.equals` handles the null key.

**Why not the alternatives:** using `key.hashCode() % length` can produce negative indexes; comparing keys with `==` breaks for equal-but-distinct Strings; skipping resize lets chains grow to O(n). Things to mention but not code: real HashMap stores the hash in each node, appends at the tail, treeifies long chains (Q30), and splits buckets into "stay at i" or "move to i + oldCap" on resize.

## Quick-revision cheat table

One row per collection you should be able to describe in an interview; complexities are average case.

| Collection | Backing structure | Order | Duplicates / nulls | Thread-safe | Key costs |
| --- | --- | --- | --- | --- | --- |
| ArrayList | Resizable array (1.5×) | Insertion | Dups yes, nulls yes | No | get O(1), add-end O(1)\*, insert/remove middle O(n) |
| LinkedList | Doubly linked list | Insertion | Dups yes, nulls yes | No | ends O(1), get(i) O(n) |
| Vector / Stack | Array (2×), synchronized | Insertion / LIFO | Dups yes, nulls yes | Yes (legacy) | Like ArrayList + lock cost |
| HashSet | HashMap keys | None | No dups, one null | No | add/contains O(1) |
| LinkedHashSet | HashMap + linked list | Insertion | No dups, one null | No | O(1) |
| TreeSet | Red-black tree (TreeMap) | Sorted | No dups, no null | No | O(log n) |
| EnumSet | Bit vector | Enum declaration | No dups, no null | No | O(1) |
| HashMap | Bucket array + list/tree | None | Unique keys; 1 null key, null values | No | get/put O(1) |
| LinkedHashMap | HashMap + linked list | Insertion or access | Same as HashMap | No | O(1); LRU via accessOrder |
| TreeMap | Red-black tree | Sorted keys | No null key | No | O(log n) |
| Hashtable | Bucket array, synchronized | None | No nulls | Yes (legacy) | O(1) + global lock |
| IdentityHashMap | Open-addressing array | None | Keys compared by == | No | O(1) |
| WeakHashMap | Hash table, weak keys | None | Null allowed | No | Entries vanish after GC |
| EnumMap | Array by ordinal | Enum declaration | No null key | No | O(1) |
| PriorityQueue | Binary min-heap array | Priority (head only) | Dups yes, no null | No | offer/poll O(log n), peek O(1) |
| ArrayDeque | Circular array | Insertion (both ends) | Dups yes, no null | No | ends O(1)\* |
| ConcurrentHashMap | Buckets, CAS + per-bin lock | None | No nulls | Yes | O(1), atomic compute/merge |
| CopyOnWriteArrayList | Array copied on write | Insertion | Dups yes, nulls yes | Yes | read O(1), write O(n) |
| ArrayBlockingQueue / LinkedBlockingQueue | Array / linked nodes | FIFO | No null | Yes | put/take block |
| ConcurrentSkipListMap / Set | Skip list | Sorted | No null | Yes | O(log n) |

\*amortised
