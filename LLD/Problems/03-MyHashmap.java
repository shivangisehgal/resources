
import java.util.Objects;

/*

I will have two classes - one for node, one for the hashmap table itseld.
The node (generic key and value types) will be a key, value, next Node, as well has the hashing function value stored.
I am storing hash, so that i dont have to re-calculate it every time (key.hashCode())

My hashmap class will have an array of nodes,  capacity (initial allocated size), size (current number of elements), load factor (threshold at which the hashmap is resized), load factor (default is 0.75)).
It will support generics for the key and value types.
It will have public methods to put, get, remove. 
It will have a private methods 
    - to call the hash function to get the index for the key,
    - indexFor to get the index for the key,  
    - resize the hashmap when the size exceeds the capacity.

I am assuming, we are using the hash function as java's default hash function (Object.hashCode()).

<creating the classes>

                  +----------------------+
                  |     MyHashMap<K,V>   |
                  +----------------------+
                  | Node<K,V>[] buckets  |
                  | int capacity         |
                  | int size             |
                  | float loadFactor     |
                  |       ...            |
                  +----------------------+
                  | put()               |
                  | get()               |
                  | remove()            |
                  | - resize()          |
                  | - hash()            |
                  | - indexFor()        |
                  +----------+----------+
                             |
                             |
                             v

                 +---------------------+
                 |    Node<K,V>        |
                 +---------------------+
                 | K key               |
                 | V value             |
                 | int hash            |
                 | Node next           |
                 +---------------------+


Now, the constructor will either be default (default capacity and load factor) or a single argument constructor (capacity).
Ideally, it is recommended to use a power of 2 for the capacity, so that the hash function can be more efficient.
        - why? because the hash function is modulous operation, and if the capacity is a power of 2, then the modulous operation can be replaced with a bitwise AND operation.
        - example: capacity = 16, then the index for the key will be key.hashCode() & (16 - 1) = key.hashCode() & 15.
        - this is more efficient than the modulous operation because the bitwise AND operation is faster than the modulous operation.

So, for a given capacity, we will find the next greater power of 2 of it (tableForSize method)
<Implementing the tableForSize method and justify optimization>


The key and hash are final because they are not supposed to be changed after the node is created.
Now constructors for both the classes are implemented.

I will implement the put method (while implementing this, I will also implement the hash method (returns hash code of the key) and indexFor method (returns index for the key from the hash code and capacity)))

While implementing the put, also discuss why we are storing hash in the node, and why we are not using the hash code of the key directly.

[Contract between  hashCode and equals method]
- hashCode is used to compute the index for the key.
- equals is used to check if the key is already present in the bucket.

- Equal objects must have the same hashCode.
- Different objects may have the same hashCode (collision).
- If two objects are equal according to equals(), they must return the same hashCode().

- hashCode should be consistent with equals.
- hashCode should be fast to compute.
- hence, node.hash == hash && node.key.equals(key) should be true for the same key.
- hence, it is faster to first check if the hash is the same, and only then check if the key is the same.

I will discuss the optimization of the hash function and indexFor method.

Get and Remove methods will be implemented after the put method is implemented. similar.

Then i will implement the resize method (when the size exceeds the capacity, we will resize the hashmap to double the capacity).
And add this resize method to the put method, so that when the size exceeds the capacity, we will resize the hashmap to double the capacity.

Done!


If collisions in one bucket become excessive, Java converts the linked list into a red-black tree, reducing worst-case lookup from O(n) to O(log n).

*/


public class MyHashmap<K, V>{
    private Node<K, V>[] buckets;
    private int capacity;
    private int size;
    private float loadFactor;

    private static final int DEFAULT_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    public MyHashmap(){
        this.capacity = tableForSize(DEFAULT_CAPACITY);
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        this.size = 0;

        buckets = new Node[this.capacity];
    }

    public MyHashmap(int capacity){
        // find next greater power of 2 for the given capacity
        this.capacity = tableForSize(capacity);
        this.size = 0;
        this.loadFactor = DEFAULT_LOAD_FACTOR;

        buckets = new Node[this.capacity];
    }


    //This method is used to find the next greater power of 2 for the given capacity.
    private int tableForSize(int cap){
        //the working of this method is optimized in actual implementation of java:
        // we could have simply used Math.pow(2, Math.ceil(Math.log(capacity) / Math.log(2)));
        
        //But, I want to implement it myself.
        // so, we also could use a while loop to find the next greater power of 2.
        // int newCapacity = 1;
        // while (newCapacity < capacity){
        //     newCapacity <<= 1;
        // }
        // return newCapacity;

        //Actual implementation of java::
        //I need to first find the most significant (1) bit in the number.
        //And if i make all before it to 1, (Eg: 14: 1110 -> 1111 -> 15) and + 1 in it, i will get the next greater power of 2 (=> 16).

        //This is optimized in actual implementation of java, 
        //Subtractiting 1 would make most significant (1) bit to 0, and if I right shift the original number by 1 and OR with it, i can have the potential to make get first 2 most significant.
        //If i do this again, i can get first 3 then 4 then 5 most significant.
        //But, i don't need to right shift 1 by 1, once i get first 2, then right shift by 2 will give me first 4,
        //Then i need to right shift by 4 to get first 8, and so on.
        
        int n = cap - 1;

        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;

        return n + 1;

        /* 
        Dry run example: cap = 20
        n = 19 (20 - 1) (binary: 10011)
        n |= n >>> 1; => 19 (binary: 10011) | 9 (binary: 01001) = 27 (binary: 11011)
        n |= n >>> 2; => 27 (binary: 11011) | 1101 (binary: 01101) = 31 (binary: 11111)
        n |= n >>> 4; => 31 (binary: 11111) | 1111 (binary: 01111) = 31 (binary: 11111)
        n |= n >>> 8; => 31 (binary: 11111) | 0000 (binary: 00000) = 31 (binary: 11111)
        n |= n >>> 16; => 31 (binary: 11111) | 0000 (binary: 00000) = 31 (binary: 11111)
        return n + 1; => 31 + 1 = 32 (binary: 100000)
        */
    }

    private int hash(K key){
        if (key == null)
            return 0;
        
        int h = key.hashCode();
        return h ^ (h >>> 16);
        
        //Why this is better than key.hashCode()?
        //Because, this will give me a more distributed hash code, and it will be more unique.
        //How? XOR give better probability of getting 0s and 1s than OR or AND.
        //Why >>> 16? Because, we are interested in the lower 16 bits of the hash code.

    }


    private int indexFor(int hash){
        return hash & (capacity - 1); //instead of hash % capacity.
        //This optimization is valid because capacity is a power of 2.
        
    }

    private void resize(){
        //Create a new array with double the capacity.
        //For each item in each bucket, we will need to re-hash and re-index the item.
        // - first, calculate new index of bucket using new capacity (head's hash & (new capacity - 1))
        // - then, add the node to the new bucket.
        

        capacity *= 2;
        Node<K, V>[] newBuckets = new Node[capacity];

        for (Node<K, V> head : buckets) {

            Node<K, V> curr = head;

            while (curr != null) {

                Node<K, V> next = curr.next;
                int index = curr.hash & (capacity - 1);

                curr.next = newBuckets[index];
                newBuckets[index] = curr;

                curr = next;
            }
        }

        buckets = newBuckets;
    }

    static class Node<K, V>{
        private final K key;
        private V value;
        private Node<K, V> next;
        private final int hash;

        public Node(K key, V value, int hash){
            this.key = key;
            this.value = value;
            this.hash = hash;
            this.next = null;
        }
    }


    
    
    
    //One line summary of put method:
    //1. Find the index for the key.
    //2. Check if bucket exists, if bucket exists, check if key exists.
    //3. If key exists, update the value, else, create new node and add to the last of the bucket chain.
    public void put(K key, V value) {

        int hash = hash(key);
        int index = indexFor(hash);
    
        // Check if key already exists
        Node<K, V> curr = buckets[index];
    
        while (curr != null) {
            if (curr.hash == hash && Objects.equals(curr.key, key)) {
                curr.value = value;
                return;
            }
            curr = curr.next;
        }
    
        // Insert at head of the bucket
        Node<K, V> newNode = new Node<>(key, value, hash);
        newNode.next = buckets[index];
        buckets[index] = newNode;
    
        //Discuss resize in last.
        size++;
    
        if (size > capacity * loadFactor) {
            resize();
        }
    }

    //One line summary of get method:
    //1. Find the index for the key.
    //2. Check if bucket exists, if bucket exists, check if key exists.
    //3. If key exists, return the value, else, return null.
    public V get(K key){
        int hash = hash(key);
        int index  = indexFor(hash);

        Node<K, V> head = buckets[index];
        while(head != null){
            if(head.hash == hash && Objects.equals(head.key, key)){
                return head.value;
            }
            head = head.next;
        }
        return null;
    }

    //One line summary of remove method:
    //1. Find the index for the key.
    //2. Check if bucket exists, if bucket exists, check if key exists.
    //3. If key exists, remove the node, else, do nothing.
    public void remove(K key){
        int hash = hash(key);
        int index  = indexFor(hash);

        Node<K, V> curr = buckets[index];
        Node<K, V> prev = null;
        while(curr != null){
            if(curr.hash == hash && Objects.equals(curr.key, key)){
                //remove the node
                if(prev == null){
                    buckets[index] = curr.next;
                } else {
                    prev.next = curr.next;
                }

                size--;

                return;
            }
            prev = curr;
            curr = curr.next;
        }

    }



}


