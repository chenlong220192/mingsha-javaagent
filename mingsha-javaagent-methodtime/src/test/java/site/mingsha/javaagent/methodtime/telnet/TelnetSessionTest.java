package site.mingsha.javaagent.methodtime.telnet;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TelnetSession 单元测试（mock版）。
 * Unit test for TelnetSession (with mockito).
 * mock Socket、输入输出流，验证help命令和命令分发。
 * Mock Socket, IO streams, verify help command and dispatch.
 *
 * @author mingsha
 */
public class TelnetSessionTest {
    @Test
    public void testHelpCommand() throws Exception {
        Socket mockSocket = mock(Socket.class);
        ByteArrayInputStream in = new ByteArrayInputStream("help\n".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(mockSocket.getInputStream()).thenReturn(in);
        when(mockSocket.getOutputStream()).thenReturn(out);
        TelnetSession session = new TelnetSession(mockSocket);
        Thread t = new Thread(session);
        t.start();
        t.join(200);
        t.interrupt();
        String output = out.toString("UTF-8");
        assertTrue(output.contains("help"));
    }
} 