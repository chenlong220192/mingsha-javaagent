package site.mingsha.javaagent.methodtime.util.health;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ThreadGuardian 单元测试（mock版）。
 * Unit test for ThreadGuardian (with mockito).
 * mock线程注册与健康检查。
 * Mock thread registration and health check.
 *
 * @author mingsha
 */
public class ThreadGuardianTest {
    @Test
    public void testRegisterAndGetThread() {
        ThreadGuardian guardian = new ThreadGuardian(100);
        Thread t = mock(Thread.class);
        guardian.register("test", () -> t);
        assertSame(t, guardian.getThread("test"));
    }
} 