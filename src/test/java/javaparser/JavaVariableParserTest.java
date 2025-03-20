package javaparser;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class JavaVariableParserTest {
    @Test
    public void testParser() {
        Path p = Paths.get("/Users/chenchao/IdeaProjects/citech-robot-factory/src/test/java/javaparser/ForTestOnly.java");
        new JavaVariableParser().traceVariable(p);
    }
}
