plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val skikoNativeArm64 by configurations.creating
val skikoNativeX64 by configurations.creating

abstract class UnpackSkikoNativesTask : DefaultTask() {
    @get:InputFiles
    abstract val arm64Files: ConfigurableFileCollection

    @get:InputFiles
    abstract val x64Files: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @TaskAction
    fun unpack() {
        val targetDir = outputDir.get().asFile

        fileSystemOperations.sync {
            from(arm64Files.files.map { archiveOperations.zipTree(it) }) {
                include("**/*.so")
                eachFile { path = "arm64-v8a/$name" }
            }
            from(x64Files.files.map { archiveOperations.zipTree(it) }) {
                include("**/*.so")
                eachFile { path = "x86_64/$name" }
            }
            into(targetDir)
            includeEmptyDirs = false
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}

val unpackSkikoNatives = tasks.register<UnpackSkikoNativesTask>("unpackSkikoNatives") {
    arm64Files.from(skikoNativeArm64)
    x64Files.from(skikoNativeX64)
    outputDir.set(layout.buildDirectory.dir("generated/skikoJniLibs"))
}

android {
    namespace = "io.github.numq.kortex.example.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.numq.kortex.example.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(
            unpackSkikoNatives, UnpackSkikoNativesTask::outputDir
        )
    }
}

configurations.configureEach {
    exclude(group = "org.jetbrains.skiko", module = "skiko-awt")
    exclude(group = "org.jetbrains.skiko", module = "skiko-awt-runtime-all")
    exclude(group = "org.jetbrains.skiko", module = "skiko-awt-runtime-windows-x64")
    exclude(group = "org.jetbrains.skiko", module = "skiko-awt-runtime-linux-x64")
    exclude(group = "org.jetbrains.skiko", module = "skiko-awt-runtime-macos-x64")
    exclude(group = "org.jetbrains.skiko", module = "skiko-awt-runtime-macos-arm64")
}

dependencies {
    implementation(projects.example.shared)
    implementation(libs.androidx.activity.compose)

    implementation("org.jetbrains.skiko:skiko-android:${libs.versions.skiko.get()}")

    skikoNativeArm64("org.jetbrains.skiko:skiko-android-runtime-arm64:${libs.versions.skiko.get()}")
    skikoNativeX64("org.jetbrains.skiko:skiko-android-runtime-x64:${libs.versions.skiko.get()}")
}