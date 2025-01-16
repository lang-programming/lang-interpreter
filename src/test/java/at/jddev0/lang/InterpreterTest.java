package at.jddev0.lang;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class InterpreterTest {
    @Test
    public void langSpecTest() {
        File mainTestFile = new File("src/test/resources/spec-test/test.lang");

        String currentDir = mainTestFile.getParent();

        LangInterpreter interpreter = new LangInterpreter(currentDir, "test.lang", null, new DefaultLangPlatformAPI(), null);

        try(BufferedReader br = new BufferedReader(new FileReader(mainTestFile))) {
            interpreter.interpretLines(br);
        }catch(IOException e) {
            throw new RuntimeException(e);
        }

        int testCount = interpreter.langTestStore.getTestCount();
        int passedTestCount = interpreter.langTestStore.getTestPassedCount();


        assertTrue(testCount > 0, "Lang spec tests where not initialized correctly");
        assertEquals(testCount, passedTestCount, String.format("Some Lang spec test failed: There are %d tests but only %d tests passed.", testCount, passedTestCount));
    }
}
