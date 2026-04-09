# Mingsha Java Agent

[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/site.mingsha.javaagent.methodtime/mingsha-javaagent.svg)](https://search.maven.org/artifact/site.mingsha.javaagent.methodtime/mingsha-javaagent)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/mingsha/mingsha-javaagent)

> Lightweight and high-performance runtime monitoring toolkit based on Java Agent technology for Java applications.

## Overview

Mingsha Java Agent is a monitoring and diagnostic toolkit based on Java Agent technology, focused on providing lightweight and high-performance runtime monitoring capabilities for Java applications. Through bytecode enhancement technology, it enables multi-dimensional monitoring and intervention of application performance, fault diagnosis, and security protection.

### Design Philosophy

- **Lightweight**: Minimizes impact on application performance, production-friendly
- **High Performance**: Optimized bytecode enhancement algorithms with low-latency data collection
- **Easy to Extend**: Modular design with plugin-based extensibility
- **Production Ready**: Validated in production environments, stable and reliable

## Features

### Performance Monitoring
- **Method Execution Time Statistics**: Accurate method call duration tracking to identify performance bottlenecks
- **Memory Allocation Tracking**: Monitor object creation and memory usage patterns
- **CPU Hotspot Analysis**: Identify compute-intensive hotspot methods
- **Thread State Monitoring**: Real-time thread state monitoring and deadlock detection
- **Garbage Collection Analysis**: GC pause time and frequency statistics

### Fault Diagnosis
- **Exception Capture Statistics**: Automatically collect exception information and stack traces
- **Slow Query Tracking**: SQL execution time monitoring and optimization suggestions
- **Deadlock Detection**: Automatic thread deadlock identification
- **Memory Leak Tracking**: Object reference chain analysis and leak detection
- **Resource Leak Detection**: File handles, network connections monitoring

### Code-Level Intervention
- **Code Hot Replacement**: Runtime code modification and bug fix support
- **AOP Injection**: Non-invasive feature enhancement and cross-cutting concerns handling
- **Fault Injection**: Chaos engineering testing and fault tolerance verification
- **Dynamic Log Level Adjustment**: Production environment log level dynamic control
- **Traffic Recording and Replay**: Request/response data recording and replay

### Security Protection
- **RASP Protection**: Runtime Application Self-Protection
- **Sensitive Operation Auditing**: File and network operation monitoring and auditing
- **Deserialization Protection**: Deserialization vulnerability attack protection
- **SQL Injection Detection**: Real-time SQL injection attack detection
- **Encryption Key Monitoring**: Key usage and leak detection

### Observability Enhancement
- **Distributed Tracing**: Full链路 call tracing and performance analysis
- **Metrics Exposure**: Prometheus integration and monitoring alerts
- **Log Context Injection**: Automatic log and request correlation
- **Topology Dependency Discovery**: Service dependency automatic discovery
- **Configuration Dynamic Refresh**: Runtime configuration hot update

### Resource Optimization
- **Connection Pool Monitoring**: Database connection pool usage monitoring
- **Cache Hit Rate Statistics**: Cache strategy optimization and invalidation analysis
- **Class Loading Analysis**: Class loading performance optimization and conflict resolution
- **Object Pool Optimization**: Object reuse and memory allocation optimization
- **File IO Monitoring**: File read/write performance analysis and optimization

## Technology Stack

### Core Technologies
- **Java Agent**: JVM bytecode enhancement technology
- **ASM**: Bytecode manipulation framework
- **H2 Database**: Lightweight embedded database
- **Telnet**: Remote management interface
- **Maven**: Project build and dependency management

### Monitoring Technologies
- **JMX**: Java Management Extensions
- **JVM TI**: JVM Tool Interface
- **Bytecode Injection**: Runtime method enhancement
- **Sampling Statistics**: Performance data collection

### Development Tools
- **Checkstyle**: Code style checking
- **JUnit**: Unit testing framework
- **Make**: Build automation
- **Git**: Version control

## Documentation

### Technical Documentation
- [Java Agent Feature Overview](docs/Java%20Agent%20功能全景分类表.md) - Java Agent technology overview and selection guide

### Module Documentation
- [MethodTime Module Documentation](mingsha-javaagent-methodtime/README.md) - Method execution time monitoring module

## Contributing

### Development Environment Setup

1. **Fork the project**
```bash
git clone https://github.com/chenlong220192/mingsha-javaagent.git
cd mingsha-javaagent
```

2. **Create a feature branch**
```bash
git checkout -b feature/your-feature-name
```

3. **Commit code**
```bash
git add .
git commit -m "feat: add your feature description"
git push origin feature/your-feature-name
```

### Code Standards

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use Checkstyle for code style verification
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) specification

### Testing Requirements

- New features must include unit tests
- Test coverage must be at least 80%
- All tests must pass

### Submitting Pull Request

1. Ensure code passes all checks
2. Update relevant documentation
3. Add test cases
4. Submit Pull Request with change description

## Support

### Issue Reporting

- **GitHub Issues**: [Submit Issue](https://github.com/chenlong220192/mingsha-javaagent/issues)
- **Email Support**: chenlong220192@gmail.com
- **Documentation Feedback**: Documentation improvement suggestions welcome

### Community Discussion

- **GitHub Discussions**: [Join Discussion](https://github.com/chenlong220192/mingsha-javaagent/discussions)

---

## License

This project is licensed under [MIT License](./LICENSE).

---

<div align="center">

**If this project is helpful to you, please give it a ⭐️**

</div>
