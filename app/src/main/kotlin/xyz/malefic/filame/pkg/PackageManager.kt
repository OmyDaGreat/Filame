package xyz.malefic.filame.pkg

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.charleskorn.kaml.Yaml
import xyz.malefic.filame.config.ConfigFile
import xyz.malefic.filame.config.FilameConfig
import xyz.malefic.filame.config.PackageBundle
import xyz.malefic.filame.git.GitError
import xyz.malefic.filame.git.GitManager
import xyz.malefic.filame.git.prepareGitRepo
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Manages package operations including installation, removal, and tracking
 */
class PackageManager(
    private val config: FilameConfig,
    private val repoDir: File = File(System.getProperty("user.home"), ".config/filame/repo"),
) {
    /**
     * Check if any AUR helper is installed
     */
    fun isAurHelperInstalled() = AurHelperManager.isAnyInstalled()

    /**
     * Get the installed AUR helper (yay takes priority over paru), or null if neither is installed
     */
    fun getAurHelper() = AurHelperManager.getInstalled()

    /**
     * Install an AUR helper
     */
    fun installAurHelper(helper: AurHelper) = AurHelperManager.install(helper)

    /**
     * Search for packages in official repos and AUR
     */
    fun searchPackages(query: String): Either<String, List<PackageSearchResult>> =
        either<Throwable, List<PackageSearchResult>> {
            val results = mutableListOf<PackageSearchResult>()

            // Search official repos with pacman
            val pacmanProcess =
                ProcessBuilder("pacman", "-Ss", query)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()

            val pacmanReader = BufferedReader(InputStreamReader(pacmanProcess.inputStream))
            var line: String?
            var currentPackage: String? = null

            while (pacmanReader.readLine().also { line = it } != null) {
                if (line!!.startsWith(" ")) {
                    // This is a description line
                    if (currentPackage != null) {
                        results.add(PackageSearchResult(currentPackage, "official", line.trim()))
                        currentPackage = null
                    }
                } else {
                    // This is a package line
                    val parts = line.split("/")
                    if (parts.size >= 2) {
                        val packageInfo = parts[1].split(" ")
                        currentPackage = packageInfo[0]
                    }
                }
            }
            pacmanProcess.waitFor()

            // Search AUR with yay or paru if available
            val aurHelper = getAurHelper()
            if (aurHelper != null) {
                val aurProcess =
                    ProcessBuilder(aurHelper.command, "-Ssa", query)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start()

                val aurReader = BufferedReader(InputStreamReader(aurProcess.inputStream))
                currentPackage = null

                while (aurReader.readLine().also { line = it } != null) {
                    if (line!!.startsWith(" ")) {
                        // This is a description line
                        if (currentPackage != null) {
                            // Only add if not already in results from pacman
                            if (!results.any { it.name == currentPackage }) {
                                results.add(PackageSearchResult(currentPackage, "aur", line.trim()))
                            }
                            currentPackage = null
                        }
                    } else {
                        // This is a package line
                        val parts = line.split("/")
                        if (parts.size >= 2) {
                            val packageInfo = parts[1].split(" ")
                            currentPackage = packageInfo[0]
                        }
                    }
                }
                aurProcess.waitFor()
            }

            results
        }.mapLeft { it.message ?: "Unknown error" }

    /**
     * Check if a package is installed
     */
    fun isPackageInstalled(packageName: String): Boolean =
        try {
            val process = ProcessBuilder("pacman", "-Qq", packageName).start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }

    /**
     * Get installation status of all tracked packages
     */
    fun getPackageStatuses(): Map<PackageBundle, Boolean> =
        config.packageBundles.associateWith { pkg ->
            isPackageInstalled(pkg.name)
        }

    /**
     * Install a package
     */
    fun installPackage(pkg: PackageBundle): Either<String, Unit> =
        either<Throwable, Unit> {
            val command =
                if (pkg.source == "aur") {
                    val aurHelper = getAurHelper()
                    ensure(aurHelper != null) {
                        raise(Exception("An AUR helper (yay or paru) is required for AUR packages but not installed"))
                    }
                    arrayOf(aurHelper.command, "-S", "--noconfirm", pkg.name)
                } else {
                    arrayOf("sudo", "pacman", "-S", "--noconfirm", pkg.name)
                }

            val process =
                ProcessBuilder(*command)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()

            val exitCode = process.waitFor()

            ensure(exitCode == 0) {
                raise(Exception("Failed to install package ${pkg.name}"))
            }
        }.mapLeft { it.message ?: "Unknown error" }

    /**
     * Install all tracked packages that are not yet installed
     */
    fun installMissingPackages(): Either<String, List<String>> =
        either {
            val installed = mutableListOf<String>()
            val statuses = getPackageStatuses()

            for ((pkg, isInstalled) in statuses) {
                if (!isInstalled) {
                    installPackage(pkg).bind()
                    installed.add(pkg.name)
                }
            }

            installed
        }

    /**
     * Apply configuration files for a package bundle
     */
    fun applyPackageConfig(bundle: PackageBundle): Either<String, List<String>> =
        either<Throwable, List<String>> {
            val appliedFiles = mutableListOf<String>()

            for (configFile in bundle.configFiles) {
                val source = File(repoDir, configFile.destinationPath)
                if (!source.exists()) {
                    continue
                }

                val destination = File(configFile.sourcePath)
                destination.parentFile?.mkdirs()

                Files.copy(
                    source.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
                appliedFiles.add(configFile.sourcePath)
            }

            appliedFiles
        }.mapLeft { it.message ?: "Unknown error" }

    /**
     * Export configuration files for a package bundle to repo
     */
    fun exportPackageConfig(bundle: PackageBundle): Either<String, List<String>> =
        either<Throwable, List<String>> {
            val exportedFiles = mutableListOf<String>()

            for (configFile in bundle.configFiles) {
                val source = File(configFile.sourcePath)
                if (!source.exists()) {
                    continue
                }

                val destination = File(repoDir, configFile.destinationPath)
                destination.parentFile?.mkdirs()

                Files.copy(
                    source.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
                exportedFiles.add(configFile.destinationPath)
            }

            exportedFiles
        }.mapLeft { it.message ?: "Unknown error" }

    /**
     * Export package bundle metadata to repo as package.yaml
     * This enables two-way sync of package configurations
     */
    fun exportPackageMetadata(bundle: PackageBundle): Either<String, String> =
        either<Throwable, String> {
            // Determine the package directory name from config files or use the package name
            val packageDirName =
                if (bundle.configFiles.isNotEmpty()) {
                    // Extract directory from first config file destination path
                    val firstDestPath = bundle.configFiles[0].destinationPath
                    firstDestPath.substringBefore("/")
                } else {
                    bundle.name
                }

            val packageDir = File(repoDir, packageDirName)
            packageDir.mkdirs()

            val metadataFile = File(packageDir, "package.yaml")
            val yaml =
                Yaml.default.encodeToString(
                    PackageBundle.serializer(),
                    bundle,
                )
            metadataFile.writeText(yaml)

            metadataFile.relativeTo(repoDir).path
        }.mapLeft { it.message ?: "Unknown error" }

    /**
     * High-level: export bundle's config files + metadata and push to remote.
     */
    fun exportBundleAndPush(
        bundle: PackageBundle,
        commitMessage: String,
        credentialProvider: (() -> Pair<String, String>?)? = null,
        saveCredentialsIfUsed: Boolean = true,
    ): Either<GitError, Unit> =
        either {
            val (gitManager, git) = config.prepareGitRepo().bind()
            try {
                exportPackageConfig(bundle).mapLeft { GitError.IoError(it) }.bind()
                exportPackageMetadata(bundle).mapLeft { GitError.IoError(it) }.bind()
                gitManager.pushWithCommitAndRetry(git, commitMessage, credentialProvider, saveCredentialsIfUsed).bind()
            } finally {
                try {
                    git.close()
                } catch (_: Exception) {
                }
            }
        }

    /**
     * Export all tracked package configs / metadata and push as a single commit.
     * Returns a Pair(totalConfigFilesExported, metadataBundlesExported) on success.
     */
    fun exportAllAndPush(
        commitMessage: String,
        credentialProvider: (() -> Pair<String, String>?)? = null,
        saveCredentialsIfUsed: Boolean = true,
    ): Either<GitError, Pair<Int, Int>> =
        either {
            val (_, git) = config.prepareGitRepo().bind()
            var totalExported = 0
            var metadataExported = 0
            try {
                for (bundle in config.packageBundles) {
                    val exported = exportPackageConfig(bundle).mapLeft { GitError.IoError(it) }.bind()
                    totalExported += exported.size

                    exportPackageMetadata(bundle).mapLeft { GitError.IoError(it) }.bind()
                    metadataExported++
                }

                GitManager(config).pushWithCommitAndRetry(git, commitMessage, credentialProvider, saveCredentialsIfUsed).bind()

                totalExported to metadataExported
            } finally {
                try {
                    git.close()
                } catch (_: Exception) {
                }
            }
        }

    /**
     * Export a single bundle (configs + metadata), push to remote, and return the metadata file path relative to the repo.
     * This mirrors exportBundleAndPush but returns the metadata path for UI consumption.
     */
    fun exportBundleAndPushWithMetadata(
        bundle: PackageBundle,
        commitMessage: String,
        credentialProvider: (() -> Pair<String, String>?)? = null,
        saveCredentialsIfUsed: Boolean = true,
    ): Either<GitError, String> =
        either {
            val (gitManager, git) = config.prepareGitRepo().bind()
            try {
                exportPackageConfig(bundle).mapLeft { GitError.IoError(it) }.bind()
                val metadataPath = exportPackageMetadata(bundle).mapLeft { GitError.IoError(it) }.bind()
                gitManager.pushWithCommitAndRetry(git, commitMessage, credentialProvider, saveCredentialsIfUsed).bind()
                metadataPath
            } finally {
                try {
                    git.close()
                } catch (_: Exception) {
                }
            }
        }

    /**
     * Scan repository directory for package bundles
     * Looks for directories with package metadata and config files
     */
    fun scanRepoForPackages(): Either<String, List<PackageBundle>> =
        either<Throwable, List<PackageBundle>> {
            val bundles = mutableListOf<PackageBundle>()

            if (!repoDir.exists()) {
                return@either emptyList()
            }

            // Look for package directories (each subdirectory is a potential package)
            repoDir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.forEach { packageDir ->
                val metadataFile = File(packageDir, "package.yaml")

                if (metadataFile.exists()) {
                    // Parse package metadata
                    try {
                        val yaml = metadataFile.readText()
                        val bundle =
                            Yaml.default.decodeFromString(
                                PackageBundle.serializer(),
                                yaml,
                            )
                        bundles.add(bundle)
                    } catch (_: Exception) {
                        // Skip packages with invalid metadata
                    }
                } else {
                    // Create a basic package bundle from directory structure
                    val configFiles =
                        packageDir
                            .walkTopDown()
                            .filter { it.isFile && !it.name.startsWith(".") }
                            .map { file ->
                                val relativePath = file.relativeTo(packageDir).path
                                val userHome = System.getProperty("user.home")
                                ConfigFile(
                                    sourcePath = "$userHome/.config/${packageDir.name}/$relativePath",
                                    destinationPath = "${packageDir.name}/$relativePath",
                                    description = "",
                                )
                            }.toList()

                    if (configFiles.isNotEmpty()) {
                        bundles.add(
                            PackageBundle(
                                name = packageDir.name,
                                source = "official",
                                description = "",
                                configFiles = configFiles,
                            ),
                        )
                    }
                }
            }

            bundles
        }.mapLeft { it.message ?: "Unknown error" }

    /**
     * Update all installed packages
     */
    fun updatePackages(): Either<String, Unit> =
        either<Throwable, Unit> {
            // Update official packages
            val pacmanProcess =
                ProcessBuilder("sudo", "pacman", "-Syu", "--noconfirm")
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()

            ensure(pacmanProcess.waitFor() == 0) {
                raise(Exception("Failed to update official packages"))
            }

            // Update AUR packages if yay or paru is installed
            val aurHelper = getAurHelper()
            if (aurHelper != null) {
                val aurProcess =
                    ProcessBuilder(aurHelper.command, "-Sua", "--noconfirm")
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start()

                ensure(aurProcess.waitFor() == 0) {
                    raise(Exception("Failed to update AUR packages"))
                }
            }
        }.mapLeft { it.message ?: "Unknown error" }

    /**
     * Remove a package
     */
    fun removePackage(packageName: String): Either<String, Unit> =
        either<Throwable, Unit> {
            val process =
                ProcessBuilder("sudo", "pacman", "-R", "--noconfirm", packageName)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()

            val exitCode = process.waitFor()

            ensure(exitCode == 0) {
                raise(Exception("Failed to remove package $packageName"))
            }
        }.mapLeft { it.message ?: "Unknown error" }
}
