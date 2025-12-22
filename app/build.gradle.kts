plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
    kotlin("plugin.serialization") version libs.versions.kotlin
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotter)
    implementation(libs.jgit)
    implementation(libs.serialization)
    implementation(libs.kaml)
    implementation(libs.coroutines)
    implementation(libs.bundles.arrow)
}

testing {
    suites {
        @Suppress("unused")
        val test by getting(JvmTestSuite::class) {
            useKotlinTest(libs.versions.kotlin)
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "xyz.malefic.filame.MainKt"
}

tasks.shadowJar {
    archiveBaseName.set("filame")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}
