# Mingsha Java Agent

[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/site.mingsha.javaagent.methodtime/mingsha-javaagent.svg)](https://search.maven.org/artifact/site.mingsha.javaagent.methodtime/mingsha-javaagent)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/mingsha/mingsha-javaagent)

> 基于 Java Agent 技术的应用监控与诊断工具集，提供轻量级、高性能的运行时监控能力。

## 📖 简介

Mingsha Java Agent 是一个基于 Java Agent 技术的应用监控与诊断工具集，专注于为 Java 应用提供轻量级、高性能的运行时监控能力。通过字节码增强技术，实现对应用性能、故障诊断、安全防护等多维度的监控和干预。

### 🎯 设计理念

- **轻量级**: 最小化对应用性能的影响，生产环境友好
- **高性能**: 优化的字节码增强算法，低延迟数据收集
- **易扩展**: 模块化设计，支持功能插件化扩展
- **生产就绪**: 经过生产环境验证，稳定可靠

## ✨ 功能特性

### 🔍 性能监控
- **方法执行时间统计**: 精确统计方法调用耗时，识别性能瓶颈
- **内存分配跟踪**: 监控对象创建和内存使用模式
- **CPU热点分析**: 识别计算密集型热点方法
- **线程状态监控**: 实时监控线程状态和死锁检测
- **垃圾回收分析**: GC停顿时间和频率统计

### 🛠️ 故障诊断
- **异常捕获统计**: 自动收集异常信息和堆栈
- **慢查询追踪**: SQL执行时间监控和优化建议
- **死锁检测**: 自动识别线程死锁情况
- **内存泄漏追踪**: 对象引用链分析和泄漏检测
- **资源泄露检测**: 文件句柄、网络连接等资源监控

### 🔧 代码级干预
- **代码热替换**: 支持运行时代码修改和Bug修复
- **AOP植入**: 无侵入式功能增强和横切关注点处理
- **故障注入**: 混沌工程测试和容错能力验证
- **动态日志级别调整**: 生产环境日志级别动态控制
- **流量录制回放**: 请求响应数据录制和回放

### 🛡️ 安全防护
- **RASP防护**: 运行时应用安全防护
- **敏感操作审计**: 文件、网络操作监控和审计
- **反序列化防护**: 反序列化漏洞攻击防护
- **SQL注入检测**: SQL注入攻击实时检测
- **加密密钥监控**: 密钥使用和泄露检测

### 📊 可观测性增强
- **分布式追踪**: 全链路调用追踪和性能分析
- **指标暴露**: Prometheus集成和监控告警
- **日志上下文注入**: 自动关联日志和请求追踪
- **拓扑依赖发现**: 服务依赖关系自动发现
- **配置动态生效**: 运行时配置热更新

### ⚡ 资源优化
- **连接池监控**: 数据库连接池使用情况监控
- **缓存命中率统计**: 缓存策略优化和失效分析
- **类加载分析**: 类加载性能优化和冲突解决
- **对象池优化**: 对象复用和内存分配优化
- **文件IO监控**: 文件读写性能分析和优化

## 🏗️ 技术栈

### 核心技术
- **Java Agent**: JVM字节码增强技术
- **ASM**: 字节码操作框架
- **H2 Database**: 轻量级嵌入式数据库
- **Telnet**: 远程管理接口
- **Maven**: 项目构建和依赖管理

### 监控技术
- **JMX**: Java管理扩展
- **JVM TI**: JVM工具接口
- **字节码注入**: 运行时方法增强
- **采样统计**: 性能数据收集

### 开发工具
- **Checkstyle**: 代码风格检查
- **JUnit**: 单元测试框架
- **Make**: 构建自动化
- **Git**: 版本控制

## 📚 文档导航

### 📖 技术文档
- [Java Agent 功能全景分类表](docs/Java%20Agent%20功能全景分类表.md) - Java Agent 技术全景分类和选型指南

### 🔧 模块文档
- [MethodTime 模块文档](mingsha-javaagent-methodtime/README.md) - 方法执行时间监控模块详细文档

## 🤝 贡献指南

### 开发环境搭建

1. **Fork 项目**
```bash
git clone https://github.com/chenlong220192/mingsha-javaagent.git
cd mingsha-javaagent
```

2. **创建特性分支**
```bash
git checkout -b feature/your-feature-name
```

3. **提交代码**
```bash
git add .
git commit -m "feat: add your feature description"
git push origin feature/your-feature-name
```

### 代码规范

- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 使用 Checkstyle 进行代码风格检查
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范

### 测试要求

- 新增功能必须包含单元测试
- 测试覆盖率不低于 80%
- 所有测试必须通过

### 提交 Pull Request

1. 确保代码通过所有检查
2. 更新相关文档
3. 添加测试用例
4. 提交 Pull Request 并描述变更内容

## 🆘 技术支持

### 问题反馈

- **GitHub Issues**: [提交 Issue](https://github.com/chenlong220192/mingsha-javaagent/issues)
- **邮件支持**: chenlong220192@gmail.com
- **文档反馈**: 欢迎提交文档改进建议

### 社区交流

- **GitHub Discussions**: [参与讨论](https://github.com/chenlong220192/mingsha-javaagent/discussions)

---

## 📄 许可证

本项目采用 [MIT License](./LICENSE) 许可证。

---

<div align="center">

**如果这个项目对您有帮助，请给一个 ⭐️**

</div>
