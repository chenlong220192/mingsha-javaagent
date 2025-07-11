#======================================================================
#
# author: mingsha
# date: 2025-07-11
#
# 本 Makefile 用于统一管理 mingsha-javaagent 项目的构建、测试、打包、部署等任务。
# 支持多环境、跳过测试、子模块扩展、彩色输出和 emoji 提示。
#======================================================================

.DEFAULT_GOAL := help

# 指定 shell 类型，pipefail 保证管道出错时立即失败
SHELL := /bin/bash -o pipefail

# 获取当前 Makefile 所在绝对路径，便于后续路径引用
export BASE_PATH := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

# ----------------------------- emoji & colors <-----------------------------
# 定义常用 emoji 和颜色变量，用于美化输出
ROCKET := 🚀
GEAR := ⚙️
TEST := 🧪
PACKAGE := 📦
DOCKER := 🐳
HELM := 🎯
CLEAN := 🧹
HELP := ❓
INFO := ℹ️
SUCCESS := ✅
WARNING := ⚠️
ERROR := ❌
RED=\033[31m
GREEN=\033[32m
YELLOW=\033[33m
BLUE=\033[34m
CYAN=\033[36m
BOLD=\033[1m
RESET=\033[0m
# ----------------------------- emoji & colors >-----------------------------

# ----------------------------- variables <-----------------------------
# SKIP_TEST: 是否跳过测试，true/false，默认false
SKIP_TEST ?= false
# ----------------------------- variables >-----------------------------

# ----------------------------- include <-----------------------------
# 支持后续拆分子模块 Makefile，自动包含 Makefile.* 文件
-include Makefile.*
# ----------------------------- include >-----------------------------

# ----------------------------- help <-----------------------------
.PHONY: help
# help: 显示所有可用目标、环境变量、用法示例，支持彩色和 emoji 美化
help: ## $(HELP) 显示帮助信息
	@printf "${BOLD}${CYAN}╔══════════════════════════════════════════════════════════════╗${RESET}\n"
	@printf "${BOLD}${CYAN}║                ${ROCKET} mingsha-javaagent 构建工具 ${ROCKET}              ║${RESET}\n"
	@printf "${BOLD}${CYAN}╚══════════════════════════════════════════════════════════════╝${RESET}\n"
	@printf "\n"
	@printf "${BOLD}${YELLOW}%-8s:${RESET}\n" "使用方法"
	@printf "  make <target> [SKIP_TEST=true|false]\n"
	@printf "\n"
	@printf "${BOLD}${YELLOW}%-8s:${RESET}\n" "环境变量"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "SKIP_TEST" "- 跳过测试 (默认: false, 可选: true)"
	@printf "\n"
	@printf "${BOLD}${YELLOW}%-8s:${RESET}\n" "可用目标"
	@awk 'BEGIN {FS = ":.*?## "; max=22} /^[a-zA-Z0-9_.-]+:.*?## / {cmd=$$1; desc=$$2; icon=""; if(cmd=="help"){icon="$(HELP)"} else if(cmd=="clean"){icon="$(CLEAN)"} else if(cmd=="package"||cmd=="install"){icon="$(PACKAGE)"} else if(cmd=="test"){icon="$(TEST)"} else if(cmd=="check"){icon="$(INFO)"} else if(cmd=="lint"){icon="$(WARNING)"} else if(cmd=="verify"){icon="$(GEAR)"} else if(cmd=="ci"){icon="$(SUCCESS)"} else{icon="$(INFO)"}; printf "  ${GREEN}%-*s${RESET} %s %s\n", max, cmd, icon, desc}' max=22 $(MAKEFILE_LIST)
	@printf "\n"
	@printf "${BOLD}${YELLOW}%-8s:${RESET}\n" "示例"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make help" "${HELP} 显示此帮助信息"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make clean" "${CLEAN} 清理构建文件"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make package" "${PACKAGE} 构建安装包"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make test" "${TEST} 运行单元测试"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make check" "${INFO} 代码静态检查（Checkstyle）"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make format" "${INFO} 自动格式化代码（需本地IDE支持）"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make lint" "${WARNING} 代码风格检查（Checkstyle）"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make verify" "${GEAR} 验证构建与测试"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make install" "${PACKAGE} 安装到本地Maven仓库"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make ci" "${SUCCESS} CI流程：检查+测试+打包"
	@printf "\n"
	@printf "${BOLD}${CYAN}╔══════════════════════════════════════════════════════════════╗${RESET}\n"
	@printf "${BOLD}${CYAN}║                       ${SUCCESS} 构建愉快！${SUCCESS}                        ║${RESET}\n"
	@printf "${BOLD}${CYAN}╚══════════════════════════════════════════════════════════════╝${RESET}\n"
# ----------------------------- help >-----------------------------

# all: 默认目标，等价于 package
all: package ## ${PACKAGE} 构建安装包

# clean: 清理构建产物
clean: ## ${CLEAN} 清理构建文件
	@printf "${CLEAN} ${YELLOW}清理构建产物...${RESET}\n"
	$(BASE_PATH)/mvnw --batch-mode --errors --fail-at-end --update-snapshots -f ${BASE_PATH}/pom.xml clean

# package: 构建 jar 包
package: ## ${PACKAGE} 构建安装包
	@printf "${PACKAGE} ${GREEN}开始打包...${RESET}\n"
	@if [ "$(SKIP_TEST)" = "true" ]; then \
		$(BASE_PATH)/mvnw --batch-mode --errors --fail-at-end --update-snapshots -f ${BASE_PATH}/pom.xml clean package -D maven.test.skip=true; \
	else \
		$(BASE_PATH)/mvnw --batch-mode --errors --fail-at-end --update-snapshots -f ${BASE_PATH}/pom.xml clean package; \
	fi

# test: 运行单元测试
test: ## ${TEST} 运行单元测试
	@printf "${TEST} ${BLUE}运行单元测试...${RESET}\n"
	$(BASE_PATH)/mvnw --batch-mode --errors --fail-at-end --update-snapshots -f ${BASE_PATH}/pom.xml test

# check: 代码静态检查（Checkstyle）
check: ## ${INFO} 代码静态检查（Checkstyle）
	@printf "${INFO} ${CYAN}执行Checkstyle检查...${RESET}\n"
	$(BASE_PATH)/mvnw --batch-mode --errors --fail-at-end --update-snapshots -f ${BASE_PATH}/pom.xml checkstyle:check

# format: 自动格式化代码（需本地IDE支持）
format: ## ${INFO} 自动格式化代码（需本地IDE支持）
	@printf "${INFO} ${CYAN}请在IDE中统一格式化代码，或使用IDE插件。${RESET}\n"

# lint: 代码风格检查（Checkstyle）
lint: check ## ${WARNING} 代码风格检查（Checkstyle）

# verify: 验证构建与测试
verify: ## ${GEAR} 验证构建与测试
	@printf "${GEAR} ${CYAN}执行mvn verify...${RESET}\n"
	$(BASE_PATH)/mvnw --batch-mode --errors --fail-at-end --update-snapshots -f ${BASE_PATH}/pom.xml verify

# install: 安装到本地Maven仓库
install: ## ${PACKAGE} 安装到本地Maven仓库
	@printf "${PACKAGE} ${CYAN}安装到本地Maven仓库...${RESET}\n"
	$(BASE_PATH)/mvnw --batch-mode --errors --fail-at-end --update-snapshots -f ${BASE_PATH}/pom.xml install

# ci: CI流程，包含检查、测试、打包
ci: ## ${SUCCESS} CI流程：检查+测试+打包
	@printf "${SUCCESS} ${CYAN}执行CI流程...${RESET}\n"
	$(BASE_PATH)/mvnw --batch-mode --errors --fail-at-end --update-snapshots -f ${BASE_PATH}/pom.xml checkstyle:check
	$(BASE_PATH)/mvnw --batch-mode --errors --fail-at-end --update-snapshots -f ${BASE_PATH}/pom.xml test
	$(BASE_PATH)/mvnw --batch-mode --errors --fail-at-end --update-snapshots -f ${BASE_PATH}/pom.xml clean package -D maven.test.skip=true
