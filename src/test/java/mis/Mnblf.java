package mis;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Chao Chen
 */
public class Mnblf {
    public volatile String w = "0";

    public static void main(String[] args) throws IOException {

        C2 c = new C2();
        c.foo1();
        CyclicBarrier barrier = new CyclicBarrier(3);
        Mnblf mnblf = new Mnblf();
        int[] nums = {-1, 0, 3, 3, 3, 5, 9, 12};
        int idx = mnblf.search(nums, 3);

        System.out.println(idx);
        idx = mnblf.leftBound(nums, 3);
        System.out.println("left bound: "+idx);
        idx = mnblf.rightBound(nums,3);
        System.out.println("right bound: "+idx);

//        ArrayBlockingQueue queue = new ArrayBlockingQueue(10);
//
//        queue.offer(10);
//        queue.offer(10);
//
//        Deque<Integer> deque = new ArrayDeque<>();
//        deque.push(1);
//
//        int[] arr = {3, 2, 1, 0, 0};
//        int res = mnblf.findKthLargest(arr, arr.length / 2);
//        System.out.println(res);

    }

    public int search(int[] nums, int target) {
        int l = 0;
        int h = nums.length;
        while (l < h) {
            int mid = l + (h - l) / 2;
            // System.out.println(l+" "+mid+" "+h);
            if (nums[mid] == target) {
                return mid;
            } else if (nums[mid] > target) {
                h = mid;
            } else {
                l = mid + 1;
            }
            // System.out.println(l+" "+mid+" "+h);
        }
        return l;
    }

    public int findKthLargest(int[] nums, int k) {
        int rank = nums.length - k;
        int start = 0;
        int end = nums.length - 1;
        while (start < end) {
            int pivot = partition(nums, start, end);
            if (rank > pivot) {
                start = pivot + 1;
            } else if (rank < pivot) {
                end = pivot - 1;
            } else
                return nums[pivot];
        }
        return nums[start];

    }

    private int partition(int[] nums, int start, int end) {
        int pivot = nums[start];
        while (start < end) {
            while (start < end && nums[end] >= pivot) end--;
            if (start < end) {
                nums[start] = nums[end];
                start++;
            }
            while (start < end && nums[start] < pivot) start++;
            if (start < end) {
                nums[end] = nums[start];
                end--;
            }

        }
        nums[start] = pivot;

        return start;
    }

    public int leftBound(int[] nums, int target) {
        if (nums.length == 0) return -1;
        int left = 0;
        int right = nums.length;

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] >= target) {
                right = mid;
            } else if (nums[mid] < target) {
                left = mid + 1;
            }
        }
        return left;
    }

    public int rightBound(int[] nums, int target) {
        if (nums.length == 0) return -1;
        int left = 0;
        int right = nums.length;

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] <= target) {
                left = mid + 1;
            } else if (nums[mid] > target) {
                right = mid;
            }
        }
        return left;
    }

    ReentrantLock lock = new ReentrantLock();
    AtomicInteger sentinel = new AtomicInteger(0);
    AtomicBoolean zeroTurn = new AtomicBoolean(true);
    Condition odd = lock.newCondition();
    Condition even = lock.newCondition();
    Condition zero = lock.newCondition();

    public void printZero(int n) throws InterruptedException {
        try {
            for (int i = 0; i < n; i++) {
                lock.lock();
                while (!zeroTurn.get()) zero.await();
                System.out.println(0);
                zeroTurn.getAndSet(false);
                if (sentinel.incrementAndGet() % 2 == 0) even.signal();
                else odd.signal();
            }

        } finally {
            lock.unlock();
        }
    }
}
