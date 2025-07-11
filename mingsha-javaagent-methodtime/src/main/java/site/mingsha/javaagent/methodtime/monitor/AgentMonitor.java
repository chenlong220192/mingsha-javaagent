package site.mingsha.javaagent.methodtime.monitor;

import site.mingsha.javaagent.methodtime.collector.MethodTimeBuffer;
import site.mingsha.javaagent.methodtime.config.AgentConfig;

/**
 * agent自监控：拦截延迟、CPU/内存占用、数据丢失率。
 * Agent self-monitoring: intercept latency, CPU/memory usage, data loss rate.
 * 支持CPU熔断、线程健康、异常统计等高可用特性。
 * Supports CPU fuse, thread health, error statistics and other HA features.
 *
 * @author mingsha
 */
public class AgentMonitor implements Runnable {
    private volatile boolean running = true;
    private long lastTotalLost = 0;
    private long totalLost = 0;
    private long lastCheckTime = System.currentTimeMillis();
    private volatile boolean fuseActive = false;
    private volatile long lastFuseTime = 0;
    private long collectErrorCount = 0;
    private long writeErrorCount = 0;
    private long failoverErrorCount = 0;

    /**
     * 停止监控线程。
     * Stop the monitor thread.
     */
    public void stop() { running = false; }

    /**
     * 判断当前是否处于CPU熔断状态。
     * Check if CPU fuse is active.
     * @return true-熔断，false-未熔断 | true if fuse active, false otherwise
     */
    public boolean isFuseActive() { return fuseActive; }

    /**
     * 监控主循环，定期采集CPU、内存、丢失率、熔断等信息，并输出日志。
     * Main monitor loop, periodically collects CPU, memory, loss rate, fuse status, and logs.
     * 若CPU超阈值则触发熔断，低于阈值-10%自动恢复。
     * If CPU exceeds threshold, triggers fuse; recovers automatically when below threshold-10%.
     */
    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                break;
            }
            double cpuLoad = getProcessCpuLoad();
            int fuseThreshold = AgentConfig.getCpuFuseThreshold();
            if (cpuLoad >= fuseThreshold / 100.0) {
                fuseActive = true;
                lastFuseTime = System.currentTimeMillis();
                System.err.printf("[mingsha-agent][熔断] CPU占用 %.2f%% 超阈值 %d%%，暂停采集...\n", cpuLoad * 100, fuseThreshold);
            } else if (fuseActive && cpuLoad < (fuseThreshold - 10) / 100.0) {
                // 低于阈值-10%自动恢复
                fuseActive = false;
                System.out.println("[mingsha-agent][熔断] CPU恢复，采集线程恢复工作");
            }
            // 采集数据丢失率（队列满时丢弃）
            long lost = totalLost - lastTotalLost;
            lastTotalLost = totalLost;
            // CPU/内存占用
            long usedMem = getUsedMemory();
            // TODO: 统计拦截延迟（需在字节码增强处埋点）
            System.out.printf("[mingsha-agent][监控] CPU: %.2f%%, 内存: %d MB, 5s丢失: %d 条\n", cpuLoad * 100, usedMem / 1024 / 1024, lost);
        }
    }

    /**
     * 记录采集数据丢失事件。
     * Record a data loss event.
     */
    public void recordLost() { totalLost++; }

    /**
     * 获取当前监控状态字符串（CPU、内存、丢失总数）。
     * Get current monitor status string (CPU, memory, total loss).
     * @return 状态字符串 | status string
     */
    public String getStatus() {
        double cpuLoad = getProcessCpuLoad();
        long usedMem = getUsedMemory();
        return String.format("CPU: %.2f%%, 内存: %d MB, 总丢失: %d 条", cpuLoad * 100, usedMem / 1024 / 1024, totalLost);
    }

    /**
     * 获取当前进程CPU占用率。
     * Get current process CPU usage rate.
     * @return CPU占用（0~1）| CPU usage (0~1)
     */
    private double getProcessCpuLoad() {
        try {
            com.sun.management.OperatingSystemMXBean os =
                    (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return os.getProcessCpuLoad();
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * 获取当前进程已用内存。
     * Get used memory of current process.
     * @return 已用内存字节数 | used memory in bytes
     */
    private long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * 记录采集异常次数。
     * Record collect error count.
     */
    public void recordCollectError() { collectErrorCount++; }
    /**
     * 记录写入异常次数。
     * Record write error count.
     */
    public void recordWriteError() { writeErrorCount++; }
    /**
     * 记录转存异常次数。
     * Record failover error count.
     */
    public void recordFailoverError() { failoverErrorCount++; }

    /**
     * 获取各环节异常统计信息。
     * Get error statistics for all stages.
     * @return 异常统计字符串 | error statistics string
     */
    public String getErrorStats() {
        return String.format("采集异常: %d, 写入异常: %d, 转存异常: %d", collectErrorCount, writeErrorCount, failoverErrorCount);
    }
} 