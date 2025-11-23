# OOPS

## What is OOPs?

* OOP is an approach to design programs that combine both data and functions so that both can operate into a single unit. Such a unit is called an **Object**.
* It is based on the concept that systems should be constructed using reusable components known as **objects**.
* All computations are performed within the context of objects, which are instances of programming constructs referred to as **classes**.
* Classes are both **data abstractions** and **procedural abstractions** that operate on objects.
* Leverages the benefits of **modularity** and **reusability** in order to provide a solution.

**Example:**

* **Object:** Car
* **Class:** Implements logic in object — wheels, mileage, etc. (*BLUEPRINT*, not real, cannot be instantiated)

## Why Should We Study OOPS?

* **Duplicate code is considered bad practice:**
  * Duplicated code can lead to performance issues, as any changes made to duplicated code must be made in multiple places.
  * OOP provides **code reusability (inheritance)**.

* **Code will always need to be changed:**
  * OOP provides **modularity (encapsulation)** — changing code is easy.

## Advantages of OOP

* **Improved software development productivity:**

  * OOP's modularity, extensibility, and reusability.
  * Example: Modular design allows separate objects for cart management, user authentication, and order processing.
  * Extensible: Vehicle → Car, Motorcycle.

* **Improved software maintainability:**

  * Updating specific parts of the system without large-scale changes due to modular OOP design.
  * Example: Fixing a bug in payment module without affecting other modules.

* **Faster development:**

  * Reusable code.

* **Lower cost of development:**

  * Likelihood of costly errors decreases, and you don’t have to start from scratch.

* **Higher-quality software:**

  * Modularity allows testing individual components.
  * More time for testing due to reusability.

## Disadvantages of OOP

* **Learning may require time and practice:**

  * Concepts like inheritance and polymorphism.

* **Larger program size:**

  * As compared to procedural languages.

* **Slower programs (may be):**

  * Additional instructions.

* **Not suitable for all problems, performance overhead:**

  * Some problems are better suited for other paradigms (functional, logic).
  * Simpler problems may not need OOP.

# Classes and Objects

## 1. Class

* A class is a blueprint or template for creating objects.
* It defines the properties (attributes) and behaviors (methods) that the objects created from the class will have.
* A class encapsulates data for creating objects.

## 2. Object

* An object is an instance of a class.
* It represents a real-world entity and contains data (state) and behavior (methods).
* Objects are created from classes using the `new` keyword and returns a reference to it.


# Constructors & Interview Ques.

A **constructor** is a special method in Java used to **initialize objects**.
It is called **automatically** when an object is created using `new`.

### Key Properties

* Name = **same as class name**
* **No return type** (not even `void`)
* Can contain `return;` *but cannot return a value*
* Compiler adds a **default constructor** if none is written
* Runs **only once per object creation**

### Default Values in Java

Java assigns default values *before* the constructor runs:

| Type      | Default |
| --------- | ------- |
| `int`     | `0`     |
| `double`  | `0.0`   |
| `boolean` | `false` |
| `Object`  | `null`  |

### Types of Constructors

### 1. Default Constructor (Compiler Provided)
Constructor with **no arguments**, created automatically if no constructor is defined.
**Example**

```java
public class Car {
    String color; // default: null
}
Car c = new Car(); // compiler-created default constructor
```

---

### 2. No-Argument (User-Defined) Constructor

```java
public class Car {
    String color;

    public Car() { // user-defined no-arg constructor
        color = "Blue";
    }
}
```
### 3. Parameterized Constructor

Used for custom initialization.

```java
public class Car {
    String color;

    public Car(String color) {
        this.color = color;
    }
}
```
**Contructor overloading**:
Multiple constructors with different parameter lists.

```java
public class Car {
    String color;
    int price;

    Car() {
        this("Black", 500000);
    }

    Car(String color) {
        this.color = color;
    }

    Car(String color, int price) {
        this.color = color;
        this.price = price;
    }
}
```

---

### 4. Copy Constructor (Custom in Java)

Java does NOT have automatic copy constructors (C++ does).
We implement manually:

```java
public class My {
    int x;

    public My(int x) {
        this.x = x;
    }

    public My(My m) {     // Copy constructor
        this.x = m.x;     // shallow copy
    }
}
```

### 5. Private Constructor

Private constructors **restrict object creation** from outside.

#### **Use-Case 1 — Prevent Instantiation (Utility Classes)**

```java
public class MathUtils {
    private MathUtils() {}  // prevent instantiation

    public static int add(int a, int b) {
        return a + b;
    }
}
```

#### **Use-Case 2 — Singleton Pattern**

```java
public class Singleton {
    private static Singleton instance;
    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null)
            instance = new Singleton();
        return instance;
    }
}
```

#### **Use-Case 3 — Initialization in Derived Classes**

```java
public class BaseClass {
    private BaseClass() {}

    public static BaseClass createInstance() {
        return new BaseClass();
    }
}
```

#### **Use-Case 4 — Prevent Subclassing**

```java
public final class FinalClass {
    private FinalClass() {}
}
```

**Utility Class Example**

```java
public class StringUtils {
    private StringUtils() {}

    public static String reverse(String s) {
        return new StringBuilder(s).reverse().toString();
    }
}
```

## **Assignment Operator vs Copy Constructor**

 **C++**

* Assignment operator and copy constructor **both exist**
* Default versions create **shallow copy of non-static members**
* You can override them to create **deep copy**

--- 

**Java**

**Assignment Operator (`=`)**

* **Primitives:** value copied
* **Objects:** reference copied (no new object)

**Copy Constructor (Manual)**

* Your custom constructor
* Creates **shallow copy** unless you deep copy fields manually.


```java
class My {
    public int x;

    public My(int x) {
        this.x = x;
    }

    public My(My m) { // copy constructor
        this.x = m.x;
    }
}

public class Main {
    public static void main(String[] args) {
        int a = 10;
        int b = a;      // primitive → value copied

        String str1 = new String("Hello");
        String str2 = str1;   // reference copied
        // BUT strings are immutable → behaves like value copy

        My my = new My(20);
        My my2 = my;          // reference copy
        My my3 = new My(my);  // copy constructor → new object

        my.x = 30;

        System.out.println(my.x);   // 30
        System.out.println(my2.x);  // 30 → same reference
        System.out.println(my3.x);  // 20 → copy constructor → new object
    }
}
```

## Shallow Copy vs Deep Copy

### Shallow Copy

* Copies values of primitive fields
* Copies **references** for objects
  → Modified nested objects affect both copies

**Example**

```java
class Engine {
    String type;
    Engine(String type) { this.type = type; }
}

class Car {
    String model;
    Engine engine;

    Car(String model, Engine engine) {
        this.model = model;
        this.engine = engine; // shallow copy
    }
}

public class Main {
    public static void main(String[] args) {
        Engine engine = new Engine("V6");

        Car car1 = new Car("Sedan", engine);
        Car car2 = new Car("SUV", car1.engine);

        car2.engine.type = "V8"; // affects both

        System.out.println(car1.engine.type); // V8
    }
}
```

### **Deep Copy**

You manually create new nested objects.

```java
Car(Car other) {
    this.model = other.model;
    this.engine = new Engine(other.engine.type);
}
```

## Constructors & Inheritance

* Child constructor **must** call parent constructor (explicitly or implicitly)
* If parent has **no no-arg constructor**, child **must use `super(...)`**
* `super()` must be **first line**

### Example

```java
class Vehicle {
    Vehicle(String type) { }
}

class Car extends Vehicle {
    Car() {
        super("Four Wheeler"); // must call parent
    }
}
```

### `this()` and `super()` in Constructors

**`this()`**

Calls another constructor in the same class.
* Must be **first statement**
* Cannot be used with `super()` in the same constructor
  
**`super()`**

Calls parent class constructor.
* Must be **first statement**
* If not written, compiler inserts `super()`

#### Interview Ques: Why Constructors Cannot Be `static`, `final`, or `abstract`?

**Not static**
Because constructors run when **object is created**, but static members run **without objects**.
**Not final**
Final = cannot be overridden.
Constructors **cannot** be inherited → nothing to override.
**Not abstract**
Abstract method = must be overridden.
But constructor cannot be inherited → cannot be overridden → meaningless.

#### Interview Ques: Why must `super()` be the first statement?
Java must first build the parent part of the child object.The child object contains the parent object inside it. So the JVM requires the parent constructor to run before anything else. Else, compilation error.

* Constructor = special method for initialization
* Compiler provides default constructor only if none exists
* Assignment in Java copies references for objects
* Manual copy constructor = shallow copy unless deep copy implemented
* Private constructors used for utilities, Singleton, preventing subclassing
* `this()` & `super()` cannot coexist & must be first line
* Constructors cannot be `static`, `final`, `abstract`
* Child constructors must call parent constructors

---
Here is a clean, **fully-formatted Markdown version** of everything from your screenshots.
As requested:

* **Heading starts with `###`**
* **No numbering anywhere**
* **Crisp, organised, developer-ready formatting**

---

## `this` Keyword

Refers to current instance of class. Especially usefull in constructor chaining.


### 1. Referencing Current Class Instance Variables

**Purpose:**
To distinguish between instance variables and local variables with the same name.

**Example:**

```java
public class Car {
    private String color;

    public void setColor(String color) {
        this.color = color; // 'this.color' is instance var, 'color' is parameter
    }
}
```

**Key Point:**
Ensures the instance variable is assigned using the parameter, not the local variable.

### 2. Invoking Current Class Constructors (Constructor Chaining)

**Purpose:**
To call another constructor in the same class.

**Example:**

```java
public class Car {
    private String color;
    private int speed;

    // Constructor 1
    public Car() {
        this("Black", 0); // Calls Constructor 2
    }

    // Constructor 2
    public Car(String color, int speed) {
        this.color = color;
        this.speed = speed;
    }
}
```

**Key Point:**
`this(...)` must be the **first statement** inside a constructor.

### 3. Invoking Current Class Methods

**Purpose:**
To call another method of the same class.

**Example:**

```java
public class Car {
    public void start() {
        System.out.println("Car started");
    }

    public void accelerate() {
        this.start(); // Calls start()
        System.out.println("Car accelerating");
    }
}
```

**Key Points:**

* Can be used to call any method inside the same class.
* Optional, but improves clarity.

### 4. Returning the Current Object

**Purpose:**
To return the current object, enabling **method chaining**.

**Example:**

```java
public class Car {
    private String color;

    public Car setColor(String color) {
        this.color = color;
        return this; // Return current object
    }

    public void display() {
        System.out.println("Color: " + color);
    }
}

Car myCar = new Car().setColor("Red").display();
```

**Key Points:**

* Used in fluent APIs and builder patterns.
* Allows multiple method calls in a single statement.


### 5. Passing Current Object as an Argument

**Purpose:**
To pass the current object to another method or constructor.

**Example:**

```java
public class Car {
    private String model;

    public Car(String model) {
        this.model = model;
    }

    public void register(Car car) {
        System.out.println("Registering " + car.model);
    }

    public void selfRegister() {
        this.register(this); // Passes current object
    }
}

Car myCar = new Car("Sedan");
myCar.selfRegister(); // Output: Registering Sedan
```

**Key Point:**
Useful when a method needs to operate on the current object.


### 6. Inner Classes

**Purpose:**
To refer to the outer class instance from an inner class.

**Example:**

```java
public class OuterClass {
    private String outerField = "Outer";

    public class InnerClass {
        private String innerField = "Inner";

        public void display() {
            System.out.println("Outer Field: " + OuterClass.this.outerField);
            System.out.println("Inner Field: " + innerField);
        }
    }

    public InnerClass getInnerClass() {
        return new InnerClass();
    }
}

OuterClass outer = new OuterClass();
OuterClass.InnerClass inner = outer.getInnerClass();
inner.display();
```

**Key Point:**
`OuterClass.this` removes ambiguity between outer and inner class fields.

### Summary Table

| Use Case                           | Description                                         | Example Usage                |
| ---------------------------------- | --------------------------------------------------- | ---------------------------- |
| Referencing Instance Variables     | Distinguishes between instance and local variables  | `this.color = color;`        |
| Invoking Constructors              | Calls another constructor in the same class         | `this("Black", 0);`          |
| Invoking Methods                   | Calls another method in the same class              | `this.start();`              |
| Returning Current Object           | Enables method chaining                             | `return this;`               |
| Passing Current Object as Argument | Passes current object to another method/constructor | `this.register(this);`       |
| Inner Classes                      | Refers to outer instance from inner class           | `OuterClass.this.outerField` |

**Disadvantage:** `this` cannot be used in static methods. Static methods do not belong to any specific instance; they are associated with the class itself. Since there is no instance in a static context, using this leads to a compilation error‍.

---

# Pillars of OOPS

## Polymorphism and It's Types

Polymorphism allows the same method name to behave differently based on context — either at **compile time** or **runtime**.

### Compile-Time Polymorphism (Method Overloading)

**Definition:**
Achieved through *method overloading*, where multiple methods in the same class share the same name but have different parameter lists (type, count, or order).

**Behavior:**
The method to call is decided at **compile time** based on the method signature.

**Example:**

```java
public class Calculator {

    int add(int a, int b) {
        return a + b;
    }

    double add(double a, double b) {
        return a + b;
    }

    int add(int a, int b, int c) {
        return a + b + c;
    }
}
```

**How It's Achieved:**

* Compiler selects the method using the signature.
* Decision happens during compilation.

---

### Runtime Polymorphism (Method Overriding)

**Definition:**
Achieved through *method overriding*, where a subclass provides a specific implementation of a method already defined in its superclass.

**Behavior:**
The method executed is determined at **runtime**, based on the actual object — not the reference type.
(dynamic binding)

**Example:**

```java
class Animal {
    void sound() {
        System.out.println("Animal makes a sound");
    }
}

class Dog extends Animal {
    @Override
    void sound() {
        System.out.println("Dog barks");
    }
}

// Usage:
Animal myDog = new Dog();
myDog.sound(); // Output: Dog barks
```

**How It's Achieved:**

* JVM decides the method at runtime.
* Requires **inheritance** and **method overriding**.

---

## Shallow Copy and Deep Copy

### Shallow Copy

**Definition:**
A copy where the new object references the **same memory locations** as the original for referenced fields.

**Relevance in Polymorphism:**

* No independent copies → changes in one object affect the other.
* Not directly polymorphism, but affects object behavior in inheritance chains.

**Example:**

```java
class Car {
    String model;
    Engine engine;

    Car(String model, Engine engine) {
        this.model = model;
        this.engine = engine; // Shallow copy
    }
}

class Engine {
    String type;

    Engine(String type) {
        this.type = type;
    }
}
```

---

### Deep Copy

**Definition:**
A copy where the new object receives **independent copies** of all referenced objects.

**Relevance in Polymorphism:**

* Changes in one object do not impact another.
* Useful when multiple objects need separate internal states.

**Example:**

```java
class Car {
    String model;
    Engine engine;

    Car(String model, Engine engine) {
        this.model = model;
        this.engine = new Engine(engine.type); // Deep copy
    }
}
```

---

### Summary Table

| Concept                   | Description                                       | Example Usage                           |
| ------------------------- | ------------------------------------------------- | --------------------------------------- |
| Compile-Time Polymorphism | Method overloading; resolved at compile time.     | `int add(int a, int b)`                 |
| Runtime Polymorphism      | Method overriding; resolved at runtime.           | `myDog.sound()`                         |
| Shallow Copy              | Copies references; both objects share state.      | `car2.engine = car1.engine`             |
| Deep Copy                 | Copies all objects independently; no shared state | `this.engine = new Engine(engine.type)` |


## Inheritance and It's types

Inheritance allows a subclass to acquire fields and methods from a superclass. It promotes **code reuse**, **modularity**, and helps create **hierarchical relationships** between classes.

---

### Types of Inheritance in Java

### Single Inheritance

**Definition:**
A subclass inherits from a single superclass.

**Example:**

```java
class Animal {
    void eat() {
        System.out.println("Animal eats");
    }
}

class Dog extends Animal {
    void bark() {
        System.out.println("Dog barks");
    }
}
```

---

### Multilevel Inheritance

**Definition:**
A subclass inherits from another subclass.

**Example:**

```java
class Animal {
    void eat() {
        System.out.println("Animal eats");
    }
}

class Dog extends Animal {
    void bark() {
        System.out.println("Dog barks");
    }
}

class Puppy extends Dog {
    void sleep() {
        System.out.println("Puppy sleeps");
    }
}
```

---

### Hierarchical Inheritance

**Definition:**
Multiple subclasses inherit from the same superclass.

**Example:**

```java
class Animal {
    void eat() {
        System.out.println("Animal eats");
    }
}

class Dog extends Animal {
    void bark() {
        System.out.println("Dog barks");
    }
}

class Cat extends Animal {
    void meow() {
        System.out.println("Cat meows");
    }
}
```

---

### Multiple Inheritance (Not Supported in Java)

**Definition:**
A subclass inherits from multiple superclasses.

**Note:**
Java does **not** support it due to the **Diamond Problem**.
Instead, Java uses **interfaces** to achieve similar behavior.

---

## Specific Things About Inheritance in Java

### Access Modifiers

Inherited members follow superclass access rules:

* **public** – accessible everywhere
* **protected** – accessible within same package + subclasses
* **default** (no modifier) – accessible within same package
* **private** – not inherited

---

### Method Overriding

A subclass provides a specific implementation of a method already defined in the superclass.

**Example:**

```java
class Animal {
    void sound() {
        System.out.println("Animal makes a sound");
    }
}

class Dog extends Animal {
    @Override
    void sound() {
        System.out.println("Dog barks");
    }
}
```

---

### `super` Keyword

Used to access superclass constructors and methods.

**Example:**

```java
class Dog extends Animal {
    void sound() {
        super.sound(); // Calls Animal's sound()
        System.out.println("Dog barks");
    }
}
```

---

## Multiple Inheritance in Java (via Interfaces)

Java achieves multiple inheritance through **interfaces**, but behavior differs **before** and **after** Java 8.

---

### Before Java 8

#### Interfaces as Multiple Inheritance of Type

* A class could implement multiple interfaces → inherit multiple method signatures
* Interfaces had **only abstract methods**
* **Limitation:** If two interfaces had the same method signature → no conflict resolution

**Example:**

```java
interface Flyable {
    void fly();
}

interface Swimmable {
    void swim();
}

class Duck implements Flyable, Swimmable {
    public void fly() {}
    public void swim() {}
}
```

#### No Multiple Inheritance of State or Implementation

Interfaces had no fields or concrete methods → no shared implementation possible.

---

### After Java 8

#### Default Methods

Interfaces can now have **default methods** with implementations.

This allows inheritance of **type + implementation**.

#### Handling Conflicts

If two interfaces provide default methods with the same signature:

* The implementing class **must override** the method
* It can choose a specific interface's implementation using `InterfaceName.super.method()`

**Example:**

```java
interface Flyable {
    default void move() {
        System.out.println("Flying.");
    }
}

interface Swimmable {
    default void move() {
        System.out.println("Swimming.");
    }
}

class Duck implements Flyable, Swimmable {
    @Override
    public void move() {
        Flyable.super.move();
        Swimmable.super.move();
    }
}
```
### Static Methods in Interfaces

* Supported from Java 8
* Not inherited by implementing classes

### Private Methods in Interfaces (Java 9+)

* Used to share helper logic for default methods

### Key Differences (Before vs After Java 8)

| Aspect                | Before Java 8         | After Java 8            |
| --------------------- | --------------------- | ----------------------- |
| Method Implementation | Only abstract methods | Default methods allowed |
| Multiple Inheritance  | Type only             | Type + implementation   |
| Conflict Resolution   | No mechanism          | Must explicitly resolve |
| Static Methods        | Not allowed           | Allowed                 |
| Private Methods       | Not allowed           | Allowed (Java 9+)       |

---

### Summary Table

| Concept                  | Description                                | Example Usage                         |
| ------------------------ | ------------------------------------------ | ------------------------------------- |
| Single Inheritance       | One subclass inherits from one superclass  | `class Dog extends Animal`            |
| Multilevel Inheritance   | Subclass inherits from another subclass    | `class Puppy extends Dog`             |
| Hierarchical Inheritance | Multiple subclasses from same superclass   | `class Dog, class Cat extends Animal` |
| Multiple Inheritance     | Not supported directly; interfaces instead | `class D implements A, B`             |
| Diamond Problem          | Ambiguity in multiple inheritance          | `interface A, B` with default methods |

### Diamond Problem

The diamond problem is a classic issue in multiple inheritance where a class inherits from two or more classes (or interfaces) that have the same method signature, causing ambiguity about which method to use.

Java handles the diamond problem differently **before** and **after** Java 8.

---

### Before Java 8

#### No Multiple Inheritance of Implementation

* Java did **not** allow a class to extend multiple classes.
* Interfaces could only have **abstract methods** (no implementation).
* Because interfaces had no method bodies, **no ambiguity existed**.

**Example:**

```java
interface A {
    void method();
}

interface B {
    void method();
}

class C implements A, B {
    @Override
    public void method() {
        System.out.println("Method implementation.");
    }
}
```

**Explanation:**
`C` simply provides one implementation of `method()`. No conflict → no diamond problem.

#### Limitation Before Java 8

* Since interfaces could not have concrete methods, Java had **no way** to support multiple inheritance of implementation.

---

### After Java 8

#### Default Methods Introduced

* Interfaces can now have **default methods** with implementations.
* This introduced the possibility of **multiple inheritance of implementation**.
* If two interfaces provide the same default method, a conflict occurs.

---

### Solving the Diamond Problem (Java 8+)

If a class implements two interfaces with conflicting default methods, it must explicitly resolve the conflict by:

* Overriding the method
* Choosing one interface’s implementation using `InterfaceName.super.method()`
* Or providing its own implementation

**Example:**

```java
interface A {
    default void method() {
        System.out.println("A's method.");
    }
}

interface B {
    default void method() {
        System.out.println("B's method.");
    }
}

class C implements A, B {
    @Override
    public void method() {
        A.super.method(); // Choose A's implementation
        B.super.method(); // Choose B's implementation
    }
}
```

---

### Key Rules

* The implementing class **must** resolve the conflict — Java will not choose automatically.
* If one interface's default method is chosen, it must be explicitly called using:

  ```java
  InterfaceName.super.method();
  ```

---

### Key Differences (Before vs After Java 8)

| Aspect               | Before Java 8                        | After Java 8                                |
| -------------------- | ------------------------------------ | ------------------------------------------- |
| Multiple Inheritance | Only type (no implementation)        | Type + implementation (via default methods) |
| Diamond Problem      | Not applicable                       | Must resolve explicitly                     |
| Conflict Resolution  | No conflicts (abstract methods only) | Must override and choose with `super`       |

---

### Example After Java 8

```java
interface Flyable {
    default void move() {
        System.out.println("Flying.");
    }
}

interface Swimmable {
    default void move() {
        System.out.println("Swimming.");
    }
}

class Duck implements Flyable, Swimmable {
    @Override
    public void move() {
        Flyable.super.move();    // Choose Flyable's implementation
        Swimmable.super.move();  // Choose Swimmable's implementation
    }
}
```

## Encapsulation in Java

Encapsulation is a core OOP principle that bundles **data (fields)** and **methods that operate on the data** into a single class. It also restricts direct access to some fields using access modifiers like `private`, `protected`, and `public`.

---

### Key Concepts of Encapsulation

#### Bundling

**Definition:**
Combining data and methods into a single unit (class).

**Example:**

```java
public class BankAccount {
    private String accountNumber;
    private double balance;

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    public double getBalance() {
        return balance;
    }
}
```

---

#### Data Hiding

**Definition:**
Restricting direct access to fields using `private` and exposing controlled access via getters/setters.

**Example:**

```java
public class BankAccount {
    private String accountNumber;

    // Getter
    public String getAccountNumber() {
        return accountNumber;
    }

    // Setter
    public void setAccountNumber(String accountNumber) {
        if (accountNumber != null && !accountNumber.isEmpty()) {
            this.accountNumber = accountNumber;
        }
    }
}
```

---

### Advantages of Encapsulation

#### Data Protection

* Fields are hidden from outside access → prevents unintended modifications.
* Example: `balance` cannot be edited directly.

#### Controlled Access

* Getters and setters enable validation before updating fields.

#### Flexibility

* Internal implementation can be changed without affecting other parts of the program.

#### Reusability

* Encapsulated classes are modular and reusable.

#### Maintainability

* Makes debugging easier by localizing changes inside specific classes.

---

### Disadvantages of Encapsulation

#### Complexity

* Requires getters/setters → slightly more conceptual overhead for beginners.

#### Performance Overhead

* Accessing fields through methods is marginally slower than direct access (but negligible in modern JVMs).

#### Verbosity

* More boilerplate (getters/setters) unless using Lombok or IDE auto-generation.

#### Over-Encapsulation

* Too many getters/setters can create unnecessary complexity in simple programs.

---

## Abstraction in Java

Abstraction hides internal implementation details and exposes only the essential features. It lets you focus on **what** an object does rather than **how** it does it.

Example idea: `brewCoffee()` hides all brewing steps internally.

Java achieves abstraction using:

* **Abstract Classes** (partial abstraction)
* **Interfaces** (full abstraction; especially after Java 8)

---

## Abstract Classes in Java

An abstract class is declared using `abstract` and **cannot be instantiated**.
It can contain:

* Abstract methods (no body)
* Concrete methods (with body)
* Constructors
* Instance fields (static, non-static, final, etc.)

A concrete subclass **must** implement all abstract methods.

**Example:**

```java
abstract class Animal {
    abstract void makeSound();        // abstract method

    void sleep() {                    // concrete method
        System.out.println("Sleeping...");
    }
}

class Dog extends Animal {
    @Override
    void makeSound() {
        System.out.println("Bark");
    }
}

public class Main {
    public static void main(String[] args) {
        Animal a = new Dog();
        a.makeSound();   // Bark
        a.sleep();       // Sleeping...
    }
}
```

### When to Use Abstract Classes

Use an abstract class when:

* Subclasses share common **fields**, **methods**, or **initialization logic**
* You want to provide some default behavior
* You need protected members or non-static fields

Abstract classes represent **partial abstraction**.

---

## Interfaces in Java

An interface defines a **contract**: what methods a class must implement.

Before Java 8 → only abstract methods and constants.
After Java 8 → supports:

* Default methods
* Static methods
* (Java 9+) Private methods

**Key Features:**

* 100% abstraction conceptually (until Java 8)
* No constructors (cannot be instantiated)
* Supports **multiple inheritance**
* Fields are always `public static final`
* Methods are:

  * `public abstract` (by default)
  * `default` (Java 8+)
  * `static` (Java 8+)

**Example:**

```java
interface Shape {
    double calculateArea();
}

class Circle implements Shape {
    private double radius;
    Circle(double r) { this.radius = r; }

    @Override
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}

class Rectangle implements Shape {
    private double l, w;
    Rectangle(double l, double w) { this.l = l; this.w = w; }

    @Override
    public double calculateArea() {
        return l * w;
    }
}

public class Main {
    public static void main(String[] args) {
        Shape c = new Circle(5);
        Shape r = new Rectangle(4, 6);

        System.out.println(c.calculateArea());
        System.out.println(r.calculateArea());
    }
}
```

---

## Default & Static Methods in Interfaces (Java 8+)

```java
interface Vehicle {
    void start();

    default void honk() {          // default method
        System.out.println("Honking");
    }

    static void service() {        // static method
        System.out.println("Servicing...");
    }
}
```

**Why default methods?**

* To add new methods to interfaces **without breaking existing implementations**
* To provide shared default behavior

Static methods are called via the interface name.

---

## Abstract Class vs Interface (Java 8+)

| Aspect       | Abstract Class             | Interface                      |
| ------------ | -------------------------- | ------------------------------ |
| Methods      | Abstract + concrete        | Abstract + default + static    |
| Fields       | Any type (instance/static) | Only `public static final`     |
| Constructors | Allowed                    | Not allowed                    |
| Inheritance  | Single inheritance         | Multiple inheritance           |
| Use Case     | Shared code/state          | Contract for unrelated classes |

---

## Common Interview Questions

### **1. What is abstraction?**

Hiding implementation details and exposing only essential features. Achieved via abstract classes and interfaces.

### **2. What is an abstract class?**

A class with the `abstract` keyword. Cannot be instantiated. Can have abstract and concrete methods, constructors, and fields.

### **3. Can an abstract class have a constructor?**

Yes. Called when a subclass is instantiated. Used for shared initialization.

### **4. Can an abstract class have static or final methods?**

Yes.
Final methods must have a body (cannot be abstract).

### **5. What is an interface?**

A contract defining methods a class must implement. Supports multiple inheritance.

### **6. What are default methods?**

Methods in an interface with a default implementation (Java 8+). Enable backward compatibility.

### **7. Can a class implement multiple interfaces? Extend multiple classes?**

* Yes, multiple interfaces
* No, only one class (single inheritance)

### **8. Can abstract methods be private or static?**

No — they must be visible to subclasses and belong to an instance.

### **9. Can you instantiate an abstract class?**

No. You can only instantiate subclasses.

### **10. When to use abstract class vs interface?**

* **Abstract Class:** shared code, state, protected members
* **Interface:** common contract for unrelated classes, multiple inheritance
---

## Extra Interview-Specific Notes

* Abstract classes existed to give **more flexibility** than interfaces before Java 8
* After Java 8, interfaces became more powerful but **cannot hold state**
* Default methods allow safe interface evolution
* Abstract classes can implement interfaces and leave methods abstract
* Any class extending an abstract class **must** implement all abstract methods
* `@Override` is optional but ensures the method actually overrides a parent/interface method

  * If not, the compiler throws an error

---

## Quick Interview Answers

### *Why use an abstract class over an interface?*

Because you need:

* Shared state
* Constructors
* Partially implemented behavior
* Protected members

### *Why were default methods introduced?*

To prevent breaking all implementing classes when a new method is added to an interface.

### *Can abstract methods be private or static?*

No.
They must be overridable.

### **INTERFACES + ABSTRACT CLASS IMPLEMENTATION (SUPER CRISP VERSION)**

---

## **1. Types of Methods in Interfaces (Java 8+)**

### **1️⃣ Abstract Methods**

* Implicitly `public abstract`
* Must be implemented by concrete classes (unless inherited by an abstract class)

```java
interface A {
    void m1();  // abstract
}
```

---

### **2️⃣ Default Methods (Java 8+)**

* Have a body
* Can be overridden

```java
interface A {
    default void m2() {
        System.out.println("Default");
    }
}
```

---

### **3️⃣ Static Methods (Java 8+)**

* Have a body
* **Not inherited** by implementing classes
* Must be called with `InterfaceName.method()`

```java
interface A {
    static void m3() { System.out.println("Static"); }
}
```

---

### **4️⃣ Private Methods (Java 9+)**

* For **internal code reuse** inside the interface
* Cannot be accessed or overridden by implementing classes

```java
interface A {
    private void helper() {}
    private static void log() {}
}
```

---

# **2. Abstract Class Implementing an Interface**

An abstract class may:

✔ implement **all** interface abstract methods
✔ implement **some** methods
✔ implement **none**

But:

➡ A **concrete** class must implement whatever remains.

```java
interface A {
    void m1();
    default void m2(){ System.out.println("Default"); }
}

abstract class B implements A {
    // m1 not implemented → allowed
}

class C extends B {
    @Override public void m1() { System.out.println("C"); }
}
```

---

# **3. All Method-Implementation Scenarios **

This section is now **ultra-crisp** but complete.

---

### **CASE 1 — Abstract method NOT implemented by abstract class**

```java
interface A { void m1(); }
abstract class B implements A { }
```

---

### **CASE 2 — Abstract class implements interface abstract method**

```java
abstract class B implements A {
    @Override public void m1() {}
}
```

---

### **CASE 3 — Concrete class must implement remaining methods**

```java
abstract class B implements A { }
class C extends B {
    @Override public void m1() {}
}
```

---

### **CASE 4 — Abstract class overrides default method**

```java
interface A { default void m2(){ System.out.println("A"); } }
abstract class B implements A {
    public void m2(){ System.out.println("B"); }
}
```

---

### **CASE 5 — Concrete class overrides default method**

```java
class C extends B {
    @Override public void m2(){ System.out.println("C"); }
}
```

---

### **CASE 6 — Abstract class keeps default method**

```java
interface A { default void m2(){} }
abstract class B implements A { }
```

---

### **CASE 7 — Static interface method**

➡ **Cannot be overridden** by class or abstract class

```java
interface A { static void m3(){} }
abstract class B implements A { /* cannot override m3 */ }
A.m3();   // only valid call
```

---

### **CASE 8 — Private interface methods**

➡ Not visible to implementing classes
➡ Cannot be overridden

```java
interface A {
    private void helper(){}
    private static void log(){}
}
```

---

### **CASE 9 — Abstract class implements multiple interfaces**

```java
interface A { void m1(); }
interface B { void m2(); }

abstract class C implements A, B {
    public void m1(){}     // implemented
    // m2 left for child
}
```

---

### **CASE 10 — Conflicting default methods**

➡ Abstract class **must override** to resolve conflict
➡ Can choose which interface’s default to call

```java
interface A { default void m1(){ System.out.println("A"); } }
interface B { default void m1(){ System.out.println("B"); } }

abstract class C implements A, B {
    @Override
    public void m1(){
        A.super.m1();  // pick A's version
    }
}
```

---

# **4. Ultra-Crisp Interview Summary**

### **Interface method types**

* abstract
* default
* static
* private (`private` + `private static`)

---

### **Rules**

* Default methods **can** be overridden
* Static methods **cannot** be overridden
* Private methods are **internal only**
* Abstract class may implement **0/1/all** abstract methods
* Concrete class must finish whatever is left

---

### **Abstract Class + Interface (Key Points)**

* If abstract class doesn’t implement all interface methods → stays abstract
* Default methods can be:

  * inherited
  * overridden
  * overridden with an explicit super call (in conflict cases)

---

## Encapsulation vs Abstraction in Java

Encapsulation and abstraction are closely related OOP concepts, but they address **different goals**.
Encapsulation focuses on **protecting data**, while abstraction focuses on **hiding implementation details** and exposing only what is necessary.

### Differences

| Aspect             | Encapsulation                                                        | Abstraction                                                                 |
| ------------------ | -------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| **Definition**     | Bundling data and methods into a single unit and restricting access. | Hiding complex implementation details and exposing only essential features. |
| **Purpose**        | Protects data and provides controlled access.                        | Reduces complexity and simplifies interaction with objects.                 |
| **Implementation** | Achieved using access modifiers (`private`, `protected`, `public`).  | Achieved using abstract classes and interfaces.                             |
| **Focus**          | Focuses on **how** data is accessed and modified.                    | Focuses on **what** the object does, not how it does it.                    |
| **Example**        | `private` fields with getter/setter methods.                         | Abstract classes and interfaces with abstract methods.                      |

---

### Example Combining Encapsulation and Abstraction

```java
// Abstraction: Hiding implementation details
abstract class Vehicle {

    // Abstract method (no implementation)
    abstract void start();

    // Concrete method
    void stop() {
        System.out.println("Vehicle stopped");
    }
}
```

```java
// Encapsulation: Protecting data and providing controlled access
class Car extends Vehicle {
    private String model;   // Encapsulated field

    // Getter
    public String getModel() {
        return model;
    }

    // Setter
    public void setModel(String model) {
        this.model = model;
    }

    @Override
    void start() {
        System.out.println("car started");
    }
}
```

```java
public class Main {
    public static void main(String[] args) {
        Vehicle myCar = new Car();   // Abstraction: interacted through Vehicle reference
        myCar.start();               // Output: "car started"
        myCar.stop();                // Output: "Vehicle stopped"

        // Encapsulation: Accessing model via getter/setter
        ((Car) myCar).setModel("Sedan");
        System.out.println(((Car) myCar).getModel());   // Output: "Sedan"
    }
}
```

## Access Modifiers

Keywords that **control visibility** of classes, methods, constructors, and variables.

Java has **4** access modifiers:

* `public`
* `private`
* `protected`
* *default* (no keyword)

---

### Project Structure

Most access-modifier questions assume:

```
project/
   ├── package1/
   │      └── A.java
   └── package2/
          └── B.java
```

Visibility depends on:

* **same class**
* **same package**
* **subclass**
* **different package**

---

### SUMMARY TABLE

| Access Modifier | Same Class | Same Package | Subclass (different package) | Other Package              |
| --------------- | ---------- | ------------ | ---------------------------- | -------------------------- |
| **public**      | ✔          | ✔            | ✔                            | ✔                          |
| **protected**   | ✔          | ✔            | ✔                            | ❌ (except via inheritance) |
| **default**     | ✔          | ✔            | ❌                            | ❌                          |
| **private**     | ✔          | ❌            | ❌                            | ❌                          |

---

### **Interview Points**

**Private is NOT inherited.**
Even subclasses cannot access it.

**Protected is inherited.**
Subclass in another package can access it **only using `this` or directly**, not through object.

```java
sub.x;   // ❌ not allowed if x is protected in different package
```

**Classes can only be:**
* `public`
* *default*

(Classes **cannot** be `protected` or `private` at top level.)

**Constructors follow same visibility rules.**

**Most restrictive → least restrictive:**
private → default → protected → public

---

# Class Relationships and Diagrams in OOP

Class relationships describe how different classes interact with each other in a system. These relationships are represented using **UML diagrams**.

Below are all key class relationships, their purpose, UML symbols, and Java examples.

---

## 1. Inheritance (Generalization)

**Definition:**
A relationship where a class (subclass) inherits properties and behaviors from another class (superclass).

**Purpose:**
Promotes code reuse and establishes a hierarchical relationship.

**UML Representation:**
Hollow arrow pointing from subclass → superclass.

**Example:**

```java
class Animal {
    void eat() {}
}

class Dog extends Animal {
    void bark() {}
}
```

**Diagram:**

```
[Dog] <>-----|> [Animal]
```

---

## 2. Association

**Definition:**
A relationship where one class is related to another, but there is no ownership.

**Purpose:**
Represents a simple connection between two classes.

**UML Representation:**
Solid line.

**Example:**

```java
class Teacher {
    String name;
}

class Student {
    Teacher teacher;
}
```

**Diagram:**

```
[Student] ---- [Teacher]
```

---

## 3. Aggregation

**Definition:**
A “whole–part” relationship where one class (whole) contains another (part), but the part can exist independently.

**Purpose:**
Represents weak ownership.

**UML Representation:**
Hollow diamond on the whole-class side.

**Example:**

```java
class Department {
    List<Professor> professors;
}

class Professor {
    String name;
}
```

**Diagram:**

```
[Department] o---- [Professor]
```

---

## 4. Composition

**Definition:**
A “whole–part” relationship where the part **cannot exist without** the whole.

**Purpose:**
Represents strong ownership.

**UML Representation:**
Solid diamond on the whole-class side.

**Example:**

```java
class Car {
    Engine engine;
}

class Engine {
    String model;
}
```

**Diagram:**

```
[Car] *---- [Engine]
```

---

## 5. Dependency

**Definition:**
A relationship where one class depends on another for functionality. No ownership.

**Purpose:**
Represents a temporary/weak link.

**UML Representation:**
Dashed arrow from dependent → independent class.

**Example:**

```java
class Printer {
    void print(Document doc) {}
}

class Document {
    String content;
}
```

**Diagram:**

```
[Printer] --> [Document]
```

---

## 6. Realization (Interface Implementation)

**Definition:**
A relationship where a class implements an interface.

**Purpose:**
Allows a class to provide implementations for abstract interface methods.

**UML Representation:**
Hollow arrow pointing from class → interface.

**Example:**

```java
interface Drivable {
    void start();
    void stop();
}

class Car implements Drivable {
    public void start() {}
    public void stop() {}
}
```

**Diagram:**

```
[Car] <>----|> [Drivable]
```

---

## Summary Table

| Relationship    | Description                                         | UML Representation        | Example                   |
| --------------- | --------------------------------------------------- | ------------------------- | ------------------------- |
| **Inheritance** | Subclass inherits from superclass.                  | Hollow arrow              | `Dog extends Animal`      |
| **Association** | Simple connection between classes.                  | Solid line                | `Student — Teacher`       |
| **Aggregation** | Weak whole-part. Part can exist independently.      | Hollow diamond            | `Department — Professor`  |
| **Composition** | Strong whole-part. Part cannot exist without whole. | Solid diamond             | `Car — Engine`            |
| **Dependency**  | Temporary/weak relationship.                        | Dashed arrow              | `Printer — Document`      |
| **Realization** | Class implements interface.                         | Hollow arrow to interface | `Car implements Drivable` |

---

## Key Points

1. **Inheritance:** Subclass inherits from superclass.
2. **Association:** Simple connection without ownership.
3. **Aggregation:** Weak whole-part relationship.
4. **Composition:** Strong whole-part relationship.
5. **Dependency:** Temporary or weak link.
6. **Realization:** Class implements an interface.

# Generics and Wildcards in Java

**Generics** and **wildcards** are powerful features in Java that allow type-safe programming by enabling the use of types as parameters. They help create reusable, flexible code while maintaining type safety.

---

## One-Line Summary

* `<T>` defines a type
* `?` is unknown
* `? extends` = read-only
* `? super` = write-friendly
* Generics provide strong typing; wildcards provide flexibility

---

### Why Generics?

* Enable type safety at compile-time
* Remove need for manual type casting
* Increase code reusability
* Make APIs cleaner and easier to maintain

---

## 1. Generic Classes

**Definition:**
Classes that accept a type parameter.

**Purpose:**
Allow working with multiple data types safely.

**Key Points**

* Use `<T>` to define a type parameter.
* Multiple type parameters allowed: `<T, U>`.
* Generics work only with reference types (not primitives).

**Example**

```java
class Box<T> {
    private T item;
    public void setItem(T item) { this.item = item; }
    public T getItem() { return item; }
}
```

**Multiple Type Parameters**

```java
class Pair<T, U> {
    T first; U second;
}
```

**No primitive generics**

```java
// Not allowed
Box<int> box;

// Use wrapper
Box<Integer> box;
```

---

## 2. Generic Methods

**Definition:**
Methods that use type parameters (placed before return type).

**Purpose:**
Make methods type-agnostic.

**Example**

```java
public <T> void print(T value) { System.out.println(value); }
```

**Notes**

* `<T>` must appear before return type.
* Can be static because type parameters belong to the method, not the class.

---

## 3. Benefits of Generics

### 1. Type Safety

Compile-time checking; fewer runtime errors.

### 2. Code Reusability

One class/method works with multiple types.

### 3. Readability and Maintainability

Explicit types make code clear.

### 4. No Type Casting Needed

Avoids `ClassCastException`.

**Without generics**

```java
Object obj = list.get(0);
String s = (String) obj; // Risky
```

**With generics**

```java
String s = list.get(0); // Safe
```

---

## Wildcards in Java

### Why Wildcards?

Used when the exact type is not known or not important.

**Types**

* `?` – unknown type
* `? extends T` – T or its subclasses
* `? super T` – T or its superclasses

---

## 4. Unbounded Wildcard — `?`

**Use when:** Type doesn't matter; only reading.

```java
void printList(List<?> list) {
    for (Object item : list) System.out.println(item);
}
```

---

## 5. Upper Bounded Wildcard — `? extends T`

**Meaning:** Accepts T or its subclasses.
**Use when:** Read-only operations.

```java
void printNumbers(List<? extends Number> list) {
    for (Number num : list) System.out.println(num);
}
```

---

## 6. Lower Bounded Wildcard — `? super T`

**Meaning:** Accepts T or its superclasses.
**Use when:** Writing or adding values.

```java
void addNumbers(List<? super Integer> list) {
    list.add(10);
}
```

---

## 7. Generics vs Wildcards (When to Use What)

### Use Generics (`<T>`) When:

* Consistent type across parameters
* Adding elements to a collection
* Returning types that must be preserved

```java
public <T> T getFirst(List<T> list) { return list.get(0); }
```

### Use Wildcards (`?`) When:

* Only reading from a collection
* Type is irrelevant to your logic
* More flexibility is required

```java
void printList(List<?> list)
```

---

## 8. Why You Can't Add to a `List<?>`

`List<?>` means unknown type → compiler cannot ensure safety.

```java
list.add(value); // Compile-time error
```

---

## 9. Wildcards Are Unsafe for Return Types

A wildcard always forces return value to be `Object`.

```java
Object x = list.get(0);
```

Generics preserve the type:

```java
String x = list.get(0);
```

---

## 10. Best Practices

1. Use generics for type-safe classes and methods.
2. Use wildcards for flexible method parameters.
3. Use `? extends` for reading and `? super` for writing (PECS rule).
4. Avoid excessive wildcard usage for readability.
5. Prefer generic methods when returning typed data.


---

## Summary Table

| Concept                | Description                      | Example Usage                           |
| ---------------------- | -------------------------------- | --------------------------------------- |
| **Generic Classes**    | Classes using type parameters    | `class Box<T>`                          |
| **Generic Methods**    | Methods using type parameters    | `public <T> void printArray(T[] array)` |
| **Unbounded Wildcard** | Represents any unknown type      | `List<?>`                               |
| **Upper Bounded**      | Any subtype of a specific type   | `List<? extends Number>`                |
| **Lower Bounded**      | Any supertype of a specific type | `List<? super Integer>`                 |

----
# Pointers in C++, References in C++, References in Java.

## C++ Pointers

* C++ pointers are memory addresses that can be manipulated directly (e.g., pointer arithmetic).
* They can be reassigned or set to `null`.

## C++ References

* C++ references are aliases for existing variables.
* They cannot be null and cannot be reassigned once initialized.

## Java References

* Java references are similar to C++ pointers in that they point to objects in memory.
* They do **not** support pointer arithmetic.
* They are managed by the JVM’s garbage collector.


## Difference Between C++ Pointers, C++ References, and Java References

| Feature               | C++ Pointers                                   | C++ References                     | Java References                      |
| --------------------- | ---------------------------------------------- | ---------------------------------- | ------------------------------------ |
| **Definition**        | Stores the memory address of another variable. | An alias for an existing variable. | A handle to an object in memory.     |
| **Declaration**       | `int* ptr = &var;`                             | `int& ref = var;`                  | `MyClass obj = new MyClass();`       |
| **Nullability**       | Can be null (`nullptr`).                       | Cannot be null.                    | Can be null.                         |
| **Reassignment**      | Can point to different memory locations.       | Cannot be reassigned.              | Can be reassigned to another object. |
| **Memory Management** | Direct memory manipulation.                    | No direct memory manipulation.     | Managed by JVM (GC).                 |
| **Arithmetic**        | Supports pointer arithmetic.                   | No arithmetic support.             | No arithmetic support.               |
| **Dereferencing**     | Explicit dereference (`*ptr`).                 | Auto-dereferenced.                 | No explicit dereferencing.           |


```cpp
#include <iostream>
using namespace std;

int main() {
    int var = 10;
    int* ptr = &var;

    *ptr = 20;
    cout << var; // Output: 20
}
```

---

### ✅ C++ Reference Example

```cpp
#include <iostream>
using namespace std;

int main() {
    int var = 10;
    int& ref = var;

    ref = 20;
    cout << var; // Output: 20
}
```

---

### ✅ Java Reference Example

```java
class MyClass {
    int value;
    void setValue(int v) { value = v; }
    int getValue() { return value; }
}

public class Main {
    public static void main(String[] args) {
        MyClass obj1 = new MyClass();
        MyClass obj2 = obj1;

        obj2.setValue(30);
        System.out.println(obj1.getValue()); // Output: 30
    }
}
```


## Conclusion
