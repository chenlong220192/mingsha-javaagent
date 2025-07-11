package site.mingsha.javaagent.methodtime.collector;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MethodTimeRecord 单元测试（真实对象）。
 * Unit test for MethodTimeRecord (use real object for final fields).
 *
 * @author mingsha
 */
public class MethodTimeRecordTest {
    @Test
    public void testConstructorAndFields() {
        MethodTimeRecord rec = new MethodTimeRecord("C", "m", 1L, 2L, 100L, "main", "extra");
        assertEquals("C", rec.className);
        assertEquals("m", rec.methodName);
        assertEquals(1L, rec.startTime);
        assertEquals(2L, rec.endTime);
        assertEquals(100L, rec.durationNs);
        assertEquals("main", rec.threadName);
        assertEquals("extra", rec.extraInfo);
    }
    @Test
    public void testRealRecord() {
        MethodTimeRecord rec = new MethodTimeRecord("mockC", "mockM", 1, 2, 3, "t", "e");
        assertEquals("mockC", rec.className);
        assertEquals("mockM", rec.methodName);
    }
} 