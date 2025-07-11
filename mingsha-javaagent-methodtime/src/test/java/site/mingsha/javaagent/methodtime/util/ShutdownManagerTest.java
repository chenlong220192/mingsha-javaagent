package site.mingsha.javaagent.methodtime.util;

import org.junit.jupiter.api.Test;
import site.mingsha.javaagent.methodtime.telnet.TelnetServer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ShutdownManager 单元测试（mock版）。
 * Unit test for ShutdownManager (with mockito).
 * mock TelnetServer和Thread，验证register和unload流程。
 * Mock TelnetServer and Thread, verify register and unload.
 *
 * @author mingsha
 */
public class ShutdownManagerTest {
    @Test
    public void testRegisterAndUnload() {
        TelnetServer telnet = mock(TelnetServer.class);
        Thread storage = mock(Thread.class);
        assertDoesNotThrow(() -> ShutdownManager.register(telnet, storage));
        assertDoesNotThrow(ShutdownManager::unload);
    }
} 