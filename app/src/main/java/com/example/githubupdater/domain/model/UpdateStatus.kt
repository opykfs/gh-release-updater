package com.example.githubupdater.domain.model

/**
 * 单个追踪项的检测状态。
 */
enum class UpdateStatus {
    /** 尚未检测 */
    IDLE,

    /** 检测中 */
    CHECKING,

    /** 已是最新 */
    UP_TO_DATE,

    /** 有新版本可更新 */
    UPDATE_AVAILABLE,

    /**
     * 远程存在更高版本，但用户曾选择「跳过此版本」。
     * 与 UP_TO_DATE 不同：本状态不代表已是最新，仅表示该 tag 被用户忽略；
     * 点击卡片可清除跳过标记并恢复更新提醒。
     */
    SKIPPED,

    /** 检测失败（网络/仓库不存在/限流等），errorMessage 记录原因 */
    FAILED,

    /**
     * 仓库请求成功但 Release 无 .apk 安装包。
     * 不是失败，按中性提示展示（架构风险表「WARN 处理」的落地）。
     */
    NO_APK,
}
