package mis;

/**
 * @author Chao Chen
 */
public class Verifier {
    public static void main(String[] args) {
        Thread innerThread = new Thread(new Runnable() {

            public void run() {

                call();
            }
        });

        innerThread.start();
        System.out.println("Main thread exiting");
    }
    public static void call(){
        System.out.println("call I am a new thread !");
    }
}
