package xyz.malefic.filame.`package`

import com.charleskorn.kaml.Yaml
import xyz.malefic.filame.config.ConfigFile
import xyz.malefic.filame.config.FilameConfig
import xyz.malefic.filame.config.PackageBundle
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
     * Check if a specific AUR helper is installed
     */
    fun isAurHelperInstalled(helper: AurHelper) = AurHelperManager.isInstalled(helper)

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
    fun searchPackages(query: String): Result<List<PackageSearchResult>> =
        try {
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

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }

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
    fun installPackage(pkg: PackageBundle): Result<Unit> {
        return try {
            val command =
                if (pkg.source == "aur") {
                    val aurHelper =
                        getAurHelper()
                            ?: return Result.failure(
                                Exception("An AUR helper (yay or paru) is required for AUR packages but not installed"),
                            )
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

            if (exitCode != 0) {
                Result.failure(Exception("Failed to install package ${pkg.name}"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Install all tracked packages that are not yet installed
     */
    fun installMissingPackages(): Result<List<String>> {
        val installed = mutableListOf<String>()
        val statuses = getPackageStatuses()

        for ((pkg, isInstalled) in statuses) {
            if (!isInstalled) {
                val result = installPackage(pkg)
                if (result.isSuccess) {
                    installed.add(pkg.name)
                } else {
                    return Result.failure(result.exceptionOrNull()!!)
                }
            }
        }

        return Result.success(installed)
    }

    /**
     * Apply configuration files for a package bundle
     */
    fun applyPackageConfig(bundle: PackageBundle): Result<List<String>> {
        val appliedFiles = mutableListOf<String>()
        try {
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
            return Result.success(appliedFiles)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Export configuration files for a package bundle to repo
     */
    fun exportPackageConfig(bundle: PackageBundle): Result<List<String>> {
        val exportedFiles = mutableListOf<String>()
        try {
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
            return Result.success(exportedFiles)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Export package bundle metadata to repo as package.yaml
     * This enables two-way sync of package configurations
     */
    fun exportPackageMetadata(bundle: PackageBundle): Result<String> =
        try {
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

            Result.success(metadataFile.relativeTo(repoDir).path)
        } catch (e: Exception) {
            Result.failure(e)
        }

    /**
     * Scan repository directory for package bundles
     * Looks for directories with package metadata and config files
     */
    fun scanRepoForPackages(): Result<List<PackageBundle>> {
        return try {
            val bundles = mutableListOf<PackageBundle>()

            if (!repoDir.exists()) {
                return Result.success(emptyList())
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

            Result.success(bundles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update all installed packages
     */
    fun updatePackages(): Result<Unit> {
        return try {
            // Update official packages
            val pacmanProcess =
                ProcessBuilder("sudo", "pacman", "-Syu", "--noconfirm")
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()

            if (pacmanProcess.waitFor() != 0) {
                return Result.failure(Exception("Failed to update official packages"))
            }

            // Update AUR packages if yay or paru is installed
            val aurHelper = getAurHelper()
            if (aurHelper != null) {
                val aurProcess =
                    ProcessBuilder(aurHelper.command, "-Sua", "--noconfirm")
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start()

                if (aurProcess.waitFor() != 0) {
                    return Result.failure(Exception("Failed to update AUR packages"))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove a package
     */
    fun removePackage(packageName: String): Result<Unit> =
        try {
            val process =
                ProcessBuilder("sudo", "pacman", "-R", "--noconfirm", packageName)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                Result.failure(Exception("Failed to remove package $packageName"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
