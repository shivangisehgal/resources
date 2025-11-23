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

### Compile Time (Static) Polymorphism

### Run Time (Dynamic) Polymorphism

### Shallow copy, Deep Copy

## Inheritance and It's types

### Advantages & Disadvantages

### Conclusion

### Single Inheritance

### Multi Level Inheritance

### Hierarchical Inheritance

### Multiple Inheritance

### Diamond Problem

### Advantages & Disadvantages

### Conclusion

## Encapsulation

### Key Features of Encapsulation

### Examples & Explanation

### Advantages & Disadvantages

### Conclusion

## Abstraction

### Problem Without Abstraction

### Solution Using Abstraction

### Advantages of Abstract Class

### Disadvantages of Abstract Class

# Interface in Java

## Example of an Interface

## Advantages of Using Interface

## Disadvantages of using Interface

## Abstract Class vs Interface

## When to use Abstract Class vs Interface

## Multiple Inheritance with Interfaces

## Interview Questions - Abstract Class, Default Keyword & Interface

## Conclusion

# Access Modifiers

## Project Structure

### Public Access Modifier

### Private Access Modifier

### Protected Access Modifier

### Default Access Modifier

### Summary & Comparison of all Access Modifiers

# Class Diagrams

## Inheritance

## Association

## Aggregation

## Difference between Association & Aggregation

## Composition

## Dependency

## Difference between Association & Dependency

## Realization

## Summing all at ONE PLACE

# Generics & WildCards

## Generic Method

## Generic Classes

## Note on Usage of Generics

## Benefits of Generics

## WildCards in Generics

### Unbounded WildCards

### Upper Bound WildCards

### Lower Bound WildCards

## Generics vs WildCards

## When to use Wildcards vs Generics


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
