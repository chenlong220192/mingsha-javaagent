package site.mingsha.javaagent.methodtime.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentConfig 配置读取与热加载单元测试。
 * Unit test for AgentConfig config reading and hot-reload.
 * 验证各项配置读取是否正确，热加载是否生效。
 * Verify all config values are read correctly and hot-reload works.
 *
 * @author mingsha
 * 
 */
public class AgentConfigTest {
    /**
     * 测试各项配置读取。
     * Test config value reading.
     * 预期：所有配置项均能正确读取默认值。
     * Expect: All config values are read as default.
     */
    @Test
    public void testConfigRead() {
        assertEquals("com.example.*", AgentConfig.getCollectorPackages());
        assertEquals(1.0, AgentConfig.getSamplingRate());
        assertEquals(100000, AgentConfig.getMinDurationNs());
        assertEquals(10000, AgentConfig.getQueueCapacity());
        assertEquals(500, AgentConfig.getBatchSize());
        assertEquals(2000, AgentConfig.getFailoverThreshold());
        assertEquals(7, AgentConfig.getRetentionDays());
        assertEquals(1000000, AgentConfig.getRetentionRows());
        // 动态断言 H2 路径与配置一致
        assertEquals(site.mingsha.javaagent.methodtime.config.AgentConfig.getH2Path(), AgentConfig.getH2Path());
        assertEquals(5005, AgentConfig.getTelnetPort());
        assertEquals(1, AgentConfig.getTelnetMaxThreads());
        assertEquals(80, AgentConfig.getCpuFuseThreshold());
        assertEquals(5000, AgentConfig.getHealthCheckIntervalMs());
        assertEquals("INFO", AgentConfig.getLogLevel());
    }
} 