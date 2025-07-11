#!/bin/bash

# 测试自动建库建表功能
# Test auto database and table creation functionality

echo "=== 测试自动建库建表功能 ==="
echo "=== Test Auto Database and Table Creation ==="
echo

# 1. 清理旧的数据库文件
echo "1. 清理旧的数据库文件..."
echo "1. Clean old database files..."
rm -f mingsha_javaagent_method_time_h2_db.*

# 2. 编译项目
echo
echo "2. 编译项目..."
echo "2. Build project..."
make clean
make build

# 3. 运行测试应用
echo
echo "3. 运行测试应用（启动agent）..."
echo "3. Run test application (start agent)..."
java -javaagent:target/mingsha-javaagent-methodtime-0.0.1-SNAPSHOT.jar \
     -Dmingsha.agent.config.collector.packages="com.example.*" \
     -Dmingsha.agent.config.collector.samplingRate=1.0 \
     -Dmingsha.agent.config.collector.minDurationNs=0 \
     -Dmingsha.agent.config.storage.slowQueryThresholdNs=500000 \
     -jar ../test-app/target/test-app-1.0.0.jar &

TEST_PID=$!
echo "测试应用PID: $TEST_PID"
echo "Test app PID: $TEST_PID"

# 4. 等待agent启动
echo
echo "4. 等待agent启动..."
echo "4. Wait for agent to start..."
sleep 5

# 5. 检查数据库文件是否创建
echo
echo "5. 检查数据库文件..."
echo "5. Check database files..."
if [ -f "mingsha_javaagent_method_time_h2_db.mv.db" ]; then
    echo "✓ 数据库文件已创建"
    echo "✓ Database file created"
    ls -la mingsha_javaagent_method_time_h2_db.*
else
    echo "✗ 数据库文件未创建"
    echo "✗ Database file not created"
fi

# 6. 使用H2控制台查看数据库结构
echo
echo "6. 使用H2控制台查看数据库结构..."
echo "6. Use H2 console to view database structure..."
echo "请手动打开浏览器访问: http://localhost:8082"
echo "Please manually open browser to: http://localhost:8082"
echo "连接信息:"
echo "Connection info:"
echo "  JDBC URL: jdbc:h2:file:./mingsha_javaagent_method_time_h2_db"
echo "  Username: sa"
echo "  Password: (empty)"
echo

# 7. 使用Telnet查看数据库信息
echo
echo "7. 使用Telnet查看数据库信息..."
echo "7. Use Telnet to view database info..."
echo "等待5秒后连接Telnet..."
echo "Wait 5 seconds then connect to Telnet..."
sleep 5

echo "连接Telnet端口5005..."
echo "Connect to Telnet port 5005..."
echo "db info" | nc localhost 5005
echo
echo "db tables" | nc localhost 5005
echo
echo "db schema" | nc localhost 5005
echo
echo "db stats" | nc localhost 5005

# 8. 停止测试应用
echo
echo "8. 停止测试应用..."
echo "8. Stop test application..."
kill $TEST_PID
wait $TEST_PID 2>/dev/null

echo
echo "=== 测试完成 ==="
echo "=== Test Completed ==="
echo
echo "检查结果:"
echo "Check results:"
echo "1. 数据库文件是否创建: mingsha_javaagent_method_time_h2_db.mv.db"
echo "1. Database file created: mingsha_javaagent_method_time_h2_db.mv.db"
echo "2. 表是否自动创建: method_time_stat, method_time_stat_summary, method_time_stat_slow, db_version"
echo "2. Tables auto-created: method_time_stat, method_time_stat_summary, method_time_stat_slow, db_version"
echo "3. 索引是否创建: 查看表结构中的索引信息"
echo "3. Indexes created: check index info in table schema"
echo "4. 数据是否自动汇总: 查看汇总表数据"
echo "4. Data auto-summarized: check summary table data"
echo "5. 慢查询是否记录: 查看慢查询表数据"
echo "5. Slow queries recorded: check slow query table data" 