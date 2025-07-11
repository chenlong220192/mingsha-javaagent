package site.mingsha.javaagent.methodtime.collector;

import site.mingsha.javaagent.methodtime.config.AgentConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 方法耗时数据内存缓冲队列。
 * In-memory buffer queue for method time records.
 * 支持高并发采集、批量消费，保障主进程性能与数据完整性。
 * Supports high-concurrency collection and batch consumption, ensuring main process performance and data integrity.
 *
 * @author mingsha
 */
public class MethodTimeBuffer {
    private static ArrayBlockingQueue<MethodTimeRecord> queue =
            new ArrayBlockingQueue<>(AgentConfig.getQueueCapacity());

    /**
     * 向缓冲队列中添加采集数据。
     * Offer a method time record to the buffer queue.
     * @param record 采集数据 | method time record
     * @return true-成功，false-队列已满 | true if success, false if queue is full
     */
    public static boolean offer(MethodTimeRecord record) {
        return queue.offer(record);
    }

    /**
     * 批量获取并移除队列中的采集数据。
     * Poll and remove a batch of records from the queue.
     * @param batchSize 批量大小 | batch size
     * @return 数据列表 | list of records
     */
    public static List<MethodTimeRecord> pollBatch(int batchSize) {
        List<MethodTimeRecord> batch = new ArrayList<>(batchSize);
        queue.drainTo(batch, batchSize);
        return batch;
    }

    /**
     * 获取当前队列中数据条数。
     * Get current size of the buffer queue.
     * @return 条数 | number of records
     */
    public static int size() {
        return queue.size();
    }

    /**
     * 获取队列总容量（已用+剩余）。
     * Get total capacity of the buffer queue (used + remaining).
     * @return 容量 | total capacity
     */
    public static int capacity() {
        return queue.remainingCapacity() + queue.size();
    }

    /**
     * [仅测试用] 重建缓冲队列容量。
     * [Test only] Reset buffer queue with new capacity.
     * @param capacity 新容量 | new capacity
     */
    static void resetQueueForTest(int capacity) {
        queue = new java.util.concurrent.ArrayBlockingQueue<>(capacity);
    }
} 