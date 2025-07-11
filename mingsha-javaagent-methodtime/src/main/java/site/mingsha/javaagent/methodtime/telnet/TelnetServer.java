package site.mingsha.javaagent.methodtime.telnet;

import site.mingsha.javaagent.methodtime.config.AgentConfig;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Telnet 管理服务，支持端口冲突自动切换、最大线程数限制。
 * Telnet management server, supports auto port switch and max thread limit.
 * 提供远程管理、监控、动态配置等能力。
 * Provides remote management, monitoring, dynamic config, etc.
 *
 * @author mingsha
 */
public class TelnetServer implements Runnable {
    private ServerSocket serverSocket;
    private final ExecutorService pool;
    private volatile boolean running = true;
    private int port;

    public TelnetServer() {
        this.port = AgentConfig.getTelnetPort();
        this.pool = Executors.newFixedThreadPool(AgentConfig.getTelnetMaxThreads());
    }

    /**
     * 启动 Telnet 管理服务，自动处理端口冲突并限制最大线程数。
     * Start Telnet management server, auto switch port on conflict, limit max threads.
     * 监听客户端连接并分发到 TelnetSession。
     * Listen for client connections and dispatch to TelnetSession.
     */
    @Override
    public void run() {
        while (true) {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("[mingsha-agent] Telnet 管理端口: " + port);
                break;
            } catch (IOException e) {
                port++;
            }
        }
        while (running) {
            try {
                Socket client = serverSocket.accept();
                pool.execute(new TelnetSession(client));
            } catch (IOException ignore) {}
        }
    }

    /**
     * 优雅关闭 Telnet 服务，释放端口和线程池。
     * Gracefully shutdown Telnet server, release port and thread pool.
     */
    public void shutdown() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignore) {}
        pool.shutdownNow();
    }
} 