Here is the streamlined SDE-2 interview playbook for **Evaluate a Nested Predicate Expression Tree**. This is a classic **Composite Pattern / AST Evaluation** question (frequently asked at Stripe, Robinhood, Databricks, and Confluent for rule engines and feature flags) that tests tree recursion, short-circuit evaluation, dot-path resolution, and defending against `StackOverflowError` on adversarial inputs.

---

## Part 1: Problem Statement, Example & Clarifying Questions

### Problem Statement

Design and implement `boolean evaluate(PredicateNode root, Map<String, Object> document)` that evaluates a boolean expression tree against a given document.

* **Composite (Internal) Nodes:** Logical operators (`AND`, `OR`, and optionally `NOT`) containing a list of child `PredicateNode`s.
* **Leaf Nodes:** Comparison conditions (`EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, etc.) comparing a field in `document` (e.g., `"age"` or nested `"user.country"`) against an expected target value.
* **Follow-Up Requirements:**
1. Support nested field paths (e.g., `"user.address.city"`).
2. Protect against adversarially deep expression trees (e.g., 50,000 nested `AND` nodes that would crash recursive DFS with a `StackOverflowError`).



### Example

**Document:**

```json
{
  "role": "admin",
  "age": 28,
  "user": {
    "country": "IN",
    "verified": true
  }
}

```

**Expression Tree:**

```text
          AND
         /   \
role == "admin"   OR
                 /  \
         age > 30    user.country == "IN"

```

**Evaluation:**

* `role == "admin"` $\rightarrow$ `true`
* `OR` branch: `age > 30` (`28 > 30` is `false`), `user.country == "IN"` (`true`) $\rightarrow$ `false || true` = `true`
* Root `AND`: `true && true` $\rightarrow$ **Returns `true**`

---

### 4 Quick Clarifying Questions (Ask Before Coding!)

1. *"What should happen if a field referenced in a leaf predicate is missing from the document or its value is `null`?"* (Standard rule: evaluates to `false` unless explicitly checking `EQUALS null`, rather[ than throwing a `NullPointerException`).
2. *"What should an `AND` or `OR` node with an empty children list evaluate to?"* (By standard boolean algebra: identity element for `AND` is `true`, and for `OR` is `false`—or throw `IllegalArgumentException` if empty operators are invalid).
3. *"Should we support nested field paths like `"user.country"` where the document contains nested `Map`s?"* (Yes, we'll build a clean helper for dot-separated path lookup).
4. *"Can the expression tree be untrusted/user-supplied and deeply nested?"* (Yes—we will start with clean short-circuiting recursion with a `MAX_DEPTH` guard, and then show how to evaluate iteratively on the heap).

---

## Part 2: Two-Step Interview Progression

### Approach 1: Clean Recursive Evaluator with Short-Circuiting, Nested Paths & Depth Guard

#### Simple Java Concepts Used

1. **Short-Circuit Evaluation (Crucial Optimization!):**
* In an `AND` node, the moment **any** child evaluates to `false`, we **immediately return `false**` without evaluating the remaining children!
* In an `OR` node, the moment **any** child evaluates to `true`, we **immediately return `true**` without evaluating the remaining children!


2. **Nested Field Path Resolution (`"user.address.city"`):**
* We split the field string on `"\\."` and walk down the nested `Map<String, Object>` step by step.


3. **Adversarial Depth Protection (Guard #1: `MAX_DEPTH` Limit):**
* In production APIs (like GraphQL, JSON parsers, or rule engines), the first line of defense against a malicious 50,000-deep tree is passing `currentDepth` into the recursion and throwing an `IllegalArgumentException("Max expression depth exceeded")` if `currentDepth > MAX_DEPTH` (e.g., `100`).



#### How to Narrate This in the Interview

> *"I'll model the expression tree using a `PredicateNode` structure where a node is either a logical operator (`AND`, `OR`, `NOT`) with children, or a `LEAF` comparison with a `field`, `operator`, and `expectedValue`.*
> *To evaluate the tree, I'll start with a recursive DFS that leverages **short-circuiting**: for an `AND` node, I stop and return `false` on the first `false` child; for an `OR` node, I stop and return `true` on the first `true` child.*
> *To support nested field paths like `"user.country"`, I'll write a `resolveField()` helper that splits the path by `.` and traverses nested `Map`s safely.*
> *Finally, as a first line of defense against adversarially deep trees causing a `StackOverflowError`, I'll enforce a configurable `MAX_DEPTH` guard during traversal."*

#### Java Code (Recursive + Nested Paths + Depth Guard)

```java
import java.util.*;

public class PredicateEvaluator {

    // Guard against adversarially deep expression trees blowing up the JVM call stack
    private static final int MAX_DEPTH = 500;

    public enum NodeType {
        AND, OR, NOT, LEAF
    }

    public enum ComparisonOp {
        EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN
    }

    // Represents a node in the serialized predicate tree
    public static class PredicateNode {
        final NodeType type;
        // Used when type == AND, OR, NOT
        final List<PredicateNode> children;
        // Used when type == LEAF
        final String fieldPath;
        final ComparisonOp op;
        final Object expectedValue;

        // Constructor for Logical Operator Nodes (AND, OR, NOT)
        public PredicateNode(NodeType type, List<PredicateNode> children) {
            this.type = type;
            this.children = children != null ? children : Collections.emptyList();
            this.fieldPath = null;
            this.op = null;
            this.expectedValue = null;
        }

        // Constructor for Leaf Condition Nodes (e.g., "user.age" > 18)
        public PredicateNode(String fieldPath, ComparisonOp op, Object expectedValue) {
            this.type = NodeType.LEAF;
            this.children = Collections.emptyList();
            this.fieldPath = fieldPath;
            this.op = op;
            this.expectedValue = expectedValue;
        }
    }

    // Special sentinel to distinguish "field missing in document" vs "field mapped to null"
    private static final Object MISSING = new Object();

    public boolean evaluate(PredicateNode root, Map<String, Object> document) {
        if (root == null || document == null) {
            return false;
        }
        return evaluateRecursive(root, document, 0);
    }

    private boolean evaluateRecursive(PredicateNode node, Map<String, Object> doc, int depth) {
        // 1. Adversarial Depth Guard: Fail fast before JVM throws StackOverflowError
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Expression tree exceeds max allowed depth of " + MAX_DEPTH);
        }

        switch (node.type) {
            case AND:
                // Short-circuit AND: Stop at the very first child that returns false!
                for (PredicateNode child : node.children) {
                    if (!evaluateRecursive(child, doc, depth + 1)) {
                        return false;
                    }
                }
                return true; // All children evaluated to true

            case OR:
                // Short-circuit OR: Stop at the very first child that returns true!
                for (PredicateNode child : node.children) {
                    if (evaluateRecursive(child, doc, depth + 1)) {
                        return true;
                    }
                }
                return false; // No child evaluated to true

            case NOT:
                if (node.children.size() != 1) {
                    throw new IllegalArgumentException("NOT node must have exactly 1 child");
                }
                return !evaluateRecursive(node.children.get(0), doc, depth + 1);

            case LEAF:
                return evaluateLeaf(node, doc);

            default:
                throw new UnsupportedOperationException("Unknown node type: " + node.type);
        }
    }

    // Evaluates a single leaf condition like "user.age > 21"
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean evaluateLeaf(PredicateNode leaf, Map<String, Object> doc) {
        Object actualValue = resolveFieldPath(doc, leaf.fieldPath);

        // If the field path doesn't exist at all in the document, predicate is false
        if (actualValue == MISSING) {
            return false;
        }

        switch (leaf.op) {
            case EQUALS:
                return Objects.equals(actualValue, leaf.expectedValue);

            case NOT_EQUALS:
                return !Objects.equals(actualValue, leaf.expectedValue);

            case GREATER_THAN:
            case LESS_THAN:
                // Safely verify both values are non-null and Comparable of compatible types
                if (actualValue instanceof Comparable && leaf.expectedValue instanceof Comparable
                        && actualValue.getClass().equals(leaf.expectedValue.getClass())) {
                    int cmp = ((Comparable) actualValue).compareTo(leaf.expectedValue);
                    return leaf.op == ComparisonOp.GREATER_THAN ? cmp > 0 : cmp < 0;
                }
                return false;

            default:
                return false;
        }
    }

    // Supports both flat fields ("age") and nested paths ("user.address.city")
    private Object resolveFieldPath(Map<String, Object> doc, String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            return MISSING;
        }

        // Fast path: If there is no dot, do a single O(1) map lookup
        if (fieldPath.indexOf('.') == -1) {
            return doc.containsKey(fieldPath) ? doc.get(fieldPath) : MISSING;
        }

        // Nested path: Split by '.' and walk down nested Maps
        String[] parts = fieldPath.split("\\.");
        Object current = doc;

        for (String part : parts) {
            if (!(current instanceof Map<?, ?>)) {
                return MISSING; // Tried to traverse into a primitive or null
            }
            Map<?, ?> currentMap = (Map<?, ?>) current;
            if (!currentMap.containsKey(part)) {
                return MISSING;
            }
            current = currentMap.get(part);
        }

        return current;
    }
}

```

---

### Approach 2: Follow-Up — Evaluating Arbitrarily Deep Trees Without Recursion (Heap Stack)

#### Why the Interviewer Asks This

> **Interviewer:** *"Throwing an exception at `MAX_DEPTH = 500` prevents a crash, which is great. What if we actually **must** evaluate valid machine-generated trees that are 50,000 levels deep without throwing a `StackOverflowError`, while STILL keeping short-circuit evaluation?"*

#### How to Narrate This in the Interview

> *"To evaluate a 50,000-level deep tree without using the JVM call stack, I can move the stack frames onto the **Heap** using an explicit `Deque<Frame>` (`ArrayDeque`).*
> *Each `Frame` holds the `PredicateNode` and an `index` pointing to the next child to evaluate. Because we only push **one child at a time** onto the stack, the moment a child finishes and sets `lastResult`, the parent `Frame` inspects `lastResult`:*
> * *If the parent is `AND` and `lastResult == false`, the parent pops itself immediately (**short-circuiting** all remaining children!).*
> * *If the parent is `OR` and `lastResult == true`, the parent pops itself immediately (**short-circuiting** all remaining children!).*"
> 
> 

#### Java Code (Iterative Short-Circuiting Evaluator — Immune to `StackOverflowError`)

```java
// Drop-in replacement for evaluate() that runs in O(D) Heap memory with 0 recursion!
private static class StackFrame {
    final PredicateNode node;
    int childIndex = 0; // Tracks which child we should evaluate next

    StackFrame(PredicateNode node) {
        this.node = node;
    }
}

public boolean evaluateIterative(PredicateNode root, Map<String, Object> doc) {
    if (root == null || doc == null) return false;

    Deque<StackFrame> stack = new ArrayDeque<>();
    stack.push(new StackFrame(root));

    // Holds the boolean result returned by the most recently finished child frame
    Boolean lastChildResult = null;

    while (!stack.isEmpty()) {
        StackFrame current = stack.peek();
        PredicateNode node = current.node;

        // Case 1: LEAF node -> Evaluate immediately and pass result up to parent
        if (node.type == NodeType.LEAF) {
            lastChildResult = evaluateLeaf(node, doc);
            stack.pop();
            continue;
        }

        // Case 2: We just returned from evaluating a child of this operator node!
        if (current.childIndex > 0 && lastChildResult != null) {
            if (node.type == NodeType.AND && !lastChildResult) {
                // SHORT-CIRCUIT AND: A child was false, so this entire AND node is false!
                lastChildResult = false;
                stack.pop();
                continue;
            }
            if (node.type == NodeType.OR && lastChildResult) {
                // SHORT-CIRCUIT OR: A child was true, so this entire OR node is true!
                lastChildResult = true;
                stack.pop();
                continue;
            }
            if (node.type == NodeType.NOT) {
                // NOT has only 1 child: invert its result and pop
                lastChildResult = !lastChildResult;
                stack.pop();
                continue;
            }
        }

        // Case 3: If there are more children left to evaluate, push the NEXT child only
        if (current.childIndex < node.children.size()) {
            PredicateNode nextChild = node.children.get(current.childIndex);
            current.childIndex++;
            stack.push(new StackFrame(nextChild));
            lastChildResult = null; // Reset before diving into the new child
        } else {
            // Case 4: All children finished without short-circuiting!
            // - For AND: all children were true -> result is true
            // - For OR: all children were false -> result is false
            lastChildResult = (node.type == NodeType.AND);
            stack.pop();
        }
    }

    return lastChildResult != null && lastChildResult;
}

```

---

## Part 3: Complexity & Unit Test Suite (How to Close the Interview)

### 1. Complexity Analysis

Let **$N$** = total nodes in the predicate tree, **$D$** = maximum depth of the predicate tree, and **$P$** = maximum segments in a nested field path (e.g., `"a.b.c"` has $P = 3$).

* **Time Complexity:** $\mathcal{O}(N \cdot P)$ in the worst case (when no branch short-circuits), or $\mathcal{O}(N)$ when fields are flat ($P = 1$). With short-circuiting, best-case time can be as fast as $\mathcal{O}(D \cdot P)$.
* **Space Complexity:**
* **Approach 1 (Recursive):** $\mathcal{O}(D)$ on the JVM call stack, capped at `MAX_DEPTH`.
* **Approach 2 (Iterative):** $\mathcal{O}(D)$ on the Heap. Notice that because we push children **one at a time** rather than all at once, the stack size is strictly proportional to the depth $D$, never the width of a node's children!



### 2. High-Signal Test Cases to Mention in the Interview

When asked *"How would you test this?"*, walk through these 6 categories:

1. **Happy Path (Nested `AND`/`OR`/`NOT`):** The example tree above returning `true`.
2. **Short-Circuit Verification:** An `AND` node where child 1 is `false` and child 2 references a missing/expensive field—verify child 2 is never evaluated.
3. **Nested Field Paths & Edge Cases:**
* Valid nested path: `"user.address.city" == "Gurugram"` $\rightarrow$ `true`.
* Path traverses through a primitive: `"age.invalidSubfield" == 10` $\rightarrow$ returns `false` cleanly (no `ClassCastException`).
* Missing field vs. explicit `null`: `"missingField" != "abc"` $\rightarrow$ returns `false` (missing fields shouldn't accidentally match `NOT_EQUALS`).


4. **Type Mismatch Safety:** Document has `"age": "twenty"` (a `String`), predicate checks `"age" > 18` (`Integer`) $\rightarrow$ safely returns `false` instead of throwing `ClassCastException`.
5. **Empty Operator Children:** `AND([])` $\rightarrow$ `true`, `OR([])` $\rightarrow$ `false`.
6. **Adversarial Depth Test:** Programmatically construct a chain of 20,000 nested `AND` nodes wrapping a single leaf `x == 1`:
* Verify `evaluateRecursive` throws a clean `IllegalArgumentException` at depth 501 (not a fatal `StackOverflowError`).
* Verify `evaluateIterative` evaluates all 20,000 levels and returns `true` effortlessly.
