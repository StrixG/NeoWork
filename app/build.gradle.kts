import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.hilt)
}

val secrets = Properties().apply {
    load(rootProject.file("secrets.properties").inputStream())
}

android {
    namespace = "com.obrekht.neowork"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.obrekht.neowork"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_KEY", "\"${secrets.getProperty("API_KEY", "")}\"")
        buildConfigField("String", "BASE_URL", "\"http://94.228.125.136:8080\"")
        buildConfigField("String", "MAPKIT_API_KEY", "\"${secrets.getProperty("MAPKIT_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    androidResources {
        generateLocaleConfig = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
}

dependencies {
    implementation(libs.coroutines)
    implementation(libs.serialization)
    implementation(libs.jetpack.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.fragment)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    implementation(libs.bundles.navigation)
    implementation(libs.workmanager)
    implementation(libs.datastore)
    implementation(libs.paging)
    implementation(libs.imagepicker)

    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.bundles.retrofit)
    implementation(libs.coil)
    implementation(libs.coil.video)

    implementation(libs.bundles.hilt)
    ksp(libs.hilt.compiler)
    ksp(libs.jetpack.hilt.compiler)

    implementation(libs.timber)
    implementation(libs.yandex.mapkit)
    implementation(libs.play.services.location)

    debugImplementation(libs.leakcanary)

    coreLibraryDesugaring(libs.desugar)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
