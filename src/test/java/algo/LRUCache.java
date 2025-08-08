package algo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An implementation of a Least Recently Used (LRU) cache using a
 * java.util.LinkedHashMap.
 * <p>
 * This class extends LinkedHashMap and leverages its capabilities to maintain
 * access order and automatically evict the eldest entry when the cache is full.
 *
 * @param <K> The type of the keys.
 * @param <V> The type of the values.
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int capacity;

    /**
     * Constructs an LRUCache with a specific capacity.
     *
     * @param capacity The maximum number of items the cache can hold.
     */
    public LRUCache(int capacity) {
        // Call the LinkedHashMap constructor with the following parameters:
        // - initialCapacity: The capacity of the cache.
        // - loadFactor: A standard load factor (0.75 is typical).
        // - accessOrder: true. This is the crucial part. It orders entries
        //   based on when they were last accessed, from least-recent to
        //   most-recent.
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    /**
     * This method is invoked by put and putAll after inserting a new entry
     * into the map. It returns true if the eldest entry should be removed,
     * which is the case when the cache size exceeds its capacity.
     *
     * @param eldest The least recently accessed entry in the map.
     * @return true if the eldest entry should be removed, false otherwise.
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        // Return true to trigger removal of the least recently used item
        // if the current size is greater than the configured capacity.
        return size() > capacity;
    }
    /**
     * A helper method to create an instance of the LRUCache.
     *
     * @param <K>      The key type.
     * @param <V>      The value type.
     * @param capacity The capacity of the cache.
     * @return A new instance of LRUCache.
     */
    public static <K, V> LRUCache<K, V> newInstance(int capacity) {
        return new LRUCache<>(capacity);
    }

    /**
     * Main method to demonstrate the LRU Cache functionality.
     */
    public static void main(String[] args) {
        System.out.println("--- LRU Cache Demonstration (Capacity = 3) ---");

        // Create a cache with a capacity of 3.
        LRUCache<Integer, String> cache = LRUCache.newInstance(3);

        System.out.println("Putting items (1, 2, 3)...");
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        System.out.println("Cache state: " + cache); // Expected: {1=A, 2=B, 3=C}

        System.out.println("\nAccessing item with key 1...");
        cache.get(1); // Accessing 1 makes it the most recently used.
        System.out.println("Cache state: " + cache); // Expected: {2=B, 3=C, 1=A}

        System.out.println("\nPutting a new item (4). This should evict key 2.");
        cache.put(4, "D"); // Adding 4 should cause the eldest (key 2) to be removed.
        System.out.println("Cache state: " + cache); // Expected: {3=C, 1=A, 4=D}

        System.out.println("\nPutting another item (5). This should evict key 3.");
        cache.put(5, "E"); // Adding 5 should cause the eldest (key 3) to be removed.
        System.out.println("Cache state: " + cache); // Expected: {1=A, 4=D, 5=E}
    }
}