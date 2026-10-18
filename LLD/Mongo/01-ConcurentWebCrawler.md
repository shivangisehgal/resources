## Part 1: Problem Statement, Example & Constraints

### Problem Statement

Given a starting webpage `seedUrl` and an interface `HtmlParser` that provides a method `fetchAndParse(String url)` returning all URLs on that page, implement a web crawler that collects all unique URLs reachable from `seedUrl` that share the **exact same hostname**.

As the interview progresses, you must evolve your solution through four stages:

1. **Stage 1 (Baseline):** Single-threaded crawler visiting each reachable same-host URL exactly once.
2. **Stage 2 (Concurrent):** Speed up crawling by fetching multiple URLs in parallel.
3. **Stage 3 (Bounded & Failure-Aware):** Limit maximum parallel requests so you don't crash the server, and gracefully handle network timeouts or exceptions without crashing the crawler.
4. **Stage 4 (Optimal Non-Blocking Worker Pool):** Eliminate "slow straggler" bottlenecks between levels so worker threads never sit idle.

### Provided Interface

```java
interface HtmlParser {
    // Fetches the webpage at 'url' over the network and returns a list of URLs found on it.
    // Note: This is a slow, blocking network call (takes 100ms - 2000ms per call).
    public List<String> fetchAndParse(String url);
}

```

### Example

**Input:**

* `seedUrl` = `"[http://news.yahoo.com/news/topics/](http://news.yahoo.com/news/topics/)"`
* **Web Graph (Links on each page):**
* `"[http://news.yahoo.com/news/topics/](http://news.yahoo.com/news/topics/)"` $\rightarrow$ `["[http://news.yahoo.com/news](http://news.yahoo.com/news)", "[http://news.yahoo.com/us](http://news.yahoo.com/us)", "[http://news.google.com](http://news.google.com)"]`
* `"[http://news.yahoo.com/news](http://news.yahoo.com/news)"` $\rightarrow$ `["[http://news.yahoo.com/news/topics/](http://news.yahoo.com/news/topics/)"]` *(Cycle! Points back to start)*
* `"[http://news.yahoo.com/us](http://news.yahoo.com/us)"` $\rightarrow$ `[]`



**Output:**

```text
[
  "http://news.yahoo.com/news/topics/",
  "http://news.yahoo.com/news",
  "http://news.yahoo.com/us"
]

```

*(Explanation: `"[http://news.google.com](http://news.google.com)"` is ignored because its host is `news.google.com`, not `news.yahoo.com`. Even though `/news` links back to `/news/topics/`, we only visit `/news/topics/` once.)*

### Constraints & Clarifications (Ask these at the start!)

* **Hostname matching:** Only crawl URLs matching the exact host of `seedUrl` (e.g., `[http://news.yahoo.com](http://news.yahoo.com)`).
* **Cycles:** The web graph contains cycles (Page A links to Page B, Page B links back to Page A). You must not get stuck in an infinite loop.
* **Order:** Return the visited URLs in any order.
* **Bottleneck:** `fetchAndParse(url)` is **I/O-bound** (waiting on network), not CPU-bound.

---

## Part 2: Progressive Build-Up (From Stage 1 to Stage 4)

---

### Stage 1: Single-Threaded Baseline (Brute Force BFS)

#### How to Narrate This in the Interview

> *"To start, I can model the web as a directed graph where each webpage is a node and each hyperlink is a directed edge. Since the graph can have cycles, I'll use a `HashSet` called `seen` to keep track of URLs I've already discovered so I never fetch the same page twice. I'll use a standard Breadth-First Search (BFS) with a `Queue`, starting from `seedUrl`, and filter links by hostname."*

#### Stage 1 Java Code

```java
import java.util.*;

class Solution {

    // Helper method to extract the hostname from a URL
    // Example: "http://news.yahoo.com/news/topics/" -> "news.yahoo.com"
    private String getHost(String url) {
        // Find where "://" ends, and add 3 to jump to the start of the hostname
        int start = url.indexOf("://") + 3;
        // Find the first '/' after the hostname
        int end = url.indexOf('/', start);
        // If there is no trailing '/', the rest of the string is the host
        return end == -1 ? url.substring(start) : url.substring(start, end);
    }

    public List<String> crawl(String seedUrl, HtmlParser htmlParser) {
        String targetHost = getHost(seedUrl);

        // 'seen' prevents infinite loops when pages link back to each other
        Set<String> seen = new HashSet<>();
        // Standard FIFO queue for Breadth-First Search (BFS)
        Queue<String> queue = new LinkedList<>();

        // Initialize BFS with the starting URL
        seen.add(seedUrl);
        queue.offer(seedUrl);

        while (!queue.isEmpty()) {
            String currentUrl = queue.poll();

            // SLOW NETWORK CALL: Blocks the main thread until the page downloads
            List<String> childUrls = htmlParser.fetchAndParse(currentUrl);

            for (String nextUrl : childUrls) {
                // 1. Check if the hostname matches our target
                // 2. seen.add(nextUrl) returns 'true' ONLY if nextUrl was not already in the Set
                if (getHost(nextUrl).equals(targetHost) && seen.add(nextUrl)) {
                    queue.offer(nextUrl);
                }
            }
        }

        return new ArrayList<>(seen);
    }
}

```

#### Why the Interviewer Will Ask to Optimize It

> **Interviewer:** *"This works, but `fetchAndParse(url)` takes 500ms over the network. If there are 1,000 pages, doing them one by one takes 500 seconds. How do we make it concurrent?"*

---

### Stage 2: Concurrent Level-by-Level BFS (`CompletableFuture`)

#### Simple Java Concept Refresher for Stage 2

* **`CompletableFuture.supplyAsync(() -> ...)`:** Starts a task on a background worker thread immediately and gives you back a "receipt" (`CompletableFuture`). Your main thread does **not** wait; it moves to the next line of code right away.
* **`task.join()`:** Cashes in the receipt. If the background worker already finished, `join()` gives you the answer instantly. If the worker is still downloading the page, `join()` pauses your main thread until that worker finishes.

#### How to Narrate This in the Interview

> *"Since `fetchAndParse` spends 99% of its time waiting on network I/O, our single thread is just sitting idle. We can parallelize each level of our BFS using `CompletableFuture`.*
> *In each iteration of my `while` loop, I will do **two separate passes**:*
> *1. **Fan-Out Pass:** Loop through all URLs in the current BFS `level` and fire off `CompletableFuture.supplyAsync()` for all of them. Now all URLs in this level are downloading in parallel in the background.*
> *2. **Fan-In Pass:** Loop through those `CompletableFuture` receipts and call `.join()` to collect the results and build `nextLevel`.*
> *The huge advantage here is **thread safety without locks**: only the background threads do the network call, and only the single main thread updates the `seen` `HashSet`. So we have zero race conditions."*

#### Stage 2 Java Code

```java
import java.util.*;
import java.util.concurrent.*;

class Solution {

    private String getHost(String url) {
        int start = url.indexOf("://") + 3;
        int end = url.indexOf('/', start);
        return end == -1 ? url.substring(start) : url.substring(start, end);
    }

    public List<String> crawl(String seedUrl, HtmlParser htmlParser) {
        String targetHost = getHost(seedUrl);
        Set<String> seen = new HashSet<>();
        
        // Instead of a single Queue, we process URLs wave-by-wave (level-by-level)
        List<String> currentLevel = new ArrayList<>();

        seen.add(seedUrl);
        currentLevel.add(seedUrl);

        while (!currentLevel.isEmpty()) {
            // Holds the "receipts" (CompletableFutures) for all parallel downloads in this level
            List<CompletableFuture<List<String>>> futures = new ArrayList<>();

            // PASS 1 (FAN-OUT): Kick off all network calls for this level in parallel!
            // This loop finishes in microseconds because supplyAsync does NOT block.
            for (String url : currentLevel) {
                futures.add(CompletableFuture.supplyAsync(
                    () -> htmlParser.fetchAndParse(url)
                ));
            }

            List<String> nextLevel = new ArrayList<>();

            // PASS 2 (FAN-IN): Wait for each background task to finish and collect child links.
            // Because only the main thread runs this loop, 'seen' (a regular HashSet) is 100% thread-safe!
            for (CompletableFuture<List<String>> future : futures) {
                // .join() blocks until this specific task is done.
                // While waiting on task 1, tasks 2..N are already finishing in the background!
                List<String> childUrls = future.join();

                for (String nextUrl : childUrls) {
                    if (getHost(nextUrl).equals(targetHost) && seen.add(nextUrl)) {
                        nextLevel.add(nextUrl);
                    }
                }
            }

            // Advance to the next wave of URLs
            currentLevel = nextLevel;
        }

        return new ArrayList<>(seen);
    }
}

```

#### Why the Interviewer Will Push Further

> **Interviewer:** *"Great! Two follow-up questions on this code:*
> 1. *What if a level has 10,000 URLs, or why is `supplyAsync` using the default pool bad for I/O?*
> 2. *What happens if ONE URL times out or throws a 500 error inside `fetchAndParse`?"*
> 
> 

---

### Stage 3: Making It Bounded and Failure-Aware

#### Simple Java Concept Refresher for Stage 3

1. **Why default `supplyAsync` is a trap:** When you don't pass a thread pool to `supplyAsync(task)`, Java uses `ForkJoinPool.commonPool()`, which only has `(CPU Cores - 1)` threads (around 7 threads). Worse, it's shared by the whole JVM! Conversely, if you created a new thread per URL for 10,000 URLs, you would overwhelm the target server and run out of memory.
* **Solution (Bounded):** Create a fixed thread pool (`Executors.newFixedThreadPool(16)`) and pass it as the second argument: `CompletableFuture.supplyAsync(task, executor)`. Now **at most 16 requests** can ever fly over the network at the same time.


2. **What happens on failure:** If `fetchAndParse` throws an exception, `future.join()` throws an unhandled `CompletionException` and **crashes your whole crawler**. And if a server hangs forever, `future.join()` freezes forever!
* **Solution (Failure-Aware):** Chain two built-in `CompletableFuture` methods:
* `.orTimeout(2, TimeUnit.SECONDS)` $\rightarrow$ Automatically fails the task if it takes longer than 2 seconds.
* `.exceptionally(ex -> Collections.emptyList())` $\rightarrow$ Catches any timeout or network exception and returns an empty list `[]` instead of crashing!





#### How to Narrate This in the Interview

> *"Right now, my Stage 2 code has two production vulnerabilities:*
> *First, `supplyAsync` without an executor uses Java's `ForkJoinPool.commonPool()`, which is meant for CPU-bound work and doesn't give us explicit control over concurrency. To make the crawler **bounded**, I will pass a dedicated `Executors.newFixedThreadPool(maxThreads)` into `supplyAsync`. That caps our in-flight network requests to `maxThreads`.*
> *Second, to make it **failure-aware**, if a single URL hangs or throws an exception, `future.join()` would either hang forever or throw a `CompletionException` and kill the entire crawl. I can fix both by chaining `.orTimeout(2, TimeUnit.SECONDS)` and `.exceptionally(ex -> Collections.emptyList())` onto each `CompletableFuture`, and shutting down the thread pool in a `finally` block."*

#### Stage 3 Java Code

```java
import java.util.*;
import java.util.concurrent.*;

class Solution {

    private String getHost(String url) {
        int start = url.indexOf("://") + 3;
        int end = url.indexOf('/', start);
        return end == -1 ? url.substring(start) : url.substring(start, end);
    }

    public List<String> crawl(String seedUrl, HtmlParser htmlParser) {
        String targetHost = getHost(seedUrl);
        Set<String> seen = new HashSet<>();
        List<String> currentLevel = new ArrayList<>();

        seen.add(seedUrl);
        currentLevel.add(seedUrl);

        // BOUNDED CONCURRENCY: At most 16 network calls can run in parallel at any moment.
        // Any extra submitted tasks wait safely inside the pool's internal queue.
        ExecutorService ioPool = Executors.newFixedThreadPool(16);

        try {
            while (!currentLevel.isEmpty()) {
                List<CompletableFuture<List<String>>> futures = new ArrayList<>();

                // PASS 1 (FAN-OUT): Submit tasks to our bounded ioPool with timeout + error handling
                for (String url : currentLevel) {
                    CompletableFuture<List<String>> future = CompletableFuture
                        // Pass 'ioPool' as the 2nd argument so it uses our bounded thread pool!
                        .supplyAsync(() -> htmlParser.fetchAndParse(url), ioPool)
                        // FAILURE-AWARE PART 1: If the server hangs for > 2 seconds, trigger a TimeoutException
                        .orTimeout(2, TimeUnit.SECONDS)
                        // FAILURE-AWARE PART 2: If an exception or timeout happens, log it and return an empty list []
                        // so future.join() doesn't crash the rest of the crawler!
                        .exceptionally(ex -> {
                            System.err.println("Failed to fetch " + url + ": " + ex.getMessage());
                            return Collections.emptyList();
                        });

                    futures.add(future);
                }

                List<String> nextLevel = new ArrayList<>();

                // PASS 2 (FAN-IN): Safe to call .join() now; it will never hang forever or throw!
                for (CompletableFuture<List<String>> future : futures) {
                    for (String nextUrl : future.join()) {
                        if (getHost(nextUrl).equals(targetHost) && seen.add(nextUrl)) {
                            nextLevel.add(nextUrl);
                        }
                    }
                }

                currentLevel = nextLevel;
            }
        } finally {
            // CRITICAL FOR SDE-2: Always clean up thread pools in a 'finally' block to prevent thread leaks!
            ioPool.shutdownNow();
        }

        return new ArrayList<>(seen);
    }
}

```

---

### Stage 4: The Most Optimal Solution (Eliminating the "Straggler" Bottleneck)

#### Why Stage 3 Still Has One Hidden Bottleneck

Imagine Level 1 has **10 URLs**:

* **9 fast URLs** finish in **0.1 seconds** and discover 50 new child links.
* **1 slow "straggler" URL** takes **1.9 seconds**.

In Stage 3, because the main thread must finish joining **all 10 tasks** in `currentLevel` before starting the next `while` loop iteration, **15 of your 16 worker threads sit completely idle for 1.8 seconds**, unable to start crawling those 50 newly discovered child links!

```
Stage 3 (Wave-by-Wave Barrier):
Worker 1 (Slow): [==================== 1.9s ====================]  | Starts Level 2...
Worker 2 (Fast): [== 0.1s ==] (IDLE FOR 1.8 SECONDS WAITING...)    | Starts Level 2...
Worker 3 (Fast): [== 0.1s ==] (IDLE FOR 1.8 SECONDS WAITING...)    | Starts Level 2...
                                                                ^ Barrier!

```

#### How We Fix It in Stage 4 (Continuous Worker Queue)

Instead of waiting for a whole level to finish on the main thread, **each worker thread processes the child URLs itself immediately after downloading a page** and pushes new tasks straight into the `ExecutorService`!

```
Stage 4 (Continuous Task Spawning):
Worker 1 (Slow): [==================== 1.9s ====================]
Worker 2 (Fast): [== 0.1s ==][== Child A ==][== Child B ==][== Child C ==]... (Zero idle time!)
Worker 3 (Fast): [== 0.1s ==][== Child D ==][== Child E ==][== Child F ==]... (Zero idle time!)

```

#### Simple Java Concept Refresher for Stage 4

Now that multiple worker threads are adding to `seen` and submitting new tasks at the same time, we need two thread-safe tools:

1. **`ConcurrentHashMap.newKeySet()`:** A thread-safe `Set`. When two threads call `seen.add(url)` at the exact same nanosecond for the same URL, Java guarantees **only one thread gets `true**` and the other gets `false`. No `synchronized` lock needed!
2. **`AtomicInteger activeTasks` + `CountDownLatch doneSignal`:**
* Since the main thread is no longer running a `while` loop, how does the main thread know when the entire crawl is finished?
* We keep an `AtomicInteger activeTasks` counter that tracks how many URLs are currently queued or being downloaded.
* Every time we submit a new URL, we **increment** `activeTasks` (`+1`).
* Every time a worker finishes a URL, we **decrement** `activeTasks` (`-1`).
* The moment `activeTasks` hits `0`, there are no tasks running and no tasks waiting in the queue—so that last worker triggers `doneSignal.countDown()` to wake up the main thread!



#### How to Narrate This in the Interview

> *"In Stage 3, our level-by-level barrier is super clean, but it suffers from the **straggler problem**: one slow URL in Level $N$ blocks all worker threads from starting Level $N+1$.*
> *To make this fully optimal so worker threads never sit idle, I'll let the workers themselves check child URLs and immediately submit new tasks to the `ExecutorService`.*
> *To do this safely across threads:*
> *1. I'll upgrade `seen` from a `HashSet` to `ConcurrentHashMap.newKeySet()`, where `seen.add()` is atomic.*
> *2. To know when the entire crawl is done, I'll track in-flight tasks using an `AtomicInteger activeTasks` and a `CountDownLatch(1)`. Before submitting a task, I increment `activeTasks`. In the worker's `finally` block, I decrement `activeTasks`. When the very last task finishes and decrements the counter to `0`, it calls `latch.countDown()` to unblock the main thread."*

#### Stage 4 Java Code (The Gold-Standard SDE-2 Solution)

```java
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class Solution {

    private String getHost(String url) {
        int start = url.indexOf("://") + 3;
        int end = url.indexOf('/', start);
        return end == -1 ? url.substring(start) : url.substring(start, end);
    }

    public List<String> crawl(String seedUrl, HtmlParser htmlParser) {
        String targetHost = getHost(seedUrl);

        // 1. Thread-safe Set: Multiple workers will call seen.add() at the same time.
        // ConcurrentHashMap.newKeySet() makes seen.add() atomic and lock-free!
        Set<String> seen = ConcurrentHashMap.newKeySet();
        seen.add(seedUrl);

        // 2. Bounded Thread Pool: Caps parallel network fetches at 16.
        // Also has a built-in thread-safe task queue so we don't need to manage a separate BlockingQueue!
        ExecutorService ioPool = Executors.newFixedThreadPool(16);

        // 3. Synchronization tools so the main thread knows when ALL workers are completely done:
        // 'activeTasks' counts how many tasks are currently queued or running.
        AtomicInteger activeTasks = new AtomicInteger(0);
        // 'doneLatch' allows the main thread to sleep until activeTasks drops back to 0.
        CountDownLatch doneLatch = new CountDownLatch(1);

        try {
            // Kick off the very first task for seedUrl
            submitTask(seedUrl, targetHost, htmlParser, seen, ioPool, activeTasks, doneLatch);

            // Main thread pauses here until the last worker decrements activeTasks to 0
            doneLatch.await();

        } catch (InterruptedException e) {
            // Best practice: Restore the interrupted status if the main thread is interrupted
            Thread.currentThread().interrupt();
        } finally {
            // Always shut down the thread pool when done
            ioPool.shutdownNow();
        }

        return new ArrayList<>(seen);
    }

    private void submitTask(
            String url,
            String targetHost,
            HtmlParser htmlParser,
            Set<String> seen,
            ExecutorService ioPool,
            AtomicInteger activeTasks,
            CountDownLatch doneLatch) {

        // CRITICAL: Increment activeTasks BEFORE submitting to the pool!
        // If you increment inside the lambda, a fast parent task might finish and hit 0
        // before the child task even starts running.
        activeTasks.incrementAndGet();

        ioPool.submit(() -> {
            try {
                // Slow network call happens here on the worker thread
                List<String> childUrls = htmlParser.fetchAndParse(url);

                for (String nextUrl : childUrls) {
                    // seen.add(nextUrl) is atomic! Even if 5 threads discover 'nextUrl' at the
                    // exact same time, only ONE thread gets 'true' and spawns a task.
                    if (getHost(nextUrl).equals(targetHost) && seen.add(nextUrl)) {
                        // Immediately spawn a new task without waiting for other workers!
                        submitTask(nextUrl, targetHost, htmlParser, seen, ioPool, activeTasks, doneLatch);
                    }
                }
            } catch (Exception e) {
                // FAILURE-AWARE: Catch any network exception so this worker doesn't die silently
                System.err.println("Error crawling " + url + ": " + e.getMessage());
            } finally {
                // CRITICAL: Always decrement inside 'finally' so even if fetchAndParse() throws
                // an exception, our counter still goes down and never deadlocks the main thread!
                if (activeTasks.decrementAndGet() == 0) {
                    // We were the very last active task in the entire system! Wake up the main thread.
                    doneLatch.countDown();
                }
            }
        });
    }
}

```

---

## Part 3: SDE-2 Cheat Sheet (Complexity & Interviewer Q&A)

Keep these quick answers in your pocket—interviewers almost always ask 1 or 2 of these at the last 5 minutes of the round:

### 1. Time & Space Complexity

* **Let $V$ = number of reachable URLs (vertices), $E$ = total hyperlinks across all pages (edges), and $T$ = thread pool size (`16`).**
* **Time Complexity:**
* **Single-Threaded (Stage 1):** $\mathcal{O}(V \cdot t_{\text{fetch}} + E)$, where $t_{\text{fetch}}$ is the network latency per page.
* **Concurrent (Stage 4):** $\mathcal{O}\left(\frac{V \cdot t_{\text{fetch}}}{T} + \frac{E}{T}\right)$, bounded below by the longest path (diameter $D$) of the web graph: $\mathcal{O}(D \cdot t_{\text{fetch}})$.


* **Space Complexity:** $\mathcal{O}(V)$ to store unique URLs in the `seen` set and the `ExecutorService` internal task queue.

### 2. Three Classic Interviewer Trap Questions (And How to Answer Them)

| Interviewer Question | Your Winning SDE-2 Answer |
| --- | --- |
| **"Why did you increment `activeTasks` *outside* `ioPool.submit()` instead of inside the worker lambda?"** | *"If Worker A is the only active task (`activeTasks = 1`), and it submits Child B, Worker A might hit its `finally` block and decrement `activeTasks` to `0` *before* the thread pool even starts running Child B's lambda! Incrementing before `submit()` guarantees the count never prematurely hits `0`."* |
| **"Why did you put `activeTasks.decrementAndGet()` inside a `finally` block?"** | *"Because `fetchAndParse` can throw a runtime exception or fail. If we didn't decrement inside `finally`, one failed URL would mean `activeTasks` never reaches `0`, and `doneLatch.await()` on the main thread would hang forever (deadlock)."* |
| **"In Java 21+, would you still use `Executors.newFixedThreadPool(16)`?"** | *"In Java 21+, we could use `Executors.newVirtualThreadPerTaskExecutor()` because virtual threads are super lightweight for blocking I/O. However, to keep the crawler **bounded** so we don't DDOS the target server with 10,000 simultaneous requests, we would pair Virtual Threads with a `Semaphore(16)` around `fetchAndParse()`."* |
