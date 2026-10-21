# Concurrency & Multithreading — Complete Notes for LLD Rounds

Oct 21, 2026 · @Gokul Verma

## How to use these notes

These notes replace the \~7.5-hour "Concurrency & Multithreading Complete Crash Course" for LLD rounds. All code is Java, but the ideas (threads, locks, waiting, signalling) apply to any language.

The course goes in this order, and so do these notes:

1. Threads: process vs thread, creating threads (`Thread`, `Runnable`, `Callable`)
2. Thread lifecycle and thread pools
3. Executors and `ExecutorService` (the instructor calls this "very, very important")
4. Synchronization and thread communication (`wait`/`notify`)
5. Locks and their types (`ReentrantLock`, `ReadWriteLock`, etc.)
6. Semaphores
7. Java concurrent collections
8. `Future` and `CompletableFuture`
9. LeetCode concurrency problems, then LLD problems with concurrency

The topics are circular: `Callable` needs `Future`, `Future` needs executors, executors need threads. If something in an early section references a later idea, keep reading; it is explained further down.

Interview pattern the instructor stresses: interviewers ask a definition, then "which one would you choose and why", then a follow-up trap (e.g. "Do you know `Callable`? How is it different from `Runnable`?"). Each section ends with those likely questions.

## 1. Threads: the basics

### What is a thread?

A **thread** is the smallest unit of execution. It lets a program do several tasks concurrently, so it can use multi-core CPUs and run faster.

**Analogy: baking cookies.** Baking is the *process* (the whole activity: get ingredients, mix dough, preheat oven, bake). Each person doing a step is a *thread*. Mom alone can do every step one after another. If you and your sister join (mix dough, preheat oven) while mom fetches ingredients, the job finishes faster.

**Cores vs threads.** A 4-core CPU can run only 4 threads *at the same instant*. It can still handle many more threads by **context switching**: one thread runs a bit, steps aside, another runs.

### Process vs thread

A process is an independent program with its **own memory space**. A thread is a unit of execution *inside* a process and **shares memory** with other threads of that process. Every process has at least one thread (the main thread).

Analogy: threads are family members in one house (shared brain, a look is enough to communicate). Processes are mom's friends in their own houses (you must phone them, explain context, and you won't know if one burns her hand).

| Aspect | Process | Thread |
| --- | --- | --- |
| Memory | Own separate memory space | Shares memory with threads of same process |
| Creation overhead | High (resource-intensive) | Low (lightweight), but not zero |
| Communication | Complex: pipes, sockets, message queues (IPC) | Easy: through shared memory |
| Isolation | High | Low: threads can affect each other |
| Context switch | Expensive | Cheap |
| Fault impact | A crash usually doesn't affect other processes | A crash can bring down the whole process |

Threads are cheap, not free. Creating and destroying them repeatedly still costs time, which is why thread pools exist (section 2).

**When to use which**

- **Processes** when parts must be strongly isolated. Examples: Chrome tabs (one tab crashing doesn't kill the browser, which is also why Chrome eats RAM), Photoshop/video editors, game engines.
- **Threads** when tasks in one app need to share data quickly and stay responsive. Examples: mobile app updating UI while fetching data in background; e-commerce handling cart, inventory and payments for many users; music app buffering audio while UI stays responsive; text editor spell-checking while you type; multiplayer game handling movement, music and network together.

### Creating threads in Java

Common interview question, always followed by "which would you choose and why?"

**Way 1: extend `Thread`**

```java
class MyThread extends Thread {
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Thread " + Thread.currentThread().getId() + " running: " + i);
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }
    }
}

MyThread t1 = new MyThread();
MyThread t2 = new MyThread();
t1.start();  // spawns a new thread, which then calls run()
t2.start();
```

The main thread creates `t1` and `t2`. `start()` spawns each new thread, and each one calls `run()`. Both now run in parallel with main. Output order is **not guaranteed**; it changes run to run. That is normal in multithreaded code.

**Way 2: implement `Runnable` (preferred)**

```java
class MyRunnable implements Runnable {
    public void run() { /* same business logic */ }
}

Thread t1 = new Thread(new MyRunnable());  // pass the Runnable to the Thread constructor
t1.start();
```

Why `Runnable` wins: Java allows only one `extends`. If your class extends `Thread`, it can't extend your business class. `Runnable` is an interface, so you keep inheritance free. The only cost is one extra line.

|  | Extend `Thread` | Implement `Runnable` |
| --- | --- | --- |
| Simplicity | Slightly simpler, one-liner | One extra line |
| Inheritance | Blocks extending any other class | Can still extend another class |
| Use in practice | Demos only | Always preferred |

In production, nobody creates raw threads like this. You submit `Runnable`/`Callable` tasks to an `ExecutorService` (section 3), which manages threads for you. Raw threads are for short interview demos.

**Way 3: `Callable` (returns a result)**

The follow-up trap: "Do you know `Callable`? How is it different from `Runnable`?" `Callable` is like `Runnable` but its `call()` method **returns a value** and **can throw checked exceptions**. It is used with `ExecutorService` and returns a `Future`.

```java
class MyCallable implements Callable<String> {
    private final String name;
    MyCallable(String name) { this.name = name; }
    public String call() throws Exception {  // can throw checked exceptions
        Thread.sleep(500);                     // simulate work
        return "Callable " + name + " done";
    }
}

ExecutorService executor = Executors.newFixedThreadPool(2);
Future<String> f1 = executor.submit(new MyCallable("task1"));
Future<String> f2 = executor.submit(new MyCallable("task2"));
System.out.println(f1.get());  // BLOCKING: main thread waits until task1 finishes
System.out.println(f2.get());
executor.shutdown();
```

A **`Future`** is a placeholder for a result that will be ready later. `future.get()` is a **blocking call**: the calling thread stops there until the result arrives. (Non-blocking alternatives come in the `CompletableFuture` section.)

**Checked vs unchecked exceptions (quick refresher)**

- **Checked**: checked at compile time; you must handle them with `try/catch` or declare `throws`. Examples: `IOException`, `SQLException`, `InterruptedException`.
- **Unchecked**: occur at runtime from logic errors; handling is optional. Examples: `ArithmeticException` (divide by zero), `ArrayIndexOutOfBoundsException`, `NullPointerException`.

|  | `Thread` | `Runnable` | `Callable` |
| --- | --- | --- | --- |
| Method | `run()` | `run()` | `call()` |
| Return type | `void` | `void` | Any type `V` (generic) |
| Checked exceptions | Can't throw | Can't throw (catch inside `run`) | Can throw |
| How to run | `new MyThread().start()` | `new Thread(runnable).start()` | `executor.submit(callable)` |
| Works with | Threads | Threads, executors | Executors, `Future` |

*Can you use `Callable` with a plain `Thread`?* Not directly; `Thread` only accepts `Runnable`. You could wrap a `Callable` inside a `Runnable`, but that is a hack. `Callable` is designed for `ExecutorService`.

### Best practices

- Prefer `Runnable` (or `Callable`) over extending `Thread`.
- Keep synchronization minimal; too much becomes a performance bottleneck.
- Handle `InterruptedException` properly so threads terminate gracefully.
- Avoid **thread starvation** (one thread never gets a turn and holds up the whole job, like one teammate whose code never gets reviewed blocks the release).
- Prefer high-level utilities from `java.util.concurrent` over hand-rolled ones.

### Common interview questions

**`start()` vs `run()`?** `start()` creates a new thread, which then calls `run()`. Calling `run()` directly creates **no new thread**; the current (main) thread just executes it like a normal method.

**Can you call `start()` twice?** No. The second call throws `IllegalThreadStateException`. A thread that has finished can't be restarted.

**What is thread safety and how do you achieve it?** Code is thread-safe if multiple threads can run it simultaneously without corrupting data. Tools: synchronization, immutable objects, concurrent collections, atomic variables, `ThreadLocal` variables (all covered later).

**What if an exception is thrown in `run()`?** An uncaught exception terminates *that* thread only. It does **not** propagate to the parent; the main thread keeps running. Catch it with `try/catch` inside `run()` if you want to handle it.

## 2. sleep vs wait, notify vs notifyAll

The instructor calls this one of the most confused topics in interviews. The core difference: **`sleep()` keeps the lock, `wait()` releases it.**

**Analogy: the locked house.** You unlock the front door, go in, and lock it from inside.

- **`sleep()`**: you lie down on the bed and sleep for 5 hours with the door still locked. Nobody else can get in until you wake up and leave.
- **`wait()`**: you realise you can't sleep yet (you're waiting for your partner to arrive). So you put the key back outside and wait. Mom takes the key, naps for 30 minutes, leaves, puts the key back. When a friend **notifies** you that your partner is here, you take the key again (re-acquire the lock) and go in.

|  | `sleep(ms)` | `wait()` |
| --- | --- | --- |
| Belongs to | `Thread` (static) | `Object` |
| Lock | **Keeps** the lock | **Releases** the lock |
| Wakes up when | Time runs out | Another thread calls `notify()`/`notifyAll()` (or timeout, if given) |
| Where called | Anywhere | Only inside a `synchronized` block/method on that object |
| Use | Pause / simulate work | Coordinate threads (wait for a condition) |

`synchronized` (explained fully in section 5) means only one thread can be inside that method/block at a time, because it must hold the object's lock.

### wait/notify example

```java
class SharedResource {
    synchronized void waitExample() throws InterruptedException {
        System.out.println(Thread.currentThread().getName() + " calling wait");
        wait();                       // releases lock, thread goes to WAITING
        System.out.println(Thread.currentThread().getName() + " resumed after notify");
    }
    synchronized void notifyOne() {
        System.out.println("Calling notify");
        notify();                     // wakes ONE waiting thread
    }
}
```

Run it with `T1` and `T2` both calling `waitExample()`, then a third thread sleeping 2 s and calling `notifyOne()` twice:

1. `T1` enters, prints, calls `wait()`, **releases the lock**.
2. Because the lock is free, `T2` can also enter, print, and `wait()`.
3. The notifier calls `notify()`. One waiting thread (say `T1`) is woken.
4. `T1` does **not** resume instantly. It must first **re-acquire the lock**. If another thread is inside the synchronized method, `T1` waits until it leaves.
5. The second `notify()` wakes `T2` the same way.

With `sleep()` inside a synchronized method instead, `S1` enters and sleeps 2 s **holding the lock**. `S2` stays blocked outside the whole time, and only gets in after `S1` wakes and exits.

Putting `Thread.sleep(...)` in demo code is a standard way to *simulate a task taking time* (a DB call, processing). It doesn't mean the sleep concept is being tested.

### notify() vs notifyAll()

- **`notify()`** wakes **one** arbitrary waiting thread.
- **`notifyAll()`** wakes **all** threads waiting on that object.
- Either way, woken threads move to BLOCKED/RUNNABLE and must **compete for the lock**. Only one gets into the critical section at a time; the rest keep waiting for the lock.

**Interview Q: What happens to the lock when `wait()` is called?** The thread releases the lock on that object, so other threads can acquire it. The waiting thread stays idle until notified (or its timeout expires).

## 3. Thread lifecycle and thread pools

### Thread lifecycle (single thread)

&#91;embedded content: thread lifecycle · 7 states\]

A waiting, sleeping or blocked thread always returns to RUNNABLE first and runs again only when the CPU scheduler picks it. An interrupt can also end a thread from almost any state.

| State | Meaning | How you get here |
| --- | --- | --- |
| NEW | Thread object created, `start()` not called. No system resources allocated yet. | `new Thread(...)` |
| RUNNABLE | Ready to run, waiting for the CPU scheduler to give it time. | `start()`; or returning from waiting/blocked/sleep |
| RUNNING | Actually executing on a CPU core. | Scheduler picks it |
| BLOCKED | Trying to enter a `synchronized` block whose lock another thread holds (the transcript also groups waiting on I/O here). | Lock not available |
| WAITING | Waiting **indefinitely** for another thread's action. No timeout. | `wait()`, `join()`, `LockSupport.park()` |
| TIMED\_WAITING | Waiting with a timeout; returns to RUNNABLE when time expires (or earlier if notified). | `sleep(ms)`, `wait(ms)`, `join(ms)` |
| TERMINATED | `run()` finished (normally or via exception). Object exists but can never be restarted. | End of `run()`, uncaught exception, interrupt |

The Java `Thread.State` enum itself has no separate RUNNING state; "running" is a RUNNABLE thread that currently has the CPU. The course draws them separately to make the flow clear.

Key points:

- After `start()` the thread is **RUNNABLE, not RUNNING**. It runs only when the CPU scheduler picks it.
- Coming back from sleep, wait or blocked always goes to **RUNNABLE first**, then RUNNING when the CPU is free again.
- **Waiter/chef example:** the waiter enters `synchronized(lock)`, calls `lock.wait()` (WAITING, lock released). The chef enters, calls `lock.notify()` and leaves (releases lock). The waiter re-acquires the lock and prints "food is ready".
- A thread can go to TERMINATED from almost any state, e.g. if it is interrupted and the `InterruptedException` ends `run()`.

**Lifecycle hygiene**

- Handle `InterruptedException` so threads can terminate cleanly.
- Avoid **thread leaks**: a thread stuck in WAITING/BLOCKED forever (e.g. a `notify` that never comes) never finishes. Timeouts help, but choose them per business logic: a 3 s timeout on a task that legitimately takes 5 s will kill valid work. Release locks in `finally`.

### Thread pools

A **thread pool** is a managed collection of **reusable** threads. Since a finished thread can't be restarted, without a pool you'd keep creating and destroying threads, which costs time. In production you always use pools, never raw threads.

```java
ExecutorService executor = Executors.newFixedThreadPool(3);  // 3 reusable threads
for (int i = 1; i <= 5; i++) {
    int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId + " on " + Thread.currentThread().getName());
        try { Thread.sleep(1000); } catch (InterruptedException e) {}  // simulate work
    });
}
executor.shutdown();  // graceful: finish queued tasks, accept no new ones
```

Five tasks, three threads: tasks 1–3 start immediately, task 4 takes the first thread that frees up, then task 5. Order varies between runs, which is normal.

*Why not just make 5 threads?* Because with 50 or 5,000 tasks you can't make one thread per task. Too many threads fight for the CPU and spend their time context switching instead of working.

**Benefits:** resource management (prevents overload), performance (reuse avoids creation cost), predictability (you control the thread count), task management (queueing, scheduling, monitoring).

**Pool lifecycle:** pool created with pre-made **core threads** (NEW) → they go straight to RUNNABLE (no `start()` needed; the executor handles it) → a submitted task is picked by an idle thread (RUNNING) → when done, the thread **returns to the pool** (RUNNABLE) for the next task → on shutdown, threads finish and TERMINATE.

### Choosing a pool type

| Pool | Behaviour | Best for |
| --- | --- | --- |
| `newFixedThreadPool(n)` | Exactly `n` threads, alive for the executor's whole life even when idle | **CPU-bound** work: image/video processing, heavy calculation, ML training. Fewer threads = less context switching |
| `newCachedThreadPool()` | Creates threads on demand, reuses idle ones, reclaims threads idle \~60 s | **I/O-bound, short-lived** tasks with spiky load. While a thread waits on I/O, others can be spun up |
| `ThreadPoolExecutor` (custom) | You set core size, max size, queue, keep-alive, rejection policy | Mixed or production workloads needing tuning |

*Why cached for I/O-bound?* I/O tasks spend most of their time waiting. A fixed pool keeps all `n` threads allocated even when they're idle, and can build a backlog. A cached pool adds threads during spikes and frees them when idle.

Caution: a cached pool with heavy CPU tasks can create too many threads, which then just context switch. The instructor's practical advice: **when unsure, use a fixed thread pool** (e.g. sized to your CPU core count). The Pub-Sub LLD later uses a cached pool.

### ThreadPoolExecutor: how a task is placed

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,                                   // corePoolSize
    4,                                   // maximumPoolSize
    60, TimeUnit.SECONDS,                // keep-alive for extra (non-core) threads
    new ArrayBlockingQueue<>(2),         // queue capacity 2
    new ThreadPoolExecutor.AbortPolicy() // reject when everything is full
);
```

When a task arrives:

1. If fewer than `corePoolSize` threads are busy → run it on a core thread.
2. Else if the **queue** has space → queue it.
3. Else if threads < `maximumPoolSize` → **create an extra thread** for it.
4. Else → **reject** it via the rejection policy (e.g. `AbortPolicy` throws).

Worked example with core 2, max 4, queue 2, and 10 tasks (none finishing early): 2 run on core threads, 2 wait in the queue, 2 more threads are created, and the remaining **4 are rejected**.

**Queue size trade-off**

- **Small queue:** extra threads spin up sooner → faster execution, higher CPU use, but **more rejections**.
- **Large queue:** few or no rejections, but more memory and **slower execution** (tasks sit in the queue, extra threads may never spawn).

Give pool threads custom names (via a `ThreadFactory`) for easier debugging.

### Thread starvation and fair scheduling

**Starvation:** a thread can't get regular access to shared resources and can't make progress. Analogy: three siblings and one plate. The eldest takes most, the middle one takes the rest, the youngest gets nothing.

Thread pools help by (a) capping the number of active threads and (b) **fair, FIFO queueing**: each task takes its turn from a queue. In the course demo, raw threads with different OS priorities let high-priority threads complete far more work while others starved. A `ThreadPoolExecutor` with 4 fixed workers and a FIFO queue gave every task roughly equal turns.

### Interview questions

**What happens to a pool thread after it finishes a task?** It does not terminate. It returns to the pool and waits for the next task. This reuse removes the cost of creating and destroying threads.

**How does queue size affect a `ThreadPoolExecutor`?** See the trade-off above: bigger queue = more pending capacity and memory, fewer rejections, slower; smaller queue = more threads sooner, more rejections.

**`shutdown()` vs `shutdownNow()`?** `shutdown()` is graceful: no new tasks accepted, already-submitted tasks complete. `shutdownNow()` attempts to stop everything **immediately** (interrupts running tasks) and **returns the list of tasks that never started**.

**Can a TIMED\_WAITING thread go straight to TERMINATED?** Yes. If interrupted in any state, it gets `InterruptedException` and can end.

**What is starvation and how do pools prevent it?** As above: limit active threads and use fair FIFO scheduling.

## 4. Executors and ExecutorService

Executors abstract away thread management: you **submit tasks**, the executor decides which thread runs them. This separates *what* to run (your business logic) from *how* it runs (creating, starting, reusing threads).

What you get: built-in pooling (e.g. 5 threads serve 10 tasks), queueing and scheduling (delayed, periodic tasks such as cron jobs), lifecycle control (graceful shutdown), and monitoring (`getActiveCount()`, `getQueue().size()` for tuning and debugging).

### The core interfaces and classes

| Name | What it is | Notes |
| --- | --- | --- |
| `Executor` | Most basic interface: `execute(Runnable)` | You'll never use it directly; everything builds on it |
| `ExecutorService` | Extends `Executor` with lifecycle + result handling (`submit`, `invokeAll`, `shutdown`) | **The one you'll use most. Must know.** |
| `ScheduledExecutorService` | Adds scheduling: delays and periodic runs | Good to know |
| `ThreadPoolExecutor` | Concrete implementation of `ExecutorService` | Full control: core/max threads, queue, keep-alive, rejection policy |
| `ScheduledThreadPoolExecutor` | Concrete implementation of `ScheduledExecutorService` | Delayed and repeated tasks with tuning |
| `Executors` (with an **s**) | **Factory class** that builds the above | `newFixedThreadPool`, `newCachedThreadPool`, etc. |

### Factory methods (`Executors.*`)

- `newFixedThreadPool(n)`: fixed `n` threads. The default choice.
- `newCachedThreadPool()`: grows/shrinks on demand; for I/O-bound short tasks.
- `newScheduledThreadPool(n)`: for delayed/periodic tasks.
- `newSingleThreadExecutor()`: exactly one thread, so tasks run **in order**. Useful trick: create an *array* of single-thread executors and route each key to the same one (e.g. `executors[key.hashCode() % n]`). All work for that key then runs sequentially on one thread. The course uses this in the cache LLD to guarantee **read-your-own-writes**.

### ExecutorService methods

| Method | Accepts | Returns | Notes |
| --- | --- | --- | --- |
| `execute(task)` | `Runnable` only | nothing | Fire-and-forget. If the task throws, the exception is lost; no visibility on success/failure |
| `submit(task)` | `Runnable` or `Callable` | `Future` | Use `future.get()` (blocking) for the result or exception; `isDone()`, `isCancelled()` for status |
| `invokeAll(tasks)` | Collection of `Callable` | `List<Future>` | Runs all in parallel, **waits until all finish**. One failing task throws on its own `get()`; others still complete. Overload takes a timeout |
| `shutdown()` | — | — | Graceful: finish queued work, reject new tasks |
| `shutdownNow()` | — | `List<Runnable>` | Interrupts running tasks, returns tasks never started |

```java
ExecutorService executor = Executors.newFixedThreadPool(2);
executor.execute(() -> System.out.println("Runnable via execute"));  // no result
Future<Integer> f = executor.submit(() -> 42);                       // Callable
System.out.println(f.get());   // blocks, prints 42
executor.shutdown();
```

### ScheduledExecutorService methods

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.schedule(task, 2, TimeUnit.SECONDS);                  // run ONCE after 2 s
scheduler.scheduleAtFixedRate(task, 0, 2, TimeUnit.SECONDS);    // start now, then every 2 s
scheduler.scheduleWithFixedDelay(task, 1, 3, TimeUnit.SECONDS); // 3 s gap AFTER each run ends
```

`schedule()` runs once; if it throws, it does not run again.

**Fixed rate vs fixed delay** (common interview question)

- **Fixed rate:** start times are fixed on the clock (12:00, 1:00, 2:00...), no matter how long each run takes. Runs may bunch up if a task overruns.
- **Fixed delay:** wait a fixed gap **after each run finishes** before starting the next.

Example with a 1-hour period and runs taking 2 h, 1 h, 3 h, 0.5 h, 1 h: fixed delay adds a 1 h gap after each run, so the total stretches to \~11.5 h. Fixed rate tries to keep the hourly start schedule, so it finishes much sooner (the course estimates \~5.5 h; the exact figure depends on how overruns stack). The point is the shape: fixed rate keeps the cadence, fixed delay keeps the gap.

### Interview questions

**`execute()` vs `submit()`?** `execute()` takes only a `Runnable`, returns nothing, and loses exceptions. `submit()` takes `Runnable` or `Callable` and returns a `Future` to get results, check status and catch exceptions.

**How does `ThreadPoolExecutor` decide to create a thread or queue a task?** Core threads first → then queue → then extra threads up to max → then reject (section 3).

**What if you never shut down an `ExecutorService`?** Its non-daemon worker threads keep running, so the **JVM never exits**. This causes memory leaks and resource exhaustion. Always call `shutdown()` or `shutdownNow()`.

**How do you handle exceptions from a task in an executor?** Use `submit()`; the exception is wrapped in the `Future` and rethrown (as `ExecutionException`) when you call `get()`, so catch it there.

What to remember after this section: `ExecutorService` methods (`execute`, `submit`, `invokeAll`, `shutdown`/`shutdownNow`), `ScheduledExecutorService` methods (`schedule`, `scheduleAtFixedRate`, `scheduleWithFixedDelay`), and the `Executors` factory methods.

## 5. Thread synchronization

**Synchronization** ensures only one thread at a time accesses a shared resource (a **critical section**). Done right, it prevents **data corruption** and **race conditions** and gives you thread safety.

### Why it's needed: the lost update

`count++` looks atomic but is three steps: read `count`, add 1, write back. Two threads incrementing an unsynchronized counter 5 times each should reach 10, but:

1. Both threads read `count = 2` at the same moment.
2. Both compute 3.
3. Both write 3. One increment is **lost**.

That is a **race condition**: the result depends on timing. With `synchronized`, one thread is inside at a time; the other waits outside, so every increment counts.

### synchronized method

```java
class Counter {
    private int count = 0;
    public synchronized void increment() {   // just add the keyword
        count++;
    }
}
```

**What is the lock on?** (Important.)

- Instance method → lock on **that object instance** (`this`).
- `static synchronized` method → lock on **the class object**.

So if 5 threads call `obj1.increment()`, they go one at a time. But a thread calling `obj2.increment()` is **not** blocked by them; different object, different lock.

### synchronized block (finer-grained, usually better)

A synchronized method locks the whole method, even parts that don't touch shared state (logging, API calls). Threads wait unnecessarily. A synchronized block locks only the critical section:

```java
private final Object lock = new Object();   // any object can be a lock

public void increment() {
    System.out.println("Non-critical work");   // runs concurrently
    synchronized (lock) {                       // only one thread at a time here
        count++;
    }
    System.out.println("More non-critical work");
}
```

Non-critical lines interleave across threads; the critical block runs one thread at a time. You can lock on `this`, a dedicated lock object, or a domain object. In the Pub-Sub (Kafka-like) LLD, the course synchronizes on the **topic object**.

### volatile

`volatile` guarantees **visibility**: every read and write goes to **main memory**, never a thread's local CPU cache. A change by one thread is **immediately** visible to all others.

```java
private volatile boolean running = true;

public void runTask() {
    while (running) { count++; }   // sees the update right away
}
public void stopTask() { running = false; }
```

Without `volatile`, the worker could keep using a cached `true` and never stop. Analogy: an abort button on a rocket launch needs to take effect immediately.

- Also guarantees **ordering**: operations on a volatile variable aren't reordered (true→false→true stays in that order).
- Use for **status flags and control variables**. Also used in the Singleton double-checked locking pattern.
- `volatile` does **not** make `count++` atomic. It gives visibility, not mutual exclusion.

### Atomic variables

For a single shared variable, wrapping `count++` in a lock is overkill. `java.util.concurrent.atomic` gives **lock-free, thread-safe** operations on one variable.

```java
private AtomicInteger counter = new AtomicInteger(0);

public void increment() {               // no synchronized needed
    counter.incrementAndGet();          // atomic read-modify-write
}
```

10 threads × 100 increments → always exactly **1,000**. Common methods: `incrementAndGet()`, `getAndIncrement()`, `decrementAndGet()`, `addAndGet(n)`, `get()`, `set()`, `compareAndSet(expected, new)`. Also `AtomicLong`, `AtomicBoolean`, `AtomicReference`.

### Which tool when

| Tool | Guarantees | Use for |
| --- | --- | --- |
| `synchronized` method | Mutual exclusion + visibility, whole method | Simple cases where the whole method is critical |
| `synchronized` block | Same, only the block | When only part of a method is critical (preferred) |
| `volatile` | Visibility + ordering, **no** atomicity | Flags read by many threads, written by one |
| `AtomicInteger` etc. | Atomic ops on one variable, lock-free | Counters, single-variable updates |

Rule of thumb: excessive locking causes **contention** and slows everything. Synchronize only what must be.

## 6. Thread communication and producer–consumer

Threads coordinate with `wait()`, `notify()` and `notifyAll()`. These work on an object's **monitor** (its intrinsic lock, the same lock `synchronized` takes). "Monitor" and "lock" are used interchangeably here.

- **`wait()`**: release the lock and go to WAITING until notified. Use when a thread must pause until some condition becomes true (e.g. a consumer waiting for an item).
- **`notify()`**: wake **one** thread waiting on that monitor (the scheduler picks which).
- **`notifyAll()`**: wake **all** waiters. They still compete for the lock one at a time. Use when a state change matters to every waiter (e.g. one producer, many consumers; the Pub-Sub LLD uses this to wake all subscribers).
- **All three must be called inside a `synchronized` block/method on the same object**, otherwise Java throws `IllegalMonitorStateException`.

### The standard wait pattern

Always wait in a **`while` loop**, not an `if`. A woken thread re-checks the condition, because it may still be false (another thread got there first, or a spurious wakeup).

```java
private final Object lock = new Object();
private boolean condition = false;

void doWait() throws InterruptedException {
    synchronized (lock) {
        while (!condition) {
            System.out.println(Thread.currentThread().getName() + " waiting");
            lock.wait();                 // releases lock
        }
        System.out.println(Thread.currentThread().getName() + " proceeding");
    }
}

void doNotifyAll() {
    synchronized (lock) {
        condition = true;
        lock.notifyAll();                // wake every waiter
    }                                    // lock released here
}
```

**notifyAll walkthrough (3 waiters W1, W2, W3):** W1 enters, sees condition false, waits and releases the lock. W3 then W2 do the same. Now no one holds the lock. The notifier enters, sets the flag, calls `notifyAll()`, exits (releasing the lock). All three are awake but take turns: W1 grabs the lock, re-checks the `while`, proceeds, exits; then W3; then W2.

**notify walkthrough (2 waiters):** `notify()` wakes only one (say W2), which proceeds. **W1 stays waiting forever** unless someone notifies again. A forgotten waiter wastes resources (a thread leak). When in doubt, use `notifyAll()`.

### Producer–consumer problem

Classic interview problem (asked of new grads; experienced hires get Pub-Sub instead). A producer puts items into a bounded buffer; a consumer takes them out.

- Producer must **wait when the buffer is full**.
- Consumer must **wait when the buffer is empty**.
- After producing, notify consumers ("something to consume"). After consuming, notify producers ("space is free").

```java
class ProducerConsumer {
    private final Queue<Integer> buffer = new LinkedList<>();
    private final int capacity = 5;

    public void produce() throws InterruptedException {
        int value = 0;
        while (true) {
            synchronized (this) {
                while (buffer.size() == capacity) wait();  // full → wait
                buffer.add(value);
                System.out.println("Produced " + value++);
                notifyAll();                               // wake consumer
            }
            Thread.sleep(500);                             // simulate work
        }
    }

    public void consume() throws InterruptedException {
        while (true) {
            synchronized (this) {
                while (buffer.isEmpty()) wait();           // empty → wait
                int v = buffer.poll();
                System.out.println("Consumed " + v);
                notifyAll();                               // wake producer
            }
            Thread.sleep(500);
        }
    }
}

ProducerConsumer pc = new ProducerConsumer();   // both threads share ONE object
new Thread(() -> { try { pc.produce(); } catch (InterruptedException e) {} }).start();
new Thread(() -> { try { pc.consume(); } catch (InterruptedException e) {} }).start();
```

Both threads lock the **same object** (`pc`), so only one is inside a synchronized block at a time. The lock is released between iterations (the `sleep` is outside the block), so producing and consuming interleave: produce 0, consume 0, produce 1, produce 2, consume 1, and so on. If the buffer fills, the producer waits until the consumer frees a slot. With one consumer `notify()` would work, but with several consumers you need `notifyAll()`.

### Interrupting a waiting/sleeping thread

Calling `t.interrupt()` on a thread that is in `wait()`, `sleep()` or `join()` makes it throw **`InterruptedException`** immediately. Control jumps to the `catch` block, skipping the rest of the `try`. Output: "going to sleep" → "interrupted".

## 7. Locks: ReentrantLock and ReadWriteLock

`synchronized` gives basic, automatic locking. The `java.util.concurrent.locks` package gives **explicit locks** with finer control. The course covers the two that matter 95% of the time: **`ReentrantLock`** (your default) and **`ReentrantReadWriteLock`**. (`StampedLock` and others exist but are skipped.)

Extra power explicit locks give you:

- **`tryLock()`**: try to acquire without blocking forever; returns `true`/`false`.
- **Timed attempts**: `tryLock(2, TimeUnit.SECONDS)`.
- **Interruptible** acquisition: `lockInterruptibly()`.
- **Multiple condition variables** (`lock.newCondition()`).
- **Fairness policy**: `new ReentrantLock(true)` gives the longest-waiting thread priority.

The cost: **you** must unlock. Forget it and other threads wait forever (deadlock/starvation).

### ReentrantLock

"Re-entrant" = the thread holding the lock can acquire it **again** (e.g. a locked method calling another locked method) without deadlocking itself. A hold count tracks how many times; it must unlock the same number of times.

```java
private final ReentrantLock lock = new ReentrantLock();
private int counter = 0;

public void increment() {
    lock.lock();
    try {
        counter++;                // critical section
    } finally {
        lock.unlock();            // ALWAYS in finally
    }
}

ExecutorService ex = Executors.newFixedThreadPool(5);
for (int i = 0; i < 5; i++) ex.submit(this::increment);
```

Five threads run, but one at a time through the critical section (any order). Final counter = 5.

**Golden rule: always `unlock()` in a `finally` block**, so the lock is released even if an exception or interrupt hits.

### tryLock example

```java
ReentrantLock lock = new ReentrantLock();

// Thread A: holds the lock for 5 s (sleep does NOT release it)
lock.lock();
try { Thread.sleep(5000); } finally { lock.unlock(); }

// Thread B, started 100 ms later:
if (lock.tryLock(2, TimeUnit.SECONDS)) {
    try { /* work */ } finally { lock.unlock(); }
} else {
    System.out.println("Could not acquire lock, doing something else");
}
```

Thread B gives up after 2 s and takes the `else` branch instead of hanging. Thread A finishes at 5 s and releases normally. That non-blocking option is the main selling point over `synchronized`.

### ReentrantReadWriteLock

For data that is **read often, written rarely**. It has two locks:

- **Read lock (shared):** any number of readers at once, as long as no writer holds the write lock.
- **Write lock (exclusive):** one writer, and **no readers and no other writers** while it's held.

Analogy: many people can read a page at once, but while someone writes on it, nobody else may read or write.

```java
private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
private int value;

public int read() {
    rw.readLock().lock();
    try { return value; } finally { rw.readLock().unlock(); }
}
public void write(int v) {
    rw.writeLock().lock();
    try { value = v; } finally { rw.writeLock().unlock(); }
}
```

**Course walkthrough:** 3 readers enter together and read in any order. A writer arrives and **waits** until all 3 leave. While it writes, 2 new readers arrive and wait. When the writer finishes, both readers enter together. A second writer arriving then waits for them, and so on.

### synchronized vs ReentrantLock (interview question)

|  | `synchronized` | `ReentrantLock` |
| --- | --- | --- |
| Type | Language keyword | Class (`java.util.concurrent.locks`) |
| Acquire/release | **Automatic** (on entering/leaving block) | **Manual** `lock()` / `unlock()` in `finally` |
| Try without blocking | No | Yes, `tryLock()` (the big advantage) |
| Timeout | No | `tryLock(time, unit)` |
| Interruptible wait | No | `lockInterruptibly()` |
| Fairness | No | Optional `new ReentrantLock(true)` |
| Condition variables | One (`wait`/`notify`) | Many (`newCondition()`) |
| Risk | Low | Forgetting `unlock()` |

Why `synchronized` can't "check" a lock: to test whether a block is free you have to enter it, which means acquiring (and blocking on) the lock. `tryLock()` checks without blocking.

Recommendation: for any explicit lock, **`ReentrantLock` is the go-to**. For a simple `synchronized` block, any object can serve as the lock.

## 8. Semaphores

A lock lets **exactly one** thread into a critical section. A **semaphore** lets **up to N** threads in. It holds a count of **permits** (think: tickets).

- **`acquire()`**: take a permit (count − 1). If none are left, **block** until one is released (or the thread is interrupted).
- **`release()`**: return a permit (count + 1).

Analogy: 5 tickets, 8 threads. The first 5 take tickets and go in; 3 wait. When one comes out and hands back its ticket, a waiting thread takes it.

Semaphores are central to the LeetCode concurrency problems later (Dining Philosophers etc.), so this section matters.

### Binary semaphore (mutex) vs counting semaphore

There's really only one `Semaphore` class; the "types" are just the permit count.

- **Binary semaphore** = 1 permit. Acts like a lock (mutual exclusion). Often called a **mutex**.
- **Counting semaphore** = N permits. Limits concurrency to N.

```java
Semaphore semaphore = new Semaphore(3);   // 3 permits; use 1 for a mutex

void accessResource() {
    System.out.println(name + " trying to acquire");   // all threads reach here
    try {
        semaphore.acquire();                  // at most 3 threads past this line
        System.out.println(name + " acquired");
        Thread.sleep(1000);                    // use the resource
    } catch (InterruptedException e) {
    } finally {
        semaphore.release();                   // always release in finally
    }
}
```

With 5 threads and 3 permits: 3 enter (any order), 2 wait. Each `release()` lets one waiting thread in. Lines outside `acquire`/`release` can run in any order across threads.

**Use cases**

- Limiting access to a resource pool: DB connections ("max 5 at a time"), file handles.
- Throttling: e.g. Chrome downloading only 3 of 10 files at once.
- Producer–consumer: `emptySlots = new Semaphore(capacity)`, `filledSlots = new Semaphore(0)`. Producers `acquire` an empty slot and `release` a filled one; consumers do the reverse.
- Controlling concurrency level so the system isn't overwhelmed.

### Semaphore vs lock (interview question)

|  | Lock | Semaphore |
| --- | --- | --- |
| Threads inside | 1 | Up to N permits |
| Ownership | Owned by the thread that locked it; **only that thread** can unlock | No owner: **any thread** can release a permit |
| Typical use | Mutual exclusion | Limiting access to N resources; signalling between threads |

**What if you call `release()` without `acquire()`?** It's legal. It adds a permit, so the count can go **above** its initial value (e.g. start with 2, call `release()`, now 3 threads can enter together). Useful for dynamically adding capacity, and as a **signal**: start a semaphore at 0 so a thread blocks until another thread calls `release()`. That trick appears in the LeetCode problems. Use with care; it can leave state inconsistent.

### Building a barrier with semaphores

A **barrier** means no thread passes a point until **all** threads have reached it (phase 1 by everyone, then phase 2 by everyone).

```java
class Barrier {
    private final int parties;              // original count, used to reset
    private int count;
    private final Semaphore mutex = new Semaphore(1);   // protects count
    private final Semaphore barrier = new Semaphore(0); // 0 permits: everyone blocks

    Barrier(int n) { parties = n; count = n; }

    void await() throws InterruptedException {
        mutex.acquire();
        count--;
        if (count == 0) {                   // last thread to arrive
            barrier.release(parties - 1);   // free the n-1 waiting threads
            count = parties;                // reset for reuse
            mutex.release();
        } else {
            mutex.release();
            barrier.acquire();              // block until the last thread arrives
        }
    }
}
```

With 5 threads, each takes the mutex, decrements `count`, releases the mutex and blocks on `barrier` (0 permits). The 5th thread sees `count == 0`, releases **4 permits** to free the others, resets the count, and continues. All 5 pass together. (Java's built-in equivalents are `CyclicBarrier` and `CountDownLatch`; this exercise shows how they work.)

### Building a read-write lock with semaphores

Goal: many concurrent readers, an exclusive writer.

```java
private int readCount = 0;
private final Semaphore mutex = new Semaphore(1);     // protects readCount
private final Semaphore writeLock = new Semaphore(1); // held by writer OR by the reader group

void readLock() throws InterruptedException {
    mutex.acquire();
    readCount++;
    if (readCount == 1) writeLock.acquire();  // FIRST reader blocks writers
    mutex.release();
}
void readUnlock() throws InterruptedException {
    mutex.acquire();
    readCount--;
    if (readCount == 0) writeLock.release();  // LAST reader lets writers in
    mutex.release();
}
void lockWrite()   throws InterruptedException { writeLock.acquire(); }
void unlockWrite() { writeLock.release(); }
```

The first reader "holds the write lock on behalf of all readers". Later readers just bump the count. A writer blocks until the **last** reader leaves and releases it. Only one writer can hold `writeLock` at a time.

Note: this simple version can **starve writers** if readers keep arriving. That's a fair follow-up to mention.

## 9. Java concurrent collections

Ordinary collections (`ArrayList`, `HashMap`, `LinkedList`, queues) are **not thread-safe**. With concurrent writers they get **corrupted** (two threads write the same slot, one value is overwritten and lost), and modifying while iterating throws **`ConcurrentModificationException`**.

The naive fix is `Collections.synchronizedMap(...)` / `synchronizedList(...)`, which locks the **whole** structure on every read and write. That is correct but a performance bottleneck, and you still have to lock manually while iterating.

**Priority from the course:** `ConcurrentHashMap` is **must-know** and used in nearly every LLD problem. For the rest, knowing the name and the one-line idea is enough.

### ConcurrentHashMap (must know)

**How a normal `HashMap` works:** keys are hashed into **buckets**. Each bucket holds a linked list; when a bucket grows past a threshold (typically 8) it becomes a balanced tree (red-black). `put` hashes the key, finds the bucket, and appends.

**How `ConcurrentHashMap` makes it concurrent:**

- Locks are taken **per bucket** (older Java called these "segments"), not on the whole map. Threads writing to different buckets proceed in parallel; e.g. with 16 buckets, up to 16 writers at once. Threads only wait if they hit the **same** bucket.
- Uses **CAS** (compare-and-swap) for non-blocking updates where possible.
- **Reads take no lock at all.** They read the current state optimistically.

```java
Map<String, Integer> map = new ConcurrentHashMap<>();  // same API as HashMap
map.put("a", 1);
for (Map.Entry<String, Integer> e : map.entrySet()) { ... }  // safe while others write
```

Its iterator does **not** copy the map and never throws `ConcurrentModificationException`; it walks the live structure, so it may or may not see updates made during the iteration.

**Interview one-liner:** "A synchronized `HashMap` locks the entire map, so only one thread works at a time. `ConcurrentHashMap` locks individual buckets (plus CAS, lock-free reads), so many threads can work on different buckets in parallel." Deep CAS details aren't expected.

### CopyOnWriteArrayList

Thread-safe `ArrayList` where **every write creates a fresh copy** of the underlying array.

- Writes: copy the array, modify the copy (one writer at a time), then swap the reference so the copy becomes the current array.
- Reads/iteration: run on the current (immutable) array with **no locking**, any number at once, and **no `ConcurrentModificationException`**.
- Trade-off: fast reads, **slow and memory-heavy writes**. Use for read-heavy, rarely-written lists (e.g. listener lists).

Course demo: list `[1,2,3]`; one thread adds 4, 5, 6 while another iterates. With `ArrayList` → `ConcurrentModificationException`. With `CopyOnWriteArrayList`, the reader sees `[1,2,3]` (its snapshot) and the final list is `[1,2,3,4,5,6]`.

### ConcurrentLinkedQueue / ConcurrentLinkedDeque

Thread-safe, **lock-free** queue (and deque) built on **CAS**. Good for high-throughput multi-producer, multi-consumer setups. No copying (unlike CopyOnWriteArrayList), so writes are fast with no memory overhead. The course's "Design Bounded Blocking Queue" solution uses `ConcurrentLinkedDeque`.

Why a plain linked list breaks: with one node that is both head and tail, one thread removing the head while another appends after the tail can end up attaching to a removed node. Lost inserts and items never removed.

Demo: 1,000 adds and 1,000 removes in parallel. A plain `LinkedList` often ends with a non-zero size (corruption). `ConcurrentLinkedQueue` always ends at **0**. (No `ConcurrentModificationException` there because nobody iterates.)

### Compare-and-swap (CAS), briefly

A hardware-supported atomic instruction behind lock-free ("optimistic") structures and `AtomicInteger`:

1. Read the value and compare it with the **expected** value.
2. If equal, write the new value. If not (another thread changed it), **fail**. The thread then retries or does something else; it never blocks.

Java exposes it as **`compareAndSet(expected, newValue)`**, returning `true`/`false`. Example: append to a queue tail with `tail.next.compareAndSet(null, newNode)`; if `next` is no longer null, someone else appended first, so re-read and retry.

**Pessimistic vs optimistic locking:** a lock is pessimistic (take it first, others wait). CAS is optimistic (assume no conflict, detect it, retry).

### BlockingQueue

A queue that **blocks** instead of failing:

- `take()` on an empty queue **waits** until an element arrives.
- `put()` on a full (bounded) queue **waits** until there's space.

It uses explicit locks (`ReentrantLock` + conditions) and is the ready-made producer–consumer buffer. Implementations: `ArrayBlockingQueue`, `LinkedBlockingQueue`, `PriorityBlockingQueue`, `DelayQueue` (details not needed).

Contrast: `ConcurrentLinkedQueue.poll()` on an empty queue returns **`null` immediately**. `LinkedBlockingQueue.take()` waits (in the demo, until another thread adds 99 after 2 s).

### Fail-fast vs fail-safe iterators

- **Fail-fast** (`ArrayList`, `HashMap`): throw `ConcurrentModificationException` if the collection changes during iteration.
- **Fail-safe** (`ConcurrentHashMap`, `CopyOnWriteArrayList`): iterate over a snapshot or the live structure without throwing.

### Summary

| Collection | Thread-safety technique | Reads | Writes | Use when |
| --- | --- | --- | --- | --- |
| `ConcurrentHashMap` | Per-bucket locks + CAS | Lock-free | Parallel across buckets | **Default thread-safe map** |
| `CopyOnWriteArrayList` | Copy array on every write | Lock-free snapshot | Slow, copies | Read-heavy, rarely-modified lists |
| `ConcurrentLinkedQueue`/`Deque` | Lock-free CAS | Non-blocking | Fast, no copy | High-throughput queues; returns null when empty |
| `BlockingQueue` | Locks + conditions | `take()` blocks if empty | `put()` blocks if full | Producer–consumer buffers |

## 10. Future and CompletableFuture

These aren't needed for the LeetCode problems, but every production multithreaded codebase uses them (alongside executors and concurrent collections). `CompletableFuture` is the "big brother" of `Future`.

### Future

A **`Future` is a container for the result of an asynchronous computation** that will be ready later. You start a task on another thread, keep working on the main thread, and pick up the result when you need it.

```java
ExecutorService ex = Executors.newSingleThreadExecutor();
Future<String> future = ex.submit(() -> {
    Thread.sleep(2000);              // 2 s task
    return "Task completed";
});
System.out.println(future.isDone());   // false (non-blocking check)
String result = future.get();          // BLOCKS until the task finishes
System.out.println("Result: " + result);
System.out.println(future.isDone());   // true
ex.shutdown();
```

**Timing example:** a task takes 2 min. Main does 1 min of other work, then calls `get()`, so it blocks only for the **remaining** \~1 min. If main had worked 3 min, `get()` would return immediately.

Methods:

- `submit()` starts the task asynchronously.
- `get()` blocks until done, returns the result (or throws the task's exception).
- `isDone()` is a non-blocking status check.
- `cancel(true)` cancels a not-yet-started task, or sends an **interrupt** to a running one. Java only sets the interrupt flag; the task must check `Thread.currentThread().isInterrupted()` itself to actually stop.

**Limitations of `Future`** (each is solved by `CompletableFuture`):

1. **No composition/chaining**: you can't say "when this finishes, do that".
2. **No built-in exception handling**: only manual `try/catch` around `get()`.
3. **Blocking between dependent steps**: if T2 needs T1's result, you must call `T1.get()` (block), then start T2, then `T2.get()` (block again). The main thread sits idle at each pit stop when it could be doing other work.
4. **No completion notification**: no callbacks; you must poll `isDone()` or call `get()`.

### CompletableFuture

Lets you **chain** async steps so they run one after another **without blocking the caller in between**. You block (if at all) once, at the very end.

With two dependent 10 s steps: using `Future` the main thread blocks 10 s, then 10 s again. Using `CompletableFuture` you chain them; the 20 s pipeline runs on its own and the main thread is free the whole time until it finally calls `join()`/`get()`.

```java
CompletableFuture<Void> cf = CompletableFuture
    .supplyAsync(() -> fetchUser())        // runs on another thread (ForkJoinPool)
    .thenApply(user -> enrich(user))       // transform result when ready
    .thenAccept(u -> System.out.println(u))// consume result, returns nothing
    .exceptionally(ex -> { log(ex); return null; });

doOtherWork();   // main thread is free meanwhile
cf.join();       // block once, at the end
```

**Key methods** (in an interview, `supplyAsync` + `thenApply` + `join` is enough)

| Method | What it does |
| --- | --- |
| `supplyAsync(supplier)` | Run a task on another thread; returns a `CompletableFuture` with its result |
| `get()` | Block for the result; throws **checked** exceptions (use in production) |
| `join()` | Block for the result; throws **unchecked** exceptions (cleaner demo code; the course uses it) |
| `complete(value)` | Manually complete the future with a value |
| `isDone()` | Non-blocking status check |
| `thenApply(fn)` | Transform the result; runs **on the same thread** that finished the previous step |
| `thenApplyAsync(fn)` | Same transform, still sequential, but **on a different pool thread**, to keep an important thread (event loop, I/O thread) free of heavy work |
| `thenAccept(consumer)` | Use the result, return nothing |
| `thenCombine(other, fn)` | Wait for **two** futures and combine their results |
| `allOf(f1, f2, ...)` | Complete when **all** complete |
| `anyOf(f1, f2, ...)` | Complete when **any one** completes |
| `exceptionally(fn)` | Handle an exception (provide a fallback) |
| `handle((res, ex) -> ...)` | Handle **both** success and failure |
| `orTimeout(t, unit)` | If not done in time, complete **exceptionally** with `TimeoutException` |
| `completeOnTimeout(v, t, unit)` | If not done in time, complete **normally** with default `v` |

Common confusion: `thenApplyAsync` does **not** run in parallel with the previous step. Both versions are sequential; only the executing thread differs.

`thenApply` vs `thenCombine`: `thenApply` transforms **one** future's result; `thenCombine` waits for **two** futures and merges them.

After `orTimeout`/`completeOnTimeout`, the underlying task still keeps running in the background; only the future you're waiting on completes early.

**When to use which:** `Future` only for a single simple async task where blocking is fine. Otherwise (chaining, combining, callbacks, exception handling) always `CompletableFuture`.

## 11. LeetCode concurrency problems

The course solves five problems. Most rely on **semaphores**, so make sure section 8 is solid. General method the instructor recommends: **draw each thread and the function it calls, decide the initial permits, then decide who releases whose permit after each step.**

| # | Problem | Core idea | Tools |
| --- | --- | --- | --- |
| 1 | Print Zero Even Odd (LC 1116) | Hand a "turn" between threads | 3 semaphores |
| 2 | FizzBuzz Multithreaded (LC 1195) | `number` thread acts as controller | 4 semaphores |
| 3 | Design Bounded Blocking Queue (LC 1188) | Producer–consumer with permits = slots | 2 semaphores + `ConcurrentLinkedDeque` |
| 4 | Dining Philosophers (LC 1226) | Allow only N−1 at the table to avoid deadlock | Counting semaphore + per-fork binary semaphores |
| 5 | Web Crawler Multithreaded (LC 1242) | Parallel BFS over URLs | `ExecutorService`, `ConcurrentHashMap`, `AtomicInteger` |

### 1. Print Zero Even Odd

Three threads call `zero()`, `even()`, `odd()`. Output for n = 5: `0102030405`.

**Approach** (concert-ticket analogy): only the zero thread starts with a ticket. After printing 0, it hands a ticket to **odd** or **even** depending on whose turn is next. Odd/even print their number and **always** hand the ticket back to zero.

```java
class ZeroEvenOdd {
    private final int n;
    private final Semaphore zeroSem = new Semaphore(1);  // zero goes first
    private final Semaphore oddSem  = new Semaphore(0);
    private final Semaphore evenSem = new Semaphore(0);

    public ZeroEvenOdd(int n) { this.n = n; }

    public void zero(IntConsumer printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i++) {
            zeroSem.acquire();
            printNumber.accept(0);
            if (i % 2 == 1) oddSem.release(); else evenSem.release();
        }
    }
    public void odd(IntConsumer printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i += 2) {
            oddSem.acquire();
            printNumber.accept(i);
            zeroSem.release();                // always back to zero
        }
    }
    public void even(IntConsumer printNumber) throws InterruptedException {
        for (int i = 2; i <= n; i += 2) {
            evenSem.acquire();
            printNumber.accept(i);
            zeroSem.release();
        }
    }
}
```

The key insight is the **initial permits** (1, 0, 0). Each thread only loops as many times as it needs to print.

### 2. FizzBuzz Multithreaded

Four threads: `fizz()`, `buzz()`, `fizzbuzz()`, `number()`. For n = 15: `1, 2, fizz, 4, buzz, fizz, 7, 8, fizz, buzz, 11, fizz, 13, 14, fizzbuzz`.

**Approach:** only `number` starts with a permit and acts as the **controller**. For each `i` it decides: divisible by 15 → release fizzbuzz; by 3 only → release fizz; by 5 only → release buzz; otherwise print `i` and release itself. Fizz/buzz/fizzbuzz print and **always release `number`**.

```java
class FizzBuzz {
    private final int n;
    private final Semaphore numSem  = new Semaphore(1);
    private final Semaphore fizzSem = new Semaphore(0);
    private final Semaphore buzzSem = new Semaphore(0);
    private final Semaphore fbSem   = new Semaphore(0);

    public FizzBuzz(int n) { this.n = n; }

    public void number(IntConsumer printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i++) {
            numSem.acquire();
            if (i % 15 == 0)      fbSem.release();
            else if (i % 3 == 0)  fizzSem.release();
            else if (i % 5 == 0)  buzzSem.release();
            else { printNumber.accept(i); numSem.release(); }
        }
    }
    public void fizz(Runnable printFizz) throws InterruptedException {
        for (int i = 3; i <= n; i += 3) {
            if (i % 5 == 0) continue;           // those belong to fizzbuzz
            fizzSem.acquire(); printFizz.run(); numSem.release();
        }
    }
    public void buzz(Runnable printBuzz) throws InterruptedException {
        for (int i = 5; i <= n; i += 5) {
            if (i % 3 == 0) continue;
            buzzSem.acquire(); printBuzz.run(); numSem.release();
        }
    }
    public void fizzbuzz(Runnable printFizzBuzz) throws InterruptedException {
        for (int i = 15; i <= n; i += 15) {
            fbSem.acquire(); printFizzBuzz.run(); numSem.release();
        }
    }
}
```

The course loops each helper a fixed number of times; counting exactly how many times each must print (as above) avoids threads hanging at the end.

### 3. Design Bounded Blocking Queue

`enqueue` must block when full; `dequeue` must block when empty; `size` returns the count. Many producer and consumer threads.

**Approach:** this is producer–consumer.

- `emptySlots` semaphore starts at **capacity** (that many producers may insert).
- `filledSlots` semaphore starts at **0** (nothing to consume yet).
- Producer: acquire `emptySlots` → insert → release `filledSlots`.
- Consumer: acquire `filledSlots` → remove → release `emptySlots`.

The semaphores control **how many** threads get in, but several can still be inside together, so the underlying queue must itself be thread-safe. Instead of locking a plain queue, use a concurrent collection.

```java
class BoundedBlockingQueue {
    private final Semaphore emptySlots;
    private final Semaphore filledSlots = new Semaphore(0);
    private final ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();

    public BoundedBlockingQueue(int capacity) { emptySlots = new Semaphore(capacity); }

    public void enqueue(int element) throws InterruptedException {
        emptySlots.acquire();
        deque.addFirst(element);
        filledSlots.release();
    }
    public int dequeue() throws InterruptedException {
        filledSlots.acquire();
        int value = deque.pollLast();
        emptySlots.release();
        return value;
    }
    public int size() { return deque.size(); }
}
```

The Pub-Sub (Kafka) LLD is the "advanced version" of this problem.

### 4. Dining Philosophers

5 philosophers at a round table, 5 forks (one between each pair). A philosopher needs **both** adjacent forks to eat, then puts both down. Goal: no deadlock, no starvation.

**The deadlock:** if all 5 pick up their left fork at once, each waits forever for the right one.

**Approach:**

1. A **counting semaphore with N − 1 = 4 permits** lets at most 4 philosophers "sit at the table". With 4 people and 5 forks, at least one can always get both forks, eat, and free them.
2. Each fork is a **binary semaphore** (a lock), so only one philosopher holds it.
3. Fork ids: left = `p`, right = `(p + 1) % 5` (mod because the table is round).

```java
class DiningPhilosophers {
    private final Semaphore table = new Semaphore(4);      // N - 1
    private final Semaphore[] forks = new Semaphore[5];

    public DiningPhilosophers() {
        for (int i = 0; i < 5; i++) forks[i] = new Semaphore(1);
    }

    public void wantsToEat(int philosopher, Runnable pickLeftFork, Runnable pickRightFork,
                           Runnable eat, Runnable putLeftFork, Runnable putRightFork)
            throws InterruptedException {
        int left = philosopher, right = (philosopher + 1) % 5;
        table.acquire();                       // sit down (max 4)
        forks[left].acquire();  pickLeftFork.run();
        forks[right].acquire(); pickRightFork.run();
        eat.run();
        putLeftFork.run();  forks[left].release();
        putRightFork.run(); forks[right].release();
        table.release();                       // leave, let someone else sit
    }
}
```

If the number of philosophers is a variable, use `n - 1` permits.

### 5. Web Crawler Multithreaded (the instructor's "most important" one)

Given `startUrl` and an `HtmlParser` whose `getUrls(url)` is a **blocking** call (\~15 ms) returning links on that page. Return all URLs reachable that share the start URL's **hostname**, each crawled once. Single-threaded is too slow.

**Why threads help:** sequentially, 4 pages cost 15 × 4 = 60 ms. When page 1 returns two links, crawling both in parallel turns that into \~45 ms here, and far more savings on big sites.

**Design:**

- `ExecutorService` (fixed pool of 5): each URL is a task; the pool assigns free threads and queues the rest.
- `ConcurrentHashMap` of visited URLs: many threads check and mark at once. Prevents re-crawling (and infinite loops when pages link back).
- `AtomicInteger pending`: number of URLs submitted but not fully processed. Main thread waits until it hits 0, then shuts the pool down.
- Skip URLs with a different hostname.

```java
class Solution {
    private final ConcurrentHashMap<String, Boolean> visited = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final AtomicInteger pending = new AtomicInteger(0);
    private String hostname;
    private HtmlParser parser;

    public List<String> crawl(String startUrl, HtmlParser htmlParser) {
        hostname = getHost(startUrl);
        parser = htmlParser;
        visited.put(startUrl, true);
        pending.incrementAndGet();
        executor.submit(() -> crawlTask(startUrl));

        while (pending.get() > 0) {                 // wait, but not a hot spin
            try { Thread.sleep(10); } catch (InterruptedException e) { break; }
        }
        executor.shutdown();
        return new ArrayList<>(visited.keySet());
    }

    private void crawlTask(String url) {
        try {
            for (String next : parser.getUrls(url)) {          // blocking ~15 ms
                if (getHost(next).equals(hostname)
                        && visited.putIfAbsent(next, true) == null) {  // atomic check-and-mark
                    pending.incrementAndGet();                 // count BEFORE submitting
                    executor.submit(() -> crawlTask(next));
                }
            }
        } finally {
            pending.decrementAndGet();   // only after all children are submitted
        }
    }

    private String getHost(String url) { return url.split("/")[2]; }  // "http://host/path"
}
```

The subtle bug to avoid: **decrement `pending` only after you've submitted all child URLs.** If you decrement first, the count can briefly hit 0 and the main thread shuts the pool down while work remains. The `sleep(10)` in the wait loop avoids burning CPU with busy waiting.

`putIfAbsent` makes check-and-mark a single atomic step, slightly safer than the course's separate "check visited, then put".

## 12. Applying this in LLD rounds

This video stops after the LeetCode problems. The full concurrent LLD designs (Pub-Sub/Kafka, cache) are separate videos in the series. These are the hooks the instructor points to along the way:

- **Interview bar:** concurrency is now expected in LLD rounds, and the same ideas back-port to earlier LLD problems: an elevator system handling many concurrent calls, a car rental system booking cars concurrently, a vending machine with simultaneous users.
- **Pub-Sub (Kafka-like):** `synchronized` on the **topic object** to guard each topic; `notifyAll()` to wake **all** subscribers when a message is published; a **cached thread pool** for the work.
- **Cache with read-your-own-writes:** an array of `newSingleThreadExecutor()` instances; route each key to the same executor (e.g. by hash) so all operations on one key run in order on one thread.
- **Shared maps everywhere:** `ConcurrentHashMap` is the default for any shared state.
- **Toolkit you'll actually use:** executors to manage threads, concurrent collections for shared data, `synchronized`/`ReentrantLock` for critical sections, semaphores for limiting access, `CompletableFuture` for async flows. Never create raw threads in production code.

## Quick revision cheat sheet

**One-liners to say in interviews**

| Question | Answer |
| --- | --- |
| Process vs thread? | Process has its own memory; threads share their process's memory. Threads are lighter and talk easily but can crash each other. |
| `Thread` vs `Runnable`? | Prefer `Runnable`: Java allows one `extends`, so implementing keeps inheritance free. |
| `Runnable` vs `Callable`? | `Callable.call()` returns a value and can throw checked exceptions; used via `ExecutorService` → `Future`. |
| `start()` vs `run()`? | `start()` creates a new thread; `run()` just runs on the current thread. |
| `start()` twice? | `IllegalThreadStateException`. |
| `sleep()` vs `wait()`? | `sleep` keeps the lock; `wait` releases it and needs `notify`. `wait` only inside `synchronized`. |
| `notify()` vs `notifyAll()`? | One waiter vs all waiters; all still compete for the lock. |
| Why thread pools? | Reuse threads; avoid creation cost; cap thread count. |
| Fixed vs cached pool? | Fixed for CPU-bound; cached for short I/O-bound bursts. Unsure → fixed. |
| `ThreadPoolExecutor` order? | Core threads → queue → extra threads up to max → reject. |
| `execute()` vs `submit()`? | `execute`: Runnable, no result, exceptions lost. `submit`: Runnable/Callable, returns `Future`. |
| `shutdown()` vs `shutdownNow()`? | Graceful vs immediate (interrupts, returns unstarted tasks). |
| No shutdown? | JVM never exits; leaks. |
| Fixed rate vs fixed delay? | Fixed start times vs fixed gap after each run ends. |
| `synchronized` method lock? | Instance → `this`; static → class object. |
| `volatile`? | Visibility (main memory) + ordering; not atomic. |
| Atomic variables? | Lock-free thread-safe ops on one variable via CAS. |
| `synchronized` vs `ReentrantLock`? | Automatic vs manual; lock adds `tryLock`, timeout, fairness, conditions. Unlock in `finally`. |
| Read-write lock? | Many readers or one writer, never both. |
| Lock vs semaphore? | 1 thread vs N permits; lock owned by a thread, semaphore permits aren't. |
| `release()` without `acquire()`? | Legal; adds a permit above the initial count. |
| `HashMap` vs `ConcurrentHashMap`? | Whole-map lock vs per-bucket locks + CAS + lock-free reads. |
| Fail-fast vs fail-safe? | Throw `ConcurrentModificationException` vs iterate safely (concurrent collections). |
| `Future` vs `CompletableFuture`? | `CompletableFuture` adds chaining, callbacks, exception handling, combining. |
| `get()` vs `join()`? | Checked vs unchecked exceptions. |

**Pattern recognition for coding problems**

- "Threads must take turns in an order" → one semaphore per thread, initial permits decide who starts, each releases the next.
- "At most N at once" → counting semaphore with N permits.
- "Block when full / empty" → two semaphores (empty slots = capacity, filled = 0) or a `BlockingQueue`.
- "Deadlock from circular waiting" → limit participants to N−1 (or order lock acquisition).
- "Everyone waits until all arrive" → barrier (`CyclicBarrier`, or semaphore at 0 + counter).
- "Parallel graph/BFS work" → `ExecutorService` + `ConcurrentHashMap` visited set + `AtomicInteger` pending count.

**Golden rules:** use executors not raw threads · always `unlock()`/`release()` in `finally` · `wait()` inside a `while` loop · synchronize only the critical section · prefer concurrent collections over locking whole structures · output order in multithreaded code is never guaranteed.
