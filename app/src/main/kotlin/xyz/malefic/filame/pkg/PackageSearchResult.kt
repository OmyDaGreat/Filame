package xyz.malefic.filame.pkg

/**
 * Represents the result of a package search.
 *
 * @property name The name of the package.
 * @property source The source or origin of the package.
 * @property description A brief description of the package.
 */
data class PackageSearchResult(
    val name: String,
    val source: String,
    val description: String,
)
