//package bytecode_manipulation;
//
//import javassist.CannotCompileException;
//import javassist.NotFoundException;
//import org.junit.jupiter.api.Test;
//
//import java.io.IOException;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * @author Chao Chen
// */
//public class MeasurementDecoratorTest {
//    private static final String SAMPLE_CLASS_NAME = "com.waitingforcode.SamplePrinter";
//    private static final String OTHER_CLASS_NAME = "com.waitingforcode.OtherPrinter";
//
//    @Test
//    public void should_decorate_class() throws NotFoundException, IOException, CannotCompileException, IllegalAccessException, InstantiationException, InterruptedException {
//        MeasurementDecorator.decorateClass(SAMPLE_CLASS_NAME).toClass();
//        // It works because class loader was requested to load new representation of SamplePrinter
//        SamplePrinter printer = new SamplePrinter();
//        printer.printLine();
//        printer.printLine();
//
//        List<Long> stats = StatsHolder.getStats(SAMPLE_CLASS_NAME, "printLine");
//
//        assertThat(stats).hasSize(2);
//    }
//
//    @Test
//    public void should_not_decorate_class_after_loading() throws InterruptedException, IOException, CannotCompileException, NotFoundException, IllegalAccessException, InstantiationException {
//        MeasurementDecorator.decorateClass(OTHER_CLASS_NAME);
//        // It won't work because class loader doesn't know about new Class representation for OtherPrinter
//        OtherPrinter printer = new OtherPrinter();
//        printer.printLine();
//        printer.printLine();
//
//        List<Long> stats = StatsHolder.getStats(OTHER_CLASS_NAME, "printLine");
//
//        assertThat(stats).isNull();
//    }
//}
