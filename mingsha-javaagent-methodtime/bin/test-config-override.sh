#!/bin/bash

#======================================================================
# mingsha-javaagent-methodtime 配置覆盖测试脚本
# 演示如何通过JVM系统属性覆盖Agent配置
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
echo -e "${CYAN}║           🚀 mingsha-javaagent-methodtime 配置覆盖测试 🚀           ║${NC}"
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
TEST_DIR="$PROJECT_DIR/target/config-test"
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"

echo -e "${BLUE}📁 测试目录: $TEST_DIR${NC}"
echo

# 测试1: 默认配置
echo -e "${YELLOW}🧪 测试1: 默认配置${NC}"
echo -e "${CYAN}运行命令:${NC}"
echo "java -javaagent:$AGENT_JAR -jar $TEST_APP_JAR"
echo

if [ "$USE_SIMPLE_TEST" = true ]; then
    # 创建简单的测试应用
    cat > TestApp.java << 'EOF'
public class TestApp {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 测试应用启动 ===");
        System.out.println("应用将运行10秒...");
        
        for (int i = 0; i < 10; i++) {
            System.out.println("运行中... " + (i + 1) + "/10");
            Thread.sleep(1000);
        }
        
        System.out.println("=== 测试应用结束 ===");
    }
}
EOF
    javac TestApp.java
    java -javaagent:"$AGENT_JAR" TestApp &
    TEST_PID=$!
    sleep 15
    kill $TEST_PID 2>/dev/null || true
else
    timeout 15 java -javaagent:"$AGENT_JAR" -jar "$TEST_APP_JAR" || true
fi

echo
echo -e "${GREEN}✅ 测试1完成${NC}"
echo

# 测试2: 自定义配置
echo -e "${YELLOW}🧪 测试2: 自定义配置${NC}"
echo -e "${CYAN}运行命令:${NC}"
echo "java -javaagent:$AGENT_JAR \\"
echo "     -Dmingsha.agent.config.collector.packages=com.custom.* \\"
echo "     -Dmingsha.agent.config.collector.samplingRate=0.5 \\"
echo "     -Dmingsha.agent.config.storage.h2.path=./custom_db \\"
echo "     -Dmingsha.agent.config.manage.telnet.port=5006 \\"
echo "     -Dmingsha.agent.config.log.level=DEBUG \\"
echo "     -jar $TEST_APP_JAR"
echo

if [ "$USE_SIMPLE_TEST" = true ]; then
    java -javaagent:"$AGENT_JAR" \
         -Dmingsha.agent.config.collector.packages=com.custom.* \
         -Dmingsha.agent.config.collector.samplingRate=0.5 \
         -Dmingsha.agent.config.storage.h2.path=./custom_db \
         -Dmingsha.agent.config.manage.telnet.port=5006 \
         -Dmingsha.agent.config.log.level=DEBUG \
         TestApp &
    TEST_PID=$!
    sleep 15
    kill $TEST_PID 2>/dev/null || true
else
    timeout 15 java -javaagent:"$AGENT_JAR" \
         -Dmingsha.agent.config.collector.packages=com.custom.* \
         -Dmingsha.agent.config.collector.samplingRate=0.5 \
         -Dmingsha.agent.config.storage.h2.path=./custom_db \
         -Dmingsha.agent.config.manage.telnet.port=5006 \
         -Dmingsha.agent.config.log.level=DEBUG \
         -jar "$TEST_APP_JAR" || true
fi

echo
echo -e "${GREEN}✅ 测试2完成${NC}"
echo

# 测试3: 生产环境配置
echo -e "${YELLOW}🧪 测试3: 生产环境配置${NC}"
echo -e "${CYAN}运行命令:${NC}"
echo "java -javaagent:$AGENT_JAR \\"
echo "     -Dmingsha.agent.config.collector.packages=com.production.* \\"
echo "     -Dmingsha.agent.config.collector.samplingRate=0.1 \\"
echo "     -Dmingsha.agent.config.collector.minDurationNs=1000000 \\"
echo "     -Dmingsha.agent.config.storage.h2.path=./prod_db \\"
echo "     -Dmingsha.agent.config.storage.retentionDays=30 \\"
echo "     -Dmingsha.agent.config.monitor.cpu.fuseThreshold=90 \\"
echo "     -jar $TEST_APP_JAR"
echo

if [ "$USE_SIMPLE_TEST" = true ]; then
    java -javaagent:"$AGENT_JAR" \
         -Dmingsha.agent.config.collector.packages=com.production.* \
         -Dmingsha.agent.config.collector.samplingRate=0.1 \
         -Dmingsha.agent.config.collector.minDurationNs=1000000 \
         -Dmingsha.agent.config.storage.h2.path=./prod_db \
         -Dmingsha.agent.config.storage.retentionDays=30 \
         -Dmingsha.agent.config.monitor.cpu.fuseThreshold=90 \
         TestApp &
    TEST_PID=$!
    sleep 15
    kill $TEST_PID 2>/dev/null || true
else
    timeout 15 java -javaagent:"$AGENT_JAR" \
         -Dmingsha.agent.config.collector.packages=com.production.* \
         -Dmingsha.agent.config.collector.samplingRate=0.1 \
         -Dmingsha.agent.config.collector.minDurationNs=1000000 \
         -Dmingsha.agent.config.storage.h2.path=./prod_db \
         -Dmingsha.agent.config.storage.retentionDays=30 \
         -Dmingsha.agent.config.monitor.cpu.fuseThreshold=90 \
         -jar "$TEST_APP_JAR" || true
fi

echo
echo -e "${GREEN}✅ 测试3完成${NC}"
echo

# 清理
echo -e "${BLUE}🧹 清理测试文件...${NC}"
cd "$PROJECT_DIR"
rm -rf "$TEST_DIR"

echo
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                       ✅ 测试完成！✅                        ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo
echo -e "${GREEN}📋 配置覆盖功能说明:${NC}"
echo "1. 通过 -Dmingsha.agent.config.xxx=value 可以覆盖默认配置"
echo "2. 系统属性优先级高于内置YAML配置"
echo "3. Agent启动时会打印当前使用的配置信息"
echo "4. 支持字符串、数字、布尔值等类型自动转换"
echo
echo -e "${GREEN}📖 更多信息请查看: docs/配置覆盖指南.md${NC}" 