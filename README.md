# LLD — Logging System

Interview study notes + walkthrough for the Logger LLD question.

This implementation follows the design discussed in the reference video/transcript and the corresponding GitHub implementation.

## 1. Interview Question

> Design a Logger / Logging System.

The system should:

- Support multiple log levels.
- Support multiple output destinations.
- Support multiple output formats.
- Be extensible for new levels, destinations and formats.
- Work correctly when multiple threads log concurrently.

The important part is not simply printing a message. The goal is to separate the responsibilities so the logger can be extended without turning into one large `if/else` class.

---

## 2. Requirements

### Extensibility

Clarify these with the interviewer:

1. **What log levels?**

   Current implementation:
   `TRACE, DEBUG, INFO, WARN, ERROR, FATAL`

2. **Where should logs go?**

   Current implementation:
   - Console
   - File

   Future possibilities:
   - Database
   - Kafka
   - Cloud storage
   - Network service

3. **What format?**

   Current implementation:
   - Plain text
   - JSON

   Future possibilities:
   - XML
   - Other structured formats

### Concurrency

If two threads log at the same time:

```text
Thread 1 -> "Hello"
Thread 2 -> "World"
```

We must not get:

```text
HelWorldlo
```

The exact order is not important. Either complete message can appear first, but individual writes must not interleave.

---

# 3. Core Mental Model

Remember these four questions:

| Question | Component | Pattern |
|---|---|---|
| What is being logged? | `LogMessage` | Data object |
| Which level handles it? | `LogHandler` chain | Chain of Responsibility |
| Where should it go? | `LogAppender` | Observer-style subscription |
| How should it look? | `LogFormatter` | Strategy |

And:

| Responsibility | Component | Pattern |
|---|---|---|
| One globally shared logger | `Logger` | Singleton |

The whole system:

```text
Client
  |
  v
Logger
  |
  v
LogMessage
  |
  v
DEBUG -> INFO -> WARN -> ERROR -> FATAL
                     |
                     +--> ConsoleAppender
                     |
                     +--> FileAppender
                              |
                              v
                         LogFormatter
                         /          \
                      Text          JSON
```

---

# 4. Package Structure

```text
logger/
├── appenders/
│   ├── ConsoleAppender.java
│   ├── FileAppender.java
│   └── LogAppender.java
│
├── enums/
│   └── LogLevel.java
│
├── formatter/
│   ├── JsonFormatter.java
│   ├── LogFormatter.java
│   └── PlainTextFormatter.java
│
├── handlers/
│   ├── DebugHandler.java
│   ├── ErrorHandler.java
│   ├── FatalHandler.java
│   ├── InfoHandler.java
│   ├── LogHandler.java
│   └── WarnHandler.java
│
├── model/
│   └── LogMessage.java
│
├── LogHandlerConfiguration.java
├── Logger.java
└── Main.java
```

---

# 5. `LogLevel`

```java
public enum LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FATAL
}
```

A log level represents the severity/category of a message.

Example:

```text
INFO  -> normal application information
WARN  -> something important but not necessarily broken
ERROR -> something failed
FATAL -> severe failure
```

### Code note

The enum contains `TRACE`, but the current handler chain starts at `DEBUG` and there is no `TraceHandler`.

So the current implementation has:

```text
TRACE -> enum only
DEBUG -> handler
INFO  -> handler
WARN  -> handler
ERROR -> handler
FATAL -> handler
```

---

# 6. `LogMessage`

Instead of passing:

```text
level
message
timestamp
```

separately through the system, bind them together:

```java
public class LogMessage {
    private LogLevel level;
    private String message;
    private long timestamp;
}
```

Every component can now work with one object:

```text
Logger
  |
  v
LogMessage
  |
  v
Handler
  |
  v
Appender
```

Example:

```text
LogMessage
├── level     = ERROR
├── message   = "Payment failed"
└── timestamp = ...
```

---

# 7. Strategy Pattern — Formatter

The formatting requirement is:

```text
Plain Text
JSON
```

and future formats should be easy to add.

Create an abstraction:

```java
public interface LogFormatter {
    String format(LogMessage message);
}
```

Concrete strategies:

```text
              LogFormatter
              /          \
             v            v
   PlainTextFormatter   JsonFormatter
```

### Why Strategy?

Formatting is a behavior that can vary independently.

Without Strategy, `Logger` or an appender might contain:

```java
if (format == TEXT) {
    ...
} else if (format == JSON) {
    ...
}
```

Adding XML would then require changing existing logic.

With Strategy:

```java
class XmlFormatter implements LogFormatter {
    ...
}
```

No change to the core logger is required.

### Interview line

> "Formatting is an independently varying behavior, so I'll encapsulate it behind the `LogFormatter` strategy."

---

# 8. Plain Text Formatter

The current format is:

```text
2026-08-16 20:15:32 [ERROR] - Payment failed
```

The timestamp is stored as a `long`, then converted into a readable local date/time.

The formatter does:

```text
epoch timestamp
      |
      v
Instant
      |
      v
LocalDateTime
      |
      v
formatted String
```

---

# 9. JSON Formatter

The same `LogMessage` can be represented in JSON-like form:

```json
{"timestamp": 2026-08-16 20:15:32, "level": "ERROR", "message": "Payment failed"}
```

The important idea is that the Logger does not care whether the final representation is text or JSON.

---

# 10. Appenders — Output Destination

Formatting answers:

> How should the log look?

An appender answers:

> Where should the log go?

```java
public interface LogAppender {
    void append(LogMessage message);
}
```

Current implementations:

```text
LogAppender
   /      \
  v        v
Console   File
```

Future:

```text
DatabaseAppender
KafkaAppender
CloudAppender
...
```

---

# 11. ConsoleAppender

The console appender receives a formatter through its constructor.

Conceptually:

```java
System.out.println(formatter.format(message));
```

Notice the separation:

```text
Appender  -> WHERE?
Formatter -> HOW?
```

The console appender does not need to know how text or JSON formatting works.

---

# 12. FileAppender

The file appender:

1. Receives a formatter.
2. Opens the file in append mode.
3. Writes the formatted message.
4. Adds a newline.
5. Flushes the writer.

The constructor uses append mode:

```java
new FileWriter(fileName, true)
```

The `true` is important because existing logs should remain.

```text
old logs
   +
new log
```

not:

```text
new log
   ->
replace old file
```

---

# 13. Chain of Responsibility — Log Handlers

The system has one handler for each level:

```text
DebugHandler
InfoHandler
WarnHandler
ErrorHandler
FatalHandler
```

They are connected as a linked chain:

```text
DEBUG -> INFO -> WARN -> ERROR -> FATAL
```

Each handler has:

```java
protected LogHandler next;
```

and:

```java
protected abstract boolean canHandle(LogLevel level);
```

---

# 14. How the Handler Chain Works

Suppose:

```java
logger.error("Payment failed");
```

The message starts at the head:

```text
DebugHandler
    |
    v
InfoHandler
    |
    v
WarnHandler
    |
    v
ErrorHandler
```

Each handler asks:

```text
Can I handle ERROR?
```

Debug:

```text
NO -> next
```

Info:

```text
NO -> next
```

Warn:

```text
NO -> next
```

Error:

```text
YES
```

So ErrorHandler handles the message and stops the chain.

The core logic is:

```java
if (canHandle(message.getLevel())) {
    notifyObservers(message);
} else if (next != null) {
    next.handle(message);
}
```

---

# 15. Why Chain of Responsibility?

Without the chain, Logger could become:

```java
if (level == DEBUG) {
    debugHandler.handle(message);
} else if (level == INFO) {
    infoHandler.handle(message);
} else if (level == ERROR) {
    errorHandler.handle(message);
}
```

Now Logger knows every concrete handler.

With the chain:

```java
handlerChain.handle(message);
```

Logger only starts the chain.

### Interview line

> "I don't want Logger to know which concrete handler corresponds to each level. It should simply start the chain and let the handlers decide who can handle the message."

---

# 16. Observer Pattern — Handler to Appenders

Once a handler accepts a message, it notifies its configured appenders.

For example:

```text
ErrorHandler
   |
   +--> ConsoleAppender
   |
   +--> FileAppender
```

The handler maintains:

```java
List<LogAppender> appenders;
```

and:

```java
public void notifyObservers(LogMessage message) {
    for (LogAppender appender : appenders) {
        appender.append(message);
    }
}
```

So the handler acts like a subject and appenders behave like observers.

### Why?

One level can have multiple destinations.

Example:

```text
INFO
  └── Console

ERROR
  ├── Console
  └── File
```

If later we want database logging:

```text
ERROR
  ├── Console
  ├── File
  └── Database
```

No change to `ErrorHandler`'s core logic is required.

---

# 17. Strategy vs Observer vs Chain

This is a common interview confusion.

### Strategy

Answers:

> How should the message be formatted?

```text
PlainText
JSON
XML
```

### Chain of Responsibility

Answers:

> Which handler should process this level?

```text
DEBUG -> INFO -> WARN -> ERROR -> FATAL
```

### Observer

Answers:

> Which destinations should receive the message?

```text
ERROR
 ├── Console
 └── File
```

### Singleton

Answers:

> How many Logger instances should exist?

```text
ONE
```

---

# 18. Concurrency — CopyOnWriteArrayList

`LogHandler` stores its appenders using:

```java
new CopyOnWriteArrayList<>()
```

Why?

Imagine:

```text
Thread 1 -> iterating over appenders

Thread 2 -> adds/removes an appender
```

With a normal `ArrayList`, concurrent modification during iteration can cause:

```text
ConcurrentModificationException
```

`CopyOnWriteArrayList` creates a new copy when the collection is modified, allowing an existing iterator to continue safely.

This is a good fit because:

```text
logging / reading appenders -> frequent
configuration changes       -> relatively rare
```

### Important distinction

`CopyOnWriteArrayList` protects:

> the appender collection

It does NOT protect the actual file write.

---

# 19. Concurrency — FileAppender

Two threads might reach the file appender:

```text
Thread 1 -> append("Hello")
Thread 2 -> append("World")
```

The implementation uses:

```java
public synchronized void append(LogMessage message)
```

Therefore only one thread can execute the append operation at a time.

Valid:

```text
Hello
World
```

or:

```text
World
Hello
```

Invalid:

```text
HelWorldlo
```

### Important

`synchronized` guarantees mutual exclusion, not a fixed ordering.

It does not mean Thread 1 always writes before Thread 2.

---

# 20. Why Two Different Concurrency Mechanisms?

Remember:

```text
CopyOnWriteArrayList
        |
        v
safe concurrent access to appender list


synchronized append()
        |
        v
safe file writing
```

They solve different problems.

---

# 21. Singleton Logger

The logger uses eager Singleton initialization:

```java
private static final Logger INSTANCE = new Logger();
```

The constructor is private:

```java
private Logger() {
    handlerChain = LogHandlerConfiguration.build();
}
```

and clients access it using:

```java
public static Logger getInstance() {
    return INSTANCE;
}
```

So:

```text
Service A ----\
Service B ----- > Singleton Logger
Service C ----/
```

### Why Singleton?

The goal is one globally shared logger/configuration instead of independent logger objects.

---

# 22. Logger API

The client gets:

```java
Logger logger = Logger.getInstance();
```

and uses:

```java
logger.debug("...");
logger.info("...");
logger.warn("...");
logger.error("...");
logger.fatal("...");
```

The convenience methods all delegate to:

```java
log(LogLevel level, String message)
```

For example:

```java
public void error(String msg) {
    log(LogLevel.ERROR, msg);
}
```

---

# 23. Complete `logger.error()` Flow

This is the most important flow to memorize.

Client:

```java
logger.error("Payment failed");
```

### Step 1 — Logger adds the level

```text
error()
  |
  v
log(ERROR, "Payment failed")
```

### Step 2 — Create `LogMessage`

```text
LogMessage
├── ERROR
├── Payment failed
└── current timestamp
```

### Step 3 — Start the chain

```text
handlerChain.handle(message)
```

### Step 4 — Traverse

```text
Debug
  ↓
Info
  ↓
Warn
  ↓
Error
```

### Step 5 — ErrorHandler accepts

```text
canHandle(ERROR) == true
```

### Step 6 — Notify appenders

Suppose:

```text
ErrorHandler
 ├── ConsoleAppender
 └── FileAppender
```

### Step 7 — Each appender formats

```text
LogMessage
   ↓
PlainTextFormatter
   ↓
2026-08-16 20:15:32 [ERROR] - Payment failed
```

### Step 8 — Output

```text
ConsoleAppender -> console
FileAppender    -> logs.txt
```

---

# 24. Configuration

`LogHandlerConfiguration` constructs the chain:

```text
debug -> info -> warn -> error -> fatal
```

It returns the head:

```text
debug
```

The Logger stores that head.

The configuration class also provides:

```java
addAppenderForLevel(level, appender)
```

For example:

```text
INFO  -> Console
ERROR -> Console
ERROR -> File
```

creates:

```text
INFO
 └── Console

ERROR
 ├── Console
 └── File
```

---

# 25. Current `Main` Configuration

The repository's example configures:

```text
INFO
 └── ConsoleAppender(PlainTextFormatter)

ERROR
 ├── ConsoleAppender(PlainTextFormatter)
 └── FileAppender(PlainTextFormatter, "logs.txt")
```

So:

```java
logger.info("This is some key information");
```

goes to:

```text
CONSOLE
```

while:

```java
logger.error("Oh no! there's an error");
```

goes to:

```text
CONSOLE + logs.txt
```

---

# 26. Class Responsibility Table

| Component | Responsibility |
|---|---|
| `LogLevel` | Represents severity |
| `LogMessage` | Holds level, message, timestamp |
| `LogFormatter` | Formatting abstraction |
| `PlainTextFormatter` | Plain-text representation |
| `JsonFormatter` | JSON representation |
| `LogAppender` | Destination abstraction |
| `ConsoleAppender` | Console output |
| `FileAppender` | File output |
| `LogHandler` | Chain node + appender notification |
| `DebugHandler` | Handles DEBUG |
| `InfoHandler` | Handles INFO |
| `WarnHandler` | Handles WARN |
| `ErrorHandler` | Handles ERROR |
| `FatalHandler` | Handles FATAL |
| `LogHandlerConfiguration` | Builds/configures chain |
| `Logger` | Singleton facade/orchestrator |
| `Main` | Example usage |

---

# 27. SOLID

## Single Responsibility

Responsibilities are separated:

```text
Logger       -> logging API / orchestration
LogMessage   -> data
Handler      -> level routing
Appender     -> destination
Formatter    -> representation
Configuration-> wiring
```

## Open/Closed

Adding a formatter:

```java
class XmlFormatter implements LogFormatter
```

doesn't require modifying the existing formatter consumers.

Adding an appender:

```java
class DatabaseAppender implements LogAppender
```

doesn't require changing the Logger.

## Dependency Inversion

Higher-level components use abstractions:

```text
LogFormatter
LogAppender
```

instead of hard-coding concrete implementations.

---

# 28. Extensibility Examples

### Add XML

```java
class XmlFormatter implements LogFormatter {
    @Override
    public String format(LogMessage message) {
        ...
    }
}
```

### Add Database

```java
class DatabaseAppender implements LogAppender {
    @Override
    public void append(LogMessage message) {
        ...
    }
}
```

### Add a new log level

Add a corresponding handler and wire it into the chain.

**Code caveat:** the current `addAppenderForLevel()` uses a `switch`, so adding a new level also requires updating that configuration switch. The important design point is that Logger itself does not contain level-specific routing logic.

---

# 29. Async Logging — Natural Follow-up

If the interviewer asks:

> What if file I/O is slow?

The current flow is synchronous:

```text
Application
   ↓
Logger
   ↓
Handler
   ↓
FileAppender
   ↓
Disk
```

A scalable design can become:

```text
Application
   ↓
Logger
   ↓
BlockingQueue
   ↓
Worker Thread(s)
   ↓
FileAppender
   ↓
Disk
```

The application thread no longer has to perform the slow disk write itself.

Possible production improvements:

- Blocking queue
- Worker threads
- Batch writes
- Delayed/batched flush
- Log rotation
- Size-based/date-based files
- Database/Kafka/cloud appenders

Do not over-engineer the initial interview solution unless asked.

---

# 30. Interview Walkthrough

If asked to solve this from scratch, use this sequence.

### Step 1 — Clarify

Ask:

```text
What log levels?
What destinations?
What formats?
Should they be extensible?
Can multiple threads log simultaneously?
```

### Step 2 — Identify abstractions

```text
LogMessage
LogFormatter
LogAppender
LogHandler
Logger
```

### Step 3 — Explain patterns

> "I'll use Strategy for formatting because formats vary."

> "I'll use Chain of Responsibility for log-level routing so Logger doesn't contain level-specific branching."

> "I'll use an observer-style subscription between handlers and appenders because a handler can notify multiple destinations."

> "I'll make Logger a Singleton for a globally shared logger."

### Step 4 — Explain concurrency

```text
CopyOnWriteArrayList -> safe appender iteration/configuration
synchronized append  -> serialized file writes
```

### Step 5 — Walk through one request

```text
logger.error()
 -> LogMessage
 -> Handler Chain
 -> ErrorHandler
 -> Console + File
 -> Formatter
 -> Output
```

---

# 31. 30-Second Interview Answer

> "I'll model each log as a `LogMessage` containing its level, message and timestamp. `Logger` will be a Singleton and expose methods such as `info()` and `error()`. It creates the `LogMessage` and starts a Chain of Responsibility made of level-specific handlers. Each handler decides whether it can process the message, and once it accepts it, it notifies its configured appenders. Appenders represent destinations such as console or file. Each appender receives a `LogFormatter` Strategy so the representation can independently be changed between text, JSON, XML, etc. For concurrency, I'll use `CopyOnWriteArrayList` for safe appender iteration and synchronize file writes so multiple threads cannot interleave their writes."

---

# 32. One-Page Revision

```text
REQUIREMENTS
- Multiple levels
- Multiple destinations
- Multiple formats
- Extensible
- Thread-safe

PATTERNS
Logger             -> Singleton
Formatter          -> Strategy
Handler            -> Chain of Responsibility
Handler/Appenders  -> Observer-style subscription

CHAIN
DEBUG -> INFO -> WARN -> ERROR -> FATAL

APPENDERS
Console
File

FORMATTERS
Plain Text
JSON

CONCURRENCY
CopyOnWriteArrayList
        -> appender collection

synchronized append()
        -> file writes

FLOW
logger.error()
    ↓
LogMessage
    ↓
Handler Chain
    ↓
ErrorHandler
    ↓
notifyObservers()
    ↓
Console + File
    ↓
Formatter
    ↓
Output
```

---

# 33. Final Mental Model

Do not memorize every class.

Memorize:

```text
WHAT?
  LogMessage

WHICH LEVEL?
  Chain of Responsibility

WHERE?
  Appender / Observer

HOW?
  Formatter / Strategy

WHO OWNS THE SYSTEM?
  Singleton Logger
```

If you can explain those five answers and walk through:

```text
logger.error("...")
```

from client → logger → handler → appenders → formatter → output, you understand the design well enough to reconstruct the code in an interview.
