package site.mingsha.javaagent.methodtime.util.health;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程健康守护：监控并自动重启关键线程。
 * Thread health guardian: monitors and auto-restarts critical threads.
 * 保证采集、写入、监控等核心线程异常退出后自动自愈。
 * Ensures auto-healing of core threads (collect, write, monitor, etc.) after abnormal exit.
 *
 * @author mingsha
 */
public class ThreadGuardian implements Runnable {
    private final Map<String, ThreadFactory> threadFactories = new ConcurrentHashMap<>();
    private final Map<String, Thread> threads = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private final long checkIntervalMs;

    /**
     * 线程工厂接口。
     * Thread factory interface.
     */
    public interface ThreadFactory {
        Thread newThread();
    }

    public ThreadGuardian(long checkIntervalMs) {
        this.checkIntervalMs = checkIntervalMs;
    }

    /**
     * 注册受监控线程及其工厂。
     * Register a monitored thread and its factory.
     * @param name 线程标识 | thread name
     * @param factory 线程工厂 | thread factory
     */
    public void register(String name, ThreadFactory factory) {
        threadFactories.put(name, factory);
        Thread t = factory.newThread();
        threads.put(name, t);
        t.start();
    }
    /**
     * 获取指定名称的线程。
     * Get thread by name.
     * @param name 线程标识 | thread name
     * @return 线程实例 | thread instance
     */
    public Thread getThread(String name) {
        return threads.get(name);
    }
    /**
     * 停止健康守护线程。
     * Stop the health guardian thread.
     */
    public void stop() { running = false; }
    /**
     * 健康守护主循环，定期检查所有受监控线程，异常退出时自动重启。
     * Main loop: periodically checks all monitored threads, auto-restarts on abnormal exit.
     */
    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(checkIntervalMs);
            } catch (InterruptedException e) {
                break;
            }
            for (Map.Entry<String, ThreadFactory> entry : threadFactories.entrySet()) {
                String name = entry.getKey();
                Thread t = threads.get(name);
                if (t == null || !t.isAlive()) {
                    System.err.println("[mingsha-agent][自愈] 线程 " + name + " 异常退出，自动重启...");
                    Thread newT = entry.getValue().newThread();
                    threads.put(name, newT);
                    newT.start();
                }
            }
        }
    }
} 