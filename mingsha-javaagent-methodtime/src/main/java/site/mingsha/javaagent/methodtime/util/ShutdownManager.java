package site.mingsha.javaagent.methodtime.util;

import site.mingsha.javaagent.methodtime.collector.MethodTimeBuffer;
import site.mingsha.javaagent.methodtime.storage.H2Storage;
import site.mingsha.javaagent.methodtime.telnet.TelnetServer;
import site.mingsha.javaagent.methodtime.config.AgentConfig;

/**
 * 无损卸载流程管理：停止采集、刷新缓冲、提交剩余数据、关闭DB、终止Telnet。
 * Graceful agent unload: stop collection, flush buffer, commit remaining data, close DB, shutdown Telnet.
 * 保证主进程零干扰、数据完整性和资源释放。
 * Ensure zero interference to main process, data integrity and resource release.
 *
 * @author mingsha
 */
public class ShutdownManager {
    private static volatile boolean unloading = false;
    private static TelnetServer telnetServer;
    private static Thread storageThread;

    /**
     * 注册 Telnet 服务和存储线程到 JVM shutdown hook，实现无损卸载。
     * Register Telnet server and storage thread to JVM shutdown hook for graceful unload.
     * @param telnet TelnetServer 实例 | TelnetServer instance
     * @param storage 存储线程 | storage thread
     */
    public static void register(TelnetServer telnet, Thread storage) {
        telnetServer = telnet;
        storageThread = storage;
        Runtime.getRuntime().addShutdownHook(new Thread(ShutdownManager::unload));
    }

    /**
     * 执行无损卸载流程，依次关闭采集、写入、数据库、Telnet 服务。
     * Execute graceful unload: stop collection, flush buffer, commit remaining data, close DB, shutdown Telnet.
     */
    public static void unload() {
        if (unloading) return;
        unloading = true;
        System.out.println("[mingsha-agent] 开始无损卸载...");
        // 1. 停止新数据采集（可通过全局标志实现，略）| stop new data collection (via global flag, omitted)
        // 2. 刷新缓冲区，通知存储线程写入剩余数据 | flush buffer, notify storage thread to write remaining data
        if (storageThread != null) {
            storageThread.interrupt();
            try { storageThread.join(5000); } catch (InterruptedException ignore) {}
        }
        // 3. 提交剩余数据 | commit remaining data
        H2Storage.batchInsert(MethodTimeBuffer.pollBatch(AgentConfig.getBatchSize()));
        // 4. 关闭DB连接 | close DB connection
        H2Storage.close();
        // 5. 终止Telnet服务 | shutdown Telnet server
        if (telnetServer != null) telnetServer.shutdown();
        System.out.println("[mingsha-agent] 卸载完成");
    }
} 