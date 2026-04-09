# 升级文档 (2026.04.09)

## 版本信息
- **版本号**: 0.0.1-SNAPSHOT
- **升级日期**: 2026-04-09
- **升级内容**: 依赖版本升级到最新

## 升级的依赖

### 字节码操作
| 依赖项 | 旧版本 | 新版本 | 说明 |
|--------|--------|--------|------|
| ASM | 9.7 | 9.8.1 | Java 字节码操作库 |

### YAML 处理
| 依赖项 | 旧版本 | 新版本 | 说明 |
|--------|--------|--------|------|
| SnakeYAML | 2.2 | 2.3 | YAML 解析库，安全修复 |

### Maven 插件
| 依赖项 | 旧版本 | 新版本 | 说明 |
|--------|--------|--------|------|
| maven-jar-plugin | 3.4.0 | 3.4.1 | JAR 打包 |
| maven-javadoc-plugin | 3.10.1 | 3.11.2 | Javadoc 生成 |
| maven-surefire-plugin | 3.5.0 | 3.5.2 | 单元测试 |
| maven-failsafe-plugin | 3.5.0 | 3.5.2 | 集成测试 |

### 测试相关
| 依赖项 | 旧版本 | 新版本 | 说明 |
|--------|--------|--------|------|
| Awaitility | 4.2.1 | 4.2.2 | 异步测试 |

## 升级说明

### 为什么升级
1. **安全修复**: SnakeYAML 2.3 包含安全漏洞修复
2. **性能优化**: ASM 9.8.1 提升字节码处理性能
3. **稳定性**: Maven 插件更新提高构建稳定性

### 测试验证
- [ ] 项目编译通过
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 性能测试通过

## 升级步骤

```bash
# 1. 更新代码
git pull origin develop

# 2. 清理并编译
mvn clean compile -DskipTests

# 3. 运行测试（包括集成测试）
mvn verify
```

## 注意事项

1. 确保使用 JDK 8 或更高版本
2. ASM 9.8.1 需要验证与现有字节码增强代码的兼容性

## 回滚方案

如需回滚，执行以下命令：

```bash
git revert <commit-hash>
```

## 相关链接

- [GitHub 仓库](https://github.com/chenlong220192/mingsha-javaagent)
- [ASM 官方文档](https://asm.ow2.io/)
- [问题反馈](https://github.com/chenlong220192/mingsha-javaagent/issues)
