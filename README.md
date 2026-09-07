# GitHub 更新检测器（gh-release-updater）

![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Material Design 3](https://img.shields.io/badge/UI-Material%20Design%203-6750A4)

一款**个人开源**的 Android 工具：读取手机已安装应用的版本号，将「已安装应用」手动绑定到「GitHub 仓库」，打开应用即自动调用 GitHub Releases API 比对版本；发现新版本后在用户确认后下载，下载完成后由用户再次确认再拉起系统安装器安装。全应用遵循 Material Design 3（含 Material You 动态取色、深浅色跟随系统）。

> 灵感来源：GitHub 上发布的 App 没有统一的应用商店更新渠道，本工具把这些来源的更新检查统一起来，做「检测 → 确认下载 → 确认安装」的最小闭环。**请仅在你信任的仓库上使用，安装未知来源 APK 有风险，本工具不校验 APK 签名。**

## 功能

- 列出本机已安装应用（launcher 应用、按名称排序、排除自身），可搜索并选择。
- 建立「应用 ↔ GitHub 仓库」绑定关系并持久化（DataStore Preferences），重启仍保留。
- 打开应用自动检测全部追踪项（并行）；单项失败（断网/404/限流/无 APK）隔离显示中文错误，不影响其他项。
- 检测到新版本弹出「发现新版本」对话框；仅在用户点击「下载更新」后开始下载（DownloadManager，有进度与通知）。
- 下载完成弹出「安装确认」对话框；仅在用户点击「安装」后经 FileProvider 拉起系统安装器；未授权「安装未知来源应用」时先引导到系统设置。
- 支持删除追踪；支持「跳过此版本」不再反复提醒——**跳过状态会如实显示为「已跳过版本 vX」，点击该卡片即可恢复提醒**（不会误报“已是最新”）。
- 每次检测都会实时读取本机实际安装版本号，避免“已更新却仍提示旧版本/有新版本”的误判。

## 技术栈

| 领域 | 选型 |
|---|---|
| 语言/UI | Kotlin + Jetpack Compose + Material 3（Material You） |
| 架构 | 单 Activity + MVVM（ViewModel / Repository / DataSource 接口分层） |
| 网络 | Retrofit2 + converter-kotlinx-serialization + OkHttp |
| 持久化 | DataStore Preferences + kotlinx.serialization(JSON) |
| 下载/安装 | 系统 DownloadManager + FileProvider + ACTION_VIEW |
| 版本比较 | 自研轻量语义化比较器（`VersionComparator`） |
| 构建 | AGP 8.7.3 / Kotlin 2.0.21 / Gradle 8.9（见 `gradle/libs.versions.toml`） |

## 安装 / 构建

### 直接安装（普通用户）

到右侧 **Releases** 下载最新的 `app-release.apk`，在手机上允许「安装未知来源应用」后安装即可。每个正式版都会附 debug/release 两种包。

### 从源码构建（开发者）

1. 克隆后，用 Android Studio（Ladybug / 2024.2.1+）打开工程根目录。
2. Gradle Sync 通过后，连接真机点击 **Run ▶**；或命令行打包：
   ```bash
   gradle assembleDebug    # 产物：app/build/outputs/apk/debug/app-debug.apk
   gradle assembleRelease  # 产物：app/build/outputs/apk/release/app-release.apk
   ```
3. 首次构建需联网下载依赖；Windows 下如工程目录名含非 ASCII 字符，`gradle.properties` 已内置 `android.overridePathCheck=true` 规避路径检查。

### 关于签名（维护者须知）

- 仓库内**不含任何签名密钥**。`keystore.properties` 与 `keystore/` 均已被 `.gitignore` 忽略。
- `app/build.gradle.kts` 的 release 构建：若工程根存在 `keystore.properties`（含 `storeFile/storePassword/keyAlias/keyPassword`）则使用正式签名，否则自动回退为 debug 签名——保证任何人 clone 后都能构建出可安装包。
- 作者本地正式签名文件不会公开，因此在 GitHub Releases 上发布的 APK 若换电脑重建，需要沿用同一 `keystore` 才能覆盖升级。

## 使用流程

1. **添加追踪**：主界面右下角 `+` → 搜索并选择已安装应用（灰显=已追踪）→ 填写 GitHub 仓库地址（支持 `owner/repo` 或完整 `https://github.com/owner/repo` 链接）→ 保存。
2. **自动检测**：打开应用即自动检测；列表项显示「已是最新 / 有新版本 / 已跳过 vX / 检测失败（原因）」。
3. **下载**：检测到新版本自动弹出对话框 → 点「下载更新」（也可「跳过此版本」「暂不」）。下载过程卡片内显示进度条/百分比，系统通知栏可查看。
4. **安装**：下载完成后弹「下载完成」→ 点「安装」→ 若首次使用会跳转到系统「允许安装未知应用」设置，授权后返回重新点「安装」。
5. 点击列表项右上角菜单可删除追踪；误点了「跳过此版本」后，点击该卡片即可恢复更新提醒。

## GitHub 限流说明

未认证调用 GitHub API 限流约 60 次/小时/IP。超限时界面会提示「GitHub 接口限流，请稍后重试」，稍后刷新即可。本工具不做 Token/认证。

## 真机冒烟清单（A1–A6）

- A1：添加追踪页能列出已装应用，正确显示应用名、包名、版本号。
- A2：选择应用 + 填写仓库 → 保存；杀掉进程重启后绑定仍在；删除生效。
- A3：打开应用自动检测；仓库正确且无新版显示「已是最新」；有新版显示「有新版本」；断网/错误仓库显示中文错误且不影响其他项。
- A4：检测到新版本弹「发现新版本」（当前/最新版本）；仅点「下载更新」才下载；下载有可见进度。
- A5：下载完成弹「下载完成」；仅点「安装」才拉起系统安装器；「稍后」无任何动作。
- A6：主界面、添加页、两个对话框均使用 Material 3 组件与主题；深浅色跟随系统、Android 12+ 动态取色生效。
- A7：对某版本点「跳过」→ 显示「已跳过 vX」而非“已是最新”；点卡片可恢复并重新弹窗。
- A8：把某追踪应用更新到最新版后重新打开 → 实时显示新版本号并判定「已是最新」，不再反复提示。

## 目录结构

```
app/src/main/java/com/example/githubupdater/
  data/            网络与本地数据实现、版本比较器
  di/              依赖容器 AppContainer
  domain/          纯模型与 Repository 接口
  ui/              Compose 界面（theme/navigation/trackinglist/addtracking/components）
  util/            GitHubRepoParser
app/src/test/      纯逻辑单元测试（VersionComparator、GitHubRepoParser）
docs/              开发过程文档（PRD/架构/QA 等，供参考）
```

## 免责声明

本工具仅供学习与个人使用。使用前请确认目标仓库是可信的官方发布源；对由此产生的任何设备/数据风险，作者不承担责任。请遵守目标应用与仓库的开源/使用条款。
