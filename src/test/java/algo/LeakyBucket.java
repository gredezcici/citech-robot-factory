package algo;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class LeakyBucket {

    private final long capacity; // Maximum capacity of the bucket
    private long waterLevel;     // Current water level in the bucket
    private long leakRate;       // Rate at which water leaks (in milliseconds)
    private long lastLeakTime;   // The last time the bucket leaked

    public LeakyBucket(long capacity, long leakRate) {
        this.capacity = capacity;
        this.waterLevel = 0;
        this.leakRate = leakRate;
        this.lastLeakTime = System.currentTimeMillis();
    }

    // Method to add a request to the bucket
    public synchronized boolean allowRequest() {
        long currentTime = System.currentTimeMillis();
        
        // Leak water over time
        long timeElapsed = currentTime - lastLeakTime;
        long waterToLeak = timeElapsed / leakRate;

        // Adjust water level to simulate leaking
        if (waterToLeak > 0) {
            waterLevel = Math.max(waterLevel - waterToLeak, 0);
            lastLeakTime = currentTime;
        }

        // If there's enough space in the bucket, allow the request
        if (waterLevel < capacity) {
            waterLevel++;
            return true;  // Request is allowed
        } else {
            return false; // Bucket is full, request is denied
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Create a LeakyBucket with a capacity of 5 requests and a leak rate of 1000ms (1 second)
        LeakyBucket bucket = new LeakyBucket(5, 1000);

        // Simulate incoming requests
        for (int i = 0; i < 10; i++) {
            if (bucket.allowRequest()) {
                System.out.println("Request " + (i + 1) + " allowed.");
            } else {
                System.out.println("Request " + (i + 1) + " denied (bucket full).");
            }
            TimeUnit.MILLISECONDS.sleep(500); // Simulate some time between requests
        }
    }
}
