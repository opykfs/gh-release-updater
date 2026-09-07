package com.example.githubupdater.util

/**
 * GitHub 仓库地址解析/校验。
 * 支持输入：owner/repo、完整链接 https://github.com/owner/repo[/…]、
 * 无协议 github.com/owner/repo、http(s)://www.github.com/owner/repo 等。
 */
object GitHubRepoParser {

    /** 解析失败原因（便于上层映射文案，保持本对象纯逻辑、无 Android 依赖）。 */
    enum class RepoParseError {
        /** 输入为空 */
        EMPTY,

        /** 是 URL 但主机不是 github.com */
        NOT_GITHUB,

        /** 无法解析出合法的 owner/repo */
        INVALID_FORMAT,
    }

    data class ParseResult(
        val owner: String = "",
        val repo: String = "",
        val error: RepoParseError? = null,
    ) {
        val isSuccess: Boolean get() = error == null
    }

    // GitHub owner：字母/数字/中划线，不以中划线开头/结尾
    private val OWNER_REGEX =
        Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$")
    // GitHub repo：字母/数字/中划线/下划线/点，不以点开头
    private val REPO_REGEX = Regex("^[A-Za-z0-9](?:[A-Za-z0-9._-]{0,99}[A-Za-z0-9])?$")

    private const val GITHUB_HOST = "github.com"
    private const val WWW_GITHUB_HOST = "www.github.com"

    /** 解析输入，成功时 error=null；失败时给出错误码。 */
    fun parse(input: String?): ParseResult {
        val raw = input?.trim().orEmpty()
        if (raw.isEmpty()) return ParseResult(error = RepoParseError.EMPTY)

        var isUrl = false
        var rest = raw

        // 去掉协议前缀
        if (rest.startsWith("https://", ignoreCase = true) ||
            rest.startsWith("http://", ignoreCase = true)
        ) {
            isUrl = true
            rest = rest.substring(rest.indexOf("://") + 3)
        }

        // 识别 github 主机（含 www. 前缀，保留原大小写用于后续取路径）
        val lower = rest.lowercase()
        val hostPrefix = when {
            lower.startsWith(WWW_GITHUB_HOST) -> WWW_GITHUB_HOST
            lower.startsWith(GITHUB_HOST) -> GITHUB_HOST
            else -> null
        }
        if (hostPrefix != null) {
            isUrl = true
            rest = rest.substring(hostPrefix.length).removePrefix("/")
        } else if (isUrl) {
            // 带协议但不是 github.com
            return ParseResult(error = RepoParseError.NOT_GITHUB)
        }

        // 去掉查询/锚点/尾部斜杠
        val clean = rest.trimEnd('/')
            .substringBefore('?')
            .substringBefore('#')

        val segments = clean.split('/').filter { it.isNotEmpty() }

        if (segments.isEmpty() || segments[0].isEmpty()) {
            return ParseResult(error = RepoParseError.INVALID_FORMAT)
        }

        if (!isUrl && segments.size != 2) {
            // 简写形式必须恰好 owner/repo
            return ParseResult(error = RepoParseError.INVALID_FORMAT)
        }
        if (segments.size < 2) {
            return ParseResult(error = RepoParseError.INVALID_FORMAT)
        }

        val owner = segments[0].removePrefix("@")
        val repo = segments[1].removeSuffix(".git")

        if (owner.isEmpty() || repo.isEmpty() ||
            !OWNER_REGEX.matches(owner) || !REPO_REGEX.matches(repo)
        ) {
            return ParseResult(error = RepoParseError.INVALID_FORMAT)
        }
        return ParseResult(owner = owner, repo = repo)
    }
}
