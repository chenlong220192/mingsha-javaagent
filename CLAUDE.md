# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`mingsha-javaagent-methodtime` is a lightweight Java Agent for **nanosecond-precision method-level execution time profiling**. It uses ASM bytecode manipulation to inject timing code at class-load time, buffers data via `ArrayBlockingQueue`, persists to an embedded H2 database, and exposes a Telnet management interface. Package: `site.mingsha.javaagent.methodtime`.

## Build Commands

Use `make` at the project root (wraps `./mvnw`):

```bash
make help          # Show all targets
make package       # Build agent JAR + distribution (runs tests)
make test          # Run unit tests only
make check         # Run Checkstyle
make lint          # Alias for check
make verify        # mvn verify (unit + integration tests)
make install       # Install to local Maven repo
make ci            # checkstyle + test + clean package
SKIP_TEST=true make package  # Skip tests for fast build
```

Maven profiles:
- `-Pci` — CI build
- `-Pcoverage` — run tests with JaCoCo coverage report (`target/site/jacoco/index.html`)
- `-Pperformance` — run `*PerformanceTest.java` benchmarks

Single test: `mvn test -Dtest=ClassName#methodName -pl mingsha-javaagent-methodtime`

## Architecture

```
mingsha-javaagent-methodtime/src/main/java/site/mingsha/javaagent/methodtime/
├── enhancer/        # ClassFileTransformer entry; ASM bytecode injection at load-time
├── collector/      # Thread-safe ArrayBlockingQueue buffer; MethodTimeRecord data model
├── storage/        # H2 embedded DB; batch INSERT; slow-query table; CSV failover; compensation thread
├── telnet/         # TelnetServer; commands: agent status|reload|export, db stats|schema, raw SQL
├── monitor/        # CPU/memory monitoring; CPU fuse (pauses collection when CPU > threshold)
└── util/           # ThreadGuardian (auto-restarts dead threads), ShutdownManager, ExceptionHandler
```

**Data flow:** Business method entry/exit (ASM-injected `System.nanoTime()`) → `MethodTimeBuffer` (producer-consumer queue) → `H2Storage` batch write → H2 file.

**Key patterns:**
- **Singleton agent entry:** `MethodTimeAgent.premain()`/`agentmain()` initializes all subsystems
- **CPU fuse:** collection pauses when CPU > threshold, resumes when CPU drops below threshold − 10%
- **Failover:** DB write failure triggers local CSV backup; compensation thread replays on restart
- **Thread guardian:** monitors and auto-restarts storage, H2 cleaner, compensation, and Telnet threads

## Configuration

Default: `src/main/resources/agent-config.yml`. All values overridable via JVM system properties:

```bash
java -javaagent:target/...jar \
  -Dmingsha.agent.config.collector.packages="com.example.*" \
  -Dmingsha.agent.config.collector.samplingRate=0.5 \
  -Dmingsha.agent.config.storage.h2.path="./my_app_db" \
  -jar app.jar
```

Hot-reload: connect via `telnet localhost 5005` and run `agent reload`.

## Telnet Commands

```bash
agent status      # Overall agent health
agent reload      # Hot-reload config
agent export /tmp/out.csv  # Export collected data
agent errors      # View data-loss and error records
db stats          # Record counts per table
db schema         # Show all table DDL
# Raw SQL queries also supported (sandboxed)
```

## Code Style

Google Java Style Guide, enforced by Checkstyle (`config/checkstyle.xml`). Run `make check` before committing.

## Commit Convention

Conventional Commits: `feat()`, `fix()`, `docs()`, `refactor()`, `test()`, `chore()`.

## Testing

- Unit tests: `**/*Test.java`
- Integration tests: `**/*IT.java` (run with `mvn verify`)
- Performance tests: `**/*PerformanceTest.java` (run with `-Pperformance`)
- Coverage minimum: 80% line, 70% branch (JaCoCo)

Helper scripts in `mingsha-javaagent-methodtime/bin/`: `run-tests.sh`, `test-auto-db.sh`, `test-config-override.sh`, `test-telnet-commands.sh`.

## Documentation

All technical docs are in `mingsha-javaagent-methodtime/docs/` (Chinese):
- `00-快速开始.md` — 5-minute quickstart
- `01-架构设计.md` — Architecture design with ASCII diagrams
- `06-配置覆盖指南.md` — JVM property override reference
- `07-API接口.md` — Telnet command reference
- `11-开发指南.md` — Plugin interfaces, test examples, release process
