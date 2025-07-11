package site.mingsha.javaagent.methodtime.telnet;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.regex.Pattern;
import site.mingsha.javaagent.methodtime.storage.H2Storage;
import site.mingsha.javaagent.methodtime.monitor.AgentMonitor;
import site.mingsha.javaagent.methodtime.config.AgentConfig;

/**
 * Telnet 会话，支持丰富的管理命令和数据库查询。
 * Telnet session, supports rich management commands and database queries.
 * 提供远程命令、监控、动态配置、导出、异常统计、数据库信息等能力。
 * Provides remote command, monitoring, dynamic config, export, error stats, database info, etc.
 *
 * @author mingsha
 */
public class TelnetSession implements Runnable {
    private final Socket client;
    private static final Pattern SELECT_ONLY = Pattern.compile("^\\s*select\\b.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static AgentMonitor monitorInstance;
    
    /**
     * 设置全局监控实例，便于命令查询。
     * Set global monitor instance for command query.
     * @param monitor AgentMonitor 实例 | monitor instance
     */
    public static void setMonitorInstance(AgentMonitor monitor) { monitorInstance = monitor; }

    public TelnetSession(Socket client) {
        this.client = client;
    }

    /**
     * 获取H2数据库连接URL，支持内存模式和文件模式
     * Get H2 database connection URL, support memory mode and file mode
     * @return 数据库连接URL | database connection URL
     */
    private String getH2DbUrl() {
        String path = AgentConfig.getH2Path();
        if (path.startsWith("mem:")) {
            return "jdbc:h2:" + path + ";DB_CLOSE_DELAY=-1";
        } else {
            return "jdbc:h2:file:" + path + ";AUTO_SERVER=TRUE";
        }
    }

    /**
     * Telnet 会话主循环，处理命令输入、SQL 查询、状态与配置管理。
     * Main loop for Telnet session, handles command input, SQL query, status and config management.
     * 支持丰富的管理命令、只读SQL、动态配置、导出、异常统计、数据库信息等。
     * Supports rich management commands, read-only SQL, dynamic config, export, error stats, database info, etc.
     */
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
            out.println("欢迎使用 mingsha-agent 管理端口，输入 help 查看命令");
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.equalsIgnoreCase("help")) {
                    printHelp(out);
                } else if (line.equalsIgnoreCase("agent help")) {
                    printAgentHelp(out);
                } else if (line.equalsIgnoreCase("db help")) {
                    printDbHelp(out);
                } else if (line.equalsIgnoreCase("agent errors")) {
                    if (monitorInstance != null) {
                        out.println(monitorInstance.getErrorStats());
                    } else {
                        out.println("[异常统计不可用]");
                    }
                } else if (line.equalsIgnoreCase("agent reload")) {
                    try {
                        AgentConfig.reload();
                        out.println("[配置已热加载]");
                    } catch (Exception e) {
                        out.println("[热加载失败] " + e.getMessage());
                    }
                } else if (line.equalsIgnoreCase("agent status")) {
                    if (monitorInstance != null) {
                        out.println(monitorInstance.getStatus());
                    } else {
                        out.println("[监控信息不可用]");
                    }
                } else if (line.equalsIgnoreCase("agent config")) {
                    printAgentConfig(out);
                } else if (line.equalsIgnoreCase("agent version")) {
                    out.println("mingsha-javaagent-methodtime v0.0.1-SNAPSHOT");
                } else if (line.equalsIgnoreCase("agent info")) {
                    printAgentInfo(out);
                } else if (line.startsWith("agent export ")) {
                    String file = line.substring("agent export ".length()).trim();
                    if (file.isEmpty()) {
                        out.println("[用法] agent export <file>");
                    } else {
                        out.println(H2Storage.exportAllToCsv(file));
                    }
                } else if (line.equalsIgnoreCase("db info")) {
                    printDatabaseInfo(out);
                } else if (line.equalsIgnoreCase("db tables")) {
                    printDatabaseTables(out);
                } else if (line.equalsIgnoreCase("db schema")) {
                    printTableSchema(out);
                } else if (line.equalsIgnoreCase("db stats")) {
                    printDatabaseStats(out);
                } else if (line.equalsIgnoreCase("db size")) {
                    printDatabaseSize(out);
                } else if (line.startsWith("db query ")) {
                    String sql = line.substring("db query ".length()).trim();
                    executeDbQuery(out, sql);
                } else if (SELECT_ONLY.matcher(line).matches() && isSafeSql(line)) {
                    executeDbQuery(out, line);
                } else {
                    out.println("不支持的命令或SQL被阻断，输入 help 查看支持的命令");
                }
            }
        } catch (IOException ignore) {}
        finally {
            try { client.close(); } catch (IOException ignore) {}
        }
    }

    /**
     * 打印主帮助信息
     */
    private void printHelp(PrintWriter out) {
        out.println("=== mingsha-agent 管理命令 ===");
        out.println("基础命令:");
        out.println("  help                    - 显示此帮助信息");
        out.println("  agent help              - 显示Agent管理命令");
        out.println("  db help                 - 显示数据库查询命令");
        out.println();
        out.println("Agent管理:");
        out.println("  agent status            - 查看Agent状态");
        out.println("  agent config            - 查看当前配置");
        out.println("  agent info              - 查看Agent详细信息");
        out.println("  agent version           - 查看版本信息");
        out.println("  agent errors            - 查看异常统计");
        out.println("  agent reload            - 热加载配置");
        out.println("  agent export <file>     - 导出数据到CSV");
        out.println();
        out.println("数据库查询:");
        out.println("  db info                 - 查看数据库信息");
        out.println("  db tables               - 查看所有表");
        out.println("  db schema               - 查看表结构");
        out.println("  db stats                - 查看数据库统计");
        out.println("  db size                 - 查看数据库大小");
        out.println("  db query <sql>          - 执行SQL查询");
        out.println("  select ...              - 直接执行SELECT查询");
        out.println();
        out.println("示例:");
        out.println("  select count(*) from method_time_stat");
        out.println("  db query select avg(duration_ns) from method_time_stat");
    }

    /**
     * 打印Agent管理命令帮助
     */
    private void printAgentHelp(PrintWriter out) {
        out.println("=== Agent管理命令 ===");
        out.println("  agent status            - 查看CPU、内存、丢失率等状态");
        out.println("  agent config            - 查看当前所有配置项");
        out.println("  agent info              - 查看Agent详细信息");
        out.println("  agent version           - 查看版本信息");
        out.println("  agent errors            - 查看采集/写入/转存异常统计");
        out.println("  agent reload            - 热加载agent-config.yml配置");
        out.println("  agent export <file>     - 导出全部数据到CSV文件");
        out.println();
        out.println("示例:");
        out.println("  agent status");
        out.println("  agent export /tmp/data.csv");
    }

    /**
     * 打印数据库查询命令帮助
     */
    private void printDbHelp(PrintWriter out) {
        out.println("=== 数据库查询命令 ===");
        out.println("  db info                 - 查看数据库连接信息");
        out.println("  db tables               - 查看所有表列表");
        out.println("  db schema               - 查看所有表结构");
        out.println("  db stats                - 查看数据统计信息");
        out.println("  db size                 - 查看数据库文件大小");
        out.println("  db query <sql>          - 执行自定义SQL查询");
        out.println("  select ...              - 直接执行SELECT查询");
        out.println();
        out.println("数据库表说明:");
        out.println("  method_time_stat        - 主表：存储所有方法耗时记录");
        out.println("  method_time_stat_summary - 汇总表：按方法统计调用次数、平均耗时等");
        out.println("  method_time_stat_slow   - 慢查询表：存储超过阈值的慢查询记录");
        out.println("  db_version              - 版本表：记录数据库版本信息");
        out.println();
        out.println("常用查询示例:");
        out.println("  select count(*) from method_time_stat");
        out.println("  select * from method_time_stat_summary order by total_calls desc limit 10");
        out.println("  select * from method_time_stat_slow order by duration_ns desc limit 10");
        out.println("  select class_name, method_name, avg(duration_ns) from method_time_stat group by class_name, method_name");
        out.println("  select * from method_time_stat where duration_ns > 1000000 order by duration_ns desc limit 10");
        out.println("  select count(*) from method_time_stat where create_time > dateadd('hour', -1, current_timestamp)");
    }

    /**
     * 打印Agent配置信息
     */
    private void printAgentConfig(PrintWriter out) {
        out.println("=== Agent配置信息 ===");
        out.println("采集配置:");
        out.println("  采集包范围: " + AgentConfig.getCollectorPackages());
        out.println("  采样率: " + AgentConfig.getSamplingRate());
        out.println("  最小耗时阈值: " + AgentConfig.getMinDurationNs() + " ns");
        out.println("  缓冲队列容量: " + AgentConfig.getQueueCapacity());
        out.println();
        out.println("存储配置:");
        out.println("  批量写入条数: " + AgentConfig.getBatchSize());
        out.println("  转存阈值: " + AgentConfig.getFailoverThreshold());
        out.println("  数据留存天数: " + AgentConfig.getRetentionDays());
        out.println("  数据留存条数: " + AgentConfig.getRetentionRows());
        out.println("  慢查询阈值: " + AgentConfig.getSlowQueryThresholdNs() + " ns (" + (AgentConfig.getSlowQueryThresholdNs()/1000000) + " ms)");
        out.println("  H2数据库路径: " + AgentConfig.getH2Path());
        out.println();
        out.println("管理配置:");
        out.println("  Telnet端口: " + AgentConfig.getTelnetPort());
        out.println("  Telnet最大线程数: " + AgentConfig.getTelnetMaxThreads());
        out.println();
        out.println("监控配置:");
        out.println("  CPU熔断阈值: " + AgentConfig.getCpuFuseThreshold() + "%");
        out.println("  健康检查间隔: " + AgentConfig.getHealthCheckIntervalMs() + " ms");
        out.println();
        out.println("日志配置:");
        out.println("  日志级别: " + AgentConfig.getLogLevel());
    }

    /**
     * 打印Agent详细信息
     */
    private void printAgentInfo(PrintWriter out) {
        out.println("=== Agent详细信息 ===");
        out.println("版本: mingsha-javaagent-methodtime v0.0.1-SNAPSHOT");
        out.println("作者: mingsha");
        out.println("技术栈: ASM + H2 + Telnet");
        out.println("启动时间: " + new java.util.Date());
        out.println("JVM版本: " + System.getProperty("java.version"));
        out.println("操作系统: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        out.println("内存信息:");
        Runtime rt = Runtime.getRuntime();
        out.println("  总内存: " + (rt.totalMemory() / 1024 / 1024) + " MB");
        out.println("  已用内存: " + ((rt.totalMemory() - rt.freeMemory()) / 1024 / 1024) + " MB");
        out.println("  空闲内存: " + (rt.freeMemory() / 1024 / 1024) + " MB");
        out.println("  最大内存: " + (rt.maxMemory() / 1024 / 1024) + " MB");
    }

    /**
     * 打印数据库信息
     */
    private void printDatabaseInfo(PrintWriter out) {
        try (Connection conn = DriverManager.getConnection(getH2DbUrl(), "sa", "")) {
            DatabaseMetaData meta = conn.getMetaData();
            out.println("=== 数据库信息 ===");
            out.println("数据库产品: " + meta.getDatabaseProductName());
            out.println("数据库版本: " + meta.getDatabaseProductVersion());
            out.println("驱动名称: " + meta.getDriverName());
            out.println("驱动版本: " + meta.getDriverVersion());
            out.println("连接URL: " + conn.getMetaData().getURL());
            out.println("用户名: " + meta.getUserName());
            out.println("数据库路径: " + AgentConfig.getH2Path());
        } catch (Exception e) {
            out.println("[数据库信息查询失败] " + e.getMessage());
        }
    }

    /**
     * 打印数据库表列表
     */
    private void printDatabaseTables(PrintWriter out) {
        try (Connection conn = DriverManager.getConnection(getH2DbUrl(), "sa", "")) {
            DatabaseMetaData meta = conn.getMetaData();
            out.println("=== 数据库表列表 ===");
            ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String tableType = rs.getString("TABLE_TYPE");
                out.println("表名: " + tableName + " (类型: " + tableType + ")");
            }
        } catch (Exception e) {
            out.println("[表列表查询失败] " + e.getMessage());
        }
    }

    /**
     * 打印表结构
     */
    private void printTableSchema(PrintWriter out) {
        try (Connection conn = DriverManager.getConnection(getH2DbUrl(), "sa", "")) {
            DatabaseMetaData meta = conn.getMetaData();
            
            // 获取所有表
            ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"});
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                out.println("=== " + tableName + " 表结构 ===");
                
                ResultSet rs = meta.getColumns(null, null, tableName, null);
                out.println("字段名\t\t数据类型\t\t是否为空\t\t默认值\t\t说明");
                out.println("------\t\t--------\t\t--------\t\t--------\t\t----");
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("TYPE_NAME");
                    String isNullable = rs.getString("IS_NULLABLE");
                    String defaultValue = rs.getString("COLUMN_DEF");
                    String remarks = rs.getString("REMARKS");
                    out.println(columnName + "\t\t" + dataType + "\t\t" + isNullable + "\t\t" + 
                               (defaultValue != null ? defaultValue : "NULL") + "\t\t" + 
                               (remarks != null ? remarks : ""));
                }
                out.println();
            }
        } catch (Exception e) {
            out.println("[表结构查询失败] " + e.getMessage());
        }
    }

    /**
     * 打印数据库统计信息
     */
    private void printDatabaseStats(PrintWriter out) {
        try (Connection conn = DriverManager.getConnection(getH2DbUrl(), "sa", "");
             Statement stmt = conn.createStatement()) {
            out.println("=== 数据库统计信息 ===");
            
            // 主表统计
            out.println("【主表统计】");
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM method_time_stat");
            if (rs.next()) {
                out.println("总记录数: " + rs.getLong("total"));
            }
            
            rs = stmt.executeQuery("SELECT COUNT(*) as today FROM method_time_stat WHERE DATE(create_time) = CURRENT_DATE");
            if (rs.next()) {
                out.println("今日记录数: " + rs.getLong("today"));
            }
            
            rs = stmt.executeQuery("SELECT AVG(duration_ns) as avg_duration FROM method_time_stat");
            if (rs.next()) {
                double avg = rs.getDouble("avg_duration");
                out.println("平均耗时: " + String.format("%.2f", avg) + " ns (" + String.format("%.2f", avg/1000000) + " ms)");
            }
            
            rs = stmt.executeQuery("SELECT MAX(duration_ns) as max_duration FROM method_time_stat");
            if (rs.next()) {
                long max = rs.getLong("max_duration");
                out.println("最大耗时: " + max + " ns (" + (max/1000000) + " ms)");
            }
            
            // 汇总表统计
            out.println();
            out.println("【汇总表统计】");
            rs = stmt.executeQuery("SELECT COUNT(*) as summary_count FROM method_time_stat_summary");
            if (rs.next()) {
                out.println("汇总方法数: " + rs.getLong("summary_count"));
            }
            
            rs = stmt.executeQuery("SELECT SUM(total_calls) as total_calls FROM method_time_stat_summary");
            if (rs.next()) {
                out.println("总调用次数: " + rs.getLong("total_calls"));
            }
            
            rs = stmt.executeQuery("SELECT class_name, method_name, total_calls, avg_duration_ns FROM method_time_stat_summary ORDER BY total_calls DESC LIMIT 5");
            out.println();
            out.println("调用次数最多的方法 (Top 5):");
            while (rs.next()) {
                out.println("  " + rs.getString("class_name") + "." + rs.getString("method_name") + " - " + rs.getLong("total_calls") + " 次");
            }
            
            rs = stmt.executeQuery("SELECT class_name, method_name, avg_duration_ns FROM method_time_stat_summary ORDER BY avg_duration_ns DESC LIMIT 5");
            out.println();
            out.println("平均耗时最长的方法 (Top 5):");
            while (rs.next()) {
                long avg = rs.getLong("avg_duration_ns");
                out.println("  " + rs.getString("class_name") + "." + rs.getString("method_name") + " - " + avg + " ns (" + (avg/1000000) + " ms)");
            }
            
            // 慢查询表统计
            out.println();
            out.println("【慢查询统计】");
            rs = stmt.executeQuery("SELECT COUNT(*) as slow_count FROM method_time_stat_slow");
            if (rs.next()) {
                out.println("慢查询记录数: " + rs.getLong("slow_count"));
            }
            
            rs = stmt.executeQuery("SELECT COUNT(*) as slow_today FROM method_time_stat_slow WHERE DATE(create_time) = CURRENT_DATE");
            if (rs.next()) {
                out.println("今日慢查询数: " + rs.getLong("slow_today"));
            }
            
            rs = stmt.executeQuery("SELECT class_name, method_name, duration_ns FROM method_time_stat_slow ORDER BY duration_ns DESC LIMIT 5");
            out.println();
            out.println("最慢查询 (Top 5):");
            while (rs.next()) {
                long duration = rs.getLong("duration_ns");
                out.println("  " + rs.getString("class_name") + "." + rs.getString("method_name") + " - " + duration + " ns (" + (duration/1000000) + " ms)");
            }
            
        } catch (Exception e) {
            out.println("[数据库统计查询失败] " + e.getMessage());
        }
    }

    /**
     * 打印数据库大小信息
     */
    private void printDatabaseSize(PrintWriter out) {
        try {
            String dbPath = AgentConfig.getH2Path();
            if (dbPath.startsWith("mem:")) {
                // 内存模式：无法获取文件大小，显示内存模式信息
                // Memory mode: cannot get file size, show memory mode info
                out.println("=== 数据库大小信息 ===");
                out.println("数据库模式: 内存模式 (mem:" + dbPath.substring(4) + ")");
                out.println("文件大小: 不适用 (数据存储在内存中)");
                out.println("注意: 进程退出后数据将丢失");
            } else {
                // 文件模式：获取文件大小
                // File mode: get file size
                File dbFile = new File(dbPath + ".mv.db");
                if (dbFile.exists()) {
                    long sizeBytes = dbFile.length();
                    double sizeMB = sizeBytes / 1024.0 / 1024.0;
                    out.println("=== 数据库大小信息 ===");
                    out.println("数据库模式: 文件模式");
                    out.println("数据库文件: " + dbPath + ".mv.db");
                    out.println("文件大小: " + String.format("%.2f", sizeMB) + " MB (" + sizeBytes + " bytes)");
                } else {
                    out.println("[数据库文件不存在] " + dbPath + ".mv.db");
                }
            }
        } catch (Exception e) {
            out.println("[数据库大小查询失败] " + e.getMessage());
        }
    }

    /**
     * 执行数据库查询
     */
    private void executeDbQuery(PrintWriter out, String sql) {
        if (!isSafeSql(sql)) {
            out.println("[SQL安全检查失败] 只允许SELECT查询，禁止高危操作");
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(getH2DbUrl(), "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            
            // 输出表头
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= colCount; i++) {
                header.append(meta.getColumnName(i)).append("\t");
            }
            out.println(header.toString());
            out.println("------\t".repeat(colCount));
            
            // 输出数据
            int row = 0;
            while (rs.next() && row < 100) { // 最多输出100行
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= colCount; i++) {
                    String value = rs.getString(i);
                    sb.append(value != null ? value : "NULL").append("\t");
                }
                out.println(sb.toString());
                row++;
            }
            
            if (row == 0) {
                out.println("[无数据]");
            } else if (row == 100) {
                out.println("[结果已截断，最多显示100行]");
            }
            
        } catch (Exception e) {
            out.println("[SQL执行异常] " + e.getMessage());
        }
    }

    /**
     * SQL 安全校验，仅允许 select 且禁止高危操作。
     * SQL safety check, only allow select and forbid dangerous operations.
     * @param sql SQL语句 | SQL statement
     * @return true-安全，false-危险 | true if safe, false if dangerous
     */
    private boolean isSafeSql(String sql) {
        // 禁止 ;、update、delete、insert、drop、alter、create、union、with、子查询等危险操作
        // Forbid ;, update, delete, insert, drop, alter, create, union, with, subquery, etc.
        String lower = sql.toLowerCase();
        if (lower.contains(";") || lower.contains("update") || lower.contains("delete") || lower.contains("insert") || lower.contains("drop") || lower.contains("alter") || lower.contains("create") || lower.contains("union") || lower.contains("with")) {
            return false;
        }
        // 禁止子查询（简单检测：出现 select ... from ... select ...）
        // Forbid subquery (simple check: select ... from ... select ...)
        int first = lower.indexOf("select");
        int next = lower.indexOf("select", first + 1);
        if (first >= 0 && next > first) return false;
        return true;
    }
} 