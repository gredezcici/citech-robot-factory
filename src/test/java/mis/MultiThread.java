package mis;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * @author Chao Chen
 */
public class MultiThread {
    private int count = 1;
    private Object obj = new Object();
    private FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
        @Override
        public String call() throws Exception {
            return "MyTask";
        }
    });

    public void run() {
        Thread t1 = new Thread(
                () -> {
                    try {
                        task.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // logic 1
                    count++;

                    synchronized (obj) {
                        obj.notify();
                    }
                }
        );

        Thread t2 = new Thread(
                () -> {
                    task.run();

                    synchronized (obj) {
                        try {
                            obj.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    // logic 2
                    if (count > 1) {
                        System.out.println("t2 running");
                    }
                }
        );
        t1.start();
        t2.start();
    }
    static int x;
    public static void main(String[] args) {

//        for (int i = 0; i < 100; i++) {
//            System.out.println(i);
//            MultiThread m = new MultiThread();
//            m.run();
//        }



    }
}
