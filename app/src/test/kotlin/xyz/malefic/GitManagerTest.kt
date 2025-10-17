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
                packageBundles = emptyList(),
            )

        val gitManager = GitManager(config)
        val repoDir = gitManager.getRepoDir()
        assertTrue(repoDir.path.contains(".config/filame/repo"))
    }

    @Test
    fun testInitializeRepoWithoutGithubUrl() {
        val config =
            FilameConfig(
                deviceName = "test-device",
                githubRepo = "",
                packageBundles = emptyList(),
            )

        val gitManager = GitManager(config)
        val result = gitManager.initializeRepo()
        assertTrue(result.isFailure)
    }

    @Test
    fun testGitManagerWithPackageBundles() {
        val config =
            FilameConfig(
                deviceName = "test-device",
                githubRepo = "https://github.com/test/repo.git",
                packageBundles = listOf(
                    PackageBundle(
                        name = "vim",
                        source = "official",
                        description = "Text editor",
                        configFiles = listOf(
                            ConfigFile(
                                sourcePath = "/home/user/.vimrc",
                                destinationPath = "vim/.vimrc",
                                description = "Vim config"
                            )
                        )
                    )
                )
            )

        val gitManager = GitManager(config)
        val repoDir = gitManager.getRepoDir()
        assertTrue(repoDir.path.contains(".config/filame/repo"))
    }
}
