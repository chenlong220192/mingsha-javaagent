package site.mingsha.javaagent.methodtime.enhancer;

import org.junit.jupiter.api.Test;
import java.lang.instrument.Instrumentation;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MethodTimeAgent 单元测试（mock版）。
 * Unit test for MethodTimeAgent (with mockito).
 * mock Instrumentation，验证premain/agentmain流程。
 * Mock Instrumentation, verify premain/agentmain entry.
 *
 * @author mingsha
 */
public class MethodTimeAgentTest {
    @Test
    public void testPremainAndAgentmain() {
        Instrumentation inst = mock(Instrumentation.class);
        assertDoesNotThrow(() -> MethodTimeAgent.premain("", inst));
        assertDoesNotThrow(() -> MethodTimeAgent.agentmain("", inst));
        verify(inst, atLeastOnce()).addTransformer(any(), anyBoolean());
    }
} 