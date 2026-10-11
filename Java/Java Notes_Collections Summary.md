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





- **Pehle mental model:** `interface` batata hai *kya operations milenge*; implementation batati hai *data andar kaise rakha hai*. Neeche `n` = elements ki count hai. `O(1)` ka matlab size badhne par kaam lagbhag same; `O(n)` ka matlab poori collection scan karni pad sakti hai.

- **Tumhari hierarchy mein Java 21 ka chhota correction:**
  - `Collection → SequencedCollection → List / Deque`
  - `Set → SequencedSet → SortedSet → NavigableSet`
  - `Map → SequencedMap → SortedMap → NavigableMap`
  - `Map` **`Collection` ka child nahi** hai.
  - `BlockingQueue` aur `Deque`, dono `Queue` ke branches hain. `BlockingDeque` dono ko combine karta hai.
  - `ConcurrentMap` alag Map branch hai; `ConcurrentSkipListMap` sorted/navigable **aur** concurrent dono hai. [Java 21 API hierarchy](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/package-summary.html)

- **Common method language, jo har interview mein kaam aayegi:**
  - `add(e)` / `offer(e)` = insert. Full capacity par `add` exception de sakta hai; `offer` `false` de sakta hai.
  - `remove()` / `poll()` = head nikaalo. Empty par `remove` exception, `poll` `null`.
  - `element()` / `peek()` = head dekho. Empty par `element` exception, `peek` `null`.
  - `put(e)` / `take()` sirf `BlockingQueue` mein: zarurat ho to **wait** karte hain.
  - `map.get(k)` value dhoondhta hai; `map.put(k,v)` purani value replace karke purani value return karta hai.
  - `computeIfAbsent(k, f)` ka seedha matlab: “key ki value nahi hai to `f` se banao aur store karo.” `ConcurrentHashMap` mein same key ke liye is method ko atomic tarike se use kar sakte ho; `get` ke baad `put` likhna equivalent nahi. [Queue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Queue.html), [BlockingQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/BlockingQueue.html), [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html)

## 1. List: position important, duplicates allowed

- **`ArrayList`**
  - Diagram: `[A][B][C][ ]` → `add(D)` → `[A][B][C][D]`
  - Andar **resizable array**. `get(i)` seedha index par jaata hai: `O(1)`.
  - End par `add(e)` usually `O(1)`; capacity khatam ho to bada array bana kar elements copy hote hain. Beech mein `add(i,e)` / `remove(i)` par elements shift: `O(n)`.
  - Caveat: thread-safe nahi; `subList()` original list ka **view** hai, independent copy nahi. Iterator ka fail-fast behavior bug detect karne ke liye hai, synchronization guarantee nahi. [ArrayList API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html)

- **`LinkedList`**
  - Diagram: `null ← [A] ⇄ [B] ⇄ [C] → null`
  - Har node mein value aur previous/next links. `addFirst`, `addLast`, `removeFirst`, `removeLast`: `O(1)`.
  - `get(i)` ko node tak chalna padta hai: `O(n)`. Index pata hone par beech mein insert/remove karne ke liye pehle wahan tak pahunchna padta hai.
  - `List` **aur** `Deque` dono hai; `null` allow karta hai. Queue/stack ke liye aam taur par `ArrayDeque` behtar choice hota hai. [LinkedList API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedList.html)

- **`Vector`**
  - Diagram: `ArrayList jaisa array + synchronized individual methods`
  - `get`, `add`, `remove` jaise methods synchronized hain; array ki indexing/shifting costs `ArrayList` jaisi.
  - Caveat: `if (!v.contains(x)) v.add(x)` do calls ka combination atomic nahi. Iteration ke waqt external synchronization ki zarurat pad sakti hai. Legacy class; naye code mein use case ke hisaab se `ArrayList` ya concurrent collection choose karo. [Vector API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Vector.html)

- **`Stack`**
  - Diagram: `push(A), push(B) → [A][B] ↑top`; `pop() → B`
  - `Vector` ka subclass. `push` end par add, `pop` end se remove, `peek` top dekhta hai.
  - Caveat: legacy design ki wajah se stack ke saath index wale `Vector` methods bhi expose hote hain. Modern LIFO ke liye `Deque<Integer> stack = new ArrayDeque<>();` aur `push/pop/peek` use karo. [Stack API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Stack.html)

- **`CopyOnWriteArrayList`**
  - Diagram: readers → `old [A][B]`; writer → `copy [A][B][C]` → new array publish
  - Har write (`add`, `set`, `remove`) par naya array copy hota hai: write `O(n)`. Readers purane stable snapshot ko padh sakte hain.
  - Caveat: **reads bahut, writes kam** hon tab useful. Iterator snapshot dekhta hai; banne ke baad ki updates usmein nahi aati, aur iterator se `remove` allowed nahi. Thread-safe hai. [CopyOnWriteArrayList API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CopyOnWriteArrayList.html)

## 2. Set: duplicate elements nahi

- **`HashSet`**
  - Diagram: `element → hashCode → bucket`; bucket mein `equals` se final match
  - Andar `HashMap` use hota hai: set element map ki **key** hoti hai. `add`, `contains`, `remove` average `O(1)`.
  - Caveat: iteration order guaranteed nahi; ek `null` allowed. Element ko set mein daalne ke baad uske `equals/hashCode` mein istemal hone wale fields mutate mat karo—phir `contains`/`remove` fail ho sakte hain. [HashSet API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashSet.html)

- **`LinkedHashSet`**
  - Diagram: `hash buckets` + insertion chain `A ⇄ B ⇄ C`
  - Hash se fast lookup; linked chain se **insertion order** mein iteration.
  - `add(existing)` normally us element ko last par nahi le jaata. Java 21 ke `addFirst/addLast` existing element ko respective end par move kar sakte hain.
  - Caveat: sorted nahi hai; insertion order aur sorted order alag concepts hain. [LinkedHashSet API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedHashSet.html)

- **`EnumSet`**
  - Diagram: enum constants `MON TUE WED THU` → bits `1 0 1 0`
  - Enum value ko bit position se represent karta hai; set operations bahut efficient. `EnumSet.of(...)`, `allOf(...)`, `noneOf(...)`, `complementOf(...)` common factories hain.
  - Caveat: sirf **ek enum type** ke constants; `null` nahi. Iteration enum declaration order mein hoti hai. [EnumSet API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/EnumSet.html)

- **`CopyOnWriteArraySet`**
  - Diagram: `add(C)` → existing array mein `C` scan → absent ho to copied array publish
  - Andar `CopyOnWriteArrayList`; uniqueness check ke liye scan, phir write par copy.
  - Caveat: **chhota set, frequent reads, rare writes**. `HashSet` jaisa average `O(1)` lookup assume mat karo. Snapshot iterator hai. [CopyOnWriteArraySet API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CopyOnWriteArraySet.html)

- **`TreeSet`**
  - Diagram: balanced search tree; `10 ← 20 → 30` (illustrative)
  - Elements comparator/natural order se sorted. `add`, `contains`, `remove`: `O(log n)`.
  - `first/last`; `lower(x)` = strictly smaller, `floor(x)` = smaller/equal, `ceiling(x)` = bigger/equal, `higher(x)` = strictly bigger.
  - Caveat: uniqueness **comparison result `0`** par based hoti hai. Comparator inconsistent with `equals` ho to surprising results. `subSet/headSet/tailSet` views hain. Java 21 mein `addFirst/addLast` throw karte hain, kyunki position comparator decide karta hai. [TreeSet API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html)

- **`ConcurrentSkipListSet`**
  - Diagram: sorted bottom lane `1 → 3 → 5 → 8`; upper shortcut lane `1 ───→ 5`
  - Concurrent sorted set, skip-list based; search/add/remove expected `O(log n)`.
  - Caveat: `null` nahi; weakly consistent iterator concurrent changes ke saath chal sakta hai, fixed snapshot nahi deta. `addFirst/addLast` unsupported. [ConcurrentSkipListSet API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentSkipListSet.html)

## 3. Queue aur Deque: kis end se element niklega?

- **`PriorityQueue`**
  - Diagram: input `7, 2, 5` → heap mein arrange → `peek/poll → 2`
  - Array-backed **heap**. `offer` par element upar move ho sakta hai; `poll` par last element root par aakar neeche move hota hai. Dono `O(log n)`; `peek` `O(1)`.
  - Caveat: iteration **sorted order mein nahi**; sirf head guaranteed smallest (default comparator). `remove(Object)`/`contains` `O(n)`; `null` nahi; thread-safe nahi. [PriorityQueue API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html)

- **`ConcurrentLinkedQueue`**
  - Diagram: `head → [A] → [B] → [C] ← tail`
  - Thread-safe, unbounded, FIFO linked queue. `offer` tail par, `poll` head se; concurrent changes coordinate karne ke liye CAS use hota hai.
  - Caveat: **blocking nahi**: empty `poll()` turant `null` deta hai. `size()` ko nodes count karne pad sakte hain, aur concurrent updates ke beech result badal sakta hai. `null` nahi; iterator weakly consistent. [ConcurrentLinkedQueue API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentLinkedQueue.html)

- **`ArrayDeque`**
  - Diagram: circular array `[C][ ][ ][A][B]`; head/tail index wrap kar sakte hain.
  - Dono ends par `addFirst/addLast`, `pollFirst/pollLast`, `peekFirst/peekLast`; usually `O(1)`. Capacity badhe to array copy hota hai.
  - `push = addFirst`, `pop = removeFirst` (stack); `offer = offerLast`, `poll = pollFirst` (queue).
  - Caveat: `null` nahi; thread-safe nahi. `get(i)` jaisa indexed List operation nahi. [ArrayDeque API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayDeque.html)

- **`LinkedList` as `Deque`**
  - Diagram: `first ⇄ ... ⇄ last`
  - Dono ends `O(1)`; `offerFirst/offerLast`, `pollFirst/pollLast`, `peekFirst/peekLast` isi structure par kaam karte hain.
  - Caveat: `null` allow karta hai, isliye `poll()` ka `null` “empty” aur “stored null” mein confusion de sakta hai. [Deque API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Deque.html)

- **`ConcurrentLinkedDeque`**
  - Diagram: `head ⇄ [A] ⇄ [B] ⇄ tail`; kai threads dono ends par
  - Thread-safe, unbounded linked deque. End operations concurrent coordination se hote hain.
  - Caveat: `null` nahi; `size()` `O(n)` ho sakta hai; iterator weakly consistent. **Blocking** `take()` method ismein nahi hai. [ConcurrentLinkedDeque API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentLinkedDeque.html)

## 4. BlockingQueue: producer/consumer ka wait

- **`ArrayBlockingQueue`**
  - Diagram: fixed ring `[A][B][ ][ ]`; producer `put` → fill; full ho to wait; consumer `take` → free slot
  - Constructor mein **fixed capacity**. Array aur lock/conditions se threads ko coordinate karta hai.
  - Caveat: FIFO; `put` full par wait, `take` empty par wait; `offer` turant `false` de sakta hai. Optional fair lock setting throughput ko affect kar sakti hai. [ArrayBlockingQueue API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ArrayBlockingQueue.html)

- **`LinkedBlockingQueue`**
  - Diagram: `head → [A] → [B] → tail`; producer/consumer wait when needed
  - Linked nodes; capacity optional. Agar capacity specify na karo to bahut badi default bound hoti hai.
  - Caveat: “Unbounded” samajhkar unlimited safe memory assume mat karo; producers consumers se tez rahe to memory pressure badhega. FIFO. [LinkedBlockingQueue API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/LinkedBlockingQueue.html)

- **`PriorityBlockingQueue`**
  - Diagram: priority heap `2, 5, 7`; `take() → 2`
  - Thread-safe priority heap. Empty par consumer ka `take()` wait karta hai.
  - Caveat: **full par producer wait nahi karega**—queue effectively unbounded hai. FIFO nahi; equal-priority elements ka order guaranteed nahi. Iterator sorted nahi. [PriorityBlockingQueue API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/PriorityBlockingQueue.html)

- **`DelayQueue`**
  - Diagram: `task A: 5 sec`, `task B: 1 sec` → `take()` ko B tabhi milega jab B ka delay expire ho.
  - Elements `Delayed` implement karte hain; priority expiry time se banti hai. Head expire na hua ho to `take()` wait.
  - Caveat: `poll()` expired item na ho to `null`, chahe queue mein future items pade hon. `size()` unexpired elements bhi count karta hai. [DelayQueue API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/DelayQueue.html)

- **`SynchronousQueue`**
  - Diagram: `Producer put(A) ⇄ Consumer take() → A`
  - **Storage capacity zero.** Har insert ko kisi consumer ke saath direct handoff chahiye.
  - Caveat: `put` receiver aane tak wait karta hai; `take` producer aane tak. `size()` hamesha `0`; ise normal data-store queue mat samajhna. [SynchronousQueue API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/SynchronousQueue.html)

## 5. Map: key → value

- **`HashMap`**
  - Diagram: `key → hashCode → bucket → equals check → value`
  - `put`: bucket dhoondho; same key mile to value replace; nahi to new entry. `get/remove` bhi hash se bucket par jaate hain. Average `O(1)`.
  - Java 8+ mein bahut collisions wali bucket list se tree ban sakti hai, specific conditions par. Resize hone par entries naye table mein redistribute hoti hain.
  - Caveat: ek `null` key, multiple `null` values; order guaranteed nahi; thread-safe nahi. `get(k)==null` se “key absent” aur “key mapped to null” distinguish nahi hota—`containsKey` dekho. Mutable key ka hash badalna dangerous. [HashMap API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html)

- **`LinkedHashMap`**
  - Diagram: `HashMap buckets` + order chain `A ⇄ B ⇄ C`
  - Hash lookup ke saath predictable iteration order. Default **insertion order**; constructor mein `accessOrder=true` do to successful accesses entry ko recent end ki taraf le jaate hain.
  - `removeEldestEntry` se LRU-style eviction policy bana sakte ho. Java 21: `putFirst/putLast` explicit order badalte hain.
  - Caveat: default mode mein existing key par `put` uska insertion position nahi badalta. Access-order mode mein `get` bhi order badal sakta hai, isliye iteration ke saath surprising modification ho sakti hai. [LinkedHashMap API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedHashMap.html)

- **`Hashtable`**
  - Diagram: `hash buckets + synchronized methods`
  - Legacy thread-safe hash map; individual `get/put/remove` synchronized.
  - Caveat: `null` key/value nahi. Multiple method calls mila kar atomic action nahi banta. Modern concurrent use mein generally `ConcurrentHashMap` choose hota hai. [Hashtable API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Hashtable.html)

- **`IdentityHashMap`**
  - Diagram: `key1 == key2 ? same entry : different entry`
  - Keys compare karne ke liye `equals()` ki jagah **reference identity `==`** use karta hai; hash ke liye identity hash.
  - Caveat: do alag `new String("x")` objects same text ke bawajood alag keys ho sakte hain. Normal business-data map ke liye nahi; object identity track karne ke special cases ke liye. [IdentityHashMap API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/IdentityHashMap.html)

- **`WeakHashMap`**
  - Diagram: `map --weak reference--> key`; bahar strong reference na rahe → GC key collect kare → entry disappear
  - Keys weakly referenced. Cache/metadata jaisi situations mein use ho sakta hai.
  - Caveat: removal ka time deterministic nahi—GC par depend karta hai. Agar **value khud key ko strongly reference** kare, entry expected tarah release nahi hogi. Thread-safe nahi. [WeakHashMap API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/WeakHashMap.html)

- **`EnumMap`**
  - Diagram: enum ordinal `RED=0, GREEN=1, BLUE=2` → value array slots `[v0][v1][v2]`
  - Enum keys ke liye compact, fast map. Iteration enum declaration order mein.
  - Caveat: keys ek hi enum type ki; `null` key nahi, `null` values allowed. [EnumMap API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/EnumMap.html)

- **`TreeMap`**
  - Diagram: balanced search tree, **keys sorted**
  - `put/get/remove`: `O(log n)`. `firstKey/lastKey`, `lowerKey/floorKey/ceilingKey/higherKey`, `subMap/headMap/tailMap` range operations.
  - Caveat: key uniqueness comparator ke `0` result se decide hoti hai. Range maps **backed views** hain: view mein change original ko affect karta hai. Java 21 mein `putFirst/putLast` unsupported, kyunki key order comparator decide karta hai. [TreeMap API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html)

- **`ConcurrentSkipListMap`**
  - Diagram: bottom sorted lane `1 → 3 → 5 → 8`; upper lanes search shortcuts dete hain
  - Concurrent **sorted** map; `get/put/remove` expected `O(log n)`.
  - Caveat: `null` key/value nahi; iterators weakly consistent; `size()` ko entries count karni pad sakti hain. `entrySet` se mile entries snapshots ho sakte hain—`setValue` se original map update assume mat karo. [ConcurrentSkipListMap API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentSkipListMap.html)

- **`ConcurrentHashMap`**
  - Diagram: `key → bucket`; empty bucket mein CAS se entry; occupied bucket update mein us bucket par coordination; `get` generally bina lock
  - Alag buckets par writes saath chal sakti hain. Same bucket par writes coordinate hoti hain. Key ki **value** badalne se bucket nahi badalta; bucket key ke hash se decide hota hai.
  - `putIfAbsent(k,v)`: key absent ho tabhi insert. `replace(k,old,new)`: value `old` ho tabhi replace. `computeIfAbsent(k,f)`: absent value ko atomically calculate/store. Counter use case mein `merge` ya `compute` useful.
  - Caveat: `null` keys/values nahi; `size()`/`containsValue()` concurrent updates ke waqt stable snapshot nahi. Iterator weakly consistent. `remove(oldKey)` + `put(newKey,value)` **ek atomic key rename nahi** hai. [ConcurrentHashMap API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html)

## 6. Java 21 Sequenced interfaces

- **`SequencedCollection`**
  - Diagram: `first → A → B → C ← last`; `reversed()` view: `C → B → A`
  - Common API: `addFirst/addLast`, `getFirst/getLast`, `removeFirst/removeLast`, `reversed()`.
  - Caveat: interface mein method hona ye guarantee **nahi** karta ki har implementation add-at-end support karegi; sorted sets explicit positioning reject karte hain. `reversed()` generally **view** hai, copy nahi. [SequencedCollection API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedCollection.html)

- **`SequencedSet`**
  - Diagram: `ordered + unique`
  - `LinkedHashSet`: insertion order; `TreeSet`: sorted order. Dono ordered hain, lekin order ka reason alag hai.
  - Caveat: `TreeSet.addFirst()` unsupported; `LinkedHashSet.addFirst()` element ko first par move kar sakta hai. [SequencedSet API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedSet.html)

- **`SequencedMap`**
  - Diagram: `first entry → (A,1) → (B,2) ← last entry`
  - `firstEntry/lastEntry`, `pollFirstEntry/pollLastEntry`, `reversed()`, `sequencedKeySet()` jaise methods.
  - Caveat: `LinkedHashMap.putFirst/putLast` supported; `TreeMap` mein keys ka sorted position fixed hai, isliye explicit positioning unsupported. [SequencedMap API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedMap.html)

- **Interview mein implementation choose karne ka quick rule:**
  - Indexed access → `ArrayList`
  - Stack/normal deque → `ArrayDeque`
  - Unique + no order → `HashSet`
  - Unique + insertion order → `LinkedHashSet`
  - Unique + sorted → `TreeSet`
  - Key/value + no order → `HashMap`
  - Key/value + insertion/LRU order → `LinkedHashMap`
  - Key/value + sorted → `TreeMap`
  - Concurrent key/value → `ConcurrentHashMap`
  - Concurrent **sorted** key/value → `ConcurrentSkipListMap`
  - Producer–consumer with capacity → `ArrayBlockingQueue` / bounded `LinkedBlockingQueue`
  - Frequent reads, rare writes → `CopyOnWriteArrayList` / `CopyOnWriteArraySet`

- **Sabse common interview traps:** `PriorityQueue` ka iterator sorted nahi; `HashMap`/`HashSet` ka order guaranteed nahi; `LinkedList` indexed access fast nahi; `ConcurrentLinkedQueue` blocking nahi; `PriorityBlockingQueue` full hone par block nahi karti; `SynchronousQueue` mein storage nahi; `volatile` ya synchronized *individual* operations se multiple calls automatically atomic nahi bante; aur Java 21 ka `reversed()` usually reverse **view** deta hai, copied collection nahi.
