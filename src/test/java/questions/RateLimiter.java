package questions;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RateLimiter {


    public class TokenBucketRateLimiter {
        private final long capacity;          // max tokens in the bucket
        private final long refillTokens;      // tokens to add per interval
        private final long refillIntervalMs;  // interval in milliseconds

        private AtomicLong availableTokens;   // current tokens
        private volatile long lastRefillTimestamp;

        public TokenBucketRateLimiter(long capacity, long refillTokens, long refillIntervalMs) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillIntervalMs = refillIntervalMs;
            this.availableTokens = new AtomicLong(capacity); // start full
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        private synchronized void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTimestamp;

            if (elapsed > 0) {
                long tokensToAdd = (elapsed / refillIntervalMs) * refillTokens;
                if (tokensToAdd > 0) {
                    long newTokens = Math.min(capacity, availableTokens.get() + tokensToAdd);
                    availableTokens.set(newTokens);
                    lastRefillTimestamp = now;
                }
            }
        }

        /**
         * Try to consume a token. Returns true if allowed, false otherwise.
         */
        public boolean tryConsume() {
            refill();
            if (availableTokens.get() > 0) {
                availableTokens.decrementAndGet();
                return true;
            }
            return false;
        }

        /**
         * Try to consume n tokens. Returns true if allowed, false otherwise.
         */
        public boolean tryConsume(int tokens) {
            refill();
            if (availableTokens.get() >= tokens) {
                availableTokens.addAndGet(-tokens);
                return true;
            }
            return false;
        }

        // For debugging
        public long getAvailableTokens() {
            refill();
            return availableTokens.get();
        }


    }


    public class ScheduledTokenBucket {
        private final long capacity;                 // max tokens in the bucket
        private final long refillTokens;             // tokens per interval
        private final long refillIntervalMs;         // interval length
        private final AtomicLong availableTokens;    // current tokens

        private final ScheduledExecutorService scheduler;

        public ScheduledTokenBucket(long capacity, long refillTokens, long refillIntervalMs) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillIntervalMs = refillIntervalMs;
            this.availableTokens = new AtomicLong(capacity);

            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            startRefillTask();
        }

        private void startRefillTask() {
            scheduler.scheduleAtFixedRate(() -> {
                long current = availableTokens.get();
                if (current < capacity) {
                    long newValue = Math.min(capacity, current + refillTokens);
                    availableTokens.set(newValue);
                }
            }, refillIntervalMs, refillIntervalMs, TimeUnit.MILLISECONDS);
        }

        /**
         * Try to consume one token.
         */
        public boolean tryConsume() {
            return tryConsume(1);
        }

        /**
         * Try to consume n tokens.
         */
        public boolean tryConsume(int tokens) {
            long current;
            do {
                current = availableTokens.get();
                if (current < tokens) {
                    return false;
                }
            } while (!availableTokens.compareAndSet(current, current - tokens));
            return true;
        }

        public long getAvailableTokens() {
            return availableTokens.get();
        }

        public void shutdown() {
            scheduler.shutdownNow();
        }
    }

    public static class LeakyBucketRateLimiter {
        private final long capacity;       // Max requests in the bucket
        private final long leakRate;       // Tokens leaked per interval
        private final long leakIntervalMs; // Interval duration in ms

        private long water;                // Current water level (requests queued)
        private long lastLeakTimestamp;    // Timestamp of the last leak
        private final Lock lock = new ReentrantLock();

        public LeakyBucketRateLimiter(long capacity, long leakRate, long leakIntervalMs) {
            this.capacity = capacity;
            this.leakRate = leakRate;
            this.leakIntervalMs = leakIntervalMs;
            this.water = 0;
            this.lastLeakTimestamp = System.currentTimeMillis();
        }

        /**
         * Leaks water from the bucket based on elapsed time.
         * This method is not thread-safe and must be called from within a locked block.
         */
        private void leak() {
            long now = System.currentTimeMillis();
            long elapsedTime = now - lastLeakTimestamp;

            if (elapsedTime > leakIntervalMs) {
                long intervalsPassed = elapsedTime / leakIntervalMs;
                long leakedAmount = intervalsPassed * leakRate;

                if (leakedAmount > 0) {
                    this.water = Math.max(0, this.water - leakedAmount);
                    // Important: Only advance the timestamp by the intervals we've processed.
                    // This preserves the remainder time for the next calculation.
                    this.lastLeakTimestamp += intervalsPassed * leakIntervalMs;
                }
            }
        }

        /**
         * Tries to add a request to the bucket.
         *
         * @return true if the request is accepted, false if it is dropped.
         */
        public boolean tryConsume() {
            lock.lock();
            try {
                leak(); // Update bucket level first
                if (water < capacity) {
                    water++;
                    return true;
                }
                return false; // Bucket is full
            } finally {
                lock.unlock();
            }
        }

        /**
         * Returns the current number of requests in the bucket.
         * This is a snapshot and may change immediately after the call.
         *
         * @return The current water level.
         */
        public long getCurrentLevel() {
            lock.lock();
            try {
                leak(); // Ensure the level is up-to-date before returning
                return water;
            } finally {
                lock.unlock();
            }
        }
    }

}
