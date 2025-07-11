package site.mingsha.javaagent.methodtime.collector;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MethodTimeBuffer 单元测试（mock+真实对象）。
 * Unit test for MethodTimeBuffer (use real MethodTimeRecord for final fields).
 *
 * @author mingsha
 */
public class MethodTimeBufferTest {
    @BeforeEach
    @AfterEach
    public void clearQueue() throws Exception {
        Field queueField = MethodTimeBuffer.class.getDeclaredField("queue");
        queueField.setAccessible(true);
        ArrayBlockingQueue<?> queue = (ArrayBlockingQueue<?>) queueField.get(null);
        queue.clear();
    }

    @Test
    public void testOfferAndPollBatch() {
        MethodTimeRecord rec1 = new MethodTimeRecord("A", "m1", 1, 2, 1, "t", "");
        MethodTimeRecord rec2 = new MethodTimeRecord("B", "m2", 2, 3, 1, "t", "");
        assertTrue(MethodTimeBuffer.offer(rec1));
        assertTrue(MethodTimeBuffer.offer(rec2));
        List<MethodTimeRecord> batch = MethodTimeBuffer.pollBatch(2);
        assertEquals(2, batch.size());
        assertSame(rec1, batch.get(0));
        assertSame(rec2, batch.get(1));
    }

    @Test
    public void testBufferCapacity() {
        // 使用测试专用方法重建队列为容量2
        MethodTimeBuffer.resetQueueForTest(2);
        MethodTimeRecord r1 = new MethodTimeRecord("C", "m", 1, 2, 1, "t", "");
        MethodTimeRecord r2 = new MethodTimeRecord("D", "m", 2, 3, 1, "t", "");
        assertTrue(MethodTimeBuffer.offer(r1));
        assertTrue(MethodTimeBuffer.offer(r2));
        // 队列已满，应该返回false
        assertFalse(MethodTimeBuffer.offer(new MethodTimeRecord("E", "m", 3, 4, 1, "t", "")));
        MethodTimeBuffer.pollBatch(2);
    }

    @Test
    public void testConcurrentOffer() throws InterruptedException {
        MethodTimeBuffer.resetQueueForTest(10000); // 恢复大容量，保证并发测试
        int threads = 3, perThread = 5;
        Runnable task = () -> {
            for (int i = 0; i < perThread; i++) {
                MethodTimeBuffer.offer(new MethodTimeRecord("E", "m", i, i+1, 1, Thread.currentThread().getName(), ""));
            }
        };
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) ts[i] = new Thread(task);
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        List<MethodTimeRecord> batch = MethodTimeBuffer.pollBatch(threads * perThread);
        assertEquals(threads * perThread, batch.size());
    }
} 