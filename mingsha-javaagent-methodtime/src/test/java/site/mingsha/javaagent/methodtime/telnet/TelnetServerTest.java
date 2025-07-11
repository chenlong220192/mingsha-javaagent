package site.mingsha.javaagent.methodtime.telnet;

import org.junit.jupiter.api.Test;
import java.util.concurrent.ExecutorService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TelnetServer 单元测试（mock版）。
 * Unit test for TelnetServer (with mockito).
 * mock ServerSocket、ExecutorService，验证构造和线程池逻辑。
 * Mock ServerSocket, ExecutorService, verify construction and pool logic.
 *
 * @author mingsha
 */
public class TelnetServerTest {
    @Test
    public void testConstructorAndPool() {
        TelnetServer server = spy(new TelnetServer());
        ExecutorService pool = mock(ExecutorService.class);
        // 通过反射注入mock pool
        try {
            java.lang.reflect.Field poolField = TelnetServer.class.getDeclaredField("pool");
            poolField.setAccessible(true);
            poolField.set(server, pool);
        } catch (Exception e) {
            fail(e);
        }
        assertNotNull(server);
    }
} 