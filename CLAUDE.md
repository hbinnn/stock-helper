# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本代码仓库中工作时提供指导。

## 项目概述

**大富翁助手** - 一款辅助股票日内回转交易（"做T"）的应用，监控股票价格，在达到目标买卖价时提醒用户手动交易，并计算预期收益。

> **注意**：项目重构后，"做T助手"作为"大富翁助手"的一个功能模块，后续将扩展更多功能。

---

## 项目结构

```
大富翁助手/
├── app/
│   └── src/main/
│       ├── java/com/example/stockhelper/
│       │   ├── MainActivity.kt           # 主Activity，含底部Tab导航
│       │   ├── RichHelperApp.kt          # Application类
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── AppDatabase.kt   # Room数据库
│       │   │   │   └── StockDao.kt     # 股票DAO
│       │   │   ├── remote/
│       │   │   │   ├── EastMoneyApi.kt  # 东方财富API接口
│       │   │   │   └── NetworkModule.kt # 网络模块配置
│       │   │   └── repository/
│       │   │       ├── StockRepository.kt      # 仓库接口
│       │   │       └── StockRepositoryImpl.kt  # 仓库实现
│       │   ├── domain/model/
│       │   │   ├── Stock.kt           # 股票实体
│       │   │   ├── StockQuote.kt      # 行情数据
│       │   │   ├── ProfitInfo.kt      # 收益信息
│       │   │   └── AlertState.kt      # 提醒状态实体
│       │   ├── ui/
│       │   │   ├── Navigation.kt      # 导航配置
│       │   │   ├── screens/
│       │   │   │   ├── home/          # 首页（做T助手列表）
│       │   │   │   ├── add/           # 添加股票
│       │   │   │   ├── edit/          # 编辑股票
│       │   │   │   ├── detail/        # 股票详情
│       │   │   │   └── history/       # 历史记录
│       │   │   └── theme/             # 主题配置
│       │   └── util/
│       │       ├── NotificationHelper.kt    # 通知助手
│       │       ├── PriceMonitorService.kt  # 价格监控服务（Foreground Service）
│       │       ├── BootReceiver.kt        # 开机广播接收器
│       │       ├── ServiceManager.kt       # 服务启动管理
│       │       └── AlertStateResetWorker.kt # 提醒状态重置Worker
│       └── res/
│           ├── values/
│           │   ├── strings.xml       # 字符串资源
│           │   └── themes.xml         # 主题配置
│           └── xml/
│               └── network_security_config.xml  # 网络安全配置
│           └── ...
│
├── settings.gradle.kts
├── build.gradle.kts
└── ...
```

---

## 技术栈

- **平台**：Android (Kotlin)
- **UI框架**：Jetpack Compose
- **架构**：MVVM + 清洁架构
- **数据库**：Room
- **网络**：Retrofit + OkHttp + Gson
- **最低SDK**：24 (Android 7.0)
- **目标SDK**：34 (Android 14)

---

## 环境搭建

### 1. 安装JDK 17

```bash
# macOS使用Homebrew安装
brew install openjdk@17

# 配置环境变量（添加到 ~/.zshrc 或 ~/.bashrc）
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
```

### 2. 安装Android SDK

```bash
# 下载Android command line tools
cd ~/Library/Android/sdk
mkdir -p cmdline-tools
cd cmdline-tools
curl -L -o commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip
unzip -o commandlinetools.zip
mv cmdline-tools latest

# 配置环境变量
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 接受许可协议
yes | sdkmanager --licenses

# 安装必要的组件
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

### 3. 安装Gradle（可选）

如果不想使用项目自带的gradle wrapper：

```bash
brew install gradle@8
```

### 4. 克隆项目

```bash
git clone git@github.com:hbinnn/stock-helper.git
cd stock-helper
```

---

## 构建命令

### Debug构建

```bash
export ANDROID_HOME=~/Library/Android/sdk
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

# 使用项目自带的gradle wrapper
./gradlew assembleDebug

# 或使用系统gradle
/opt/homebrew/opt/gradle@8/bin/gradle assembleDebug
```

### Release构建

```bash
./gradlew assembleRelease
```

### 清理构建

```bash
./gradlew clean
```

---

## 模拟器运行

### 1. 安装模拟器组件

```bash
export ANDROID_HOME=~/Library/Android/sdk
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

# 安装模拟器和系统镜像
sdkmanager "emulator" "system-images;android-34;default;arm64-v8a"
```

### 2. 创建模拟器

```bash
# 使用avdmanager创建
echo "y" | avdmanager create avd -n "StockHelperARM" -k "system-images;android-34;default;arm64-v8a" -d "pixel_5"
```

### 3. 启动模拟器

```bash
# 启动带窗口的模拟器
export ANDROID_HOME=~/Library/Android/sdk
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
$ANDROID_HOME/emulator/emulator -avd StockHelperARM -gpu swiftshader_indirect -scale 0.75

# 或启动无窗口模式（后台运行）
$ANDROID_HOME/emulator/emulator -avd StockHelperARM -no-window -no-audio -no-boot-anim &
```

### 4. 安装APK到模拟器

```bash
export ANDROID_HOME=~/Library/Android/sdk

# 查看已连接的设备
adb devices

# 安装APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 如果已安装过，使用-r参数重新安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n com.example.stockhelper/.MainActivity

# 停止应用
adb shell am force-stop com.example.stockhelper
```

### 5. 查看日志

```bash
# 实时查看日志
adb logcat | grep -i "stock"

# 或指定标签过滤
adb logcat -s StockHelper

# 清除日志
adb logcat -c
```

---

## 真机调试

### 1. 手机开启开发者模式

- 设置 → 关于手机 → 连续点击"版本号"7次开启开发者模式
- 设置 → 开发者选项 → 开启"USB调试"

### 2. 连接手机

```bash
# 查看设备
adb devices

# 安装APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. 小米/红米手机特殊设置

如果应用安装后提示"packageinfo is null"或无法安装：
- 设置 → 隐私 → 保护 → 特殊权限 → 安装未知应用 → 开启对应应用的权限
- 设置 → 开发者选项 → 关闭"MIUI优化"（可能需要重启）

---

## 导航结构

### 底部Tab导航

```
┌─────────────────────────────────────┐
│           大富翁助手                   │
├─────────────────────────────────────┤
│                                     │
│         功能内容区                     │
│                                     │
├─────────────────────────────────────┤
│  [做T助手]        [更多]            │
└─────────────────────────────────────┘
```

- **做T助手**：嵌套NavHost，包含首页、添加股票、详情页
- **更多**：预留扩展，后续添加更多功能模块

### 页面路由

| 路由 | 说明 |
|------|------|
| `stock_helper` | 做T助手Tab（外部Tab） |
| `more` | 更多Tab（外部Tab） |
| `home` | 做T助手首页 |
| `add_stock` | 添加股票 |
| `detail/{stockCode}` | 股票详情页 |
| `edit_stock/{stockCode}` | 编辑股票 |

---

## 数据库结构

**Stock实体类 (version 5)**：

```kotlin
@Entity(tableName = "stocks")
data class Stock(
    @PrimaryKey
    val code: String,                // 股票代码
    val name: String,                // 股票名称
    val costPrice: Double,           // 成本价
    val targetBuyPrice: Double,       // 目标买入价
    val targetSellPrice: Double,      // 目标卖出价
    val buyPriceType: String,         // PRICE=固定价格, PERCENT=百分比
    val sellPriceType: String,       // PRICE=固定价格, PERCENT=百分比
    val buyPercent: Double,           // 百分比时使用
    val sellPercent: Double,         // 百分比时使用
    val shares: Int,                 // 持仓数量（股数）
    val market: String,               // sh=上海, sz=深圳
    val tradeType: String,           // BUY_FIRST=先买后卖, SELL_FIRST=先卖后买
    val tTradeType: String,          // SHARES=固定股数, PERCENT=百分比
    val tShares: Int,               // 固定做T股数
    val tSharesPercent: Int          // 做T比例（25=1/4, 33=1/3等）
)
```

**AlertState实体类 (version 5)**：

```kotlin
@Entity(tableName = "alert_states")
data class AlertState(
    @PrimaryKey
    val stockCode: String,              // 股票代码
    val buyAlertSent: Boolean = false,  // 买入提醒是否已发送
    val sellAlertSent: Boolean = false, // 卖出提醒是否已发送
    val firstConditionReached: Boolean = false,  // 第一步条件是否达成
    val firstConditionReachedAt: Long? = null,   // 第一步达成时间
    val lastUpdated: Long = System.currentTimeMillis()
)
```

**TradeRecord实体类 (version 6)**：

```kotlin
@Entity(tableName = "trade_records")
data class TradeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stockCode: String,
    val stockName: String,
    val recordType: String,        // BUY_ALERT / SELL_ALERT
    val currentPrice: Double,       // 触发时价格
    val targetPrice: Double,        // 目标价格
    val tradeType: String,         // BUY_FIRST / SELL_FIRST
    val tShares: Int,              // 做T股数
    val triggeredAt: Long = System.currentTimeMillis(),
    val isHandled: Boolean = false // 用户是否已处理
)
```

---

## 已实现功能

### 核心功能
1. **自选股管理** - 添加/删除股票，设置目标价
2. **自动获取股票信息** - 输入股票代码，自动获取名称和现价
3. **成本价设置** - 成本价默认为现价，可自行修改
4. **目标价格设置** - 支持固定价格和百分比两种模式
5. **实时行情监控** - 每15秒自动刷新
6. **价格提醒** - 到达目标价时推送通知
7. **收益计算** - 计算预期收益和收益率（按做T数量）

### 交易模式
8. **交易模式选择** - 先买后卖 / 先卖后买
   - 先买后卖：必须先达到买入价，才会提醒卖出
   - 先卖后买：必须先达到卖出价，才会提醒买入

### 做T数量设置
9. **做T数量设置** - 支持比例和固定股数两种方式
   - 比例：1/4仓(25%)、1/3仓(33%)、1/2仓(50%)、全仓(100%)
   - 固定股数：用户输入具体股数
   - 验证：先卖后买时，做T股数不能超过持仓数量
   - 收益计算：按做T数量而非全部持仓计算

### 股票编辑
10. **编辑股票功能** - 详情页点击编辑按钮进入编辑页面
    - 复用添加股票页面，编辑模式下隐藏股票代码输入
    - 加载现有数据填充表单
    - 支持修改成本价、目标价、交易模式、做T数量等

### 历史记录
11. **历史记录功能** - 记录每次提醒操作
    - 触发提醒时自动保存到数据库
    - 入口：股票详情页 → 做T历史卡片
    - 显示最近5条记录，包含买入/卖出类型、时间、触发价、目标价、做T数量

---

## 待实现功能

### 中优先级
（已实现）价格监控服务完善 - Foreground Service 后台监控
（已实现）后台刷新机制 - 15秒定时刷新
（已实现）开机启动恢复 - BootReceiver 自动启动服务

### 低优先级/优化项
1. **提醒状态重置** - 第二天自动重置提醒状态（每天9点重置）
2. **声音/震动自定义** - 允许用户自定义提醒方式
3. **数据导出/备份** - 导出/导入股票配置

---

## 东方财富API字段说明

实时行情API：`push2.eastmoney.com/api/qt/stock/get`

| 字段 | 说明 |
|------|------|
| f43 | 当前价格（分，需除100） |
| f44 | 最高价（分） |
| f45 | 最低价（分） |
| f46 | 今开（分） |
| f47 | 成交量 |
| f57 | 股票代码 |
| f58 | 股票名称 |
| f60 | 昨收（分，需除100） |
| f169 | 涨跌额 |
| f170 | 涨跌幅（%，需除100） |

## 腾讯财经API

备用行情API：`http://qt.gtimg.cn/q=sh600519`

返回格式：`v_sh600519="1~名称~代码~现价~昨收~今开~成交量~...~最高~最低~...~涨跌额~涨跌幅";`

使用说明：
- 优先使用东方财富API，失败时自动切换到腾讯财经API
- AndroidManifest需要配置networkSecurityConfig允许HTTP明文流量
- 腾讯API返回数据末尾有分号，正则表达式需注意

---

## Git使用

```bash
# 克隆仓库
git clone git@github.com:hbinnn/stock-helper.git

# 创建新分支开发功能
git checkout -b feature/new-feature

# 提交代码
git add .
git commit -m "描述"

# 推送到远程
git push -u origin feature/new-feature

# 合并到main分支
git checkout main
git merge feature/new-feature
git push
```

---

## 常见问题

### Q: 模拟器启动失败？
A: 确保已安装正确架构的系统镜像（Apple Silicon Mac需要arm64-v8a镜像）

### Q: 构建失败提示SDK找不到？
A: 确保ANDROID_HOME环境变量正确设置，指向Android SDK目录

### Q: 真机安装提示"packageinfo is null"？
A: 小米手机需要在设置中开启"安装未知应用"权限，并关闭"MIUI优化"

### Q: 无法连接GitHub？
A: 检查SSH密钥配置：ls -la ~/.ssh，确保有github.com相关的密钥配置

### Q: 昨收价格显示为0？
A: 检查东方财富API字段，f47是成交量，f60才是昨收

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | - | 初始版本 |
| 2 | - | 添加交易模式相关字段 |
| 3 | - | 添加提醒相关逻辑 |
| 4 | 2026-04-16 | 添加做T数量设置（tradeType, tTradeType, tShares, tSharesPercent） |
| 5 | 2026-04-16 | 添加编辑股票功能，支持在详情页修改股票设置 |
| 6 | 2026-04-16 | 完善价格监控服务：Foreground Service后台监控、提醒状态持久化、开机自启、每日重置 |
| 7 | 2026-04-16 | 添加腾讯财经API作为备用，双API保障更稳定 |
| 8 | 2026-04-16 | 添加历史记录功能，记录每次提醒操作 |

---

## 开发注意事项

1. **shares字段**：直接存储股数（如1000股就存1000），显示时不需乘100
2. **提醒状态**：触发后保存在 AlertState 表中，删除股票时自动清理
3. **交易模式**：影响提醒的触发逻辑，先买后卖需先达成买入价
4. **做T数量**：预估收益按做T数量计算，非全部持仓
5. **后台监控**：PriceMonitorService 使用 Foreground Service，App退后台后继续监控
6. **开机自启**：BootReceiver 在设备启动后自动启动 PriceMonitorService
7. **每日重置**：WorkManager 每天9点自动重置所有提醒状态
