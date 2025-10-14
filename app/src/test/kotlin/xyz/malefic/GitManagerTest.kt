package xyz.malefic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitManagerTest {
    @Test
    fun testGitManagerCreation() {
        val config =
            FilameConfig(
                deviceName = "test-device",
                githubRepo = "https://github.com/test/repo.git",
                configFiles = emptyList(),
            )

        val gitManager = GitManager(config)
        val repoDir = gitManager.getRepoDir()
        assertTrue(repoDir.path.contains(".config/filame/repo"))
    }

    @Test
    fun testExportConfigsWithEmptyList() {
        val config =
            FilameConfig(
                deviceName = "test-device",
                githubRepo = "https://github.com/test/repo.git",
                configFiles = emptyList(),
            )

        val gitManager = GitManager(config)
        val result = gitManager.exportConfigs()
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun testImportConfigsWithEmptyList() {
        val config =
            FilameConfig(
                deviceName = "test-device",
                githubRepo = "https://github.com/test/repo.git",
                configFiles = emptyList(),
            )

        val gitManager = GitManager(config)
        val result = gitManager.importConfigs()
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun testInitializeRepoWithoutGithubUrl() {
        val config =
            FilameConfig(
                deviceName = "test-device",
                githubRepo = "",
                configFiles = emptyList(),
            )

        val gitManager = GitManager(config)
        val result = gitManager.initializeRepo()
        assertTrue(result.isFailure)
    }

    @Test
    fun testExportConfigsWithNonExistentFiles() {
        val testDir = File(System.getProperty("java.io.tmpdir"), "filame-test-${System.currentTimeMillis()}")
        testDir.mkdirs()

        val config =
            FilameConfig(
                deviceName = "test-device",
                githubRepo = "https://github.com/test/repo.git",
                configFiles =
                    listOf(
                        ConfigFile(
                            sourcePath = "/nonexistent/file.txt",
                            destinationPath = "file.txt",
                            description = "Test file",
                        ),
                    ),
            )

        val gitManager = GitManager(config)
        val result = gitManager.exportConfigs()
        assertTrue(result.isSuccess)
        // Should skip non-existent files
        assertEquals(0, result.getOrNull()?.size)

        testDir.deleteRecursively()
    }
}
