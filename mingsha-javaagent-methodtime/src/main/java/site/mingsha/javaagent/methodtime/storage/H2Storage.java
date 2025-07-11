package site.mingsha.javaagent.methodtime.storage;

import site.mingsha.javaagent.methodtime.collector.MethodTimeRecord;
import site.mingsha.javaagent.methodtime.config.AgentConfig;
import site.mingsha.javaagent.methodtime.monitor.AgentMonitor;
import java.sql.*;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;

/**
 * H2 数据库存储，支持批量写入和失败转存本地文件。
 * H2 storage for method time records, supports batch insert and failover to local file.
 * 提供自动补偿、定期清理、CSV导出等高可用特性。
 * Provides auto-compensation, scheduled cleanup, CSV export and other HA features.
 *
 * @author mingsha
 */
public class H2Storage {
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private static final String TABLE = "method_time_stat";
    private static Connection conn;
    private static AgentMonitor monitor;
    private static boolean isMemoryMode;
    public static void setMonitor(AgentMonitor m) { monitor = m; }

    /**
     * 动态构建数据库URL，支持内存模式和文件模式
     * Dynamically build database URL, support memory mode and file mode
     * @return 数据库连接URL | database connection URL
     */
    private static String getDbUrl() {
        String path = AgentConfig.getH2Path();
        if (path.startsWith("mem:")) {
            isMemoryMode = true;
            // 内存模式：DB_CLOSE_DELAY=-1 防止连接关闭时数据库被销毁
            // Memory mode: DB_CLOSE_DELAY=-1 prevents database destruction when connection closes
            return "jdbc:h2:" + path + ";DB_CLOSE_DELAY=-1";
        } else {
            isMemoryMode = false;
            // 文件模式：AUTO_SERVER=TRUE 支持多进程访问
            // File mode: AUTO_SERVER=TRUE supports multi-process access
            return "jdbc:h2:file:" + path + ";AUTO_SERVER=TRUE";
        }
    }

    /**
     * 初始化H2数据库连接并自动建表。
     * Initialize H2 database connection and auto-create table.
     * 支持自动建库建表、索引优化、分区表等高级特性。
     * Supports auto database/table creation, index optimization, partitioning, etc.
     * @throws SQLException 数据库异常 | SQL exception
     */
    public static void init() throws SQLException {
        conn = DriverManager.getConnection(getDbUrl(), USER, PASSWORD);
        try (Statement stmt = conn.createStatement()) {
            // 1. 创建主表（如果不存在）
            // Create main table (if not exists)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "class_name VARCHAR(255) NOT NULL, " +
                    "method_name VARCHAR(255) NOT NULL, " +
                    "start_time BIGINT NOT NULL, " +
                    "end_time BIGINT NOT NULL, " +
                    "duration_ns BIGINT NOT NULL, " +
                    "thread_name VARCHAR(128), " +
                    "extra_info VARCHAR(512), " +
                    "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            
            // 创建主表索引
            // Create main table indexes
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_class_method ON " + TABLE + " (class_name, method_name)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_duration ON " + TABLE + " (duration_ns)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_create_time ON " + TABLE + " (create_time)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_thread ON " + TABLE + " (thread_name)");
            
            // 2. 创建统计汇总表（如果不存在）
            // Create statistics summary table (if not exists)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + "_summary (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "class_name VARCHAR(255) NOT NULL, " +
                    "method_name VARCHAR(255) NOT NULL, " +
                    "total_calls BIGINT DEFAULT 0, " +
                    "total_duration_ns BIGINT DEFAULT 0, " +
                    "avg_duration_ns BIGINT DEFAULT 0, " +
                    "min_duration_ns BIGINT DEFAULT 0, " +
                    "max_duration_ns BIGINT DEFAULT 0, " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            
            // 创建汇总表索引和约束
            // Create summary table indexes and constraints
            stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS uk_class_method ON " + TABLE + "_summary (class_name, method_name)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_avg_duration ON " + TABLE + "_summary (avg_duration_ns)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_total_calls ON " + TABLE + "_summary (total_calls)");
            
            // 3. 创建慢查询表（如果不存在）
            // Create slow query table (if not exists)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + "_slow (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "class_name VARCHAR(255) NOT NULL, " +
                    "method_name VARCHAR(255) NOT NULL, " +
                    "duration_ns BIGINT NOT NULL, " +
                    "thread_name VARCHAR(128), " +
                    "extra_info VARCHAR(512), " +
                    "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            
            // 创建慢查询表索引
            // Create slow query table indexes
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_duration_slow ON " + TABLE + "_slow (duration_ns)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_create_time_slow ON " + TABLE + "_slow (create_time)");
            
            // 4. 创建数据库版本表（如果不存在）
            // Create database version table (if not exists)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS db_version (" +
                    "version VARCHAR(20) PRIMARY KEY, " +
                    "description VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            
            // 5. 插入或更新数据库版本信息
            // Insert or update database version info
            stmt.executeUpdate("MERGE INTO db_version (version, description) VALUES ('1.0', 'Initial schema with main table, summary table, slow query table')");
            
            System.out.println("[mingsha-agent][数据库] 数据库初始化完成，已创建主表、汇总表、慢查询表");
            System.out.println("[mingsha-agent][数据库] Database initialization completed, created main table, summary table, slow query table");
        }
    }

    /**
     * 批量写入方法耗时数据，失败时自动转存本地文件。
     * Batch insert method time records, failover to local file on error.
     * 支持自动汇总统计和慢查询识别。
     * Supports auto summary statistics and slow query identification.
     * @param records 采集数据批量 | batch of method time records
     */
    public static void batchInsert(List<MethodTimeRecord> records) {
        if (records == null || records.isEmpty()) return;
        
        // 1. 写入主表
        // Write to main table
        String sql = "INSERT INTO " + TABLE + " (class_name, method_name, start_time, end_time, duration_ns, thread_name, extra_info) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (MethodTimeRecord r : records) {
                ps.setString(1, r.className);
                ps.setString(2, r.methodName);
                ps.setLong(3, r.startTime);
                ps.setLong(4, r.endTime);
                ps.setLong(5, r.durationNs);
                ps.setString(6, r.threadName);
                ps.setString(7, r.extraInfo);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            // 写入失败，转存本地文件 | failover to local file on error
            if (monitor != null) monitor.recordWriteError();
            saveToLocal(records);
            return;
        }
        
        // 2. 自动汇总统计
        // Auto summary statistics
        try {
            updateSummaryStatistics(records);
        } catch (SQLException e) {
            System.err.println("[mingsha-agent][汇总] 汇总统计失败: " + e.getMessage());
        }
        
        // 3. 识别并记录慢查询
        // Identify and record slow queries
        try {
            recordSlowQueries(records);
        } catch (SQLException e) {
            System.err.println("[mingsha-agent][慢查询] 慢查询记录失败: " + e.getMessage());
        }
    }

    /**
     * 本地转存数据，写入CSV文件。
     * Save records to local CSV file for failover.
     * @param records 采集数据批量 | batch of method time records
     */
    private static void saveToLocal(List<MethodTimeRecord> records) {
        String file = "method_time_backup_" + System.currentTimeMillis() + ".csv";
        try (FileWriter fw = new FileWriter(file, true)) {
            for (MethodTimeRecord r : records) {
                fw.write(String.format("%s,%s,%d,%d,%d,%s,%s\n",
                        r.className, r.methodName, r.startTime, r.endTime, r.durationNs, r.threadName, r.extraInfo));
            }
        } catch (IOException ignore) { if (monitor != null) monitor.recordFailoverError(); }
    }

    /**
     * 更新汇总统计表，自动计算各方法的调用次数、总耗时、平均耗时等。
     * Update summary statistics table, auto-calculate call count, total duration, avg duration, etc.
     * @param records 采集数据批量 | batch of method time records
     * @throws SQLException 数据库异常 | SQL exception
     */
    private static void updateSummaryStatistics(List<MethodTimeRecord> records) throws SQLException {
        // 按类名+方法名分组统计
        // Group by class_name + method_name
        java.util.Map<String, java.util.List<MethodTimeRecord>> groupedRecords = new java.util.HashMap<>();
        for (MethodTimeRecord record : records) {
            String key = record.className + "|" + record.methodName;
            groupedRecords.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(record);
        }
        
        // 批量更新汇总表
        // Batch update summary table
        String upsertSql = "MERGE INTO " + TABLE + "_summary (class_name, method_name, total_calls, total_duration_ns, avg_duration_ns, min_duration_ns, max_duration_ns, last_update) " +
                "KEY(class_name, method_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        
        try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
            for (java.util.Map.Entry<String, java.util.List<MethodTimeRecord>> entry : groupedRecords.entrySet()) {
                String[] parts = entry.getKey().split("\\|");
                String className = parts[0];
                String methodName = parts[1];
                java.util.List<MethodTimeRecord> groupRecords = entry.getValue();
                
                // 计算统计值
                // Calculate statistics
                long totalCalls = groupRecords.size();
                long totalDuration = groupRecords.stream().mapToLong(r -> r.durationNs).sum();
                long avgDuration = totalDuration / totalCalls;
                long minDuration = groupRecords.stream().mapToLong(r -> r.durationNs).min().orElse(0);
                long maxDuration = groupRecords.stream().mapToLong(r -> r.durationNs).max().orElse(0);
                
                ps.setString(1, className);
                ps.setString(2, methodName);
                ps.setLong(3, totalCalls);
                ps.setLong(4, totalDuration);
                ps.setLong(5, avgDuration);
                ps.setLong(6, minDuration);
                ps.setLong(7, maxDuration);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * 识别并记录慢查询到慢查询表。
     * Identify and record slow queries to slow query table.
     * @param records 采集数据批量 | batch of method time records
     * @throws SQLException 数据库异常 | SQL exception
     */
    private static void recordSlowQueries(List<MethodTimeRecord> records) throws SQLException {
        // 获取慢查询阈值（默认1ms）
        // Get slow query threshold (default 1ms)
        long slowThresholdNs = AgentConfig.getSlowQueryThresholdNs();
        
        // 筛选慢查询
        // Filter slow queries
        java.util.List<MethodTimeRecord> slowRecords = records.stream()
                .filter(r -> r.durationNs >= slowThresholdNs)
                .collect(java.util.stream.Collectors.toList());
        
        if (slowRecords.isEmpty()) return;
        
        // 批量插入慢查询表
        // Batch insert to slow query table
        String insertSql = "INSERT INTO " + TABLE + "_slow (class_name, method_name, duration_ns, thread_name, extra_info) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (MethodTimeRecord record : slowRecords) {
                ps.setString(1, record.className);
                ps.setString(2, record.methodName);
                ps.setLong(3, record.durationNs);
                ps.setString(4, record.threadName);
                ps.setString(5, record.extraInfo);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * 自动补偿本地转存数据入库（启动和定时调用）。
     * Auto-compensate local backup files into DB (on startup and scheduled).
     */
    public static void compensateLocalFiles() {
        File dir = new File(".");
        File[] files = dir.listFiles((d, name) -> name.startsWith("method_time_backup_") && name.endsWith(".csv"));
        if (files == null) return;
        for (File file : files) {
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line;
                ArrayList<MethodTimeRecord> batch = new ArrayList<>();
                while ((line = br.readLine()) != null) {
                    String[] arr = line.split(",", 7);
                    if (arr.length == 7) {
                        batch.add(new MethodTimeRecord(arr[0], arr[1], Long.parseLong(arr[2]), Long.parseLong(arr[3]), Long.parseLong(arr[4]), arr[5], arr[6]));
                    }
                    if (batch.size() >= AgentConfig.getBatchSize()) {
                        batchInsert(batch);
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) batchInsert(batch);
                br.close();
                if (file.delete()) {
                    System.out.println("[mingsha-agent][补偿] 已成功补偿并删除 " + file.getName());
                }
            } catch (Exception e) {
                System.err.println("[mingsha-agent][补偿] 补偿文件 " + file.getName() + " 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 导出全部采集数据为CSV文件。
     * Export all method time records to CSV file.
     * @param outFile 导出文件名 | output file name
     * @return 导出结果 | export result
     */
    public static String exportAllToCsv(String outFile) {
        try (Connection c = DriverManager.getConnection(getDbUrl(), USER, PASSWORD);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM " + TABLE);
             FileWriter fw = new FileWriter(outFile)) {
            int colCount = rs.getMetaData().getColumnCount();
            // 写表头 | write header
            for (int i = 1; i <= colCount; i++) {
                fw.write(rs.getMetaData().getColumnName(i));
                if (i < colCount) fw.write(",");
            }
            fw.write("\n");
            // 写数据 | write data
            while (rs.next()) {
                for (int i = 1; i <= colCount; i++) {
                    fw.write(rs.getString(i));
                    if (i < colCount) fw.write(",");
                }
                fw.write("\n");
            }
            return "[导出成功] " + outFile;
        } catch (Exception e) {
            return "[导出失败] " + e.getMessage();
        }
    }

    /**
     * 定期清理历史数据，根据内存模式和文件模式采用不同的驱逐策略。
     * Periodically clean up old records, use different eviction strategies for memory mode and file mode.
     * 内存模式：优先按条数清理，防止内存溢出。
     * Memory mode: prioritize cleaning by row count to prevent memory overflow.
     * 文件模式：优先按时间清理，保证数据持久性。
     * File mode: prioritize cleaning by time to ensure data persistence.
     */
    public static void cleanHistory() {
        try (Statement stmt = conn.createStatement()) {
            int days = AgentConfig.getRetentionDays();
            int rows = AgentConfig.getRetentionRows();
            
            if (isMemoryMode) {
                // 内存模式：优先按条数清理，防止内存溢出
                // Memory mode: prioritize cleaning by row count to prevent memory overflow
                cleanHistoryForMemoryMode(stmt, rows, days);
            } else {
                // 文件模式：优先按时间清理，保证数据持久性
                // File mode: prioritize cleaning by time to ensure data persistence
                cleanHistoryForFileMode(stmt, days, rows);
            }
            
            // 重新计算汇总表（基于清理后的主表数据）
            // Recalculate summary table (based on cleaned main table)
            recalculateSummaryTable(stmt);
            
            String mode = isMemoryMode ? "内存模式" : "文件模式";
            System.out.println("[mingsha-agent][清理] " + mode + "历史数据清理完成，已清理主表和慢查询表，重新计算汇总表");
        } catch (SQLException e) {
            System.err.println("[mingsha-agent][清理] H2历史数据清理异常: " + e.getMessage());
        }
    }

    /**
     * 内存模式数据清理策略：优先按条数清理，防止内存溢出
     * Memory mode data cleanup strategy: prioritize cleaning by row count to prevent memory overflow
     */
    private static void cleanHistoryForMemoryMode(Statement stmt, int maxRows, int maxDays) throws SQLException {
        // 1. 优先按条数清理主表（保留最新的maxRows条）
        // Prioritize cleaning main table by row count (keep latest maxRows)
        stmt.executeUpdate("DELETE FROM " + TABLE + " WHERE id NOT IN (SELECT id FROM " + TABLE + " ORDER BY id DESC LIMIT " + maxRows + ")");
        
        // 2. 如果条数清理后仍有超过maxDays的数据，再按时间清理
        // If there are still records older than maxDays after row count cleaning, clean by time
        stmt.executeUpdate("DELETE FROM " + TABLE + " WHERE create_time < DATEADD('DAY', -" + maxDays + ", CURRENT_TIMESTAMP)");
        
        // 3. 慢查询表也按条数优先清理
        // Clean slow query table by row count first
        stmt.executeUpdate("DELETE FROM " + TABLE + "_slow WHERE id NOT IN (SELECT id FROM " + TABLE + "_slow ORDER BY id DESC LIMIT " + (maxRows / 10) + ")");
        stmt.executeUpdate("DELETE FROM " + TABLE + "_slow WHERE create_time < DATEADD('DAY', -" + maxDays + ", CURRENT_TIMESTAMP)");
    }

    /**
     * 文件模式数据清理策略：优先按时间清理，保证数据持久性
     * File mode data cleanup strategy: prioritize cleaning by time to ensure data persistence
     */
    private static void cleanHistoryForFileMode(Statement stmt, int maxDays, int maxRows) throws SQLException {
        // 1. 优先按时间清理主表
        // Prioritize cleaning main table by time
        stmt.executeUpdate("DELETE FROM " + TABLE + " WHERE create_time < DATEADD('DAY', -" + maxDays + ", CURRENT_TIMESTAMP)");
        
        // 2. 如果时间清理后仍有超过maxRows的数据，再按条数清理
        // If there are still more than maxRows records after time cleaning, clean by row count
        stmt.executeUpdate("DELETE FROM " + TABLE + " WHERE id NOT IN (SELECT id FROM " + TABLE + " ORDER BY id DESC LIMIT " + maxRows + ")");
        
        // 3. 慢查询表也按时间优先清理
        // Clean slow query table by time first
        stmt.executeUpdate("DELETE FROM " + TABLE + "_slow WHERE create_time < DATEADD('DAY', -" + maxDays + ", CURRENT_TIMESTAMP)");
    }

    /**
     * 重新计算汇总表
     * Recalculate summary table
     */
    private static void recalculateSummaryTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("DELETE FROM " + TABLE + "_summary");
        stmt.executeUpdate("INSERT INTO " + TABLE + "_summary (class_name, method_name, total_calls, total_duration_ns, avg_duration_ns, min_duration_ns, max_duration_ns) " +
                "SELECT class_name, method_name, COUNT(*) as total_calls, SUM(duration_ns) as total_duration_ns, " +
                "AVG(duration_ns) as avg_duration_ns, MIN(duration_ns) as min_duration_ns, MAX(duration_ns) as max_duration_ns " +
                "FROM " + TABLE + " GROUP BY class_name, method_name");
    }

    /**
     * 启动定时清理任务（建议由健康守护线程注册）。
     * Start scheduled cleanup task (recommended to be registered by thread guardian).
     * @param intervalMs 清理间隔（毫秒）| cleanup interval (ms)
     * @return 清理线程 | cleanup thread
     */
    public static Thread createCleanerThread(long intervalMs) {
        return new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    cleanHistory();
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "mingsha-agent-h2cleaner");
    }

    /**
     * 关闭数据库连接。
     * Close the database connection.
     */
    public static void close() {
        try { if (conn != null) conn.close(); } catch (SQLException ignore) {}
    }
} 