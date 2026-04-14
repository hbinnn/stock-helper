# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本代码仓库中工作时提供指导。

## 项目概述

**做T助手** - 一款辅助股票日内回转交易（"做T"）的应用，监控股票价格，在达到目标买卖价时提醒用户手动交易，并计算预期收益。

## 技术栈

- **平台**：Android (Kotlin)
- **UI框架**：Jetpack Compose
- **架构**：MVVM + 清洁架构
- **数据库**：Room
- **网络**：Retrofit + OkHttp
- **最低SDK**：24 (Android 7.0)
- **目标SDK**：34 (Android 14)

## 构建命令

```bash
# 构建Debug APK
./gradlew assembleDebug

# 构建Release APK（未签名）
./gradlew assembleRelease

# 运行测试
./gradlew test
```

**注意**：需要 Android SDK（位于 `~/Library/Android/sdk`）和 JDK 17。

## 构建环境配置

```bash
export ANDROID_HOME=~/Library/Android/sdk
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
/opt/homebrew/opt/gradle@8/bin/gradle assembleDebug
```

## 关键架构

- `domain/model/` - 数据模型（Stock、StockQuote、ProfitInfo）
- `data/local/` - Room数据库（StockDao、AppDatabase）
- `data/remote/` - 东方财富API行情集成
- `data/repository/` - 仓库模式实现
- `ui/screens/` - Compose UI页面（首页、添加股票、详情）
- `util/` - 通知助手、价格监控服务、开机广播接收器

## 已实现功能

### 核心功能
1. **自选股管理** - 添加/删除股票，设置目标价
2. **自动获取股票信息** - 输入股票代码，自动通过东方财富API获取名称和现价
3. **成本价设置** - 成本价默认为现价，可自行修改
4. **目标价格设置** - 支持固定价格和百分比两种模式设置买卖目标价
5. **实时行情监控** - 每15秒自动刷新股票行情
6. **价格提醒** - 股价达到目标买卖价时发送通知
7. **收益计算** - 计算预期收益和收益率

### API集成
- 通过东方财富（EastMoney）公共API获取实时行情
- 市场自动识别：6开头=上海(sh)，0/3开头=深圳(sz)
- API返回的涨跌幅(f170)已是百分比格式（如5.0表示5.00%）

## 数据库结构

**Stock实体类 (version 2)**：
```kotlin
data class Stock(
    val code: String,           // 股票代码
    val name: String,          // 股票名称
    val costPrice: Double,     // 成本价
    val targetBuyPrice: Double,  // 目标买入价
    val targetSellPrice: Double, // 目标卖出价
    val buyPriceType: String,    // PRICE 或 PERCENT
    val sellPriceType: String,   // PRICE 或 PERCENT
    val buyPercent: Double,     // 买入百分比
    val sellPercent: Double,    // 卖出百分比
    val shares: Int,           // 持仓数量
    val market: String         // sh 或 sz
)
```

## 已知问题

1. **模拟器中文输入** - 模拟器默认没有中文输入法，需要安装Google拼音输入法

## 待开发功能

1. 股票搜索功能 - 根据名称搜索股票
2. 止损提醒 - 价格下跌到一定幅度提醒
3. 历史收益记录 - 保存交易记录
4. 自选股排序 - 按代码、名称、收益率排序
5. 数据导出/导入
