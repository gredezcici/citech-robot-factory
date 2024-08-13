package reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.SynchronousSink;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * @author Chao Chen
 */
public class FluxSample {
    public static void main(String[] args) throws InterruptedException {
        Flux<Integer> integerFlux = Flux.create((FluxSink<Integer> fluxSink) -> {
            IntStream.range(0, 5)
                    .peek(i -> System.out.println("going to emit - " + i))
                    .forEach(fluxSink::next);
        });
        //First observer. takes 1 ms to process each element
        integerFlux.delayElements(Duration.ofMillis(1)).subscribe(i -> System.out.println("First :: " + i));

//Second observer. takes 2 ms to process each element
        integerFlux.delayElements(Duration.ofMillis(2)).subscribe(i -> System.out.println("Second:: " + i));
        //        AtomicInteger atomicInteger = new AtomicInteger();
//
////Flux generate sequence
//        Flux<Integer> integerFlux2 = Flux.generate((SynchronousSink<Integer> synchronousSink) -> {
//            System.out.println("Flux generate");
//            synchronousSink.next(atomicInteger.getAndIncrement());
//        });
//
////observer
//        integerFlux2.delayElements(Duration.ofMillis(50))
//                .subscribe(i -> System.out.println("First consumed ::" + i));
        Thread.currentThread().join();
    }
}
