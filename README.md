# 🌾 FarmAura - 自动农场助手

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric-0.15.2-blue)](https://fabricmc.net)
[![Meteor](https://img.shields.io/badge/Meteor-0.5.4-orange)](https://meteorclient.com)

---

## 📖 简介

FarmAura 是一个基于 **Meteor Client** 的附加组件（Addon），为 Minecraft 1.20.1 提供**自动锄地、自动种植、自动除草**功能。

它模拟玩家手动操作，支持范围控制、延迟调节和循环模式，适合在各类服务器（包括无政府服务器）中使用。

---

## ✨ 功能特性

| 功能 | 说明 |
|------|------|
| 🌱 **自动锄地** | 在指定范围内自动锄地，将泥土/草方块变成耕地 |
| 🌿 **自动种植** | 在耕地上自动种植作物（支持小麦、胡萝卜、马铃薯等） |
| 🗑️ **自动除草** | 自动清除范围内的杂草、花、高草丛等 |
| 🔄 **循环模式** | 持续循环执行，无需手动重启 |
| 🎯 **范围控制** | 可调节工作半径（1-15 格） |
| ⏱️ **延迟控制** | 调节操作间隔，防止被服务器踢出 |
| 🗣️ **聊天反馈** | 可选的聊天栏状态提示 |
| 🌐 **汉化支持** | 设置界面完全中文化 |

### 支持的作物

| 作物 | 英文名 | 需要种子 |
|------|--------|----------|
| 小麦 | Wheat | 小麦种子 |
| 胡萝卜 | Carrot | 胡萝卜 |
| 马铃薯 | Potato | 马铃薯 |
| 甜菜根 | Beetroot | 甜菜根种子 |
| 西瓜 | Melon | 西瓜种子 |
| 南瓜 | Pumpkin | 南瓜种子 |
| 地狱疣 | Nether Wart | 地狱疣 |

---

## 📥 安装方法

### 前置要求

| 前置模组 | 版本 |
|----------|------|
| Fabric Loader | 0.15.2+ |
| Fabric API | 0.91.0+ |
| Meteor Client | 0.5.4+ |

### 安装步骤

1. **下载模组文件**：从 [Releases](https://github.com/yourusername/farm-aura/releases) 页面下载最新 `.jar` 文件
2. **放入 mods 文件夹**：将 `.jar` 文件放入 `.minecraft/mods/` 目录
3. **启动游戏**：确保 Fabric 和 Meteor Client 已正确加载
4. **打开模块**：按 `右 Ctrl` 打开 Meteor 菜单 → `战斗` 分类 → 找到 `自动农场` 并开启

---

## 🎮 使用指南

### 基本操作

1. **主手**：装备**锄头**（任何类型）
2. **副手**：装备**种子**（对应要种植的作物）
3. **开启模块**：在 Meteor 菜单中打开 `自动农场`
4. **调整设置**：配置范围、延迟、循环模式等

### 设置说明

| 设置项 | 说明 | 推荐值 |
|--------|------|--------|
| **工作半径** | 以玩家为中心的工作范围 | 3-5 |
| **操作延迟** | 每次操作的间隔（毫秒） | 50-150 |
| **循环模式** | 开启后持续工作 | 开启 |
| **启用锄地** | 是否自动锄地 | 开启 |
| **启用种植** | 是否自动种植 | 开启 |
| **启用除草** | 是否自动除草 | 开启 |
| **作物类型** | 要种植的作物 | 按需选择 |
| **聊天反馈** | 显示操作状态 | 按需开启 |

### 延迟设置建议

| 服务器类型 | 建议延迟 |
|------------|----------|
| 无政府服务器 | 10-30ms |
| 普通生存服务器 | 50-80ms |
| 有反作弊服务器 | 100-150ms |

### 快捷键

| 按键 | 功能 |
|------|------|
| 点击模块名称 | 打开设置界面 |
| 模块开关 | 开启/关闭自动农场 |

---

## 🔧 开发者指南

### 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 17+ |
| Gradle | 8.8+ |
| IntelliJ IDEA | 2024.1+ |

### 克隆项目

```bash
git clone https://github.com/yourusername/farm-aura.git
cd farm-aura
```

### IDEA 设置

1. **打开项目**：File → Open → 选择项目根目录
2. **设置 JDK**：File → Project Structure → SDK → 选择 JDK 17
3. **刷新 Gradle**：点击右侧 Gradle 面板的刷新按钮

### 编译命令

```bash
# Windows
gradlew.bat build

# Mac / Linux
./gradlew build
```

### 调试方法

1. 在 IDEA 中选择 `Minecraft Client` 运行配置
2. 点击 Debug（小虫子图标）启动游戏
3. 修改 `FarmAura.java` 中的代码
4. 按 `Ctrl + Shift + F9` 热交换代码（无需重启）

### 项目结构

```
farm-aura/
├── src/
│   └── main/
│       ├── java/
│       │   └── meteordevelopment/
│       │       └── meteorclient/
│       │           └── addons/
│       │               └── farm/
│       │                   ├── FarmAddon.java    # 入口类
│       │                   └── FarmAura.java     # 核心逻辑
│       └── resources/
│           ├── fabric.mod.json                   # 模组描述
│           └── assets/
│               └── template/
│                   └── lang/
│                       ├── en_us.json            # 英文翻译
│                       └── zh_cn.json            # 中文翻译
├── libs/
│   └── meteor-client-0.5.4-1.20.1.jar           # 依赖库
├── build.gradle.kts                              # 构建脚本
├── gradle.properties                             # Gradle 配置
└── README.md                                     # 本文件
```

---

## 🐛 常见问题

### Q: 模块没有显示在 Meteor 菜单中？

**A**: 检查以下几点：
1. 确保 `meteor-client-0.5.4-1.20.1.jar` 在 `mods` 文件夹中
2. 检查 Minecraft 版本是否为 1.20.1
3. 查看游戏日志是否有报错信息

### Q: 锄地/种植没有反应？

**A**: 确认：
1. 主手装备了锄头
2. 副手装备了对应种子
3. 范围内有可操作的方块
4. 聊天反馈开启后查看提示信息

### Q: 游戏崩溃？

**A**:
1. 尝试关闭 `聊天反馈`
2. 检查是否与其他模组冲突
3. 查看崩溃报告定位问题

### Q: 服务器没有反应？

**A**:
1. 增加 `操作延迟`（推荐 150ms+）
2. 减小 `工作半径`
3. 确保服务器没有禁用右键操作

### Q: 如何汉化？

**A**: 默认已支持中文。如果界面仍显示英文：
1. 确保游戏语言设置为 `简体中文`
2. 检查 `assets/template/lang/zh_cn.json` 文件是否存在

---

## 🛠️ 自定义开发

### 添加新作物

在 `FarmAura.java` 中：

1. 在 `CropType` 枚举中添加新作物：
```java
public enum CropType {
    Wheat, Carrot, Potato, Beetroot, Melon, Pumpkin, NetherWart,
    // 添加新作物
    Cocoa, Bamboo
}
```

2. 在 `getSeedItem()` 方法中添加对应种子：
```java
case Cocoa -> Items.COCOA_BEANS;
case Bamboo -> Items.BAMBOO;
```

3. 在语言文件中添加翻译：
```json
"meteor-client.crop-type.cocoa": "可可豆",
"meteor-client.crop-type.bamboo": "竹子"
```

### 添加新功能

1. 在 `FarmAura.java` 中添加新的 `Setting`
2. 在构造函数中注册设置
3. 在 `processPosition()` 或相应方法中添加逻辑
4. 在语言文件中添加翻译

---

## 📝 更新日志

### v1.0.0 (2026-06-21)

- ✨ 初始发布
- 🌱 支持范围锄地、种植、除草
- 🔄 支持循环/单次模式
- 🎯 可调节范围和延迟
- 🌐 完整中文汉化
- 🗣️ 可选聊天反馈

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的改动 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

---

## 📄 许可证

本项目基于 MIT 许可证开源。

---

## 🙏 致谢

- [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) - 提供强大的客户端框架
- [Fabric](https://fabricmc.net/) - 模组加载器
- 所有测试和反馈的玩家

---

## 📞 联系方式

- GitHub Issues: [提交问题](https://github.com/yourusername/farm-aura/issues)
- 作者: Miao_Miao_karin

---

**Enjoy farming! 🌾**
