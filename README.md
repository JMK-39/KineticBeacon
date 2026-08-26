# KineticBeacon

[简体中文](#简体中文) | [English](#english)

## 简体中文

### 模组定位

**KineticBeacon** 是 Kinetic 系列的信标强化模块，主要用于把原版信标扩展为可配置的区域强加载与安全区域系统。

### 主要功能

- **信标区块强加载**：允许有效信标持续加载周围区块，让机器或区域逻辑在无人停留时继续运行。
- **按信标等级控制范围**：不同信标等级可以对应不同的强加载半径。
- **全局强加载上限**：限制全部维度中由信标产生的强加载区块总量。
- **玩家个人额度**：可启用每位玩家的独立信标强加载配额，避免单个玩家占满服务器额度。
- **离线超时处理**：玩家离线超过设定时间后，可自动停用其信标的强加载和/或防刷怪效果。
- **区域防刷怪**：可在信标有效范围内阻止敌对生物自然生成。
- **状态与额度同步**：客户端可以查看当前信标状态、额度与相关提示。
- **范围可视化**：提供信标范围渲染，方便服主和玩家确认实际覆盖区域。
- **Tooltip 扩展**：在信标相关界面显示更明确的强加载和安全区域信息。
- **Jade 兼容**：安装 Jade 后可显示信标扩展信息。
- **KubeJS 事件**：提供信标等级变化事件，便于脚本联动。

### 配置文件

```text
config/kineticcore/beacon.toml
```

主要包括：

- 强加载总开关与等级半径。
- 全局区块额度与玩家额度。
- 玩家离线超时规则。
- 离线后是否关闭强加载。
- 离线后是否关闭防刷怪。
- 信标区域防刷怪规则。

服务端规则通过 KineticCore 的服务端配置体系保存和同步。

### 运行环境

- Minecraft 1.20.1
- Minecraft Forge 47.x
- Java 17
- KineticCore：必须
- Jade：可选

## English

### Overview

**KineticBeacon** extends vanilla beacons into configurable chunk-loading and protected-area controllers.

### Key Features

- Beacon-powered chunk loading.
- Loading radius based on beacon level.
- Global loaded-chunk quota.
- Optional per-player quotas.
- Offline timeout and automatic deactivation rules.
- Hostile natural-spawn prevention inside beacon areas.
- Client status/quota synchronization.
- Beacon range visualization and enhanced tooltips.
- Optional Jade integration.
- Beacon level-change event support for scripting integrations.

### Configuration

```text
config/kineticcore/beacon.toml
```

Server-side beacon rules are persisted by the server through the KineticCore configuration system.

### Requirements

- Minecraft 1.20.1
- Minecraft Forge 47.x
- Java 17
- KineticCore: required
- Jade: optional


## 开源协议与版权 (License)

Copyright (C) 2024-2026 XYAT.

本项目基于 **GNU Lesser General Public License v3.0 (LGPLv3)** 协议开源。

This project is open-sourced under the **GNU Lesser General Public License v3.0 (LGPLv3)**.
