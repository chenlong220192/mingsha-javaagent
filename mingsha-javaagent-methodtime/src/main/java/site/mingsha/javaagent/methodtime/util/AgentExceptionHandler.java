package site.mingsha.javaagent.methodtime.util;

/**
 * 全局异常捕获，保证主进程零干扰、零中断。
 * Global uncaught exception handler to ensure zero interference and no interruption to the main process.
 * 捕获所有线程未处理异常，打印日志但不影响主进程。
 * Catches all uncaught exceptions in threads, logs them without affecting the main process.
 *
 * @author mingsha
 */
public class AgentExceptionHandler implements Thread.UncaughtExceptionHandler {
    /**
     * 捕获所有线程未处理异常，打印日志但不影响主进程。
     * Catch all uncaught exceptions in threads, log them without affecting the main process.
     * @param t 线程 | thread
     * @param e 异常 | exception
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        String threadName = (t != null) ? t.getName() : "null";
        System.err.println("[mingsha-agent] 捕获异常于线程: " + threadName);
        if (e != null) {
            e.printStackTrace(System.err);
        }
        // 不抛出，防止主进程中断 | do not throw, prevent main process interruption
    }

    /**
     * 安装全局异常捕获器，保证主进程零干扰。
     * Install global uncaught exception handler to ensure zero interference to main process.
     */
    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler(new AgentExceptionHandler());
    }
} 