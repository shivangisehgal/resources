1. **Approach 1: Clean Recursive DFS** (directly addressing the prompt, plus a 1-line trick to handle `"key": null` without extra wrapper classes).
2. **Approach 2: Iterative BFS** (if the interviewer asks how to avoid `StackOverflowError` on deeply nested JSON or find the shallowest match).

---

## Part 1: Problem Statement, Example & Constraints

### Problem Statement

Given a JSON-like object `root` and a string `targetKey`, search the entire nested structure and return the value associated with `targetKey`.

* JSON Objects are represented as `Map<String, Object>`
* JSON Arrays are represented as `List<Object>`
* Primitives are `String`, `Number`, `Boolean`, or `null`

### Example

```json
{
  "user": "alice",
  "roles": [
    { "team": "payments", "secretKey": "xyz-987" }
  ]
}

```

* `targetKey = "secretKey"` $\rightarrow$ Returns `"xyz-987"`
* `targetKey = "missing"` $\rightarrow$ Returns `null`

### 3 Quick Clarifying Questions (Ask Before Coding)

1. *"Can I assume JSON Objects are `Map<String, Object>` and JSON Arrays are `List<Object>`?"* (Yes)
2. *"If the key appears multiple times, should I return the first one found?"* (Yes)
3. *"What if the key exists, but its value is explicitly `null` (e.g., `{"quota": null}`)?"* (Good catch—we should stop searching and return `null` rather than treating it as "not found").

---

## Part 2: Two-Step Interview Progression

### Approach 1: Recursive DFS (Main Solution)

#### Simple Java Concepts Used

* **`instanceof`:** Checks whether the current `Object` is a `Map` (JSON Object) or a `List` (JSON Array). If it's neither, it's a primitive (`String`, `Integer`, `Boolean`, or `null`), which cannot contain keys.
* **The `NOT_FOUND` Sentinel Trick:** Instead of creating a bulky helper class to distinguish `"key not found"` from `"key found with a null value"`, we just create one private dummy object: `private static final Object NOT_FOUND = new Object();`.
* If a recursive branch doesn't find the key, it returns `NOT_FOUND`.
* If a recursive branch returns anything *other* than `NOT_FOUND` (even `null`!), we know we found the key and immediately return it!



#### How to Narrate This in the Interview

> *"I can treat the JSON structure as a tree where internal nodes are `Map`s or `List`s and leaf nodes are primitives. I'll write a recursive Depth-First Search (DFS).*
> *1. If the current node is a `Map`, I check `map.containsKey(targetKey)` in $\mathcal{O}(1)$ time. If it's there, I return its value immediately. Otherwise, I recursively search each value in the map.*
> *2. If the current node is a `List`, I loop through its elements and recursively search each one.*
> *3. If it's a primitive or `null`, it's a leaf node, so I return.*
> *Also, to cleanly handle the edge case where a key's value in JSON is literally `null`, I'll use a dummy `NOT_FOUND` sentinel object internally so my recursion never confuses a valid `null` value with a missing key."*

#### Java Code (Recursive DFS)

```java
import java.util.*;

public class JsonSearcher {

    // Sentinel object to distinguish "key was not found" vs "key was found, and its value is null"
    private static final Object NOT_FOUND = new Object();

    public Object findKey(Object root, String targetKey) {
        if (root == null || targetKey == null) {
            return null;
        }
        Object result = dfs(root, targetKey);
        // If we got NOT_FOUND back, return null to the caller; otherwise return the actual value
        return result == NOT_FOUND ? null : result;
    }

    private Object dfs(Object current, String targetKey) {
        // Base Case: null or primitive values (String, Number, Boolean) cannot contain keys
        if (current == null) {
            return NOT_FOUND;
        }

        // Case 1: Current node is a JSON Object (Map)
        if (current instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) current;

            // 1a. Check if targetKey exists directly in this Map (O(1) lookup)
            if (map.containsKey(targetKey)) {
                return map.get(targetKey); // Even if this is null, it's != NOT_FOUND!
            }

            // 1b. Otherwise, recursively search inside all child values of this Map
            for (Object value : map.values()) {
                Object res = dfs(value, targetKey);
                if (res != NOT_FOUND) {
                    return res; // Found it! Bubble the result straight up.
                }
            }
        } 
        // Case 2: Current node is a JSON Array (List)
        else if (current instanceof List<?>) {
            List<?> list = (List<?>) current;

            // Recursively search inside each element of the List
            for (Object element : list) {
                Object res = dfs(element, targetKey);
                if (res != NOT_FOUND) {
                    return res; // Found it! Bubble the result straight up.
                }
            }
        }

        // Key was not found in this branch (or 'current' was a primitive)
        return NOT_FOUND;
    }
}

```

---

### Approach 2: Iterative BFS (The Follow-Up Optimization)

#### Why the Interviewer Asks for This

After you write the recursive DFS above, the interviewer will typically ask:

> *"What if the JSON is nested 50,000 levels deep, or what if `targetKey` exists at depth 10 and depth 1, and we want the **shallowest** match?"*

#### How to Narrate This in the Interview

> *"Deep recursion (e.g., 50,000 levels) will blow up the JVM call stack with a `StackOverflowError`, and DFS might find a deeply nested match before a shallow one.*
> *We can solve both by switching to an iterative Breadth-First Search (BFS) using a `Queue` (`ArrayDeque`) on the heap. We explore level-by-level, and as an optimization, we only push `Map` and `List` containers into the queue—skipping primitives completely."*

#### Java Code (Iterative BFS)

```java
import java.util.*;

public class JsonSearcher {

    public Object findKeyBFS(Object root, String targetKey) {
        if (root == null || targetKey == null) {
            return null;
        }

        // Queue lives on the Heap (no StackOverflowError!) and explores level-by-level
        Queue<Object> queue = new ArrayDeque<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            Object current = queue.poll();

            // Case 1: JSON Object (Map)
            if (current instanceof Map<?, ?>) {
                Map<?, ?> map = (Map<?, ?>) current;

                // If the key is at this level, return immediately!
                // (No sentinel needed in BFS because we return directly from the main function)
                if (map.containsKey(targetKey)) {
                    return map.get(targetKey);
                }

                // Add child Maps/Lists to the queue for the next level
                for (Object value : map.values()) {
                    if (value instanceof Map<?, ?> || value instanceof List<?>) {
                        queue.offer(value);
                    }
                }
            } 
            // Case 2: JSON Array (List)
            else if (current instanceof List<?>) {
                List<?> list = (List<?>) current;

                // Add child Maps/Lists to the queue
                for (Object element : list) {
                    if (element instanceof Map<?, ?> || element instanceof List<?>) {
                        queue.offer(element);
                    }
                }
            }
        }

        return null; // Key not found anywhere
    }
}

```

---

## Part 3: Quick Complexity Summary (How to Close the Interview)

* **Time Complexity:** $\mathcal{O}(N)$ for both approaches, where $N$ is the total number of elements (keys, array items, and primitives) in the JSON. In the worst case, we inspect every element once, and `map.containsKey(targetKey)` is $\mathcal{O}(1)$ on average.
* **Space Complexity:**
* **Recursive DFS:** $\mathcal{O}(D)$ call stack memory, where $D$ is the maximum nesting depth.
* **Iterative BFS:** $\mathcal{O}(W)$ heap memory, where $W$ is the maximum width (number of containers at the widest level).
