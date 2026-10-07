
## 1. Interface *(pp. 92–93)*

**What it is**: lets 2 systems interact without either knowing the other's internals. In short, it gives **abstraction**.

**Declaration** = modifiers + `interface` keyword + name + comma-separated parent interfaces + body. Only **`public` or default (package-private)** are allowed at the top level. `protected` and `private` are not.

```java
public interface NonFlyingBirds extends Bird, LivingThings { void canRun(); }
```

**⚠** The notebook says an interface "can extend from *Class*". It can't. An interface can extend **only other interfaces**, and it can extend several of them.

### Why we need interfaces

1. **Abstraction**: an interface defines *what* a class must do, not *how*. `class Eagle implements Bird { public void fly(){ /*complex logic*/ } }`
2. **Polymorphism**: an interface works as a data type. You can't do `new Bird()`, but a `Bird` reference can hold any implementing object, and the method that runs is decided at runtime.
   ```java
   Bird b1 = new Eagle(); Bird b2 = new Hen();
   b1.fly(); b2.fly();   // Eagle's fly, Hen's fly
   ```
3. **Multiple inheritance**: in Java it's possible *only* through interfaces. `class Crocodile extends LandAnimal, WaterAnimal` fails because of the diamond problem: which `canBreathe()` would run? With interfaces, `Crocodile implements LandAnimal, WaterAnimal` works because Crocodile itself provides the single implementation.

### Members

- **Methods**: implicitly `public abstract`. They can't be `final`.
- **Fields**: implicitly `public static final`, which makes them constants. They can't be `private`. `int MAX_HEIGHT_IN_FEET = 2000;` is the same as `public static final int MAX_HEIGHT_IN_FEET = 2000;`

### Implementation rules

- An overriding method **can't have a more restrictive access modifier**. Writing `protected void fly()` for an interface's `fly()` is a compile error.
- A **concrete** class must override every abstract method.
- An **abstract** class doesn't have to override them all. Its concrete child must implement whatever is left.
- A class can implement **multiple** interfaces.

```java
public abstract class Eagle implements Bird {          // Bird: canFly(), noOfLegs()
  public void canFly(){ }  public abstract void beakLength(); }
public class WhiteEagle extends Eagle {
  public void noOfLegs(){ }   // interface method
  public void beakLength(){ } // abstract-class method
}
```

### Nested interface

A nested interface is declared inside another interface or inside a class. Its purpose is to group logically related interfaces.

- If it's nested in an **interface**, it must be `public` (implicitly).
- If it's nested in a **class**, it can have any access modifier.
- Implementing the outer interface doesn't require implementing the inner one, and the reverse is also true.

```java
public interface Bird { void canFly(); interface NonFlyingBird { void canRun(); } }
class Eagle implements Bird.NonFlyingBird { public void canRun(){} }
Bird.NonFlyingBird obj = new Eagle(); obj.canRun();
```

### Interface vs Abstract Class

| Abstract Class | Interface |
| --- | --- |
| keyword `abstract`; child uses `extends` | keyword `interface`; child uses `implements` |
| Abstract + non-abstract methods | Only abstract methods (Java 8+: also `default`/`static`; Java 9+: also `private`) |
| Extends one class + implements many interfaces | Extends only other interfaces |
| Variables: static, non-static, final, non-final | Variables are constants by default |
| Members can be private/protected/public/default | Members `public` by default (Java 9: private methods allowed) |
| Multiple inheritance **not** supported | Multiple inheritance supported |
| Can implement an interface | Can't implement another interface or an abstract class |
| **Has a constructor** | **No constructor** |
| `abstract` keyword needed for abstract methods | No keyword needed; abstract and public by default |

---

## 2. Java 8 / 9 Interface Features *(p. 93)*

### Default method (Java 8)

Before Java 8, adding a method to an interface broke **every** implementing class. A `default` method has a body, so existing implementations keep compiling. *Why it was introduced: to add features to legacy interfaces, e.g. `stream()` in Collection.*

```java
interface Bird { void canFly(); default int getMinimumFlyHeight(){ return 100; } }
new Eagle().getMinimumFlyHeight(); // 100, Eagle didn't override it
```

**Default + multiple inheritance**: say `Bird` and `LivingThing` both have `default boolean canBreathe()`. Then `class Eagle implements Bird, LivingThing {}` fails ✗. Eagle **must override** `canBreathe()` ✓, and it can pick one parent's version with `Bird.super.canBreathe()`.

**Extending an interface that has a default method**, three ways:

1. **Inherit as is**: `interface Bird extends LivingThing {}` → Eagle gets `canBreathe()` for free.
2. **Make it abstract again**: `interface Bird extends LivingThing { boolean canBreathe(); }` → Eagle *must* implement it.
3. **Override it in the child interface**:
   ```java
   interface Bird extends LivingThing {
     default boolean canBreathe(){ boolean r = LivingThing.super.canBreathe(); /*add more*/ return r; } }
   ```

### Static method (Java 8)

- A static method has its body in the interface. It's `public` by default and is called via the **interface name**: `Bird.canBreathe()`.
- It **can't be overridden**. If an implementing class writes the same method, that's just a new method of the class. Adding `@Override` to it gives a compile error.

### Private & private static methods (Java 9)

- Private methods let multiple default methods share common code, which helps readability.
- They can't be abstract, so they must have a body. They're usable only inside that interface.
- A **static** method can call only **private static** methods. A non-static method (default or private) can call both static and non-static methods.

```java
interface Bird {
  void canFly();                           // = public abstract
  default void minimumFlyingHeight(){ myStaticPublicMethod(); myPrivateMethod(); myPrivateStaticMethod(); }
  static void myStaticPublicMethod(){ myPrivateStaticMethod(); } // static → only private static
  private void myPrivateMethod(){ }
  private static void myPrivateStaticMethod(){ }
}
```

---

## 3. Functional Interface & Lambda *(p. 94)*

**Functional interface** = an interface with **exactly one abstract method**, also called a **SAM** (Single Abstract Method) interface. The `@FunctionalInterface` annotation is optional. If you use it, the compiler errors when a second abstract method is added.

A functional interface *can still* have `default` methods, `static` methods, and abstract methods that come from `Object`, like `String toString();`. Those don't count toward the one.

### Three ways to implement one

```java
@FunctionalInterface interface Bird { void canFly(String val); }
// 1. implements
class Eagle implements Bird { public void canFly(String v){ print("Eagle Bird Implementation"); } }
// 2. anonymous class
Bird eagle = new Bird(){ public void canFly(String v){ print("Eagle Bird Implementation"); } };
// 3. lambda: the way to implement a functional interface
Bird eagle = (String value) -> { print("Eagle Bird Implementation"); };
eagle.canFly("vertical");
```

### Built-in types (`java.util.function`)

| Type | Abstract method | In → Out | Example |
| --- | --- | --- | --- |
| **Consumer\<T>** | `void accept(T t)` | 1 → none | `Consumer<Integer> log = v -> { if (v>10) print("Logging"); };` |
| **Supplier\<T>** | `T get()` | none → 1 | `Supplier<String> s = () -> "this is the data i am returning";` |
| **Function\<T,R>** | `R apply(T t)` | 1 → result | `Function<Integer,String> f = n -> n.toString();` |
| **Predicate\<T>** | `boolean test(T t)` | 1 → boolean | `Predicate<Integer> isEven = v -> v % 2 == 0;` |

### When a functional interface extends another interface

1. **FI extends a non-FI**: if the parent has an abstract method, the child now has 2 abstract methods ✗. It's fine if the parent's method is `default` ✓.
2. **Normal interface extends an FI**: fine. It's just no longer functional, which is allowed because it has no `@FunctionalInterface`.
3. **FI extends an FI**: the child adding a *different* abstract method gives 2 ✗. Re-declaring the *same* method (`boolean canBreathe();`) keeps 1 ✓, so `Bird b = () -> true;` works.

---

## 4. Reflection *(p. 95)*

**Reflection** lets you examine and modify classes, methods, fields, and interfaces **at runtime**. Examples: listing methods, fields, return types, modifiers, and implemented interfaces, or changing even **private** field values.

### The `Class` class

The JVM creates **one `Class` object per loaded class**. It holds that class's metadata (methods, fields, constructors). There are 3 ways to get it:

```java
Class c1 = Class.forName("Bird");   // 1
Class c2 = Bird.class;              // 2
Class c3 = new Bird().getClass();   // 3
c2.getName(); Modifier.toString(c2.getModifiers()); // "Bird", "public"
```

All the `Class` methods are getters (no setters): `getName`, `getModifiers`, `getMethods`, `getDeclaredMethods`, `getFields`, `getDeclaredFields`, `getConstructors`, `getDeclaredConstructors`, `getInterfaces`, `getSuperclass`, `getPackage`, and so on. Their return types (`Method`, `Field`, `Constructor`) come from **`java.lang.reflect`**.

### Methods

- `getMethods()` → **public** methods only, *including inherited* ones (from Object too).
- `getDeclaredMethods()` → **public + private** methods, but only those of **this class**.

```java
for (Method m : Eagle.class.getDeclaredMethods())
  print(m.getName() + " " + m.getReturnType() + " " + m.getDeclaringClass());
```

**Invoking a method:**

```java
Class c = Class.forName("Eagle");  Object o = c.newInstance();
Method fly = c.getMethod("fly", int.class, boolean.class, String.class);
fly.invoke(o, 1, true, "hello");   // fly intParam: 1 boolParam: true strParam: hello
```

### Fields

- `getFields()` → public only. `getDeclaredFields()` → public + private.
- Useful accessors: `getName()`, `getType()`, `getModifiers()`, `get(obj)`, `getInt(obj)`, `getBoolean(obj)`, and similar.
- **Public field**: `c.getField("breed").set(obj, "eagleBrownBreed")` works.
- **Private field**: calling `set` directly throws `IllegalAccessException`. Call `setAccessible(true)` first:

```java
Field f = Eagle.class.getDeclaredField("canSwim");
f.setAccessible(true);  f.set(eagleObj, true);
f.getBoolean(eagleObj); // true
```

### Constructors

Reflection can even call a **private constructor**:

```java
for (Constructor con : Eagle.class.getDeclaredConstructors()) {
  con.setAccessible(true);
  Eagle e = (Eagle) con.newInstance();  e.fly();
}
```

*(Side note: this is why reflection can break a Singleton. An Enum singleton is the safe option.)*

---

## 5. Annotations *(p. 96)*

An annotation (`@Name`) is **metadata** attached to code, and using it is optional. It can be read **at runtime** via **reflection** to drive logic. It can be applied to classes, methods, interfaces, fields, parameters, and more.

**Types**

- **Pre-defined, used on Java code**: `@Deprecated`, `@Override`, `@SuppressWarnings`, `@FunctionalInterface`, `@SafeVarargs`
- **Pre-defined meta-annotations** (used on annotations): `@Target`, `@Retention`, `@Documented`, `@Inherited`, `@Repeatable` (Java 8)
- **Custom**: declared with `@interface`

### On Java code

- **`@Deprecated`**: using the element gives a compile-time **warning**. It means "no longer improved, use the alternative". Allowed on constructors, fields, local variables, methods, packages, parameters, and types.
- **`@Override`**: the compiler checks that the method really overrides a parent method. `fly1()` marked `@Override` gives an error. Methods only.
- **`@SuppressWarnings`** (⚠ the notebook spells it "Supress"): tells the compiler to ignore warnings, e.g. `@SuppressWarnings("deprecation")`, `("unused")`, `("all")`. It can go on a method or a class. Use it carefully, because a suppressed valid warning can turn into a runtime exception.
- **`@FunctionalInterface`**: restricts the interface to 1 abstract method. Types only.
- **`@SafeVarargs`**: suppresses the "heap pollution" warning on **varargs** methods and constructors. The method must be `static` or `final` (Java 9+: also `private`).
  - **Heap pollution**: a variable of one type holding a reference to another type. Example: `Object[] arr = listOfIntegers; arr[0] = listOfStrings;`

### Meta-annotations

- **`@Target`**: restricts where the annotation can be used. `@Target(ElementType.METHOD) @interface Override`. ElementTypes: `TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER` (generics `<T>`), `TYPE_USE` (Java 8, any type usage like `List<@Ann String>`).
- **`@Retention`**: how long the annotation is kept.

| Policy | Kept in `.class`? | Available at runtime? |
| --- | --- | --- |
| `SOURCE` | No, discarded by the compiler | No |
| `CLASS` (default) | Yes | No, ignored by the JVM |
| `RUNTIME` | Yes | **Yes**, readable via reflection |

- **`@Documented`**: by default annotations don't appear in generated Javadoc. With this, they do.
- **`@Inherited`**: makes a class-level annotation pass to child classes. `ChildClass.class.getAnnotation(...)` returns the annotation with `@Inherited`, and `null` without it. It has no effect anywhere other than on classes.
- **`@Repeatable`** (Java 8): lets you use the same annotation more than once in one place. It needs a **container** annotation:

```java
@Repeatable(Categories.class) @Retention(RUNTIME) @interface Category { String name(); }
@Retention(RUNTIME) @interface Categories { Category[] value(); }

@Category(name="Bird") @Category(name="LivingThing") @Category(name="carnivorous")
class Eagle {}
for (Category c : new Eagle().getClass().getAnnotationsByType(Category.class)) print(c.name());
// Bird, LivingThing, carnivorous
```

### Custom annotations

```java
public @interface MyCustomAnnotation { }                                    // empty body
public @interface MyCustomAnnotation { String name(); }                     // @MyCustomAnnotation(name="testing")
public @interface MyCustomAnnotation { String name() default "hello"; }     // @MyCustomAnnotation alone is OK
```

Rules for elements: they take no parameters and have no body. Return types are limited to primitives, `Class`, `String`, enums, annotations, and arrays of these. A **default value can't be `null`**.

---

## 6. Exception Handling *(pp. 97–98)*

An **exception** is an event during execution that disrupts the normal flow. Java creates an **exception object** (type, message, stack trace). The runtime then walks back up the call stack (method3 → method2 → method1 → main) looking for a handler. If none is found, the program **terminates abruptly** and prints the stack trace.

### Hierarchy

```
Object → Throwable ─┬─ Error (OutOfMemoryError, StackOverflowError)
                    └─ Exception ─┬─ Unchecked / Runtime: ClassCast, Arithmetic, IndexOutOfBounds
                                  │     (→ ArrayIndexOutOfBounds, StringIndexOutOfBounds), NullPointer,
                                  │     IllegalArgument (→ NumberFormat)
                                  └─ Checked / Compile-time: ClassNotFound, Interrupted,
                                        IO (→ FileNotFound, EOF, Socket), SQL
```

(⚠ The notebook writes "IndexOutOfBound**Exception**". The real names are `IndexOutOfBoundsException` and `ArrayIndexOutOfBoundsException`.)

**Unchecked** exceptions happen at runtime, and the compiler doesn't force you to handle them:

| Exception | Trigger |
| --- | --- |
| ClassCast | `Object v = 0; (String) v;` → Integer can't be cast to String |
| Arithmetic | `5 / 0` → "/ by zero" |
| ArrayIndexOutOfBounds | `new int[2][3]` |
| StringIndexOutOfBounds | `"hello".charAt(5)` |
| NullPointer | `String v = null; v.charAt(0);` |
| NumberFormat | `Integer.parseInt("abc")` |

**Checked** exceptions are verified at compile time. `throw new ClassNotFoundException()` without handling gives "unreported exception… must be caught or declared to be thrown".

### Handling: try, catch, finally, throw, throws

- **`throws`**: "this method *might* throw this, caller handle it". The caller then has to `try/catch` it or declare `throws` itself.
- **try/catch**: `try` holds risky code and must be followed by `catch` or `finally`. You can have multiple `catch` blocks, but a catch can only name exceptions the try block can actually throw. Order goes **specific → general**: putting `catch(Exception)` before `catch(ClassNotFoundException)` gives the error "already caught". You can catch several in one block: `catch (ClassNotFoundException | InterruptedException e)`.
- **finally**: allowed after try or after catch, and at most **one**. It **always runs**, even after a `return` in try or catch. Used for closing resources, logging, and similar. It does **not** run if the JVM dies (out of memory, shutdown, process killed).
- **`throw`**: throws a new exception or **re-throws** a caught one: `catch (ClassNotFoundException e) { throw e; }`

### Custom exception

```java
public class MyCustomException extends Exception {
  MyCustomException(String msg){ super(msg); } }
static void method1() throws MyCustomException { throw new MyCustomException("some issue arise"); }
```

### Why handle exceptions?

- Error handling is kept separate from regular code. Compare that with nested `if` checks returning error codes like `-1/-2/-3`.
- The program can recover from the error.
- You can add extra debugging info.
- It improves security by hiding sensitive details.

**But** exception handling is expensive when stack traces are deep or handled far up. Avoid it when a plain check works: `if (b == 0) return -1;` beats `try { a/b } catch (ArithmeticException e)`.

---

## 7. Operators in Java *(pp. 99–109)*

- **Operator**: the action to perform.
- **Operand**: the items the action is applied to.
- **Expression**: 1 or more operands with 0 or more operators. In `5 + 3`, 5 and 3 are operands, `+` is the operator, and `5 + 3` is the expression.

(⚠ The notebook says "7 categories" but goes on to list **9**.)

**1. Arithmetic**: `+ - * / %`. Integer division truncates: `5/2 = 2`, `5%2 = 1`, `3+4 = 7`, `4-3 = 1`, `3*4 = 12`.

**2. Relational**: `== != > < >= <=` return a boolean. With `a=4, b=7`: `==` false, `!=` true, `>` false, `<` true, `>=` false, `<=` true.

**3. Logical**: `&&` (true only if all conditions are true) and `||` (true if at least one is). With `a=4, b=7`: `a<3 && a!=b` → false; `a>3 && a!=b` → true; both `||` cases → true.
**Short-circuit**: `&&` stops at the first `false`. (⚠ The notebook only mentions `&&`, but `||` also stops at the first `true`.)

**4. Unary** (single operand): `++ -- - + !`

- **Postfix** `a++`: returns the current value, *then* changes it. **Prefix** `++a`: changes the value, *then* returns it.
- `!` flips a boolean.

```java
int a = 5; boolean flag = true;
a++ → 5,  ++a → 7,  a-- → 7,  --a → 5,  !flag → false,  -a → -5,  +a → 5
```

⚠ The notebook says unary `+` "makes the value positive". It doesn't: `+x` just returns `x` (`+(-5)` is `-5`). Only `-` negates.

**5. Assignment**: `= += -= *= /= %=` put the right-hand value into the left-hand variable. With `a=5`: `v=a` → 5; `v=0; v+=a` → 5; `v-=3` → 2; `v*=a` → 10; `v/=a` → 2.

**6. Bitwise**: these work on bits and are very fast. `&` AND, `|` OR, `^` XOR, `~` NOT (`~` is also unary).

| a b | `&` | `\|` | `^` |
| --- | --- | --- | --- |
| 0 0 | 0 | 0 | 0 |
| 0 1 | 0 | 1 | 1 |
| 1 0 | 0 | 1 | 1 |
| 1 1 | 1 | 1 | 0 |

With `a=4 (0100)` and `b=6 (0110)`: `a&b = 4`, `a|b = 6`, `a^b = 2`, `~a = -5`.

**How `~4` = −5**: Java ints are signed, so the **MSB is the sign bit**. `4 = 0100` and `~` flips every bit, giving `1011`. The MSB is 1, so the MSB's place value counts as negative: `−8 + 0 + 2 + 1 = −5`. Shortcut: **`~n = −(n+1)`**.
Check with 2's complement: `5 = 0101` → 1's complement `1010` → +1 → `1011` = −5 ✓. (⚠ The notebook writes "for **4** ie 0101". That should be **5**.)

**7. Shift**

- `<<` signed left shift: fills the LSB with 0. Each shift **doubles** the number.
- `>>` signed right shift: fills the MSB with the **sign bit**. Each shift **halves** the number. `11000110 >> 1 = 11100011`; `01000110 >> 1 = 00100011`.
- `>>>` unsigned right shift: fills the MSB with **0** always. `11000110 >>> 1 = 01100011`.
- With `a=4`: `a<<1 = 8`, `a<<2 = 16`, `a>>1 = 2`, `a>>2 = 1`.

⚠ The notebook says "`<<` and `<<<` are equal". **`<<<` doesn't exist in Java.** There's no need for it, because a left shift fills the LSB with 0 regardless of sign. (Also note: for negative numbers `>>` rounds toward −∞, e.g. `-5 >> 1 = -3`.)

**8. Ternary**: a compact if-else. `(condition) ? expr1 : expr2` runs expr1 if the condition is true, otherwise expr2.
`int max = (a > b) ? a : b;  // a=4, b=5 → 5`

**9. Type comparison: `instanceof`** 🔍 checks whether an object is an instance of a given class, its subclass, or an implemented interface, and returns a boolean. It returns `true` for the object's own class **and its parent classes**.

```java
Eagle e = new Eagle();
e instanceof Eagle;  // true
e instanceof Bird;   // true (parent / interface)
null instanceof Bird; // false
```

### Operator precedence 🔍 (high → low)

| Level | Operators |
| --- | --- |
| Postfix | `a++ a--` |
| Unary / prefix | `++a --a + - ~ !` |
| Multiplicative | `* / %` |
| Additive | `+ -` |
| Shift | `<< >> >>>` |
| Relational | `< > <= >= instanceof` |
| Equality | `== !=` |
| Bitwise | `&` then `^` then `\|` |
| Logical | `&&` then `\|\|` |
| Ternary | `? :` |
| Assignment | `= += -= *= /= %= …` (right-to-left) |
