#!/bin/bash

#======================================================================
# mingsha-javaagent-methodtime Telnet命令测试脚本
# 演示新增的Telnet管理命令和数据库查询功能
#======================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
AGENT_JAR="$PROJECT_DIR/target/mingsha-javaagent-methodtime-0.0.1-SNAPSHOT.jar"

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║           🚀 mingsha-javaagent-methodtime Telnet命令测试 🚀           ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo

# 检查Agent jar包是否存在
if [ ! -f "$AGENT_JAR" ]; then
    echo -e "${RED}❌ Agent jar包不存在: $AGENT_JAR${NC}"
    echo -e "${YELLOW}请先运行: make package${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Agent jar包已找到: $AGENT_JAR${NC}"
echo

# 测试应用jar包路径
TEST_APP_JAR="$PROJECT_DIR/../mingsha-mybatis-h2-spring-boot-example-1.0.0.jar"

if [ ! -f "$TEST_APP_JAR" ]; then
    echo -e "${YELLOW}⚠️  测试应用jar包不存在: $TEST_APP_JAR${NC}"
    echo -e "${YELLOW}将使用简单的Java应用进行测试${NC}"
    USE_SIMPLE_TEST=true
else
    echo -e "${GREEN}✅ 测试应用jar包已找到: $TEST_APP_JAR${NC}"
    USE_SIMPLE_TEST=false
fi

echo

# 创建临时测试目录
TEST_DIR="$PROJECT_DIR/target/telnet-test"
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"

echo -e "${BLUE}📁 测试目录: $TEST_DIR${NC}"
echo

# 创建简单的测试应用
if [ "$USE_SIMPLE_TEST" = true ]; then
    cat > TestApp.java << 'EOF'
public class TestApp {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 测试应用启动 ===");
        System.out.println("应用将运行30秒，期间会调用一些方法...");
        
        for (int i = 0; i < 30; i++) {
            System.out.println("运行中... " + (i + 1) + "/30");
            
            // 模拟一些方法调用
            fastMethod();
            slowMethod();
            mediumMethod();
            
            Thread.sleep(1000);
        }
        
        System.out.println("=== 测试应用结束 ===");
    }
    
    private static void fastMethod() {
        // 快速方法
    }
    
    private static void slowMethod() throws InterruptedException {
        // 慢方法
        Thread.sleep(50);
    }
    
    private static void mediumMethod() throws InterruptedException {
        // 中等速度方法
        Thread.sleep(20);
    }
}
EOF
    javac TestApp.java
fi

# 启动应用
echo -e "${YELLOW}🚀 启动测试应用...${NC}"
if [ "$USE_SIMPLE_TEST" = true ]; then
    java -javaagent:"$AGENT_JAR" \
         -Dmingsha.agent.config.collector.packages=TestApp \
         -Dmingsha.agent.config.collector.samplingRate=1.0 \
         -Dmingsha.agent.config.storage.h2.path=./telnet_test_db \
         -Dmingsha.agent.config.manage.telnet.port=5006 \
         TestApp &
else
    java -javaagent:"$AGENT_JAR" \
         -Dmingsha.agent.config.collector.packages=com.example.* \
         -Dmingsha.agent.config.collector.samplingRate=1.0 \
         -Dmingsha.agent.config.storage.h2.path=./telnet_test_db \
         -Dmingsha.agent.config.manage.telnet.port=5006 \
         -jar "$TEST_APP_JAR" &
fi

TEST_PID=$!

# 等待应用启动
echo -e "${YELLOW}⏳ 等待应用启动...${NC}"
sleep 5

# 检查进程是否还在运行
if ! kill -0 $TEST_PID 2>/dev/null; then
    echo -e "${RED}❌ 测试应用启动失败${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 测试应用已启动，PID: $TEST_PID${NC}"
echo

# 等待一段时间让数据采集
echo -e "${YELLOW}⏳ 等待数据采集...${NC}"
sleep 10

# 测试Telnet命令
echo -e "${CYAN}🧪 开始测试Telnet命令...${NC}"
echo

# 创建Telnet命令测试脚本
cat > telnet_test.exp << 'EOF'
#!/usr/bin/expect -f

set timeout 10
set port [lindex $argv 0]

spawn telnet localhost $port

expect "欢迎使用 mingsha-agent 管理端口"
send "help\r"
expect "mingsha-agent 管理命令"
send "agent help\r"
expect "Agent管理命令"
send "db help\r"
expect "数据库查询命令"
send "agent status\r"
expect "CPU:"
send "agent config\r"
expect "Agent配置信息"
send "agent info\r"
expect "Agent详细信息"
send "agent version\r"
expect "v0.0.1-SNAPSHOT"
send "agent errors\r"
expect "异常统计"
send "db info\r"
expect "数据库信息"
send "db tables\r"
expect "数据库表列表"
send "db schema\r"
expect "表结构"
send "db stats\r"
expect "数据库统计信息"
send "db size\r"
expect "数据库大小信息"
send "select count(*) from method_time_stat\r"
expect "COUNT"
send "select class_name, method_name, avg(duration_ns) from method_time_stat group by class_name, method_name limit 5\r"
expect "CLASS_NAME"
send "db query select count(*) as total, avg(duration_ns) as avg_duration from method_time_stat\r"
expect "TOTAL"
send "quit\r"
expect eof
EOF

chmod +x telnet_test.exp

# 执行Telnet测试
echo -e "${BLUE}📡 执行Telnet命令测试...${NC}"
if command -v expect >/dev/null 2>&1; then
    ./telnet_test.exp 5006
    echo -e "${GREEN}✅ Telnet命令测试完成${NC}"
else
    echo -e "${YELLOW}⚠️  expect命令不可用，请手动测试Telnet命令${NC}"
    echo -e "${CYAN}手动测试命令:${NC}"
    echo "telnet localhost 5006"
    echo "help"
    echo "agent help"
    echo "db help"
    echo "agent status"
    echo "agent config"
    echo "agent info"
    echo "db info"
    echo "db stats"
    echo "db schema"
    echo "select count(*) from method_time_stat"
fi

echo

# 停止测试应用
echo -e "${YELLOW}🛑 停止测试应用...${NC}"
kill $TEST_PID 2>/dev/null || true
sleep 2

# 清理
echo -e "${BLUE}🧹 清理测试文件...${NC}"
cd "$PROJECT_DIR"
rm -rf "$TEST_DIR"

echo
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                       ✅ 测试完成！✅                        ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo
echo -e "${GREEN}📋 新增的Telnet命令功能:${NC}"
echo "1. 丰富的帮助系统：help、agent help、db help"
echo "2. Agent管理命令：status、config、info、version、errors、reload、export"
echo "3. 数据库查询命令：info、tables、schema、stats、size、query"
echo "4. 详细的统计信息：记录数、耗时统计、Top方法等"
echo "5. 安全的SQL查询：支持SELECT查询，禁止高危操作"
echo
echo -e "${GREEN}📖 更多信息请查看: docs/00-快速开始.md${NC}" 