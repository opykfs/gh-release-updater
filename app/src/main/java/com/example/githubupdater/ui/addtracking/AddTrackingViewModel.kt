package com.example.githubupdater.ui.addtracking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.githubupdater.R
import com.example.githubupdater.di.AppContainer
import com.example.githubupdater.domain.model.InstalledApp
import com.example.githubupdater.domain.model.TrackedApp
import com.example.githubupdater.domain.repo.AppRepository
import com.example.githubupdater.domain.repo.TrackingRepository
import com.example.githubupdater.util.GitHubRepoParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 添加追踪 ViewModel：加载已装应用、搜索过滤、仓库校验、保存去重。
 */
class AddTrackingViewModel(
    private val appContext: Context,
    private val appRepository: AppRepository,
    private val trackingRepository: TrackingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTrackingUiState())
    val uiState: StateFlow<AddTrackingUiState> = _uiState.asStateFlow()

    /** 按 query 过滤后的可见应用列表。 */
    val visibleApps: StateFlow<List<InstalledApp>> = _uiState
        .map { s ->
            val q = s.query.trim()
            if (q.isEmpty()) {
                s.apps
            } else {
                s.apps.filter { app ->
                    app.appLabel.contains(q, ignoreCase = true) ||
                        app.packageName.contains(q, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadInstalledApps()
        observeTracked()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true) }
            val apps = runCatching { appRepository.getInstalledApps() }
                .getOrDefault(emptyList())
            _uiState.update { it.copy(apps = apps, isLoadingApps = false) }
        }
    }

    private fun observeTracked() {
        viewModelScope.launch {
            trackingRepository.tracks.collect { list ->
                _uiState.update {
                    it.copy(trackedPackageNames = list.map { t -> t.packageName }.toSet())
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onSelectApp(packageName: String) {
        // 已追踪项不允许重复选择
        if (_uiState.value.trackedPackageNames.contains(packageName)) return
        _uiState.update {
            it.copy(
                selectedPackageName = packageName,
                repoError = null,
            )
        }
    }

    fun onRepoInputChange(input: String) {
        _uiState.update {
            it.copy(
                repoInput = input,
                repoError = null,
            )
        }
    }

    /** 保存：校验应用已选 + 仓库格式，通过则 add，成功后回调返回列表。 */
    fun save(onSaved: () -> Unit) {
        val st = _uiState.value

        val selectedPkg = st.selectedPackageName
        if (selectedPkg == null) {
            _uiState.update {
                it.copy(repoError = appContext.getString(R.string.error_select_app))
            }
            return
        }
        if (st.trackedPackageNames.contains(selectedPkg)) {
            _uiState.update {
                it.copy(repoError = appContext.getString(R.string.error_already_tracked))
            }
            return
        }

        val parsed = GitHubRepoParser.parse(st.repoInput)
        if (!parsed.isSuccess) {
            val error = when (parsed.error) {
                GitHubRepoParser.RepoParseError.EMPTY ->
                    appContext.getString(R.string.error_repo_empty)

                GitHubRepoParser.RepoParseError.NOT_GITHUB ->
                    appContext.getString(R.string.error_repo_not_github)

                GitHubRepoParser.RepoParseError.INVALID_FORMAT ->
                    appContext.getString(R.string.error_repo_invalid)

                null -> appContext.getString(R.string.error_repo_invalid)
            }
            _uiState.update { it.copy(repoError = error) }
            return
        }

        val app = st.apps.firstOrNull { it.packageName == selectedPkg }
        if (app == null) {
            _uiState.update {
                it.copy(repoError = appContext.getString(R.string.error_select_app))
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, repoError = null) }
        viewModelScope.launch {
            val track = TrackedApp(
                packageName = app.packageName,
                repoOwner = parsed.owner,
                repoName = parsed.repo,
                appLabel = app.appLabel,
                localVersionName = app.versionName,
                localVersionCode = app.versionCode,
                addedAt = System.currentTimeMillis(),
            )
            val result = trackingRepository.add(track)
            result.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                onSaved()
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        repoError = appContext.getString(R.string.error_already_tracked),
                    )
                }
            }
        }
    }

    companion object {
        /** 构造 VM 工厂。 */
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AddTrackingViewModel(
                        appContext = container.appContext,
                        appRepository = container.appRepository,
                        trackingRepository = container.trackingRepository,
                    )
                }
            }
    }
}
