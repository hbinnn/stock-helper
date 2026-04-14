# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本代码仓库中工作时提供指导。

## 项目概述

**做T助手** - 一款辅助股票日内回转交易（"做T"）的应用，监控股票价格，在达到目标买卖价时提醒用户手动交易，并计算预期收益。

## 环境搭建

### 1. 安装JDK 17

```bash
# macOS使用Homebrew安装
brew install openjdk@17

# 配置环境变量（添加到 ~/.zshrc 或 ~/.bashrc）
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
```

### 2. 安装Android SDK

Android SDK通常随Android Studio一起安装。如果没有单独安装：

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

## 技术栈

- **平台**：Android (Kotlin)
- **UI框架**：Jetpack Compose
- **架构**：MVVM + 清洁架构
- **数据库**：Room
- **网络**：Retrofit + OkHttp
- **最低SDK**：24 (Android 7.0)
- **目标SDK**：34 (Android 14)

## 关键架构

- `domain/model/` - 数据模型（Stock、StockQuote、ProfitInfo）
- `data/local/` - Room数据库（StockDao、AppDatabase）
- `data/remote/` - 东方财富API行情集成
- `data/repository/` - 仓库模式实现
- `ui/screens/` - Compose UI页面（首页、添加股票、详情）
- `util/` - 通知助手、价格监控服务、开机广播接收器

## 已实现功能

1. **自选股管理** - 添加/删除股票，设置目标价
2. **自动获取股票信息** - 输入股票代码，自动获取名称和现价
3. **成本价设置** - 成本价默认为现价，可自行修改
4. **目标价格设置** - 支持固定价格和百分比两种模式
5. **实时行情监控** - 每15秒自动刷新
6. **价格提醒** - 到达目标价时推送通知
7. **收益计算** - 计算预期收益和收益率

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

## 常见问题

### Q: 模拟器启动失败？
A: 确保已安装正确架构的系统镜像（Apple Silicon Mac需要arm64-v8a镜像）

### Q: 构建失败提示SDK找不到？
A: 确保ANDROID_HOME环境变量正确设置，指向Android SDK目录

### Q: 真机安装提示"packageinfo is null"？
A: 小米手机需要在设置中开启"安装未知应用"权限，并关闭"MIUI优化"

### Q: 无法连接GitHub？
A: 检查SSH密钥配置：ls -la ~/.ssh，确保有github.com相关的密钥配置
