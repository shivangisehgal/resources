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

### Default Constructor

### Parameterized Constructor

### Copy Constructor

### Private Constructor

### This keyword

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
