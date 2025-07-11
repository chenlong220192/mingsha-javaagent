package site.mingsha.javaagent.methodtime.monitor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentMonitor 异常统计功能单元测试。
 * Unit test for AgentMonitor error statistics.
 * 验证采集、写入、转存异常统计是否准确。
 * Verify error statistics for collect, write, and failover are correct.
 *
 * @author mingsha
 * 
 */
public class AgentMonitorTest {
    /**
     * 测试异常统计功能。
     * Test error statistics.
     * 预期：各异常类型统计数值准确。
     * Expect: Error count for each type is correct.
     */
    @Test
    public void testErrorStats() {
        AgentMonitor monitor = new AgentMonitor();
        monitor.recordCollectError();
        monitor.recordWriteError();
        monitor.recordFailoverError();
        String stats = monitor.getErrorStats();
        assertTrue(stats.contains("采集异常: 1"));
        assertTrue(stats.contains("写入异常: 1"));
        assertTrue(stats.contains("转存异常: 1"));
    }
} 