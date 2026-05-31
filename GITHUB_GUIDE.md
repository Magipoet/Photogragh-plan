# 推送到 GitHub 并自动构建指南

本项目已配置好 GitHub Actions，推送到 GitHub 后会**自动构建 APK**，无需本地配置 Android 开发环境！

---

## 📋 准备工作

### 1. 拥有 GitHub 账号
- 如果没有，先去 https://github.com 注册一个

### 2. 创建新仓库
1. 登录 GitHub → 点击右上角 **+** → **New repository**
2. 仓库名：比如 `photo-plan`（可以自定义）
3. 选择 **Public** 或 **Private** 都可以
4. **不要勾选** `Initialize this repository with a README`
5. 点击 **Create repository**

---

## 🚀 第一步：初始化 Git 并推送到 GitHub

在你的本地电脑上操作：

```bash
# 进入项目目录
cd /remote-home/share/lijl/task_all/14_Photogragh-plan

# 1. 初始化 Git
git init

# 2. 添加文件
git add .

# 3. 提交（如果提示配置用户信息，执行下面两行）
git config user.email "你的邮箱@example.com"
git config user.name "你的用户名"
git commit -m "Initial commit: 摄影策划工具 Android 应用"

# 4. 关联你的 GitHub 仓库（替换成你的仓库地址）
git remote add origin https://github.com/你的用户名/photo-plan.git

# 5. 推送到 GitHub
git branch -M main
git push -u origin main
```

💡 如果提示需要登录，使用 **Personal Access Token** 作为密码：
- GitHub 右上角头像 → Settings → Developer settings → Personal access tokens → Generate new token → 勾选 `repo` 权限

---

## 🔨 第二步：添加 Gradle Wrapper

**重要**：项目需要 Gradle Wrapper 才能在 GitHub Actions 中构建。

### 方法 A：本地有 Android Studio（推荐）

1. 用 Android Studio 打开 `PhotoPlan` 项目
2. 等待 Gradle 同步完成（会自动下载 Gradle Wrapper）
3. 同步成功后，会自动生成这些文件：
   ```
   PhotoPlan/gradlew
   PhotoPlan/gradlew.bat
   PhotoPlan/gradle/wrapper/gradle-wrapper.jar
   ```
4. 提交并推送这些文件：
   ```bash
   git add gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar
   git commit -m "Add Gradle wrapper"
   git push
   ```

### 方法 B：本地没有 Android Studio

手动下载 Gradle 并生成 wrapper：

```bash
# 安装 Gradle（macOS 示例）
brew install gradle

# 或 Linux
sudo apt install gradle

# 进入 PhotoPlan 目录
cd PhotoPlan

# 生成 wrapper
gradle wrapper --gradle-version 8.9

# 提交并推送
git add gradlew gradlew.bat gradle/
git commit -m "Add Gradle wrapper"
git push
```

---

## ⚡ 第三步：触发自动构建

推送完成后，GitHub Actions 会**自动开始构建**！

### 查看构建状态

1. 打开你的 GitHub 仓库页面
2. 点击 **Actions** 标签页
3. 看到一个正在运行的 workflow，名字叫 `Build APK`
4. 等待约 3-5 分钟

### 下载构建好的 APK

构建成功 ✅ 后：

1. 点击这个 workflow run
2. 页面底部 **Artifacts** 区域
3. 下载 `app-debug`（约 10-20MB）
4. 解压 ZIP，得到 `app-debug.apk`

---

## 📱 第四步：安装到手机

### 方法 1：发送 APK 文件
1. 将 `app-debug.apk` 发送到你的手机（微信/QQ/邮件等）
2. 在手机上点击 APK 文件
3. 首次安装可能需要：**设置 → 允许安装未知来源应用**
4. 安装完成，打开即可使用！

### 方法 2：ADB 安装
```bash
adb install app-debug.apk
```

---

## 🔄 后续开发流程

修改代码后，重新构建只需：

```bash
git add .
git commit -m "你的修改描述"
git push
```

→ GitHub Actions 自动重新构建 → 去 Actions 下载新 APK

---

## ❓ 常见问题

### Q: 构建失败怎么办？
A: 点击失败的 workflow run → 查看日志，常见原因：
- 缺少 Gradle Wrapper 文件 → 按上面「第二步」操作
- 代码语法错误 → 检查修改的代码

### Q: 构建的 APK 可以发布吗？
A: Debug 版可以自用测试。正式发布需要：
- 生成签名密钥 keystore
- 配置 release 签名
- 构建签名版 APK

### Q: 可以本地构建吗？
A: 当然！本地有 Android Studio 的话：
1. 打开项目 → 连接手机 → 点击 ▶️ Run
2. 或命令行：`./gradlew assembleDebug`
3. APK 在 `app/build/outputs/apk/debug/`

---

## 📦 项目文件清单

已生成的所有文件：
```
.
├── .github/workflows/
│   └── build-apk.yml          # GitHub Actions 自动构建配置
├── .gitignore                 # Git 忽略文件配置
├── 摄影策划工具-安卓端设计方案.md   # 完整设计方案
├── 摄影策划工具-功能方案.md          # 原始参考方案
├── GITHUB_GUIDE.md            # 本文件
└── PhotoPlan/                 # Android 项目根目录
    ├── README.md              # 项目说明
    ├── settings.gradle.kts
    ├── build.gradle.kts
    ├── gradle.properties
    ├── gradle/libs.versions.toml
    ├── gradle/wrapper/        # 需要自行生成（见第二步）
    ├── app/
    │   ├── build.gradle.kts
    │   ├── proguard-rules.pro
    │   └── src/main/
    │       ├── AndroidManifest.xml
    │       ├── res/
    │       └── java/com/photo/plan/
    │           ├── MainActivity.kt
    │           ├── PhotoPlanApp.kt
    │           ├── data/              # 数据层
    │           └── ui/                # UI 层
    └── gradlew                 # 需要自行生成
```

---

## 🎯 下一步

- [ ] 按照上面步骤推送到 GitHub
- [ ] 确保 Gradle Wrapper 文件已添加
- [ ] 等待 Actions 构建完成
- [ ] 下载 APK 安装到手机测试

有问题可以查看 GitHub Actions 的构建日志排查！
