package com.example.githubupdater

import com.example.githubupdater.util.GitHubRepoParser
import com.example.githubupdater.util.GitHubRepoParser.RepoParseError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GitHubRepoParser 单元测试。
 */
class GitHubRepoParserTest {

    // —— owner/repo 简写 ——
    @Test
    fun parseSimpleOwnerRepo() {
        val r = GitHubRepoParser.parse("ReVanced/revanced-manager")
        assertTrue(r.isSuccess)
        assertNull(r.error)
        assertEquals("ReVanced", r.owner)
        assertEquals("revanced-manager", r.repo)
    }

    @Test
    fun parseSimpleOwnerRepoTrimsWhitespace() {
        val r = GitHubRepoParser.parse("  Owner/Repo  ")
        assertTrue(r.isSuccess)
        assertEquals("Owner", r.owner)
        assertEquals("Repo", r.repo)
    }

    // —— 完整 URL ——
    @Test
    fun parseFullHttpsUrl() {
        val r = GitHubRepoParser.parse("https://github.com/ReVanced/revanced-manager")
        assertTrue(r.isSuccess)
        assertEquals("ReVanced", r.owner)
        assertEquals("revanced-manager", r.repo)
    }

    @Test
    fun parseUrlWithTrailingSlash() {
        val r = GitHubRepoParser.parse("https://github.com/owner/repo/")
        assertTrue(r.isSuccess)
        assertEquals("owner", r.owner)
        assertEquals("repo", r.repo)
    }

    @Test
    fun parseUrlWithGitSuffix() {
        val r = GitHubRepoParser.parse("https://github.com/owner/repo.git")
        assertTrue(r.isSuccess)
        assertEquals("owner", r.owner)
        assertEquals("repo", r.repo)
    }

    @Test
    fun parseHttpWwwHost() {
        val r = GitHubRepoParser.parse("http://www.github.com/owner/repo")
        assertTrue(r.isSuccess)
        assertEquals("owner", r.owner)
        assertEquals("repo", r.repo)
    }

    @Test
    fun parseSchemeLessGithubUrl() {
        val r = GitHubRepoParser.parse("github.com/owner/repo")
        assertTrue(r.isSuccess)
        assertEquals("owner", r.owner)
        assertEquals("repo", r.repo)
    }

    // —— 非法输入 ——
    @Test
    fun parseEmptyInputReturnsEmptyError() {
        val r = GitHubRepoParser.parse("")
        assertFalse(r.isSuccess)
        assertEquals(RepoParseError.EMPTY, r.error)

        val blank = GitHubRepoParser.parse("   ")
        assertEquals(RepoParseError.EMPTY, blank.error)

        val nullInput = GitHubRepoParser.parse(null)
        assertEquals(RepoParseError.EMPTY, nullInput.error)
    }

    @Test
    fun parseNonGithubHostRejected() {
        val r = GitHubRepoParser.parse("https://gitlab.com/owner/repo")
        assertFalse(r.isSuccess)
        assertEquals(RepoParseError.NOT_GITHUB, r.error)
    }

    @Test
    fun parseOwnerOnlyRejected() {
        val r = GitHubRepoParser.parse("owner")
        assertFalse(r.isSuccess)
        assertEquals(RepoParseError.INVALID_FORMAT, r.error)
    }

    @Test
    fun parseShorthandWithExtraPathRejected() {
        val r = GitHubRepoParser.parse("owner/repo/extra")
        assertFalse(r.isSuccess)
        assertEquals(RepoParseError.INVALID_FORMAT, r.error)
    }

    @Test
    fun parseInvalidRepoCharsRejected() {
        val r = GitHubRepoParser.parse("owner/re po")
        assertFalse(r.isSuccess)
        assertEquals(RepoParseError.INVALID_FORMAT, r.error)
    }
}
