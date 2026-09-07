package com.example.githubupdater

import com.example.githubupdater.data.VersionComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * VersionComparator 单元测试。
 */
class VersionComparatorTest {

    // —— v 前缀与相等 ——
    @Test
    fun compareVprefixEqualsPlain() {
        assertEquals(0, VersionComparator.compareVersions("v1.2.3", "1.2.3"))
        assertEquals(0, VersionComparator.compareVersions("V1.2.3", "1.2.3"))
    }

    @Test
    fun compareLeadingTrailingSpaces() {
        assertEquals(0, VersionComparator.compareVersions(" 1.2.3 ", "v1.2.3"))
    }

    // —— 基础大小 ——
    @Test
    fun compareOlderLocalReturnsNegative() {
        assertTrue(VersionComparator.compareVersions("1.2.3", "2.0.0") < 0)
    }

    @Test
    fun compareNewerLocalReturnsPositive() {
        assertTrue(VersionComparator.compareVersions("2.0.0", "1.2.3") > 0)
    }

    @Test
    fun compareTagUpdateScenario() {
        assertTrue(VersionComparator.compareVersions("v2.1.0", "1.8.0") > 0)
        assertTrue(VersionComparator.isUpdateAvailable("v2.1.0", "1.8.0"))
    }

    // —— 4 段 ——
    @Test
    fun fourSegmentsGreaterThanThree() {
        assertTrue(VersionComparator.compareVersions("1.2.3.1", "1.2.3") > 0)
        assertTrue(VersionComparator.compareVersions("1.2.3", "1.2.3.1") < 0)
    }

    // —— 预发布后缀 ——
    @Test
    fun prereleaseLowerThanRelease() {
        assertTrue(VersionComparator.compareVersions("1.2.3-beta", "1.2.3") < 0)
        assertTrue(VersionComparator.compareVersions("1.2.3", "1.2.3-beta") > 0)
        assertFalse(VersionComparator.isUpdateAvailable("v1.2.3-beta", "1.2.3"))
    }

    @Test
    fun prereleaseNewerBaseCountsAsUpdate() {
        // 本地 1.2.2 稳定版，远端发布 1.2.3-beta，应视为可更新
        assertTrue(VersionComparator.compareVersions("1.2.3-beta", "1.2.2") > 0)
        assertTrue(VersionComparator.isUpdateAvailable("1.2.3-beta", "1.2.2"))
    }

    @Test
    fun prereleaseIdentifiersComparedNumerically() {
        assertTrue(VersionComparator.compareVersions("1.0.0-beta.2", "1.0.0-beta.10") < 0)
        assertTrue(VersionComparator.compareVersions("1.0.0-alpha", "1.0.0-beta") < 0)
        assertTrue(VersionComparator.compareVersions("1.0.0-rc.1", "1.0.0-rc.1") == 0)
    }

    // —— 空/null 安全 ——
    @Test
    fun emptyAndNullAreSafe() {
        assertEquals(0, VersionComparator.compareVersions("", ""))
        assertEquals(0, VersionComparator.compareVersions(null, null))
        // 空版本视为最低，不抛异常
        assertTrue(VersionComparator.compareVersions("1.0.0", null) > 0)
        assertTrue(VersionComparator.compareVersions(null, "1.0.0") < 0)
        assertFalse(VersionComparator.isUpdateAvailable("", "1.0.0"))
    }

    // —— cleanVersion ——
    @Test
    fun cleanVersionStripsPrefixAndSuffix() {
        assertEquals("1.2.3", VersionComparator.cleanVersion("v1.2.3-beta"))
        assertEquals("1.2.3", VersionComparator.cleanVersion(" v1.2.3+abc "))
        assertEquals("", VersionComparator.cleanVersion(null))
    }
}
