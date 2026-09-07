package com.example.githubupdater.ui.trackinglist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.githubupdater.R
import com.example.githubupdater.data.VersionComparator
import com.example.githubupdater.di.AppContainer
import com.example.githubupdater.domain.model.TrackedApp
import com.example.githubupdater.domain.model.UpdateStatus
import com.example.githubupdater.domain.repo.AppRepository
import com.example.githubupdater.domain.repo.DownloadRepository
import com.example.githubupdater.domain.repo.DownloadState
import com.example.githubupdater.domain.repo.InstallRepository
import com.example.githubupdater.domain.repo.ReleaseRepository
import com.example.githubupdater.domain.repo.TrackingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 追踪列表 ViewModel：自动检测编排、状态更新、下载/安装确认、跳过、删除。
 */
class TrackingListViewModel(
    private val appContext: Context,
    private val trackingRepository: TrackingRepository,
    private val appRepository: AppRepository,
    private val releaseRepository: ReleaseRepository,
    private val downloadRepository: DownloadRepository,
    private val installRepository: InstallRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackingListUiState())
    val uiState: StateFlow<TrackingListUiState> = _uiState.asStateFlow()

    /**
     * 正在下载中的 packageName 集合。
     * 仅在主线程访问（onUpdateDownloadConfirmed / collect 回调均在 Main），
     * 用于防止同一项重复下载，以及刷新检测时跳过下载中项（P2-1 / P2-3）。
     */
    private val downloadingPackages = mutableSetOf<String>()

    init {
        observeTracks()
        refreshAll()
    }

    /** 响应 DataStore 变化：新增/删除/更新后重建 items（保留动态字段）。 */
    private fun observeTracks() {
        viewModelScope.launch {
            trackingRepository.tracks.collect { tracks ->
                _uiState.update { st ->
                    val items = tracks.map { t ->
                        val existing = st.items.firstOrNull { it.track.packageName == t.packageName }
                        if (existing != null) {
                            existing.copy(track = t)
                        } else {
                            TrackedItemUiState(track = t)
                        }
                    }
                    st.copy(items = items)
                }
            }
        }
    }

    /** 自动检测 / 手动刷新共用入口。全部追踪项并行检测，单项失败隔离。 */
    fun refreshAll() {
        if (_uiState.value.isCheckingAll) return
        viewModelScope.launch {
            val tracks = trackingRepository.tracks.first()
            if (tracks.isEmpty()) {
                _uiState.update { it.copy(items = emptyList(), isCheckingAll = false) }
                return@launch
            }

            // P2-3：下载中的项不参与本轮检测，保留其现有状态与 downloadProgress 不动
            val pendingChecks = tracks.filterNot { it.packageName in downloadingPackages }

            _uiState.update { st ->
                st.copy(
                    isCheckingAll = pendingChecks.isNotEmpty(),
                    items = tracks.map { t ->
                        val existing = st.items.firstOrNull { it.track.packageName == t.packageName }
                        if (t.packageName in downloadingPackages) {
                            // 下载中：保留现有 TrackedItemUiState（含进度/状态）
                            existing ?: TrackedItemUiState(track = t)
                        } else {
                            existing?.copy(
                                track = t,
                                status = UpdateStatus.CHECKING,
                                latestTag = null,
                                releaseNotes = null,
                                downloadUrl = null,
                                fileName = null,
                                errorMessage = null,
                                downloadProgress = null,
                            )
                                ?: TrackedItemUiState(track = t, status = UpdateStatus.CHECKING)
                        }
                    },
                )
            }

            // 全部都在下载、没有可检测项时直接结束
            if (pendingChecks.isEmpty()) {
                _uiState.update { it.copy(isCheckingAll = false) }
                return@launch
            }

            // 并行检测：为每个待检项创建协程；awaitAll 收集全部结果
            val results = coroutineScope {
                val deferred = pendingChecks.map { t: TrackedApp ->
                    async { t.packageName to checkSingle(t) }
                }
                deferred.awaitAll()
            }.toMap()

            _uiState.update { st ->
                st.copy(
                    isCheckingAll = false,
                    items = st.items.map { item ->
                        // 若回填瞬间该项已开始下载，则保留现有状态，不让检测结果覆盖进度
                        if (item.track.packageName in downloadingPackages) {
                            item
                        } else {
                            results[item.track.packageName] ?: item
                        }
                    },
                )
            }

            maybeAutoShowFirstUpdate()
        }
    }

    /** 检测单个仓库并持久化 lastCheckTime / lastKnownLatestTag。 */
    private suspend fun checkSingle(track: TrackedApp): TrackedItemUiState {
        val now = System.currentTimeMillis()
        return try {
            // 实时读取本机当前安装版本，纠正绑定后用户已手动更新/重装导致的陈旧 localVersionName
            val actualInstalled = appRepository.getInstalledVersionName(track.packageName)
            val base = if (!actualInstalled.isNullOrBlank()) {
                track.copy(localVersionName = actualInstalled)
            } else {
                track
            }

            val info = releaseRepository.fetchLatest(base)

            // 已装版本已不低于远程最新 tag 时，残留的跳过标记失去意义，顺手清理
            val cleaned = if (info != null && base.skippedTag != null &&
                !VersionComparator.isUpdateAvailable(info.tag, base.localVersionName)
            ) {
                base.copy(skippedTag = null)
            } else {
                base
            }

            val updated = cleaned.copy(
                lastCheckTime = now,
                lastKnownLatestTag = info?.tag ?: cleaned.lastKnownLatestTag,
            )
            trackingRepository.update(updated)

            when {
                // 请求成功但仓库 Release 无 .apk asset：中性 NO_APK，不属于失败
                info == null -> TrackedItemUiState(
                    track = updated,
                    status = UpdateStatus.NO_APK,
                    errorMessage = null,
                )

                // 已装版本不低于远程最新 tag：真正已是最新
                !VersionComparator.isUpdateAvailable(info.tag, updated.localVersionName) -> TrackedItemUiState(
                    track = updated,
                    status = UpdateStatus.UP_TO_DATE,
                    latestTag = info.tag,
                    releaseNotes = info.notes,
                    downloadUrl = info.downloadUrl,
                    fileName = info.fileName,
                )

                // 远程存在更高版本，但用户曾跳过该 tag：如实展示「已跳过」，点击卡片可恢复提醒
                updated.skippedTag != null && updated.skippedTag == info.tag -> TrackedItemUiState(
                    track = updated,
                    status = UpdateStatus.SKIPPED,
                    latestTag = info.tag,
                    releaseNotes = info.notes,
                    downloadUrl = info.downloadUrl,
                    fileName = info.fileName,
                )

                // 远程存在更高版本且未被跳过：可更新
                else -> TrackedItemUiState(
                    track = updated,
                    status = UpdateStatus.UPDATE_AVAILABLE,
                    latestTag = info.tag,
                    releaseNotes = info.notes,
                    downloadUrl = info.downloadUrl,
                    fileName = info.fileName,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TrackedItemUiState(
                track = track.copy(lastCheckTime = now),
                status = UpdateStatus.FAILED,
                errorMessage = e.message ?: appContext.getString(R.string.error_generic),
            )
        }
    }

    /** 检测结束：若还没有弹出任何对话框，自动弹出列表顺序中的首个有更新项（跳过下载中项）。 */
    private fun maybeAutoShowFirstUpdate() {
        val st = _uiState.value
        if (st.activeUpdate != null || st.activeInstall != null) return
        val first = st.items.firstOrNull {
            it.status == UpdateStatus.UPDATE_AVAILABLE &&
                it.track.packageName !in downloadingPackages
        } ?: return
        val pending = first.toPendingUpdate() ?: return
        _uiState.update { it.copy(activeUpdate = pending) }
    }

    /** 点击卡片：「有新版本」再次弹更新框；「已跳过」则清除跳过标记并重新弹框（恢复提醒）。 */
    fun onItemClicked(track: TrackedApp) {
        val item = _uiState.value.items.firstOrNull { it.track.packageName == track.packageName }
            ?: return
        if (track.packageName in downloadingPackages) return
        when (item.status) {
            UpdateStatus.UPDATE_AVAILABLE -> {
                val pending = item.toPendingUpdate() ?: return
                _uiState.update { it.copy(activeUpdate = pending) }
            }

            UpdateStatus.SKIPPED -> viewModelScope.launch {
                val cleared = item.track.copy(skippedTag = null)
                trackingRepository.update(cleared)
                _uiState.update { st ->
                    st.copy(
                        items = st.items.map {
                            if (it.track.packageName == track.packageName) {
                                it.copy(track = cleared, status = UpdateStatus.UPDATE_AVAILABLE)
                            } else it
                        },
                    )
                }
                val pending = item.toPendingUpdate() ?: return@launch
                _uiState.update { it.copy(activeUpdate = pending) }
            }

            else -> Unit
        }
    }

    /** 用户确认下载更新：启动下载并开始观察进度（同一项下载中禁止重复发起）。 */
    fun onUpdateDownloadConfirmed(update: PendingUpdate) {
        if (update.packageName in downloadingPackages) return
        downloadingPackages.add(update.packageName)
        viewModelScope.launch {
            val id = downloadRepository.start(update.downloadUrl, update.fileName)
            if (id < 0) {
                downloadingPackages.remove(update.packageName)
                _uiState.update {
                    it.copy(
                        activeUpdate = null,
                        snackbarMessage = appContext.getString(R.string.error_download_start),
                    )
                }
                return@launch
            }
            _uiState.update { st ->
                st.copy(
                    activeUpdate = null,
                    items = st.items.map { item ->
                        if (item.track.packageName == update.packageName) {
                            item.copy(downloadProgress = 0f)
                        } else item
                    },
                )
            }

            try {
                downloadRepository.observe(id).collect { state ->
                    when (state) {
                        is DownloadState.Running -> {
                            val fraction = if (state.totalBytes != null && state.totalBytes > 0) {
                                (state.downloadedBytes.toFloat() / state.totalBytes)
                                    .coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            updateItem(update.packageName) {
                                it.copy(downloadProgress = fraction)
                            }
                        }

                        is DownloadState.Completed -> {
                            // 下载结束：解除下载占位，允许再次点击
                            downloadingPackages.remove(update.packageName)
                            _uiState.update { st ->
                                st.copy(
                                    activeInstall = PendingInstall(
                                        appLabel = update.appLabel,
                                        versionName = update.newVersion,
                                        apkFile = state.file,
                                    ),
                                    items = st.items.map { item ->
                                        if (item.track.packageName == update.packageName) {
                                            item.copy(downloadProgress = null)
                                        } else item
                                    },
                                )
                            }
                        }

                        is DownloadState.Failed -> {
                            // 下载失败：解除下载占位，保留 UPDATE_AVAILABLE 供重试
                            downloadingPackages.remove(update.packageName)
                            _uiState.update { st ->
                                st.copy(
                                    snackbarMessage = state.reason,
                                    items = st.items.map { item ->
                                        if (item.track.packageName == update.packageName) {
                                            item.copy(
                                                downloadProgress = null,
                                                errorMessage = state.reason,
                                            )
                                        } else item
                                    },
                                )
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                downloadingPackages.remove(update.packageName)
                throw e
            } catch (e: Exception) {
                downloadingPackages.remove(update.packageName)
                _uiState.update {
                    it.copy(snackbarMessage = appContext.getString(R.string.error_generic))
                }
            }
        }
    }

    /** 用户确认安装：检查未知来源授权 → 拉起系统安装器。 */
    fun onInstallConfirmed(pending: PendingInstall) {
        if (!installRepository.isInstallAllowed()) {
            installRepository.openUnknownSourceSettings()
            // 保留对话框，用户授权返回后可再次点击「安装」
            _uiState.update {
                it.copy(
                    snackbarMessage = appContext.getString(R.string.snackbar_unknown_source),
                )
            }
            return
        }
        try {
            installRepository.openInstaller(pending.apkFile)
            _uiState.update { it.copy(activeInstall = null) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    activeInstall = null,
                    snackbarMessage = appContext.getString(R.string.error_install_failed),
                )
            }
        }
    }

    fun onDismissUpdateDialog() {
        _uiState.update { it.copy(activeUpdate = null) }
    }

    fun onDismissInstallDialog() {
        _uiState.update { it.copy(activeInstall = null) }
    }

    /** R11：跳过此版本，写入 skippedTag，此后自动检测不再提示该 tag（可点卡片恢复提醒）。 */
    fun onSkipVersion(update: PendingUpdate) {
        viewModelScope.launch {
            val track = _uiState.value.items
                .firstOrNull { it.track.packageName == update.packageName }
                ?.track
                ?: return@launch
            val updated = track.copy(
                skippedTag = update.newVersion,
                lastCheckTime = System.currentTimeMillis(),
            )
            trackingRepository.update(updated)
            _uiState.update { st ->
                st.copy(
                    activeUpdate = null,
                    snackbarMessage = appContext.getString(R.string.snackbar_skipped, update.newVersion),
                    items = st.items.map { item ->
                        if (item.track.packageName == update.packageName) {
                            item.copy(
                                track = updated,
                                status = UpdateStatus.SKIPPED,
                                latestTag = update.newVersion,
                            )
                        } else item
                    },
                )
            }
        }
    }

    /** R7：删除追踪项。 */
    fun onDelete(packageName: String) {
        viewModelScope.launch {
            trackingRepository.remove(packageName)
            _uiState.update { st ->
                st.copy(
                    snackbarMessage = appContext.getString(R.string.snackbar_deleted),
                    activeUpdate = st.activeUpdate?.takeIf { it.packageName != packageName },
                    items = st.items.filterNot { it.track.packageName == packageName },
                )
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun updateItem(packageName: String, transform: (TrackedItemUiState) -> TrackedItemUiState) {
        _uiState.update { st ->
            st.copy(
                items = st.items.map { item ->
                    if (item.track.packageName == packageName) transform(item) else item
                },
            )
        }
    }

    private fun TrackedItemUiState.toPendingUpdate(): PendingUpdate? {
        val tag = latestTag ?: return null
        val url = downloadUrl ?: return null
        return PendingUpdate(
            packageName = track.packageName,
            appLabel = track.appLabel,
            currentVersion = track.localVersionName,
            newVersion = tag,
            notes = releaseNotes,
            downloadUrl = url,
            fileName = fileName ?: "$tag.apk",
        )
    }

    companion object {
        /** 构造 VM 工厂（经 AppContainer 注入依赖）。 */
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    TrackingListViewModel(
                        appContext = container.appContext,
                        trackingRepository = container.trackingRepository,
                        appRepository = container.appRepository,
                        releaseRepository = container.releaseRepository,
                        downloadRepository = container.downloadRepository,
                        installRepository = container.installRepository,
                    )
                }
            }
    }
}
