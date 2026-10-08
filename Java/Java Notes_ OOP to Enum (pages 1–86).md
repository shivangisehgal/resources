# Java Notes: OOP → Enum (Pages 1–86)

> Corrections to the notebook are marked **⚠**. The notebook ends mid-way through Singleton (lazy init); the rest of that section (synchronized, double-check locking, Bill Pugh, enum, breaking a singleton) has been filled in.

---

## 1. OOP Basics

**Object** = real-world entity (Car, ATM, Dog) with **state** (properties: age, colour) and **behaviour** (methods: bark, drive). **Class** = blueprint/template; one class → many objects. `Student s = new Student();`

| Procedural | OOP |
| --- | --- |
| Program split into functions | Program split into objects |
| No data hiding; data moves freely | Data hiding; data is primary |
| No overloading / inheritance / reuse | All three present |
| Pascal, C, FORTRAN | Java, C#, Python, C++ |

### Four pillars

**1. Abstraction**: hide internal implementation, expose only essentials. Achieved via **interfaces and abstract classes**. *Analogy: you press the brake pedal; how the car slows is hidden.* Benefit: security and confidentiality.

```java
interface Car { void applyBrake(); void incSpeed(); }
class CarImp implements Car { public void applyBrake(){ /*step1,2,3 hidden*/ } }
```

**2. Encapsulation** (data hiding): bundle data + code working on it in one unit. Make fields `private`, expose `public` getters/setters. Benefits: loosely coupled code, better access control and security.

```java
class Dog { private String colour;
  String getColour(){ return this.colour; }
  void setColour(String colour){ this.colour = colour; } }
```

**3. Inheritance**: child inherits fields and methods of parent (`extends`, or via interface). Benefits: code reuse, enables polymorphism. Parent reference can't call child-only methods (`vehicle.getCarType()` fails).

- Types: **Single** (A→B), **Multilevel** (A→B→C), **Hierarchical** (A→B, A→C), **Multiple** (C extends A and B): *not supported for classes* (diamond problem), workaround via interfaces.

**4. Polymorphism** ("many forms"): same method behaves differently by situation (*a person is a father, husband, employee; water is liquid, solid, gas*).

- **Compile-time / static = overloading**: same name, different parameters, same class. Chosen by arguments (`doSum(a,b)` vs `doSum(a,b,c)`).
- **Runtime / dynamic = overriding**: child redefines parent method; same name, args, return type. Chosen at runtime (`B obj = new B(); obj.getEngine()` → B's version).

### Relationships

- **IS-A**: inheritance (Dog is-a Animal).
- **HAS-A**: an object used inside another class (School has Students; Bike has Engine). Can be 1-1, 1-many, many-many. Called **Association**:
  - **Aggregation**: both survive independently (ending one doesn't end the other).
  - **Composition**: ending one ends the other.

---

## 2. Java Basics: JVM / JRE / JDK

Java: platform-independent, OOP, portable (**WORA**: Write Once Run Anywhere). Nesting: **JDK ⊃ JRE ⊃ JVM**.

- **JVM**: abstract machine (not physical). Input: bytecode → output: machine code via **JIT compiler**. `Program.java → javac → Bytecode → JVM → Machine code → CPU`. JVM is **platform-dependent** (install per OS), but bytecode is platform-independent, which is what makes Java portable. JVM, JRE and JDK are all platform-dependent.
- **JRE** = JVM + class libraries. Can *run* programs, not *write* them.
- **JDK** = JRE + compiler (`javac`) + debugger + dev tools.
- **Editions**: **JSE** (core Java), **JEE/Jakarta EE** = JSE + Servlets + JSP + Transaction API + Persistence API, **JME** = mobile/micro APIs.

---

## 3. Variables

Container holding a value: `int n = 1;` (datatype, name, value). Java is **statically typed** (type must be declared) and **strongly typed** (restrictions on what can be assigned).

**Naming**: case-sensitive; Unicode letters/digits; may start with letter, `$` or `_`; no reserved keywords; lowercase for one word, camelCase for multiple; CONSTANTS in CAPITALS.

### Primitive types (8, stored on stack)

| Type | Size | Range | Default |
| --- | --- | --- | --- |
| `char` | 2 B (16 bit) | 0–65535 (`'\u0000'`–`'\uffff'`), ASCII/Unicode | `'\u0000'` (NUL) |
| `byte` | 1 B | −128 to 127 | 0 |
| `short` | 2 B | −32768 to 32767 | 0 |
| `int` | 4 B | −2³¹ to 2³¹−1 | 0 |
| `long` | 8 B | −2⁶³ to 2⁶³−1 (`100L`) | 0 |
| `float` | 4 B (32 bit) | fractional | 0.0f |
| `double` | 8 B (64 bit) | fractional | 0.0 |
| `boolean` | 1 bit | true/false | **⚠ false** (notebook says true) |

Integral = char, byte, short, int, long. Fractional = float, double.

**Signed 2's complement** (byte/short/int/long): the leading bit is the sign (0 = positive, 1 = negative). 2's complement = invert bits + 1. E.g. +3 = `0011`; invert → `1100`; +1 → `1101` = −3. Check: `0011 + 1101 = 0000` ✓.

### Type conversion

1. **Widening (automatic)**: small → large: byte → short → int → long. `long l = intVar;`
2. **Narrowing (explicit cast)**: large → small, manual: `byte b = (byte) intVar;`. Out-of-range values wrap around: 128 → −128, 148 → −108.
3. **Promotion in expressions**: byte/short are promoted to int when used in arithmetic, so `byte sum = a + b;` fails (result is int). If one operand is of a higher type, all are promoted: `int + double` → double (`int sum = a + doubleVar;` errors; use `double`).

### Kinds of variables

- **Instance/member**: one copy per object, created with the object.
- **Local**: declared inside a method; destroyed when the method ends.
- **Static/class**: one copy shared by all objects; accessed via class name.
- **Method parameters** and **constructor parameters**: values passed in.
- **Constant**: `static final int VAR = 10;` (`static` = one copy; `final` = value can't change).

### How float/double are stored (IEEE 754)

*Analogy: scientific notation in binary: `1.xxx × 2^exp`.* Float = **1 sign bit | 8 exponent bits | 23 mantissa bits** (double = 64 bits). Bias for float = **127**. **Example 4.125f**: binary = `100.001` → `1.00001 × 2²` → exponent 2+127 = **129** → bits: `0 | 10000001 | 00001000…0`. Back: `(−1)^sign × (1 + mantissa) × 2^(exp−127)` = 1.03125 × 4 = 4.125 ✓. **0.7f**: binary fraction repeats (`0.1011 0011 0011…`) so it can't be stored exactly; reads back ≈ **0.69970703** (slightly \< 0.7). Same for double. **Hence use `BigDecimal` where precision matters** (money).

### Reference (non-primitive) types

Class, String, Interface, Array (and Enum). Variable on the stack holds a **reference** to the actual object in the **heap** (`new` allocates the object). **Java is always pass-by-value**: for objects, the value passed is the reference (gives pointer-like behaviour).

- **String**: immutable. Literals live in the **String Constant Pool** (inside heap); `s1="hello"; s2="hello"` → both reference the same pooled literal. `new String("hello")` creates a separate normal heap object.
- **Interface reference**: can hold any implementing object (`Person p = new Engineer();`) but you **can't instantiate** an interface (`new Person()` is wrong). Same for parent references holding child objects.
- **Array**: contiguous same-type elements, object in heap: `int[] arr = new int[5];` (also `int arr[]`), indexed from 0. Can be 1D, 2D etc.

### Wrapper classes

Every primitive has an object type: int→Integer, char→Character, short→Short, byte→Byte, long→Long, float→Float, double→Double, boolean→Boolean.

- **Why**: collections work only with objects; wrappers give reference semantics (primitives live on the stack, not the heap).
- **Autoboxing**: primitive → wrapper (`Integer a1 = a;`). **Unboxing**: wrapper → primitive (`int x1 = n;`).

### Quick recap: primitives, floating point & wrappers

1. **Narrowing requires casting**: `int → byte`
   ```java
   byte b = (byte) x;
   ```
2. **byte/short/char arithmetic promotes to int**: `byte + byte → int`
3. **`float` = 32 bits**, **`double` = 64 bits**
4. **float/double use IEEE-754 binary floating point**
5. **`0.1 + 0.2 ≠` exactly `0.3`**, because 0.1 and 0.2 aren't exactly representable in binary
6. **Autoboxing**: `int → Integer`
7. **Unboxing**: `Integer → int`
8. **Integer cache**: guaranteed for **-128 to 127**
9. **`==` on Integer → reference comparison**; **`equals()` → value comparison**
10. **Unboxing `null` → `NullPointerException`**
11. **Collections use wrappers**: `List<Integer>`, not `List<int>`
12. **Overloaded methods can behave differently**:
    - `remove(1)` → removes by **index**
    - `remove(Integer.valueOf(1))` → removes by **value**

```java
Integer a = 127, b = 127;   a == b        // true  (cached)
Integer c = 128, d = 128;   c == d        // false (two different objects)
                            c.equals(d)   // true
Integer n = null;  int x = n;             // NullPointerException
System.out.println(0.1 + 0.2);            // 0.30000000000000004
```

### `==` vs `equals()`

| | `==` | `equals()` |
| --- | --- | --- |
| Primitives | compares **values** | n/a (primitives have no methods) |
| References | compares **identity** (same object in heap?) | compares **content**, *if the class overrides it* |
| Default | operator, can't be changed | `Object.equals()` is just `this == obj` |

`String`, wrappers, `List`, `Map` etc. override `equals()` to compare content. Your own classes compare by identity unless you override it.

```java
String s1 = "hello", s2 = "hello", s3 = new String("hello");
s1 == s2       // true  (same pooled literal)
s1 == s3       // false (s3 is a separate heap object)
s1.equals(s3)  // true  (same characters)
```

**Rules when overriding `equals()`**: it must be **reflexive** (`x.equals(x)`), **symmetric**, **transitive**, **consistent**, and `x.equals(null)` must be `false`. **Always override `hashCode()` too**: equal objects must have equal hash codes, otherwise `HashMap`/`HashSet` break (two "equal" keys land in different buckets).

```java
@Override public boolean equals(Object o) {
  if (this == o) return true;
  if (!(o instanceof Student s)) return false;
  return rollNumber == s.rollNumber && Objects.equals(name, s.name);
}
@Override public int hashCode() { return Objects.hash(rollNumber, name); }
```

### String intern

`intern()` returns the **canonical copy** of a string from the String Constant Pool: if an equal string is already pooled, that reference is returned; otherwise this string is added to the pool and returned.

```java
String a = "java";
String b = new String("java");   // separate heap object
String c = b.intern();           // pooled reference
a == b   // false
a == c   // true
```

- **Literals and compile-time constants** (`"ja" + "va"`) are interned automatically. Strings built **at runtime** (`s1 + s2`, `sb.toString()`) are not.
- Since **Java 7** the pool lives in the **regular heap** (earlier in PermGen), so pooled strings can be garbage collected.
- Use case: saving memory when many duplicate strings live long (e.g. repeated codes read from a file). Don't use it to make `==` work; use `equals()` for comparing strings.

---

## 4. Methods

Collection of instructions performing a task → readability + reusability.

```java
public int sum(int a, int b) throws Exception { /* body */ }
//  access  return  name   params
```

- **Access specifiers**: `public` (any class anywhere), `private` (same class only), `protected` (same package + subclasses in other packages), *default* (no keyword: same package only).
- **Return type**: primitive/class type, or `void`. **Name**: verb, camelCase starting lowercase. **Parameters**: may be empty. **Body** ends at end or at `return` (can use plain `return;` even in void).

### Types of methods

- **System-defined** (`Math.sqrt()`) vs **user-defined**.
- **Overloaded**: same name, same class; differentiated **only by arguments** (return type ignored).
- **Overridden**: subclass redefines parent's method.
- **Static**: belongs to class; call via class name; **can't access non-static members; can't be overridden**. Use for methods not modifying object state, utility methods using only arguments (e.g. factory pattern).
- **Final**: can't be overridden (overriding would be pointless if the implementation can't change).
- **Abstract**: only declaration, in abstract classes; implemented by child classes.
- **Varargs**: `int sum(int a, int... nums)`: variable number of args; **only one, and must be last**. Call with any number: `sum(3)`, `sum(3,8,9,10)`. Internally varargs is an **array** (see *Generics + varargs → heap pollution* in §8).

---

## 5. Constructors

Creates/initialises an instance. Like a method, except: **name = class name**, **no return type**, can't be `static`/`final`/`abstract`/`synchronized`. `new` triggers the call.

**Why can't it be…**

- *final*: constructors aren't inherited, so there's nothing to override.
- *abstract*: abstract means "child implements", but constructors aren't inherited.
- *static*: static can't touch instance variables, and would break `this()`/`super()` chaining.
- *defined in interface*: can't instantiate interfaces.
- *Return type?* Java implicitly returns the class type; a same-named method with a return type is just a method, not a constructor.

### Types

1. **Default**: provided by Java only when you define **no** constructor; sets default values.
2. **No-arg**: you write it, takes no args.
3. **Parameterised**: assigns instance variables from args; unspecified ones get default values.
4. **Overloaded**: multiple constructors with different parameters.
5. **Private**: nobody outside can call it (used in **Singleton**); expose a `static` method to create/return the object.

### Constructor chaining

- `this(...)`: call another constructor **in the same class**.
- `super(...)`: call **parent's** constructor. A child constructor always calls the parent's first; Java inserts `super()` implicitly if you don't. If the parent has only a parameterised constructor, you **must** call `super(args)` explicitly.

```java
Calculation(){ this(10); }
Calculation(int id){ this("sj", id); }
Calculation(String name, int id){ this.name = name; this.empID = id; }
```

---

## 6. Java Memory Management

JVM manages two memories: **Stack** and **Heap**.

**Stack**: per-thread; holds primitives, method frames, and **references** to heap objects (strong/weak/soft). Variables live only within their scope and are removed in **LIFO** order as scope ends. Full → `StackOverflowError`. **Heap**: holds objects; no ordering; **shared by all threads**; cleaned by **Garbage Collector (GC)** (Mark & Sweep).

**Walkthrough**: in `main`, `int p=10; Person personObj=new Person(); String s="24"; MemoryManagement memObj=new MemoryManagement(); memObj.test(personObj);`. `p` sits on the stack; `personObj`/`memObj` are stack references to heap objects; `"24"` goes into the string pool. When `test()` is called it gets its own stack block (copy of reference `personObj2`, `stringLiteral2="24"`, `stringLiteral3 = new String("24")`). On `}` of `test()` its block is popped; then `main`'s block is popped. Heap objects remain, now **unreferenced**, so GC removes them. GC runs periodically (JVM decides); `System.gc()` is only a hint; GC frequency rises as heap fills. **⚠** Literal → pool; `new String("24")` → separate heap object (the notebook's diagram has these arrows swapped).

### Escape analysis

"Objects go on the heap" is the language model; the **JIT compiler** can do better. **Escape analysis** checks whether an object created in a method can be seen **outside** that method or thread:

- **No escape**: object used only inside the method → candidate for optimisation.
- **Method escape**: returned, or passed to another method that stores it.
- **Thread escape**: stored in a static/shared field, visible to other threads.

If an object **doesn't escape**, the JIT (HotSpot C2) can:

1. **Scalar replacement**: skip creating the object; keep its fields as local variables in registers/stack. (HotSpot doesn't allocate whole objects on the stack; it breaks them into fields.)
2. **Lock elision**: remove `synchronized` on an object no other thread can see.

```java
int distance(int x, int y) {
  Point p = new Point(x, y);      // never leaves this method
  return p.x * p.x + p.y * p.y;   // JIT can turn this into plain ints: no heap allocation, no GC work
}
```

Enabled by default (`-XX:+DoEscapeAnalysis`). Benefit: fewer allocations → less GC pressure. It only kicks in for hot, JIT-compiled code, so don't write code relying on it.

### Reference types

- **Strong**: normal reference (`Person p = new Person();`); GC never collects while it exists.
- **Weak**: `WeakReference<Person> w = new WeakReference<>(new Person());`. Collected at the next GC even if referenced; returns `null` afterward.
- **Soft**: like weak, but collected **only when memory is short**.
- Re-pointing: `obj1 = obj2;` → obj1's old object becomes unreferenced → GC'd.

### Heap structure

**Young Generation** (Eden + S0 + S1 survivors), **Old Generation**, plus non-heap **Metaspace**. Where everything sits in the process's memory:

```
Operating System RAM
│
├── JVM Heap
│    ├── Young Generation
│    └── Old Generation
│
├── Metaspace          ← native memory
├── Thread Stacks      ← native memory
├── Code Cache         ← native memory
└── Other JVM native structures
```

So `-Xmx` limits only the **heap**; the Java process uses more RAM than that (metaspace, one stack per thread, JIT-compiled code in the code cache, GC bookkeeping, direct buffers…).

**Minor GC** (young gen, fast, frequent):

1. New objects are created in **Eden**.
2. GC **marks** unreferenced objects, **sweeps** them, moves survivors to S0 or S1 with **age +1**.
3. Next run: Eden + current survivor space are scanned; survivors go to the *other* survivor space with age +1. S0/S1 alternate; one is always empty.
4. When age reaches the **threshold** (e.g. 3), the object is **promoted to Old Generation**.

**Major GC** (old gen): same idea but runs less often; old gen holds big, long-lived, heavily-referenced objects. **Metaspace**: class variables, class metadata (info about classes), constants. Before Java 8 this was fixed-size **PermGen** (→ out-of-memory when full); Metaspace is separate from heap and **expandable**. *(Notebook mentions Java 7 in one place and Java 8 in another; the switch happened in Java 8.)*

### GC algorithms

- **Mark & Sweep**: mark unreferenced, delete them.
- **Mark & Sweep + Compaction**: after deletion, survivors are packed into contiguous blocks, leaving a contiguous free block.
- **Serial GC**: single GC thread for minor + major; slow, and all app threads pause ("stop-the-world").
- **Parallel GC**: multiple GC threads (based on CPU); shorter pauses. **Java 8 default.**
- **CMS (Concurrent Mark & Sweep)**: runs concurrently with app threads (best effort, no guarantee); **no compaction**.
- **G1**: improved CMS; tries not to pause the app, **supports compaction**. Newer Java versions use CMS/G1 → minimal pause, higher throughput, lower latency. **⚠** G1 is the **default since Java 9**; CMS was deprecated in Java 9 and **removed in Java 14**.
- **ZGC / Shenandoah**: newer low-pause collectors; do almost all work concurrently, pauses typically around a millisecond, regardless of heap size.

### GC throughput vs latency

- **Throughput** = % of total time the app spends doing *its own work* (not GC). E.g. 99% throughput → 1% of time in GC. Measures **how much work gets done overall**.
- **Latency** (pause time) = how long the app is **frozen** during a single GC pause. Measures **how responsive** the app is at any moment.
- **Footprint** = extra memory the GC needs. You can usually optimise two of the three, not all.

**The trade-off**: concurrent collectors cut pauses by running alongside the app, but they cost extra CPU and bookkeeping, so total throughput drops. Stop-the-world collectors are more efficient overall but freeze the app for longer at a time. *Analogy: cleaning a restaurant. Closing it for an hour at night (throughput) is efficient; cleaning while customers eat (latency) keeps it open but costs more effort.*

| Goal | Workload examples | Prefer |
| --- | --- | --- |
| **Throughput** | batch jobs, ETL, report generation, data processing, offline computation: nobody waits on a single request | **Parallel GC** (`-XX:+UseParallelGC`) |
| **Latency** | web APIs, trading systems, games, user-facing services: a long pause = slow response / timeout | **G1** (balanced default, `-XX:MaxGCPauseMillis`), or **ZGC / Shenandoah** for very low pauses on large heaps |

Rule of thumb: **batching → throughput → Parallel GC; user waiting → latency → G1/ZGC.**

### Releasing resources: `finalize()` vs try-with-resources

GC frees **memory** only. Files, sockets, DB connections are **OS resources** and must be closed explicitly.

**`finalize()`** (method in `Object`): the GC *may* call it on an object before reclaiming it. Problems:

- **No guarantee when, or even whether, it runs** (the program can exit first).
- Slows GC: such objects need an extra GC cycle to be collected.
- An exception thrown inside it is silently ignored.
- The object can be "resurrected" (finalize stores `this` somewhere).

Hence **deprecated in Java 9** and **deprecated for removal in Java 18**. Never use it for cleanup. Alternatives: **try-with-resources** (normal case) or `java.lang.ref.Cleaner` (safety net).

**try-with-resources** (Java 7): resources declared in `try(...)` are **closed automatically** when the block ends, normally or via exception. The resource must implement **`AutoCloseable`** (or `Closeable`).

```java
try (FileReader fr = new FileReader("a.txt");
     BufferedReader br = new BufferedReader(fr)) {
    System.out.println(br.readLine());
} catch (IOException e) {
    e.printStackTrace();
}   // br.close() then fr.close() called automatically
```

- Resources are closed in **reverse order** of declaration.
- If the body throws and `close()` also throws, the body's exception is thrown; the close exception is attached as **suppressed** (`e.getSuppressed()`). With old try/finally, the close exception would *replace* the real one.
- Java 9+: an existing **effectively final** variable can be used directly: `try (br) { ... }`.
- Replaces the verbose `finally { if (br != null) br.close(); }` pattern.

---

## 7. Classes in Java: Types

Concrete, Abstract, Super/Sub, Object, Nested (Inner, Anonymous, Member, Local, Static nested), Generic, POJO, Enum, Final, Singleton, Immutable, Wrapper.

**Concrete class**: instantiable with `new`; all methods implemented; may extend an abstract class / implement an interface; access modifier `public` or package-private.

**Abstract class** (0–100% abstraction): declared with `abstract`; can't be instantiated; can have both abstract and normal methods; **can have constructors** (called via `super()` from children); use when children share common features.

```java
abstract class Car { int mileage; Car(int m){mileage=m;}
  abstract void pressBreak(); abstract void pressClutch();
  int getNumberOfWheels(){ return 4; } }
abstract class LuxuryCar extends Car { LuxuryCar(int m){super(m);}
  abstract void pressDualBreakSystem(); void pressBreak(){/*impl*/} }
class Audi extends LuxuryCar { /* must implement remaining abstract methods */ }
```

**Super/Sub class**: every class implicitly extends **`Object`** (top of hierarchy; has `clone()`, `toString()`, `equals()`, `notify()`, `wait()`, `getClass()`…). So an `Object` reference can hold any object: `Object o = new Audi(10); o.getClass()` → `class Audi`.

### Nested classes

Class inside another. Use when a class is used by only one other class, and to group related classes. Scope = outer class's scope. Can use any access modifier.

**Static nested**: no access to outer's instance members (only static); can be created **without** an outer object: `OuterClass.NestedClass n = new OuterClass.NestedClass();`. A *private* nested class can only be instantiated inside the outer class (expose through an outer method like `display()`).

**Inner (non-static) / Member inner**: can access all outer members; needs an outer object: `OuterClass.InnerClass in = outerObj.new InnerClass();`

**Local inner**: declared inside a block/method/loop; default access only; usable only within that block; can access outer fields and the method's local variables.

**Inheritance with nested classes**

- Inner extends another inner (same outer): fine.
- Static nested extended by an unrelated class: `class X extends Outer.Nested { }`.
- Non-static inner extended by an unrelated class: the child constructor must supply the outer instance: `Outer.Inner` parent needs `new Outer().super();` in the child constructor.

**Anonymous inner class**: nameless; used to override behaviour without creating a subclass:

```java
Car audi = new Car() { @Override public void pressBreak(){ System.out.println("Audi specific"); } };
```

Behind the scenes the compiler creates a subclass (e.g. `Test$1 extends Car`) and instantiates it. Works the same for interfaces. (Not a violation of "can't instantiate abstract class".)

---

## 8. Generic Classes

*Analogy: a box labelled `<T>`; you decide at creation what it holds, and the compiler enforces it.* Avoids typecasting you'd need with `Object`.

Without generics: `Object value;` → `(int) obj.getPrintValue()` cast required. With generics:

```java
class Print<T> { T value; T getPrintValue(){return value;} void setPrintValue(T v){value=v;} }
Print<Integer> p = new Print<Integer>(); p.setPrintValue(1);
Integer v = p.getPrintValue();   // no cast
```

`T` can be any (non-primitive) object type. Any letters work (A, B, C…).

**Inheritance**

- **Non-generic subclass**: fix T when extending: `class ColorPrint extends Print<String>`; `new ColorPrint().setPrintValue("2")`.
- **Generic subclass**: `class ColorPrint<T> extends Print<T>`; specify at creation: `ColorPrint<String> c = new ColorPrint<>();`.

**Multiple types**: `class Pair<K,V>`; `Pair<String,Integer> p = new Pair<>(); p.put("hello",1243);` (diamond `<>` lets compiler infer).

**Generic method**: type parameter goes **before the return type**, scope limited to the method: `public <K,V> void printValue(Pair<K,V> a, Pair<K,V> b)`, `public <T> void setValue(T obj)`.

**Raw type**: generic class used without type argument (`Print raw = new Print();`); internally `Object` is used, so it accepts anything (loses type safety).

**Bounded generics**

- **Upper bound**: `<T extends Number>`; T must be Number or a subclass (also works with interfaces). `Print<Integer>` ✓, `Print<String>` ✗.
- **Multi-bound**: `<T extends ParentClass & Interface1 & Interface2>`; the **class must come first**, the rest are interfaces.

**Wildcards** (`?`) Why: `List<Bus>` is **not** a `List<Vehicle>` even though Bus extends Vehicle (`vehicleList = busList` ✗ and `busList = vehicleList` ✗). Wildcards fix that.

- **Upper bounded** `<? extends Vehicle>`: Vehicle and its children (accepts `List<Vehicle>`, `List<Bus>`).
- **Lower bounded** `<? super Vehicle>`: Vehicle and its parents (accepts `List<Vehicle>`, `List<Object>`).
- **Unbounded** `<?>`: for methods that only use `Object`'s methods (read-only use).

**Wildcard vs generic method**: wildcards are less restrictive. `computeList(List<? extends Number> src, List<? extends Number> dst)` accepts `List<Integer>` + `List<Float>`; `<T extends Number> computeList1(List<T> src, List<T> dst)` needs the **same** type for both (compile error otherwise). Wildcards can use `super`; generic methods can't.

**PECS rule: Producer `extends`, Consumer `super`.** Decides which wildcard to use for a parameter:

- If the collection **produces** values for you (you **read** from it) → `? extends T`. You can read items as `T`, but **can't add** (except `null`): the compiler doesn't know whether it's a `List<Integer>` or `List<Double>`.
- If the collection **consumes** values from you (you **write** into it) → `? super T`. You can **add** `T` safely, but reading only gives you `Object`.
- Both read and write → no wildcard, use exact `List<T>`.

```java
// src produces T's, dest consumes T's (this is how Collections.copy is declared)
static <T> void copy(List<? super T> dest, List<? extends T> src) {
    for (T item : src) dest.add(item);
}

List<Integer> ints = List.of(1, 2, 3);
List<Number>  nums = new ArrayList<>();
copy(nums, ints);   // ✓ Integer list produces, Number list consumes

List<? extends Number> ro = ints;  Number n = ro.get(0);  // ✓ read
// ro.add(5);                                             // ✗ compile error
List<? super Integer> wo = nums;   wo.add(5);             // ✓ write
Object o = wo.get(0);                                     // read only as Object
```

**Type erasure**: generics exist only at compile time; in bytecode `T` is replaced by `Object` (or by its bound: `T extends Number` → `Number`, `T extends Bus` → `Bus`). Applies to generic classes and generic methods alike.

**Generics + varargs → heap pollution**

```
Generics + varargs
        ↓
Generic array jaisa structure
        ↓
Type erasure
        ↓
Runtime exact generic type nahi jaanta
        ↓
Wrong generic object ghus sakta hai
        ↓
Heap pollution

@SafeVarargs =
"I promise this method aisa unsafe kaam nahi karega."
```

```java
static void unsafe(List<String>... lists) {   // really a List[] at runtime
    Object[] arr = lists;
    arr[0] = List.of(42);                      // no error: runtime only sees List
    String s = lists[0].get(0);                // ClassCastException, far from the real bug
}
```

**Heap pollution** = a variable of a parameterised type (`List<String>`) points to an object that isn't actually that type. The compiler warns "possible heap pollution" on such methods. Add **`@SafeVarargs`** only if the method **doesn't write into the varargs array or leak it**; it's allowed on `static`, `final` and `private` methods (private since Java 9) and constructors, i.e. methods that can't be overridden.

```java
@SafeVarargs
static <T> List<T> listOf(T... items) { return new ArrayList<>(Arrays.asList(items)); } // only reads: safe
```

---

## 9. POJO Class

"Plain Old Java Object": **public** class, **public default constructor**, private/protected fields with **getters & setters**, **no annotations** (`@Entity`, `@Table`, `@Id`…), **doesn't extend a class or implement an interface**.

```java
public class Student { private int rollNumber; private String name; private String address;
  /* getX()/setX() for each */ }   // ⚠ notebook declares "int name": should be String
```

Why: map incoming request data to a POJO that all other classes understand; if the format changes, only the POJO changes.

---

## 10. Enum Classes

**Properties**: a fixed collection of **constants** (implicitly `public static final`); can't extend a class (internally extends `java.lang.Enum`) but **can implement interfaces**; can have variables, constructors, methods; **can't be instantiated** (constructor is always private); nothing can extend an Enum; can have abstract methods that **every constant must implement**.

**Basic**

```java
enum EnumSample { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
```

By default each constant gets an **ordinal** starting from 0 (MONDAY=0 … SUNDAY=6), only when you don't define custom values.

- `values()`: array of all constants. `ordinal()`: its index. `valueOf("FRIDAY")`: constant matching the exact string. `name()`: constant's name (`"FRIDAY"`).

```java
for (EnumSample s : EnumSample.values()) System.out.println(s.ordinal()); // 0..6
EnumSample.valueOf("FRIDAY").name(); // FRIDAY
```

**Custom values**: each constant is effectively an *object* of the enum with its own field values; you need a **parameterised constructor** invoked for every constant.

```java
enum EnumWithCustomValues {
  MONDAY(101,"1st Day Of the Week"), TUESDAY(102,"2nd Day Of the Week") /* … SUNDAY(107,…) */;
  private int value; private String comment;
  EnumWithCustomValues(int value, String comment){ this.value=value; this.comment=comment; }
  int getValue(){return value;}  String getComment(){return comment;}
  static EnumWithCustomValues getEnumFromValue(int v){      // enum-wide lookup must be static
    for (EnumWithCustomValues e : values()) if (e.value == v) return e;  return null; }
}
getEnumFromValue(107).getComment(); // "7th Day Of the Week"
MONDAY.getValue();                  // 101
```

A **non-static** method applies to every constant; a **static** one belongs to the whole enum.

**Method override by constant**: a method defined in the enum is the default for all constants; any constant can override it with its own body.

```java
enum E { MONDAY { @Override void dummyMethod(){ print("Monday Dummy Method"); } }, TUESDAY, WEDNESDAY;
         void dummyMethod(){ print("Default Dummy Method"); } }
E.MONDAY.dummyMethod();  // Monday Dummy Method
E.TUESDAY.dummyMethod(); // Default Dummy Method
```

**Enum with abstract method**: declare `abstract void dummyMethod();` in the enum; **every constant must implement it** (each with its own body).

**Enum implements interface**

```java
interface MyInterface { String toLowerCase(); }
enum EnumImplementInterface implements MyInterface { MONDAY, TUESDAY /*…*/;
  public String toLowerCase(){ return this.name().toLowerCase(); } }
EnumImplementInterface.MONDAY.toLowerCase(); // "monday"
```

**Why enum over `static final int` constants?**

```java
static final int MONDAY=0 … SUNDAY=6;       isWeekend(int day)        // isWeekend(100) compiles!
enum EnumSample {…}                         isWeekend(EnumSample day) // only valid constants allowed
```

Enum gives **better readability** and **control over which values can be passed** (type safety).

---

## 11. Final Class

A class that **cannot be inherited**: `public final class TestClass {}`; `class X extends TestClass` → compile error "Cannot inherit from final".

**`final` on a reference variable ≠ immutable object**

```java
final List<String> list = new ArrayList<>();
list.add("a");                 // ✓ allowed: the list's contents change
list.remove("a");              // ✓ allowed
list = new ArrayList<>();      // ✗ compile error: can't re-point a final variable
```

> `final` means the variable `list` cannot point to another list. The `ArrayList` itself is still mutable.

For a truly unmodifiable list use `List.of(...)` or `Collections.unmodifiableList(list)` (the latter is a read-only *view*; changes to the original still show through).

---

## 12. Singleton Class *(pages 85–86, completed beyond the notebook)*

**Goal**: only **one object** of the class ever exists (e.g. a DB connection). Ways: Eager init · Lazy init · Synchronized block · Double-check locking (memory-visibility issue, fixed with a `volatile` instance variable) · Bill Pugh solution · Enum singleton.

**Eager initialisation**

```java
public class DBConnection {
  private static DBConnection conObject = new DBConnection(); // 1. private static, created once
  private DBConnection() {}                                   // 2. private constructor: no `new` from outside
  public static DBConnection getInstance() { return conObject; } // 3. public access point
}
DBConnection c = DBConnection.getInstance();
```

Downside: object is created as soon as the program starts, even if never used.

**Lazy initialisation** (fixes that): create only on first call, reuse afterwards.

```java
private static DBConnection conObject;
public static DBConnection getInstance() {
  if (conObject == null) { conObject = new DBConnection(); }
  return conObject;
}
```
Problem: **not thread-safe**. Two threads can both see `conObject == null` and both create an object.

**Synchronized method**: `public static synchronized DBConnection getInstance()`. Safe, but **every** call takes the lock, even after the object exists → slow.

**Double-check locking**: lock only while the object is being created.

```java
private static volatile DBConnection conObject;     // volatile is required
public static DBConnection getInstance() {
  if (conObject == null) {                          // 1st check: no lock (fast path)
    synchronized (DBConnection.class) {
      if (conObject == null) {                      // 2nd check: another thread may have created it
        conObject = new DBConnection();
      }
    }
  }
  return conObject;
}
```

Without `volatile`, instructions can be **reordered**: the reference may be assigned before the constructor finishes, so another thread sees a non-null but half-built object. `volatile` also ensures the write is visible to all threads (not just cached in one CPU core).

**Bill Pugh solution**

Bill Pugh Singleton uses a static inner class to achieve lazy initialization and thread safety without explicit synchronization.

```java
public class DBConnection {
  private DBConnection() {}
  private static class Holder {                       // not loaded until first used
    private static final DBConnection INSTANCE = new DBConnection();
  }
  public static DBConnection getInstance() { return Holder.INSTANCE; }
}
```

```
Singleton class loaded
       ↓
INSTANCE not created yet

getInstance() called for first time
       ↓
Holder class gets loaded
       ↓
INSTANCE = new Singleton()
       ↓
JVM guarantees only one thread initializes it
```

**Enum singleton**

```java
public enum DBConnection {
  INSTANCE;
  public void connect() { /* ... */ }
}
DBConnection.INSTANCE.connect();
```

**Why creation is thread-safe**: enum constants are `public static final` fields created in the enum's **static initialiser**. The JVM runs a class's static initialisation **exactly once**, holding an initialisation lock: if several threads touch the enum at the same moment, one initialises it and the others wait. So `INSTANCE` is created once, with no `synchronized`/`volatile` code from you. (Only *creation* is thread-safe; if its methods change shared fields, those still need synchronisation.) Downsides: not lazy in the Bill Pugh sense (created when the enum is first used), and can't extend another class.

### Breaking a singleton (and fixing it)

**1. Reflection**: reflection can call the private constructor.

```java
DBConnection one = DBConnection.getInstance();
Constructor<DBConnection> c = DBConnection.class.getDeclaredConstructor();
c.setAccessible(true);                 // bypasses `private`
DBConnection two = c.newInstance();    // second object! one != two
```

*Fix*: throw from the constructor if an instance already exists (works for eager and Bill Pugh), or use an **enum**: the JVM refuses `newInstance()` on enums with `IllegalArgumentException: Cannot reflectively create enum objects`.

```java
private DBConnection() {
  if (conObject != null) throw new IllegalStateException("Use getInstance()");
}
```

**2. Serialization**: if the singleton implements `Serializable`, deserialization builds a **new object** without calling the constructor.

```java
out.writeObject(DBConnection.getInstance());
DBConnection copy = (DBConnection) in.readObject();  // copy != original
```

*Fix*: add `readResolve()`; Java calls it after deserialising and uses its return value instead of the new object. Also mark instance fields `transient`.

```java
protected Object readResolve() { return getInstance(); }
```

An **enum** needs no fix: enums are serialised by name and deserialised via `valueOf()`, so the same constant comes back.

**Takeaway**: the **enum singleton** is thread-safe in creation and safe against reflection and serialization by default, which is why *Effective Java* calls it the best way to implement a singleton when you don't need lazy loading or inheritance.
