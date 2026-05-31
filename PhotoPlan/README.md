# 摄影策划工具

一款专为摄影师打造的 Android 原生应用，用于管理拍摄策划和参考样图。

## ✨ 核心功能

- 📋 **策划管理** - 创建、编辑、删除拍摄策划，每个策划独立管理
- 🖼️ **样图上传** - 从相册多选参考样图，自动保存到本地
- ✅ **勾选完成** - 拍摄现场勾选已完成的样图，自动沉底排列
- 📊 **进度跟踪** - 实时显示完成进度条
- 🔍 **全屏查看** - 点击图片放大查看，支持左右滑动切换
- 🔒 **完全离线** - 所有数据本地存储，无需联网，无需登录

## 🛠️ 技术栈

| 组件 | 技术选型 |
|------|----------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose |
| 本地数据库 | Room (SQLite) |
| 图片加载 | Coil 3 |
| 导航 | Compose Navigation |
| 异步 | Coroutines + Flow |
| 最低 SDK | API 26 (Android 8.0) |

## 📦 项目结构

```
app/src/main/java/com/photo/plan/
├── MainActivity.kt                 # 入口 Activity
├── PhotoPlanApp.kt                 # Application 类
├── data/
│   ├── local/                      # Room 数据库层
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   └── entity/
│   └── repository/                 # 数据仓库
└── ui/
    ├── home/                       # 首页（策划列表）
    ├── create/                     # 新建/编辑策划
    ├── detail/                     # 详情页（拍摄现场）
    ├── viewer/                     # 图片查看器
    ├── navigation/                 # 路由
    └── theme/                      # 主题
```

## 🚀 构建方式

### 方式一：Android Studio 本地构建（推荐）

1. 克隆项目
```bash
git clone <your-repo-url>
cd PhotoPlan
```

2. 用 Android Studio 打开项目
   - File → Open → 选择 `PhotoPlan` 目录
   - 等待 Gradle 同步完成（首次需要下载依赖，可能需要几分钟）

3. 连接手机或启动模拟器

4. 点击 ▶️ Run 按钮（或快捷键 `Shift+F10`）

5. 构建成功后，APK 位于：
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### 方式二：命令行构建

```bash
# macOS/Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### 方式三：GitHub Actions 自动构建

项目已配置 GitHub Actions，推送到 GitHub 后会自动构建。

**触发条件：**
- 推送到 `main` 或 `master` 分支时
- 手动触发（Actions 页面点击 Run workflow）

**构建产物：**
- 每次运行会上传 `app-debug.apk` 到 Actions 运行结果的 Artifacts 中
- 可直接下载安装

## 📱 安装到手机

### 方法 1：Android Studio 直接运行
连接手机开启 USB 调试 → 点击 Run → 自动安装

### 方法 2：APK 文件安装
1. 将 `app-debug.apk` 发送到手机
2. 手机上打开 APK 文件
3. 允许「安装未知来源应用」权限
4. 安装完成即可使用

## 🎯 使用流程

```
1. 打开 App → 首页（空状态）
2. 点击右下角 + → 新建策划
3. 输入策划名称 → 点击「从相册选择图片」
4. 多选参考样图 → 点击「保存策划」
5. 进入详情页，所有样图在「待拍摄」区域
6. 拍摄现场每拍完一张 → 点击右上角 ✓ 标记完成
7. 完成的图片自动沉底到「已完成」区域
8. 点击图片 → 全屏放大查看，左右滑动切换
```

## 🔧 开发说明

### 核心交互逻辑

**完成勾选排序：**
```kotlin
// Room 查询时自动排序
@Query("SELECT * FROM samples WHERE planId = :planId 
       ORDER BY isCompleted ASC, sortOrder ASC")
fun getSamplesByPlanId(planId: Long): Flow<List<SampleEntity>>
```

**图片存储：**
- 图片复制到应用内部存储：`filesDir/images/{planId}/`
- 数据库只保存本地文件路径
- 删除策划时级联删除图片目录

### 数据模型

| 表 | 字段 |
|----|------|
| plans | id, name, createdAt, updatedAt |
| samples | id, planId, localPath, isCompleted, sortOrder, createdAt |

## 📝 后续扩展

- [ ] 策划数据导出/导入
- [ ] 样图添加文字备注
- [ ] 拖拽调整图片顺序
- [ ] 策划分享（生成海报）
- [ ] 深色模式
- [ ] 桌面小组件

## 📄 License

MIT License
