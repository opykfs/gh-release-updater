# GitHub 更新检测器（gh-release-updater）· GitHub Release Updater

![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Material Design 3](https://img.shields.io/badge/UI-Material%20Design%203-6750A4)

一款**个人开源**的 Android 工具：读取手机已安装应用的版本号，将「已安装应用」手动绑定到「GitHub 仓库」，打开应用即自动调用 GitHub Releases API 比对版本；发现新版本后在用户确认后下载，下载完成后由用户再次确认再拉起系统安装器安装。全应用遵循 Material Design 3（含 Material You 动态取色、深浅色跟随系统）。

**EN:** A **personal open-source** Android utility that reads the version numbers of apps installed on your phone, lets you manually bind an installed app to a GitHub repository, and automatically calls the GitHub Releases API to compare versions every time you open it. When a newer version is found it is downloaded only after your confirmation, and the system installer is launched only after you confirm again once the download finishes. The entire UI follows Material Design 3 (with Material You dynamic color, and light/dark theme following the system).

> 灵感来源：GitHub 上发布的 App 没有统一的应用商店更新渠道，本工具把这些来源的更新检查统一起来，做「检测 → 确认下载 → 确认安装」的最小闭环。**请仅在你信任的仓库上使用，安装未知来源 APK 有风险，本工具不校验 APK 签名。**

> **EN:** Apps published on GitHub have no unified app-store update channel. This tool consolidates update checks across such sources into a minimal loop: **detect → confirm download → confirm install**. **Only use it on repositories you trust — installing APKs from unknown sources is risky, and this tool does not verify APK signatures.**

## 功能 / Features

- 列出本机已安装应用（launcher 应用、按名称排序、排除自身），可搜索并选择。
  - **EN:** Lists installed launcher apps (sorted by name, excluding itself); searchable and selectable.
- 建立「应用 ↔ GitHub 仓库」绑定关系并持久化（DataStore Preferences），重启仍保留。
  - **EN:** Persists an "app ↔ GitHub repository" binding (DataStore Preferences) that survives restarts.
- 打开应用自动检测全部追踪项（并行）；单项失败（断网/404/限流/无 APK）隔离显示中文错误，不影响其他项。
  - **EN:** Automatically checks every tracked item in parallel when the app opens; individual failures (offline / 404 / rate limit / no APK) are isolated and shown with localized errors without affecting other items.
- 检测到新版本弹出「发现新版本」对话框；仅在用户点击「下载更新」后开始下载（DownloadManager，有进度与通知）。
  - **EN:** Pops a "New version found" dialog; downloading (via DownloadManager, with progress and a notification) starts only after you tap "Download update".
- 下载完成弹出「安装确认」对话框；仅在用户点击「安装」后经 FileProvider 拉起系统安装器；未授权「安装未知来源应用」时先引导到系统设置。
  - **EN:** Shows an install-confirmation dialog when the download completes; the system installer is launched through FileProvider only after you tap "Install" — if "Install unknown apps" is not granted, you are first guided to the system settings.
- 支持删除追踪；支持「跳过此版本」不再反复提醒——**跳过状态会如实显示为「已跳过版本 vX」，点击该卡片即可恢复提醒**（不会误报"已是最新"）。
  - **EN:** Supports removing a track and "Skip this version" to stop repeated reminders — the skipped state is honestly shown as "Skipped vX", and tapping that card restores the reminder (never wrongly claims "Up to date").
- 每次检测都会实时读取本机实际安装版本号，避免"已更新却仍提示旧版本/有新版本"的误判。
  - **EN:** Every check re-reads the version actually installed on the device in real time, avoiding false "old version / update available" states after you have already updated.

## 技术栈 / Tech Stack

| 领域 (Field) | 选型 (Stack) |
|---|---|
| 语言/UI · Language/UI | Kotlin + Jetpack Compose + Material 3（Material You） |
| 架构 · Architecture | Single-Activity + MVVM（ViewModel / Repository / DataSource layered interfaces） |
| 网络 · Networking | Retrofit2 + converter-kotlinx-serialization + OkHttp |
| 持久化 · Persistence | DataStore Preferences + kotlinx.serialization (JSON) |
| 下载/安装 · Download & Install | System DownloadManager + FileProvider + ACTION_VIEW |
| 版本比较 · Version Compare | Lightweight semantic version comparator（`VersionComparator`） |
| 构建 · Build | AGP 8.7.3 / Kotlin 2.0.21 / Gradle 8.9（see `gradle/libs.versions.toml`） |

## 安装 / 构建 · Install / Build

### 直接安装（普通用户）· Direct Install (End Users)

到右侧 **Releases** 下载最新的 `app-release.apk`，在手机上允许「安装未知来源应用」后安装即可。每个正式版都会附 debug/release 两种包。

**EN:** Download the latest `app-release.apk` from the **Releases** section on the right and install it on your phone after allowing "install apps from unknown sources". Every stable release ships both debug and release packages.

### 从源码构建（开发者）· Build from Source (Developers)

1. 克隆后，用 Android Studio（Ladybug / 2024.2.1+）打开工程根目录。
   - **EN:** After cloning, open the project root in Android Studio (Ladybug / 2024.2.1+).
2. Gradle Sync 通过后，连接真机点击 **Run ▶**；或命令行打包：
   - **EN:** Once Gradle Sync passes, connect a device and hit **Run ▶**, or build from the command line:
   ```bash
   gradle assembleDebug    # 产物 / output: app/build/outputs/apk/debug/app-debug.apk
   gradle assembleRelease  # 产物 / output: app/build/outputs/apk/release/app-release.apk
   ```
3. 首次构建需联网下载依赖；Windows 下如工程目录名含非 ASCII 字符，`gradle.properties` 已内置 `android.overridePathCheck=true` 规避路径检查。
   - **EN:** The first build downloads dependencies from the network. On Windows, if the project path contains non-ASCII characters, `android.overridePathCheck=true` is already set in `gradle.properties` to bypass the path check.

## 使用流程 / Usage

1. **添加追踪**：主界面右下角 `+` → 搜索并选择已安装应用（灰显 = 已追踪）→ 填写 GitHub 仓库地址（支持 `owner/repo` 或完整 `https://github.com/owner/repo` 链接）→ 保存。
   - **EN: Add a track:** Tap `+` at the bottom right of the home screen → search and pick an installed app (greyed out = already tracked) → enter the GitHub repository (`owner/repo` or a full `https://github.com/owner/repo` URL is supported) → save.
2. **自动检测**：打开应用即自动检测；列表项显示「已是最新 / 有新版本 / 已跳过 vX / 检测失败（原因）」。
   - **EN: Auto check:** Checking runs as soon as the app opens; each item shows "Up to date / Update available / Skipped vX / Check failed (reason)".
3. **下载**：检测到新版本自动弹出对话框 → 点「下载更新」（也可「跳过此版本」「暂不」）。下载过程卡片内显示进度条/百分比，系统通知栏可查看。
   - **EN: Download:** A dialog pops up automatically when a new version is found → tap "Download update" (or "Skip this version" / "Not now"). Progress is shown on the card (bar/percentage) and in the system notification.
4. **安装**：下载完成后弹「下载完成」→ 点「安装」→ 若首次使用会跳转到系统「允许安装未知应用」设置，授权后返回重新点「安装」。
   - **EN: Install:** "Download complete" appears when finished → tap "Install" → on first use you are taken to the system "Allow installing unknown apps" setting; after granting it, return and tap "Install" again.
5. 点击列表项右上角菜单可删除追踪；误点了「跳过此版本」后，点击该卡片即可恢复更新提醒。
   - **EN:** Use the menu at the top right of an item to delete a track; if you accidentally tapped "Skip this version", tap that card again to restore update reminders.

## GitHub 限流说明 / Rate Limits

未认证调用 GitHub API 限流约 60 次/小时/IP。超限时界面会提示「GitHub 接口限流，请稍后重试」，稍后刷新即可。

**EN:** Unauthenticated GitHub API calls are rate-limited to roughly 60/hour per IP. When exceeded, the UI shows "GitHub API rate limited, please retry later"; just refresh after a while.

## 目录结构 / Project Structure

```
app/src/main/java/com/example/githubupdater/
  data/            网络与本地数据实现、版本比较器 / network & local data impl., version comparator
  di/              依赖容器 / dependency container AppContainer
  domain/          纯模型与 Repository 接口 / pure models & repository interfaces
  ui/              Compose 界面（theme/navigation/trackinglist/addtracking/components）/ Compose UI
  util/            GitHubRepoParser
app/src/test/      纯逻辑单元测试 / pure-logic unit tests（VersionComparator、GitHubRepoParser）
docs/              开发过程文档（PRD/架构/QA 等，供参考）/ dev process docs (for reference)
```

## 免责声明 / Disclaimer

本软件全程使用AI制作，如有问题请提出。

**EN:** This software was made entirely with AI assistance. If you find any issues, please let us know.

本工具仅供学习与个人使用。使用前请确认目标仓库是可信的官方发布源；对由此产生的任何设备/数据风险，作者不承担责任。请遵守目标应用与仓库的开源/使用条款。

**EN:** This tool is for learning and personal use only. Before using it, please confirm the target repository is a trusted official release source. The author accepts no responsibility for any device/data risk arising from its use. Please comply with the open-source/usage terms of the target apps and repositories.
