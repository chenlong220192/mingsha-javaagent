# mingsha-javaagent-methodtime

## 简介

mingsha-javaagent-methodtime 是一款高性能、可配置、支持无损卸载的 Java Agent，专注于方法级别纳秒级耗时采集，数据存入 H2 数据库，并提供安全的 Telnet 管理服务。

## 主要特性
- 零干扰、零中断，主进程安全
- 纳秒级方法耗时采集，支持采样率、包范围、最小阈值等多维度配置
- **自动建库建表**：首次启动自动创建数据库文件和完整表结构
- **自动汇总统计**：实时计算各方法的调用次数、平均耗时等统计信息
- **自动慢查询识别**：自动识别并记录超过阈值的慢查询
- 数据批量写入 H2，失败自动转存本地，支持自动补偿
- 丰富的Telnet管理命令，支持Agent管理、数据库查询、统计信息等
- 线程健康自愈、CPU熔断、自动清理历史数据
- 全局异常捕获，安全可靠
- **字节码增强优化**：基于 ASM 9.5，支持 Java 8+，自动栈帧计算，兼容性更好

## 📚 文档导航

### 🚀 快速入门
- **[00-快速开始](docs/00-快速开始.md)** - 5分钟快速上手指南，包含环境准备、安装配置、基本使用、配置说明、Telnet命令、数据表结构

### ⚙️ 架构设计
- **[01-架构设计](docs/01-架构设计.md)** - 系统整体架构设计和模块说明

### 🔧 技术基础
- **[02-技术栈概览](docs/02-技术栈概览.md)** - 项目技术栈介绍和选型说明
- **[03-ASM字节码增强技术](docs/03-ASM字节码增强技术.md)** - 核心技术原理详解
- **[04-H2数据库技术](docs/04-H2数据库技术.md)** - 存储技术详解和优化
- **[05-测试技术栈](docs/05-测试技术栈.md)** - 测试技术介绍和最佳实践

### 📖 使用指南
- **[06-配置覆盖指南](docs/配置覆盖指南.md)** - 通过JVM系统属性覆盖配置的详细说明
- **[07-API接口](docs/07-API接口.md)** - Telnet管理接口和SQL查询接口详细说明
- **[08-使用示例](docs/08-使用示例.md)** - 不同场景的配置示例和最佳实践
- **[09-部署指南](docs/09-部署指南.md)** - 开发、测试、生产环境部署方案
- **[10-监控指标](docs/10-监控指标.md)** - 监控指标体系、告警规则和面板配置
- **[11-故障排查](docs/11-故障排查.md)** - 常见问题诊断、日志分析和解决方案

### 👨‍💻 开发文档
- **[12-开发指南](docs/12-开发指南.md)** - 代码贡献规范、插件开发和测试指南
- **[13-性能基准测试](docs/13-性能基准测试.md)** - 性能测试结果、压力测试和优化建议

### 📔 变更日志
- **[14-CHANGELOG](docs/14-CHANGELOG.md)** - 版本变更历史、升级指南和兼容性说明

### 🎨 资源文件
- **[总体架构设计图](docs/img/总体架构设计图.svg)** - 系统架构可视化图表

---

## 快速开始

> 📖 **详细的使用指南请查看 [00-快速开始](docs/00-快速开始.md)**

### 1. 打包
```bash
make package
```

### 2. 启动应用（默认配置）
```bash
java -javaagent:target/mingsha-javaagent-methodtime-2026.04.09.jar -jar your-app.jar
```

### 3. 启动应用（自定义配置）
```bash
java -javaagent:target/mingsha-javaagent-methodtime-2026.04.09.jar \
     -Dmingsha.agent.config.collector.packages="com.myapp.* "\
     -Dmingsha.agent.config.collector.samplingRate=0.5 \
     -Dmingsha.agent.config.storage.h2.path="./my_app_db" \
     -jar your-app.jar
```

### 4. 连接管理
```bash
telnet localhost 5005
```

### 5. 查看状态
```bash
agent status
```

### 6. 查看帮助
```bash
help
```

### 7. 查看数据库统计
```bash
db stats
```

### 8. 测试自动建库建表
```bash
./bin/test-auto-db.sh
```

### 9. 测试配置覆盖
```bash
./bin/test-config-override.sh
```

### 10. 测试Telnet命令
```bash
./bin/test-telnet-commands.sh
```

## 性能与安全说明
- 采集线程、写入线程、Telnet线程、监控线程均独立，主进程零干扰
- 支持批量写入、自动补偿、自动清理、CPU 熔断、线程自愈
- 全局异常捕获，保证主进程稳定
- Telnet 端口冲突自动切换，SQL 沙箱防注入

## 常见问题与FAQ
- **如何动态调整采样率/采集包？**
  - 修改 agent-config.yml 后，通过 Telnet 执行 `agent reload` 即可热加载。
- **如何通过JVM参数覆盖配置？**
  - 使用 `-Dmingsha.agent.config.xxx=value` 参数，如 `-Dmingsha.agent.config.collector.samplingRate=0.5`。
- **数据库会自动创建吗？**
  - 是的，首次启动时会自动创建数据库文件和所有表结构（主表、汇总表、慢查询表、版本表）。
- **如何查看数据库统计信息？**
  - Telnet 执行 `db stats` 查看主表、汇总表、慢查询表的统计信息。
- **如何查看所有表结构？**
  - Telnet 执行 `db schema` 查看所有表的详细结构。
- **如何查看慢查询记录？**
  - Telnet 执行 `select * from method_time_stat_slow order by duration_ns desc limit 10`。
- **如何查看方法汇总统计？**
  - Telnet 执行 `select * from method_time_stat_summary order by total_calls desc limit 10`。
- **如何导出采集数据？**
  - Telnet 执行 `agent export /tmp/out.csv`。
- **如何保证数据不丢失？**
  - 支持本地转存与自动补偿，异常可通过 `agent errors` 查询。
- **如何扩展采集字段？**
  - 修改 MethodTimeRecord 及表结构，重启 agent。
