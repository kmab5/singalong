import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val releaseSigning = Properties().apply {
    val credentials = rootProject.file("signing.properties")
    if (credentials.exists()) credentials.inputStream().use { load(it) }
}

android {
    namespace = "com.kmab.singalong"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.kmab.singalong"
        minSdk = 31
        targetSdk = 36
        versionCode = 7
        versionName = "0.3.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    signingConfigs {
        create("release") {
            releaseSigning.getProperty("storeFile")?.let { storeFile = rootProject.file(it) }
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { animationsDisabled = true }
    lint { warningsAsErrors = false }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

val verifyReleaseSigning by tasks.registering {
    doLast {
        check(listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all {
            !releaseSigning.getProperty(it).isNullOrBlank()
        }) { "Release signing requires local signing.properties. See docs/validation/release-signing.md." }
        check(rootProject.file(releaseSigning.getProperty("storeFile")).isFile) { "Release keystore is missing. Restore the existing key; do not replace it." }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(verifyReleaseSigning) }

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.icons)
    implementation(libs.coroutines.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore)
    implementation(libs.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.mlkit.language.id)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.coroutines.test)
}
