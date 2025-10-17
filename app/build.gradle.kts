plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    kotlin("plugin.serialization") version "2.2.20"
    id("com.gradleup.shadow") version "8.3.5"
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
}

testing {
    suites {
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
    mainClass = "xyz.malefic.RunnerKt"
}

tasks.shadowJar {
    archiveBaseName.set("filame")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}
