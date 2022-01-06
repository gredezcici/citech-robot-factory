package acceptance;

import entity.RobotPart;
import org.citech.citechrobotfactory.CitechRobotFactoryApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

/** @author chaochen */
@SpringBootTest(classes = {CitechRobotFactoryApplication.class})
public class RobotPartAvailAtomicityTest {
  @Test
  void should_op_available_as_single_thread() throws InterruptedException {
    RobotPart robotPart = new RobotPart("O", "Material", "Material X", 0.1, 410);
    CountDownLatch latch = new CountDownLatch(4);
    ThreadPoolExecutor threadPool =
        new ThreadPoolExecutor(4, 8, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(4));
    Runnable runnable =
        () -> {
          int i = 100;
          while (i-- > 0) {
            robotPart.remove(1);
          }
          latch.countDown();
        };
    threadPool.execute(runnable);
    threadPool.execute(runnable);
    threadPool.execute(runnable);
    threadPool.execute(runnable);
    latch.await();
    assertThat(robotPart.getAvailable()).isEqualTo(10);
    robotPart.remove(11);
    assertThat(robotPart.getAvailable()).isEqualTo(10);
  }
}
