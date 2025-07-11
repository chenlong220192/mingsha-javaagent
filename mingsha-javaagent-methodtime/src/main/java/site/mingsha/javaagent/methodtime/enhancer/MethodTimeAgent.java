package site.mingsha.javaagent.methodtime.enhancer;

import java.lang.instrument.Instrumentation;
import site.mingsha.javaagent.methodtime.util.AgentExceptionHandler;
import site.mingsha.javaagent.methodtime.storage.H2Storage;
import site.mingsha.javaagent.methodtime.telnet.TelnetServer;
import site.mingsha.javaagent.methodtime.util.ShutdownManager;
import site.mingsha.javaagent.methodtime.monitor.AgentMonitor;
import site.mingsha.javaagent.methodtime.telnet.TelnetSession;
import site.mingsha.javaagent.methodtime.util.health.ThreadGuardian;
import site.mingsha.javaagent.methodtime.storage.H2Storage;

/**
 * JavaAgent 启动入口，负责初始化各核心模块、注册字节码增强、线程健康守护、无损卸载等。
 * JavaAgent entry point, responsible for initializing core modules, registering bytecode enhancement, thread health guardian, graceful unload, etc.
 * 支持全局异常捕获、H2存储、线程自愈、Telnet管理、自动补偿、CPU熔断、动态配置等高可用特性。
 * Supports global exception handling, H2 storage, thread self-healing, Telnet management, auto-compensation, CPU fuse, dynamic config, etc.
 *
 * @author mingsha
 */
public class MethodTimeAgent {
    private static ThreadGuardian guardian;
    private static TelnetServer telnetServer;

    /**
     * Agent 以 premain 方式启动时的入口方法。
     * Entry for agent startup via premain.
     * @param agentArgs agent 参数 | agent arguments
     * @param inst      Instrumentation 实例 | Instrumentation instance
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[mingsha-agent] premain start");
        install(agentArgs, inst);
    }

    /**
     * Agent 以 agentmain 方式动态 attach 时的入口方法。
     * Entry for agent startup via agentmain (dynamic attach).
     * @param agentArgs agent 参数 | agent arguments
     * @param inst      Instrumentation 实例 | Instrumentation instance
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("[mingsha-agent] agentmain start");
        install(agentArgs, inst);
    }

    /**
     * 核心初始化流程：
     * Core initialization process:
     * <ul>
     *   <li>安装全局异常捕获，保障主进程安全 | install global exception handler for main process safety</li>
     *   <li>初始化 H2 存储，自动建表 | initialize H2 storage, auto-create table</li>
     *   <li>启动线程健康守护，注册采集、写入、监控、Telnet、补偿等线程 | start thread guardian, register collect/write/monitor/Telnet/compensate threads</li>
     *   <li>注册无损卸载钩子，保证数据完整性 | register graceful unload hook for data integrity</li>
     *   <li>注册字节码增强，采集方法耗时 | register bytecode enhancement for method time profiling</li>
     * </ul>
     * @param agentArgs agent 参数 | agent arguments
     * @param inst      Instrumentation 实例 | Instrumentation instance
     */
    private static void install(String agentArgs, Instrumentation inst) {
        // 1. 全局异常捕获 | global exception handler
        AgentExceptionHandler.install();
        
        // 2. 打印当前配置信息 | print current configuration
        site.mingsha.javaagent.methodtime.config.AgentConfig.printConfig();
        
        // 3. 初始化 H2 存储 | initialize H2 storage
        try { H2Storage.init(); } catch (Exception e) { throw new RuntimeException(e); }
        // 4. 启动线程健康守护 | start thread guardian
        guardian = new ThreadGuardian(site.mingsha.javaagent.methodtime.config.AgentConfig.getHealthCheckIntervalMs());
        new Thread(guardian, "mingsha-agent-guardian").start();
        // 5. 注册监控线程，采集 CPU、内存、丢失率、熔断等信息 | register monitor thread, collect CPU/memory/loss/fuse info
        final site.mingsha.javaagent.methodtime.monitor.AgentMonitor monitor = new site.mingsha.javaagent.methodtime.monitor.AgentMonitor();
        H2Storage.setMonitor(monitor);
        // 6. 注册写入线程，批量写入采集数据到 H2，支持 CPU 熔断暂停 | register storage thread, batch write to H2, support CPU fuse pause
        guardian.register("storage", () -> new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (monitor.isFuseActive()) {
                        Thread.sleep(1000); // 熔断时暂停采集 | pause on fuse
                        continue;
                    }
                    java.util.List<site.mingsha.javaagent.methodtime.collector.MethodTimeRecord> batch = site.mingsha.javaagent.methodtime.collector.MethodTimeBuffer.pollBatch(site.mingsha.javaagent.methodtime.config.AgentConfig.getBatchSize());
                    if (!batch.isEmpty()) {
                        H2Storage.batchInsert(batch);
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "mingsha-agent-storage"));
        // 7. 注册 H2 数据库定期清理线程，自动清理历史数据 | register H2 cleanup thread, auto clean history
        guardian.register("h2cleaner", () -> H2Storage.createCleanerThread(60 * 60 * 1000));
        TelnetSession.setMonitorInstance(monitor);
        // 8. 注册 Telnet 服务线程，提供远程管理与查询 | register Telnet server thread for remote management/query
        TelnetServer telnetServer = new TelnetServer();
        new Thread(telnetServer, "mingsha-agent-telnet").start();
        // 9. 注册无损卸载钩子，优雅关闭所有资源 | register graceful unload hook for all resources
        ShutdownManager.register(telnetServer, guardian.getThread("storage"));
        // 10. 启动时自动补偿本地转存数据，保证数据最终一致性 | auto-compensate local backup data on startup for data consistency
        H2Storage.compensateLocalFiles();
        // 11. 注册定期补偿线程，定时补偿本地转存数据 | register scheduled compensation thread for local backup
        guardian.register("compensate", () -> new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    H2Storage.compensateLocalFiles();
                    Thread.sleep(10 * 60 * 1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "mingsha-agent-compensate"));
        // 12. 注册字节码增强，采集方法耗时 | register bytecode enhancement for method time profiling
        inst.addTransformer(new MethodTimeTransformer(), true);
    }
} 