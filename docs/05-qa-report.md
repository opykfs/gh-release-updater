# GitHub 更新检测器 — QA 独立质量验证报告（05）

> QA：严过关（Yan）｜输入：01-PRD / 02-architecture / 03-tasks / 04-code-summary + 全部实际代码
> 方式：本机无 Android SDK/JDK，未执行 gradle/test；本报告为**高强度静态审查 + 逐行逻辑走查 + 纯函数手算核对**。
> 工程师自述（04）仅作对照线索，未采信其「IS_PASS: YES」结论。

---

## 一、验证范围

| 范围 | 内容 | 结果 |
|---|---|---|
| Gradle/版本目录 | libs.versions.toml 别名 ↔ app/build.gradle.kts 引用、插件声明、AGP8.7.3/Kotlin2.0.21/Gradle8.9 | 一致 |
| Manifest/资源 | 权限、queries、组件、FileProvider authority、file_paths、strings/drawable/mipmap/colors/themes | 一致 |
| Kotlin 源码 38 个 | 全部读取并走查（模型/接口/data/repo/VM/Screen/Dialog/theme/parser/comparator） | 通过 |
| 单元测试 2 个 | 逐条断言与实现手算核对 | 通过（断言正确） |
| 核心链路 | 检测→弹框→下载→完成→安装 全链路逐行走查 | 逻辑成立，见 P2/P3 |

**一致性核查结论**（逐项核对，非采信工程师）：
- toml 的 13 个 version、21 个 library、4 个 plugin alias 与 build.gradle.kts 引用全部对上；serialization/compose 插件均已 `apply` 到 app 模块。
- Manifest：INTERNET / QUERY_ALL_PACKAGES / REQUEST_INSTALL_PACKAGES + 2 条 queries 齐备；FileProvider `com.example.githubupdater.fileprovider` 与代码 `"${context.packageName}.fileprovider"`、applicationId 一致；`@xml/file_paths` = external-files-path `Download/`，与实际 `getExternalFilesDir(DIRECTORY_DOWNLOADS)` 落盘目录匹配。
- Kotlin 引用的全部 `@string/*` 均在 strings.xml 存在；`R.*` 图标均来自 material-icons-extended（已声明）。未发现 @drawable 缺失（launcher 自适应图标 minSdk26 全覆盖）。
- 未发现 `// TODO` / `pass` 空实现残留。

---

## 二、问题清单表

分级说明：P0 阻断验收 / P1 严重 / P2 一般 / P3 建议。

| # | 级别 | 文件:行 | 问题描述 | 建议修复 |
|---|---|---|---|---|
| 1 | P2 | ui/trackinglist/TrackingListScreen.kt:117-121、TrackingListItem.kt:51-55、TrackingListViewModel.kt:171-177/180-260、data/repository/DownloadRepositoryImpl.kt:54-56 | **无「正在下载同一项」重复点击防护**：下载中该 item 的 `status` 仍为 UPDATE_AVAILABLE（仅 downloadProgress 非空），卡片仍可点击重开「发现新版本」对话框并再次 `onUpdateDownloadConfirmed` 发起第二次下载；两次 enqueue 目标为同一文件名，`start()` 每次都先 `target.delete()`，可互相删文件导致 APK 损坏/安装失败，且产生重复系统通知。 | 在 ViewModel 维护 `Set<packageName>` 进行中下载集合（或 onItemClicked/onDownload 时检查 downloadProgress != null）；下载中禁止重开对话框；下载真正结束后才清除。 |
| 2 | P2 | ui/trackinglist/TrackingListViewModel.kt:121-126 | **「仓库 Release 无 APK」被归为 FAILED（红色「检测失败」）**：fetchLatest 返回 null 仅表示无可用 .apk asset（请求成功），却走 FAILED + error_no_apk，每轮检测都展示错误、视觉上像异常，与架构风险表「WARN 文案『无可下载安装包』、可手动再检」语义不符。 | 增加独立中性状态或复用 UP_TO_DATE 但文案用「无可下载安装包」；至少不并入 FAILED。 |
| 3 | P2 | ui/trackinglist/TrackingListViewModel.kt:77-95 | **下载进行中触发手动刷新会清掉该 item 的 downloadProgress/errorMessage 并置 CHECKING**（observe 协程仍在跑，靠下一次 Running 事件才恢复），期间进度条闪烁、可能短暂丢失状态。 | refreshAll 对 downloadProgress != null 的 item 保留其进度与下载中状态（或跳过下载中项）。 |
| 4 | P3 | data/repository/DownloadRepositoryImpl.kt:127/160/168/194、ReleaseRepositoryImpl.kt:22-24/46、InstallRepositoryImpl.kt:21 | **数据层硬编码中文文案直接上屏**（下载失败/任务不存在/文件被清理/网络/404/403 等），绕过 strings.xml，违反共享约定 §2；DownloadRepository 的 Failed reason 原样进 snackbar/errorMessage。 | 转 resource id 或在 VM 层统一映射（ReleaseRepository 三条为架构 §8 指定，可与工程约定再对齐）。 |
| 5 | P3 | ui/addtracking/AddTrackingViewModel.kt:105-116、AddTrackingScreen.kt:74-108 | 「请先选择应用」「该应用已在追踪」等错误挂在 `repoError`，显示在**仓库输入框** supportingText 下，错误归属误导；且界面将仓库输入放在应用列表上方，与 PRD 线框（先选应用、再填地址）顺序相反。 | 拆分独立 `appError`；布局调整为「选择应用 → 仓库地址 → 保存」。 |
| 6 | P3 | ui/trackinglist/TrackingListViewModel.kt:277-283、data/repository/InstallRepositoryImpl.kt:51-53 | openInstaller 抛出的具体原因（如「未找到可用的安装程序」）在 onInstallConfirmed 被笼统替换为 `error_install_failed`「拉起系统安装器失败」，strings `error_no_installer` 定义后从未使用。 | catch 时透传 e.message 或改用 error_no_installer 展示。 |
| 7 | P3 | ui/trackinglist/TrackingListViewModel.kt:162-168 | **每次手动刷新后**只要仍存在 UPDATE_AVAILABLE 且无对话框，就再次自动弹出该更新（用户此前点过「暂不」也会再弹），可能打扰。 | 会话级「已自动弹过」标记（或记录 dismissed tag 集合）。 |
| 8 | P3 | util/GitHubRepoParser.kt:57-64 | 主机识别用 `lower.startsWith(host)`，缺少边界校验：如 `github.comm/foo/bar` 会被误判为 github 并解析成功（owner=m, repo=foo）；`www.github.com.evil.com/...` 依赖后续 owner 正则兜底。 | 匹配主机后校验下一位字符为 `/`、`?`、`#` 或串尾。 |
| 9 | P3 | ui/trackinglist/TrackingListViewModel.kt:307-320 | 「跳过此版本」后 item 显示「已是最新（v2.1.0）」，实际本机仍是旧版本、只是被跳过，文案易误导（工程师已自认，此处独立确认）。 | 展示「已跳过 v2.1.0」类文案或引入专门状态。 |
| 10 | P3 | data/VersionComparator.kt:23-28 | `cleanVersion` 只 removeSuffix 一个尾部 `.0`（如 `1.0.0`→`1.0`），且产品逻辑中未使用（仅测试）；`parse` 对非数字基段静默丢弃（`1.2.x`→[1,2]）。当前不影响主链路（比较用 isUpdateAvailable），风险低。 | 如需展示规范化版本再收紧；非数字基段可返回空/跳过处理。 |
| 11 | P3 | res/values/strings.xml:24、66 | `last_check`、`error_no_installer` 资源未在任何代码引用（死资源）。 | 接入使用或删除。 |
| 12 | P3 | ui/addtracking/AddTrackingViewModel.kt:155-158 | 保存成功后直接返回列表，新增项为 IDLE，**不会触发自动检测**（需用户手动刷新或重启应用），首条体验略断。 | save 成功后通知列表 VM 刷新或由列表侧监听新数据触发一次检测。 |

---

## 三、对照 A1–A6 核查结果表

| 验收 | 结论 | 依据（静态走查） |
|---|---|---|
| A1 添加页列出已装应用（名/包名/版本号） | ✅ PASS | AppRepositoryImpl（launcher 过滤、label 排序、排除自身、versionName/versionCode）；AddTrackingScreen 展示 appLabel/packageName/`版本 %s`、图标、搜索。 |
| A2 选择+填地址绑定、重启保留、支持删除 | ✅ PASS | TrackedApp/TrackedAppList @Serializable 字段与 TrackingRepositoryImpl JSON 编解码一致；add 在 DataStore edit 内做 packageName 唯一性；删除走菜单 → onDelete → remove。 |
| A3 打开自动检测；新版显示状态；错误不崩溃、单项隔离 | ✅ PASS | VM init→refreshAll；coroutineScope+async 并行；checkSingle try/catch（CancellationException 重抛）单项隔离；HttpException 404/403、IOException → 中文；FAILED 展示 errorMessage。注：P2-2「无 APK」并入 FAILED 属语义偏差。 |
| A4 检测弹框、确认才下载、可见进度 | ✅ PASS | maybeAutoShowFirstUpdate 自动弹首个；UpdateAvailableDialog 展示当前/最新；仅「下载更新」→ start/observe；Running→downloadProgress 进度条+百分比。注：P2-1 重复下载防护缺失。 |
| A5 下载完成弹框、确认才安装、稍后无动作 | ✅ PASS | Completed→activeInstall(PendingInstall)；InstallConfirmDialog 仅「安装」→ isInstallAllowed→未知来源引导/ACTION_VIEW+FileProvider+FLAG_GRANT_READ_URI_PERMISSION+NEW_TASK；「稍后」仅 onDismiss。 |
| A6 MD3 主题覆盖四页/两对话框、深浅色、动态取色 | ✅ PASS | 全页面用 material3 组件（TopAppBar/AlertDialog/Card/FAB/TextField…）；Theme.kt API31+ dynamicColor、darkTheme=isSystemInDarkTheme；颜色仅经 colorScheme，无硬编码 UI 色（colors.xml 仅图标背景）。 |

---

## 四、单测手算核对结果

### VersionComparatorTest（实现 VersionComparator.kt 逐条手算）
| 用例 | 手算过程 | 断言正确 |
|---|---|---|
| v1.2.3 == 1.2.3 / V 大写 | 去 v 前缀后 numeric [1,2,3] 相同 → 0 | ✅ |
| " 1.2.3 " vs v1.2.3 | trim → 相同 → 0 | ✅ |
| 1.2.3 < 2.0.0 | 首位 1<2 → 负 | ✅ |
| 2.0.0 > 1.2.3 | 首位 2>1 → 正 | ✅ |
| v2.1.0 > 1.8.0 且 isUpdateAvailable | 首位 2>1 → 正 → true | ✅ |
| 1.2.3.1 > 1.2.3（及反向） | 缺位补 0：第4位 1>0 → 正 | ✅ |
| 1.2.3-beta < 1.2.3 | numeric 相同，有 pre < 无 pre → 负 | ✅ |
| isUpdateAvailable(v1.2.3-beta,1.2.3)=false | 同上 → false | ✅ |
| 1.2.3-beta > 1.2.2 | numeric 第3位 3>2 → 正（提前返回，不看 pre）→ isUpdateAvailable true | ✅ |
| beta.2 < beta.10 | comparePre：beta==beta，数字 2<10 → 负 | ✅ |
| alpha < beta / rc.1 == rc.1 | 字母序 / 全等 → 负 / 0 | ✅ |
| 空/null 安全 | [] vs [] → 0；空最低 → >0 / <0；isUpdateAvailable("",1.0.0)=false | ✅ |
| cleanVersion("v1.2.3-beta")="1.2.3"、"+abc"、"null"→"" | 去后缀/元数据；null→"" | ✅ |

### GitHubRepoParserTest（实现 GitHubRepoParser.kt 逐条手算）
| 用例 | 手算过程 | 断言正确 |
|---|---|---|
| owner/repo、去空白 | 非 URL，段数=2 → 成功 | ✅ |
| https://github.com/o/r（尾斜杠/.git/www/http/无协议） | host 识别→取前两段→去 .git → 成功 | ✅ |
| 空/空白/null → EMPTY | trim 为空 → EMPTY | ✅ |
| gitlab.com → NOT_GITHUB | 带协议且非 github 主机 → NOT_GITHUB | ✅ |
| "owner" / "owner/repo/extra" / "owner/re po" → INVALID | 段数/正则不符 → INVALID | ✅ |

> 注：GitHubRepoParser 另走查完整 URL 带路径后缀（…/releases/tag/x）→ 正确取前两段；仅主机前缀无边界校验见问题 P3-8。

---

## 五、核心链路关键走查结论（补充）

1. **refreshAll**：guard 防重入；空列表直接置空；并行 async 且 CancellationException 正确上抛；单项异常不阻断他项；DataStore 写操作全部在 `edit{}` 内（read-modify-write 原子化），多并行 update 不丢数据。observeTracks 只重建 UI 快照、保留既有动态字段（status/progress），与 refresh 时序最终收敛一致。
2. **下载→安装**：enqueue id<0 → snackbar；Running→进度；Completed→activeInstall 对话框；isInstallAllowed 未授权→引导设置并保留对话框；授权→FileProvider+ACTION_VIEW+GRANT_READ+NEW_TASK（appContext 启动必须 NEW_TASK，已加）。广播注册 API33+ 用 RECEIVER_NOT_EXPORTED（系统受保护广播可收）；另有 1s 轮询兜底，不依赖广播。
3. **生命周期**：viewModelScope 驱动全部协程，observe() 的 callbackFlow 在 collect 取消时 awaitClose 会注销接收器并取消 ticker/poller；collectAsStateWithLifecycle 使用正确；Dispatchers.IO 均落在 Repository/withContext 内。
4. **模型一致性**：TrackedApp @Serializable 默认值齐全，JSON 缺字段可回退；InstallRepository 无 <O 分支（minSdk26 恒走 O+，dead code 但无害）。

---

## 六、最终结论

**VERDICT: PASS（静态审查维度）**

- P0：0 ｜ P1：0 ｜ P2：3 ｜ P3：9
- 依据：逐文件走查未发现阻断验收 A1–A6 的缺陷；版本比较器/仓库解析器与单测断言手算全部吻合；下载→安装全链路（DownloadManager→FileProvider→ACTION_VIEW→未知来源授权）authority/路径/flag 一致性成立。
- **重要前提**：本环境无法编译/运行。PASS 仅覆盖静态逻辑；需在 Android Studio（Ladybug+）完成首次 Sync/assembleDebug + `./gradlew test`，并按 README 的 A1–A6 真机冒烟复核（尤其 A4/A5 下载安装、通知栏进度、未知来源授权、DataStore 重启持久化）。P2 项建议工程师修复后再进真机回归。

---

# 第 2 轮回归复核（P2 修复复审）

> 范围：仅针对工程师对 3 个 P2 的修复做增量复核，不重复全量审查。
> 实际核验文件：ui/trackinglist/TrackingListViewModel.kt、TrackingListItem.kt、TrackingListScreen.kt、domain/model/UpdateStatus.kt、res/values/strings.xml、docs/04-code-summary.md。

## 逐项结论

| 修复 | 结论 | 核验依据 |
|---|---|---|
| P2-1 重复下载防护 | ✅ PASS | `downloadingPackages` 仅在主线程访问（onUpdateDownloadConfirmed 同步 add，viewModelScope/collect 回调均 Main）；`onItemClicked`/`maybeAutoShowFirstUpdate`/`onUpdateDownloadConfirmed` 三处均跳过下载中项（VM:205/194/212）。**remove 全覆盖无泄漏**：id<0(:217)、Completed(:254)、Failed(:273)、CancellationException(:291)、其他异常(:294) 每条 add(:213) 之后都有对应终态 remove。observe flowOn(IO) 只影响上游，collect 仍回主线程，集合无并发写。 |
| P2-2 无 APK 中性化 | ✅ PASS | `UpdateStatus` 新增 `NO_APK`（置于 FAILED 之后，未改既有序）；checkSingle `fetchLatest==null` → `NO_APK + errorMessage=null`（VM:147-153），downloadUrl/latestTag 保持 null，`toPendingUpdate`/onItemClicked 不会误弹（非 UPDATE_AVAILABLE）；TrackingListItem 新增 secondaryContainer 中性徽标（非红、非 error 色）(:192-207)。grep 确认全库唯一 when 分支已穷尽 6 态，`error_no_apk` 引用清零，strings 新增 `status_no_apk` 且被引用。 |
| P2-3 下载中刷新不清进度 | ✅ PASS | `pendingChecks = tracks.filterNot { in downloadingPackages }`（VM:85）；刷新预置态对下载中项保留原 TrackedItemUiState 不置 CHECKING/不清 downloadProgress（:92-94）；全部在下载时 `isCheckingAll=false` 直接结束（:113-116）；最终回填对「回填瞬间已在下载」的项跳过覆盖（:127-129），杜绝竞态把进度覆盖掉。下载完成/Failed 即 remove（:254/:273），**不会永久跳过该项目的后续再检**。 |

## 新增/遗留问题

- 无新增 P0 / P1 / P2。
- 遗留 P3-1（文档同步，不影响验收）：docs/02-architecture.md 4.1/类图仍写 UpdateStatus 为 5 态（未同步 NO_APK 第 6 态）；docs/05 前文单测结论不变。
- 遗留 P3（沿用第 1 轮，不属本次修复范围，均不影响 A1–A6 验收）：数据层硬编码中文、Add 页错误归属/布局顺序、error_no_installer 未用、GitHubRepoParser 主机前缀边界、刷新后重复自动弹框、跳过显示「已是最新」措辞等，见上表 P3-1/4-9/11。

## 结论

**VERDICT: PASS（第 2 轮回归）** — 3 个 P2 修复均真实落地且逻辑自洽；无残留 P2；剩余 P3 不阻断 A1–A6 验收。仍保留「需 AS 首次构建 + ./gradlew test + 真机冒烟」的总前提。
