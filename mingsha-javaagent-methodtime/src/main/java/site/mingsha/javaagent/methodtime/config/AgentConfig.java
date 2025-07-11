package site.mingsha.javaagent.methodtime.config;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;

/**
 * Agent 配置读取与持有类，支持 YAML 配置和动态热加载。
 * Agent configuration holder, supports YAML config and dynamic hot-reload.
 * 提供采集、存储、管理、监控等所有核心配置项的读取。
 * Provides access to all core config items for collection, storage, management, monitoring, etc.
 * 支持通过JVM系统属性覆盖配置：-Dmingsha.agent.config.xxx=value
 * Supports overriding config via JVM system properties: -Dmingsha.agent.config.xxx=value
 *
 * @author mingsha
 */
public class AgentConfig {
    public static final String CONFIG_FILE = "/agent-config.yml";
    public static final String SYSTEM_PROPERTY_PREFIX = "mingsha.agent.config.";
    private static final Map<String, Object> config;

    static {
        Yaml yaml = new Yaml();
        try (InputStream in = AgentConfig.class.getResourceAsStream(CONFIG_FILE)) {
            config = yaml.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load agent-config.yml", e);
        }
    }

    private static Object get(String path, Object def) {
        // 优先从系统属性读取 | prioritize system properties
        String sysPropKey = SYSTEM_PROPERTY_PREFIX + path.replace(".", ".");
        String sysPropValue = System.getProperty(sysPropKey);
        if (sysPropValue != null) {
            return parseValue(sysPropValue, def);
        }
        
        // 从YAML配置读取 | read from YAML config
        String[] keys = path.split("\\.");
        Object cur = config;
        for (String k : keys) {
            if (!(cur instanceof Map)) return def;
            cur = ((Map<?, ?>) cur).get(k);
            if (cur == null) return def;
        }
        return cur;
    }

    /**
     * 解析配置值，支持字符串、数字、布尔值等类型转换
     * Parse config value, support string, number, boolean type conversion
     */
    private static Object parseValue(String value, Object defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        
        // 根据默认值类型进行转换 | convert based on default value type
        if (defaultValue instanceof Integer) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else if (defaultValue instanceof Long) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else if (defaultValue instanceof Double) {
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else if (defaultValue instanceof Boolean) {
            return Boolean.parseBoolean(value.trim());
        } else {
            return value.trim();
        }
    }

    /**
     * 读取采集包范围配置。
     * Get collector package scope config.
     * 支持通过 -Dmingsha.agent.config.collector.packages=com.example.* 覆盖
     * Support override via -Dmingsha.agent.config.collector.packages=com.example.*
     * @return 包范围字符串 | package scope string
     */
    public static String getCollectorPackages() {
        return (String) get("collector.packages", "com.example.*");
    }
    
    /**
     * 读取采样率配置。
     * Get sampling rate config.
     * 支持通过 -Dmingsha.agent.config.collector.samplingRate=0.5 覆盖
     * Support override via -Dmingsha.agent.config.collector.samplingRate=0.5
     * @return 采样率（0~1）| sampling rate (0~1)
     */
    public static double getSamplingRate() {
        Object v = get("collector.samplingRate", 1.0);
        return v instanceof Number ? ((Number) v).doubleValue() : Double.parseDouble(v.toString());
    }
    
    /**
     * 读取最小采集耗时阈值。
     * Get min duration threshold config.
     * 支持通过 -Dmingsha.agent.config.collector.minDurationNs=100000 覆盖
     * Support override via -Dmingsha.agent.config.collector.minDurationNs=100000
     * @return 纳秒 | nanoseconds
     */
    public static long getMinDurationNs() {
        Object v = get("collector.minDurationNs", 100000L);
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
    }
    
    /**
     * 读取内存缓冲队列容量。
     * Get buffer queue capacity config.
     * 支持通过 -Dmingsha.agent.config.collector.queueCapacity=10000 覆盖
     * Support override via -Dmingsha.agent.config.collector.queueCapacity=10000
     * @return 条数 | number of records
     */
    public static int getQueueCapacity() {
        Object v = get("collector.queueCapacity", 10000);
        return v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString());
    }
    
    /**
     * 读取批量写入条数。
     * Get batch insert size config.
     * 支持通过 -Dmingsha.agent.config.storage.batchSize=500 覆盖
     * Support override via -Dmingsha.agent.config.storage.batchSize=500
     * @return 条数 | batch size
     */
    public static int getBatchSize() {
        Object v = get("storage.batchSize", 500);
        return v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString());
    }
    
    /**
     * 读取写入失败转存阈值。
     * Get failover threshold config.
     * 支持通过 -Dmingsha.agent.config.storage.failoverThreshold=2000 覆盖
     * Support override via -Dmingsha.agent.config.storage.failoverThreshold=2000
     * @return 条数 | threshold
     */
    public static int getFailoverThreshold() {
        Object v = get("storage.failoverThreshold", 2000);
        return v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString());
    }
    
    /**
     * 读取数据留存天数。
     * Get data retention days config.
     * 支持通过 -Dmingsha.agent.config.storage.retentionDays=7 覆盖
     * Support override via -Dmingsha.agent.config.storage.retentionDays=7
     * @return 天数 | days
     */
    public static int getRetentionDays() {
        Object v = get("storage.retentionDays", 7);
        return v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString());
    }
    
    /**
     * 读取数据留存最大条数。
     * Get data retention max rows config.
     * 支持通过 -Dmingsha.agent.config.storage.retentionRows=1000000 覆盖
     * Support override via -Dmingsha.agent.config.storage.retentionRows=1000000
     * @return 条数 | max rows
     */
    public static int getRetentionRows() {
        Object v = get("storage.retentionRows", 1000000);
        return v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString());
    }
    
    /**
     * 读取 H2 数据库文件路径。
     * Get H2 database file path config.
     * 必须配置，无默认值。JVM参数优先，其次配置文件，缺失时报错。
     */
    public static String getH2Path() {
        Object v = get("storage.h2.path", null);
        if (v == null || v.toString().trim().isEmpty()) {
            throw new IllegalStateException("[mingsha-agent] H2数据库路径未配置，请通过 -Dmingsha.agent.config.storage.h2.path 或 agent-config.yml 配置 storage.h2.path");
        }
        return v.toString().trim();
    }
    
    /**
     * 读取慢查询阈值配置。
     * Get slow query threshold config.
     * 支持通过 -Dmingsha.agent.config.storage.slowQueryThresholdNs=1000000 覆盖
     * Support override via -Dmingsha.agent.config.storage.slowQueryThresholdNs=1000000
     * @return 纳秒 | nanoseconds
     */
    public static long getSlowQueryThresholdNs() {
        Object v = get("storage.slowQueryThresholdNs", 1000000L); // 默认1ms
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
    }
    
    /**
     * 读取 Telnet 管理端口。
     * Get Telnet management port config.
     * 支持通过 -Dmingsha.agent.config.manage.telnet.port=5005 覆盖
     * Support override via -Dmingsha.agent.config.manage.telnet.port=5005
     * @return 端口号 | port number
     */
    public static int getTelnetPort() {
        Object v = get("manage.telnet.port", 5005);
        return v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString());
    }
    
    /**
     * 读取 Telnet 最大线程数。
     * Get Telnet max threads config.
     * 支持通过 -Dmingsha.agent.config.manage.telnet.maxThreads=1 覆盖
     * Support override via -Dmingsha.agent.config.manage.telnet.maxThreads=1
     * @return 线程数 | max threads
     */
    public static int getTelnetMaxThreads() {
        Object v = get("manage.telnet.maxThreads", 1);
        return v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString());
    }
    
    /**
     * 读取 CPU 熔断阈值。
     * Get CPU fuse threshold config.
     * 支持通过 -Dmingsha.agent.config.monitor.cpu.fuseThreshold=80 覆盖
     * Support override via -Dmingsha.agent.config.monitor.cpu.fuseThreshold=80
     * @return 百分比 | percent
     */
    public static int getCpuFuseThreshold() {
        Object v = get("monitor.cpu.fuseThreshold", 80);
        return v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString());
    }
    
    /**
     * 读取健康检查间隔。
     * Get health check interval config.
     * 支持通过 -Dmingsha.agent.config.monitor.healthCheckIntervalMs=5000 覆盖
     * Support override via -Dmingsha.agent.config.monitor.healthCheckIntervalMs=5000
     * @return 毫秒 | milliseconds
     */
    public static int getHealthCheckIntervalMs() {
        Object v = get("monitor.healthCheckIntervalMs", 5000);
        return v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString());
    }
    
    /**
     * 读取日志级别。
     * Get log level config.
     * 支持通过 -Dmingsha.agent.config.log.level=INFO 覆盖
     * Support override via -Dmingsha.agent.config.log.level=INFO
     * @return 日志级别字符串 | log level string
     */
    public static String getLogLevel() {
        return (String) get("log.level", "INFO");
    }

    /**
     * 动态热加载配置。
     * Dynamically hot-reload config from agent-config.yml.
     * 重新加载 agent-config.yml 并刷新配置。
     * Reload agent-config.yml and refresh config.
     */
    public static synchronized void reload() {
        Yaml yaml = new Yaml();
        try (InputStream in = AgentConfig.class.getResourceAsStream(CONFIG_FILE)) {
            Map<String, Object> newConfig = yaml.load(in);
            if (newConfig != null) {
                config.clear();
                config.putAll(newConfig);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to reload agent-config.yml", e);
        }
    }
    
    /**
     * 打印当前配置信息，用于调试
     * Print current config info for debugging
     */
    public static void printConfig() {
        System.out.println("[mingsha-agent] Current Configuration:");
        System.out.println("  Collector Packages: " + getCollectorPackages());
        System.out.println("  Sampling Rate: " + getSamplingRate());
        System.out.println("  Min Duration (ns): " + getMinDurationNs());
        System.out.println("  Queue Capacity: " + getQueueCapacity());
        System.out.println("  Batch Size: " + getBatchSize());
        System.out.println("  Slow Query Threshold (ns): " + getSlowQueryThresholdNs());
        System.out.println("  H2 Path: " + getH2Path());
        System.out.println("  Telnet Port: " + getTelnetPort());
        System.out.println("  CPU Fuse Threshold: " + getCpuFuseThreshold() + "%");
        System.out.println("  Log Level: " + getLogLevel());
    }
} 