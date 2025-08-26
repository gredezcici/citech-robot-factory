package jmh;

import mis.C2;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Chao Chen
 */
class Counter {

    public  volatile long count1 = 0;
    public  volatile long count2 = 0;

}

public class FSSample {
    public static void main(String[] args) {

        Counter counter1 = new Counter();
        Counter counter2 = new Counter();

        long iterations = 1_000_000_000;

        Thread thread1 = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            for (long i = 0; i < iterations; i++) {
                counter1.count1++;
            }
            long endTime = System.currentTimeMillis();
            System.out.println("total time: " + (endTime - startTime));
        });
        Thread thread2 = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            for (long i = 0; i < iterations; i++) {
                counter2.count2++;
            }
            long endTime = System.currentTimeMillis();
            System.out.println("total time: " + (endTime - startTime));
        });

        thread1.start();
        thread2.start();
        C2 c2 = new C2();
        Lock lock = new ReentrantLock();

    }
}
