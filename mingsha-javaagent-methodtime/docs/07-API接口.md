# API 接口文档

## 概述

mingsha-javaagent-methodtime 提供基于 Telnet 协议的远程管理接口，支持 SQL 查询、配置管理、数据导出等功能。所有接口都经过安全沙箱保护，确保系统安全。

## 连接方式

### Telnet 连接
```bash
# 连接到管理端口（默认5005）
telnet localhost 5005

# 如果端口冲突，会自动切换到下一个可用端口
# 连接成功后会显示欢迎信息
欢迎使用 mingsha-agent 管理端口，输入 help 查看命令
```

## 基础命令

### 1. help
**功能**: 查看所有支持的命令
**用法**: `help`
**响应**:
```
支持命令: help, select ... (只读SQL), agent status, agent reload, agent errors, agent export <file>
```

## Agent 管理命令

### 1. agent status
**功能**: 查看 Agent 运行状态
**用法**: `agent status`
**响应示例**:
```
CPU: 15.23%, 内存: 256 MB, 总丢失: 0 条
```
**字段说明**:
- `CPU`: 当前进程 CPU 使用率
- `内存`: 当前进程内存使用量（MB）
- `总丢失`: 数据丢失总数

### 2. agent reload
**功能**: 动态热加载配置文件
**用法**: `agent reload`
**响应示例**:
```
配置热加载成功
```
**说明**: 重新加载 `agent-config.yml` 配置文件，无需重启应用

### 3. agent errors
**功能**: 查看异常统计信息
**用法**: `agent errors`
**响应示例**:
```
采集异常: 0, 写入异常: 0, 转存异常: 0
```
**字段说明**:
- `采集异常`: 字节码增强采集数据时的异常次数
- `写入异常`: 数据库写入失败次数
- `转存异常`: 本地文件转存失败次数

### 4. agent export
**功能**: 导出采集数据为 CSV 文件
**用法**: `agent export <file_path>`
**示例**:
```bash
agent export /tmp/method_time_data.csv
```
**响应示例**:
```
数据导出成功: /tmp/method_time_data.csv
```
**说明**: 导出所有采集数据到指定路径的 CSV 文件

## SQL 查询接口

### 1. 查询语法
**功能**: 执行只读 SQL 查询
**用法**: `select ...`
**安全限制**: 
- 仅支持 SELECT 语句
- 禁止 UPDATE、DELETE、INSERT、DROP、ALTER、CREATE
- 禁止 UNION、WITH、子查询、多语句

### 2. 常用查询示例

#### 2.1 查询总记录数
```sql
SELECT COUNT(*) FROM method_time_stat
```

#### 2.2 查询最近1小时的数据
```sql
SELECT COUNT(*) FROM method_time_stat 
WHERE create_time > DATEADD('HOUR', -1, CURRENT_TIMESTAMP)
```

#### 2.3 查询平均耗时
```sql
SELECT 
    class_name,
    method_name,
    AVG(duration_ns) as avg_duration,
    MAX(duration_ns) as max_duration,
    COUNT(*) as call_count
FROM method_time_stat 
GROUP BY class_name, method_name 
ORDER BY avg_duration DESC
```

#### 2.4 查询慢方法（耗时超过1ms）
```sql
SELECT 
    class_name,
    method_name,
    duration_ns,
    thread_name,
    create_time
FROM method_time_stat 
WHERE duration_ns > 1000000 
ORDER BY duration_ns DESC
```

#### 2.5 查询线程统计
```sql
SELECT 
    thread_name,
    COUNT(*) as call_count,
    AVG(duration_ns) as avg_duration
FROM method_time_stat 
GROUP BY thread_name 
ORDER BY call_count DESC
```

## 数据表结构

### method_time_stat 表
```sql
CREATE TABLE method_time_stat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,           -- 主键ID
    class_name VARCHAR(255),                        -- 类名
    method_name VARCHAR(255),                       -- 方法名
    start_time BIGINT,                              -- 开始时间（纳秒）
    end_time BIGINT,                                -- 结束时间（纳秒）
    duration_ns BIGINT,                             -- 执行耗时（纳秒）
    thread_name VARCHAR(128),                       -- 线程名
    extra_info VARCHAR(512),                        -- 额外信息
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 创建时间
);
```

## 错误码说明

### 1. 连接错误
- **连接被拒绝**: 端口未开放或服务未启动
- **连接超时**: 网络问题或服务响应慢

### 2. SQL 错误
- **语法错误**: SQL 语句格式不正确
- **权限错误**: 尝试执行被禁止的操作
- **表不存在**: 数据表未创建

### 3. 命令错误
- **未知命令**: 输入了不存在的命令
- **参数错误**: 命令参数格式不正确

## 安全说明

### 1. SQL 沙箱
- 仅允许 SELECT 查询
- 自动过滤危险操作
- 防止 SQL 注入攻击

### 2. 访问控制
- 本地访问限制
- 端口冲突自动切换
- 最大连接数限制

### 3. 数据保护
- 只读查询接口
- 敏感信息过滤
- 操作日志记录

## 性能建议

### 1. 查询优化
- 使用索引字段进行查询
- 避免全表扫描
- 合理使用 LIMIT 限制结果集

### 2. 连接管理
- 及时关闭连接
- 避免长时间占用连接
- 使用连接池（如需要）

### 3. 监控建议
- 定期检查连接数
- 监控查询性能
- 关注异常统计

## 示例脚本

### 1. 自动化监控脚本
```bash
#!/bin/bash
# 连接 Telnet 并执行监控命令
telnet localhost 5005 << EOF
agent status
agent errors
SELECT COUNT(*) FROM method_time_stat WHERE create_time > DATEADD('HOUR', -1, CURRENT_TIMESTAMP)
quit
EOF
```

### 2. 数据导出脚本
```bash
#!/bin/bash
# 导出最近24小时的数据
telnet localhost 5005 << EOF
agent export /tmp/method_time_$(date +%Y%m%d_%H%M%S).csv
quit
EOF
```

### 3. 性能分析脚本
```bash
#!/bin/bash
# 分析慢方法
telnet localhost 5005 << EOF
SELECT class_name, method_name, AVG(duration_ns) as avg_duration, COUNT(*) as call_count 
FROM method_time_stat 
WHERE create_time > DATEADD('HOUR', -1, CURRENT_TIMESTAMP) 
GROUP BY class_name, method_name 
HAVING avg_duration > 1000000 
ORDER BY avg_duration DESC
quit
EOF
``` 