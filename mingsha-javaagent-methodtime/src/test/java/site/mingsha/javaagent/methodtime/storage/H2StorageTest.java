package site.mingsha.javaagent.methodtime.storage;

import site.mingsha.javaagent.methodtime.collector.MethodTimeRecord;
import org.junit.jupiter.api.*;
import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H2Storage 批量写入与导出功能单元测试。
 * Unit test for H2Storage batch insert and export functionality.
 * 验证批量写入、导出CSV是否正常。
 * Verify batch insert and CSV export are correct.
 *
 * @author mingsha
 * 
 */
public class H2StorageTest {
    /**
     * 初始化 H2 数据库。
     * Initialize H2 database for test.
     */
    @BeforeAll
    public static void setup() throws Exception {
        H2Storage.init();
    }

    /**
     * 测试批量写入与导出功能。
     * Test batch insert and export to CSV.
     * 预期：写入后可导出CSV文件，且文件存在。
     * Expect: After insert, CSV file can be exported and exists.
     */
    @Test
    public void testBatchInsertAndExport() {
        List<MethodTimeRecord> batch = new ArrayList<>();
        batch.add(new MethodTimeRecord("TestClass", "testMethod", 1, 2, 1, "main", ""));
        H2Storage.batchInsert(batch);
        String outFile = "test_export.csv";
        String result = H2Storage.exportAllToCsv(outFile);
        assertTrue(result.startsWith("[导出成功]"));
        assertTrue(new File(outFile).exists());
        new File(outFile).delete();
    }

    /**
     * 关闭 H2 数据库。
     * Close H2 database after test.
     * 并清理所有 H2 生成的数据库文件。
     */
    @AfterAll
    public static void cleanup() {
        H2Storage.close();
        // 从配置文件读取 H2 数据库路径，清理所有相关文件
        String h2Path = site.mingsha.javaagent.methodtime.config.AgentConfig.getH2Path();
        String base = h2Path.replace("./", ""); // 移除路径前缀，获取文件名
        String[] suffixes = {".mv.db", ".h2.db", ".trace.db"};
        for (String suffix : suffixes) {
            File f = new File(base + suffix);
            if (f.exists()) {
                f.delete();
            }
        }
    }
} 