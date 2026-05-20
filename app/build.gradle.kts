plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)   // Navigation type-safe routes
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// ── Version automatique depuis le tag Git (ex: v1.2.3 → code=10203) ────────
val gitVersionCode: Int by lazy {
    val tag = System.getenv("GITHUB_REF_NAME") ?: ""
    val match = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""").find(tag)
    if (match != null) {
        val (major, minor, patch) = match.destructured
        major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()
    } else 1
}

val gitVersionName: String by lazy {
    System.getenv("GITHUB_REF_NAME")?.removePrefix("v") ?: "dev"
}

android {
    namespace  = "com.bridgeflowfolk.bff"
    compileSdk = 35

    defaultConfig {
        applicationId   = "com.bridgeflowfolk.bff"
        minSdk          = 26       // Android 8.0 Oreo
        targetSdk       = 35
        versionCode     = gitVersionCode
        versionName     = gitVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath  = System.getenv("KEYSTORE_PATH")
            val keystorePass  = System.getenv("KEYSTORE_PASSWORD")
            val keyAliasName  = System.getenv("KEY_ALIAS")
            val keyPass       = System.getenv("KEY_PASSWORD")

            // Ne configure la signature que si toutes les vars sont présentes.
            // Chemin absolu attendu (ex. /tmp/keystore.jks) → pas d'ambiguïté avec file().
            if (!keystorePath.isNullOrBlank() && !keystorePass.isNullOrBlank()
                && !keyAliasName.isNullOrBlank() && !keyPass.isNullOrBlank()) {
                storeFile     = file(keystorePath)
                storePassword = keystorePass
                keyAlias      = keyAliasName
                keyPassword   = keyPass
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable        = true
        }
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            signingConfig     = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // lifecycle-runtime-compose : fournit collectAsStateWithLifecycle()
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation + serialization pour les routes type-safe
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Coil — maintenu sur 2.x (Coil 3 = breaking change de package)
    implementation(libs.coil.compose)

    // WorkManager
    implementation(libs.workmanager)
    implementation(libs.hilt.workmanager)
    ksp(libs.hilt.workmanager.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // DataStore
    implementation(libs.datastore.preferences)
}
