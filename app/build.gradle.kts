plugins {
    alias(libs.plugins.android.application)
}

// Git metadata helpers
fun gitExec(args: List<String>, fallback: String): String {
    return try {
        val pb = ProcessBuilder(listOf("git") + args).redirectErrorStream(true)
        val proc = pb.start()
        val out = proc.inputStream.bufferedReader().use { it.readText() }.trim()
        val exitCode = proc.waitFor()
        if (exitCode != 0) {
            logger.warn("Warning: git command exited with non-zero status: $exitCode")
            return fallback
        }
        out.ifEmpty { fallback }
    } catch (e: Exception) {
        logger.warn("Warning: git command failed: ${e.message}")
        fallback
    }
}

fun versionDesc(): String = gitExec(listOf("describe", "--always", "--tags", "--dirty"), "No version info")
fun gitHash(): String = gitExec(listOf("show", "--no-patch", "--pretty=format:%H"), "No git hash info")
fun gitDate(): String = gitExec(listOf("show", "--no-patch", "--pretty=format:%ai"), "No git date info")

android {
    namespace = "io.github.yappy.annplayer"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "io.github.yappy.annplayer"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        // versionName is derived from git describe
        versionName = versionDesc()

        // Expose git info to BuildConfig
        buildConfigField("String", "GIT_HASH", "\"${gitHash()}\"")
        buildConfigField("String", "GIT_DATE", "\"${gitDate()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../sign/dummy_keystore.jks")
            storePassword = "dummypass"
            keyAlias = "dummy_key"
            keyPassword = "dummypass"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        // Enable generation of BuildConfig fields
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
