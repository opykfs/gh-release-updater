package com.example.githubupdater.data

/**
 * 轻量语义化版本比较器。
 *
 * 规则：
 * - 容忍大小写 v 前缀、空串/null；
 * - 按点分数字段比较（支持 4 段，如 1.2.3.1）；
 * - 容忍预发布后缀（如 -beta、-rc.1）：数字段相同时，正式版 > 预发布版；
 * - 预发布字段比较遵循 semver：数字标识符 < 字母标识符，集合更长者更大。
 */
object VersionComparator {

    private data class ParsedVersion(
        val numeric: List<Long>,
        val pre: List<String>,
    )

    /**
     * 清理版本号：去空白、去 v 前缀、去预发布/构建元数据后缀。
     * 例：cleanVersion(" v1.2.3-beta+1 ") == "1.2.3"
     */
    fun cleanVersion(input: String?): String {
        val parsed = parse(input)
        if (parsed.numeric.isEmpty()) return ""
        val joined = parsed.numeric.joinToString(".")
        return joined.removeSuffix(".0").ifEmpty { "0" }
    }

    /**
     * 比较两个版本字符串。a > b 返回正数；a < b 返回负数；相等返回 0。
     * null/空白按「空版本」（最低）处理，保证不抛异常。
     */
    fun compareVersions(a: String?, b: String?): Int {
        val pa = parse(a)
        val pb = parse(b)
        val numericLen = maxOf(pa.numeric.size, pb.numeric.size)
        for (i in 0 until numericLen) {
            val x = pa.numeric.getOrNull(i) ?: 0L
            val y = pb.numeric.getOrNull(i) ?: 0L
            if (x != y) return if (x > y) 1 else -1
        }
        val aHasPre = pa.pre.isNotEmpty()
        val bHasPre = pb.pre.isNotEmpty()
        if (aHasPre != bHasPre) {
            // 正式版本（无预发布）优先
            return if (!aHasPre) 1 else -1
        }
        if (!aHasPre) return 0
        return comparePre(pa.pre, pb.pre)
    }

    /** 远端 tag 是否高于本地版本（即存在更新）。 */
    fun isUpdateAvailable(remoteTag: String?, localVersionName: String?): Boolean =
        compareVersions(remoteTag, localVersionName) > 0

    private fun parse(input: String?): ParsedVersion {
        var s = input?.trim() ?: return ParsedVersion(emptyList(), emptyList())
        if (s.startsWith("v", ignoreCase = true)) s = s.substring(1)
        // 去掉构建元数据 +metadata
        val plusIndex = s.indexOf('+')
        if (plusIndex >= 0) s = s.substring(0, plusIndex)
        // 拆出预发布
        val preIndex = s.indexOf('-')
        val basePart = if (preIndex >= 0) s.substring(0, preIndex) else s
        val prePart = if (preIndex >= 0) s.substring(preIndex + 1) else ""

        val numeric = basePart
            .split('.')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toLongOrNull() }

        val pre = prePart
            .split('.')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return ParsedVersion(numeric, pre)
    }

    private fun comparePre(a: List<String>, b: List<String>): Int {
        val len = maxOf(a.size, b.size)
        for (i in 0 until len) {
            val x = a.getOrNull(i)
            val y = b.getOrNull(i)
            if (x == null) return -1 // a 集合更短 -> 更低
            if (y == null) return 1  // b 集合更短 -> 更低
            val xNum = x.toLongOrNull()
            val yNum = y.toLongOrNull()
            if (xNum != null && yNum != null) {
                if (xNum != yNum) return if (xNum > yNum) 1 else -1
            } else if (xNum != null) {
                return -1 // 数字 < 字母
            } else if (yNum != null) {
                return 1
            } else {
                val cmp = x.compareTo(y)
                if (cmp != 0) return if (cmp > 0) 1 else -1
            }
        }
        return 0
    }
}
