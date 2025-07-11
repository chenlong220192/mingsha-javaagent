package site.mingsha.javaagent.methodtime.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * AgentExceptionHandler 单元测试。
 * Unit test for AgentExceptionHandler.
 * 验证全局异常捕获器能正确处理未捕获异常，不影响主进程。
 * Verify global exception handler properly handles uncaught exceptions without affecting main process.
 *
 * @author mingsha
 */
public class AgentExceptionHandlerTest {
    private ByteArrayOutputStream errContent;
    private PrintStream originalErr;
    private AgentExceptionHandler handler;

    @BeforeEach
    public void setUp() {
        // 捕获标准错误输出，避免异常堆栈污染测试输出
        errContent = new ByteArrayOutputStream();
        originalErr = System.err;
        System.setErr(new PrintStream(errContent));
        handler = new AgentExceptionHandler();
    }

    @AfterEach
    public void tearDown() {
        // 恢复标准错误输出
        System.setErr(originalErr);
    }

    @Test
    public void testUncaughtExceptionHandled() {
        // 创建真实线程，设置线程名
        Thread testThread = new Thread(() -> {}, "test-thread");
        RuntimeException testException = new RuntimeException("test exception");
        
        // 验证异常处理不会抛出异常
        assertDoesNotThrow(() -> handler.uncaughtException(testThread, testException));
        
        // 验证错误输出包含预期的日志信息
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("[mingsha-agent] 捕获异常于线程: test-thread"));
        assertTrue(errorOutput.contains("java.lang.RuntimeException: test exception"));
    }

    @Test
    public void testUncaughtExceptionWithNullThread() {
        RuntimeException testException = new RuntimeException("null thread test");
        
        // 验证处理null线程时不会抛出异常
        assertDoesNotThrow(() -> handler.uncaughtException(null, testException));
        
        // 验证错误输出包含预期的日志信息
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("[mingsha-agent] 捕获异常于线程: null"));
        assertTrue(errorOutput.contains("java.lang.RuntimeException: null thread test"));
    }

    @Test
    public void testUncaughtExceptionWithNullException() {
        Thread testThread = new Thread(() -> {}, "null-exception-thread");
        
        // 验证处理null异常时不会抛出异常
        assertDoesNotThrow(() -> handler.uncaughtException(testThread, null));
        
        // 验证错误输出包含预期的日志信息
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("[mingsha-agent] 捕获异常于线程: null-exception-thread"));
        // 验证没有异常堆栈输出
        assertFalse(errorOutput.contains("java.lang.NullPointerException"));
    }

    @Test
    public void testInstallGlobalHandler() {
        // 验证安装全局异常处理器不会抛出异常
        assertDoesNotThrow(() -> AgentExceptionHandler.install());
        
        // 验证全局异常处理器已安装
        Thread.UncaughtExceptionHandler globalHandler = Thread.getDefaultUncaughtExceptionHandler();
        assertNotNull(globalHandler);
        assertTrue(globalHandler instanceof AgentExceptionHandler);
    }

    @Test
    public void testMultipleExceptionsHandled() {
        Thread testThread1 = new Thread(() -> {}, "thread-1");
        Thread testThread2 = new Thread(() -> {}, "thread-2");
        
        RuntimeException ex1 = new RuntimeException("exception 1");
        RuntimeException ex2 = new RuntimeException("exception 2");
        
        // 验证多次异常处理不会抛出异常
        assertDoesNotThrow(() -> {
            handler.uncaughtException(testThread1, ex1);
            handler.uncaughtException(testThread2, ex2);
        });
        
        // 验证错误输出包含所有异常信息
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("捕获异常于线程: thread-1"));
        assertTrue(errorOutput.contains("捕获异常于线程: thread-2"));
        assertTrue(errorOutput.contains("java.lang.RuntimeException: exception 1"));
        assertTrue(errorOutput.contains("java.lang.RuntimeException: exception 2"));
    }

    @Test
    public void testBothNullParameters() {
        // 验证处理null线程和null异常时不会抛出异常
        assertDoesNotThrow(() -> handler.uncaughtException(null, null));
        
        // 验证错误输出包含预期的日志信息
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("[mingsha-agent] 捕获异常于线程: null"));
        // 验证没有异常堆栈输出
        assertFalse(errorOutput.contains("java.lang.NullPointerException"));
    }
} 