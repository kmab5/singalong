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
val appVersion = Properties().apply { rootProject.file("version.properties").inputStream().use { load(it) } }
fun signingValue(key: String, environment: String): String? = System.getenv(environment)?.takeIf { it.isNotBlank() } ?: releaseSigning.getProperty(key)

android {
    namespace = "com.kmab.singalong"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.kmab.singalong"
        minSdk = 31
        targetSdk = 36
        versionCode = appVersion.getProperty("versionCode").toInt()
        versionName = appVersion.getProperty("versionName")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    signingConfigs {
        create("release") {
            signingValue("storeFile", "SINGALONG_KEYSTORE_PATH")?.let { storeFile = rootProject.file(it) }
            storePassword = signingValue("storePassword", "ANDROID_KEYSTORE_PASSWORD")
            keyAlias = signingValue("keyAlias", "ANDROID_KEY_ALIAS")
            keyPassword = signingValue("keyPassword", "ANDROID_KEY_PASSWORD")
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
        check(mapOf("storeFile" to "SINGALONG_KEYSTORE_PATH", "storePassword" to "ANDROID_KEYSTORE_PASSWORD", "keyAlias" to "ANDROID_KEY_ALIAS", "keyPassword" to "ANDROID_KEY_PASSWORD").all { (key, env) ->
            !signingValue(key, env).isNullOrBlank()
        }) { "Release signing requires local signing.properties. See docs/validation/release-signing.md." }
        check(rootProject.file(requireNotNull(signingValue("storeFile", "SINGALONG_KEYSTORE_PATH"))).isFile) { "Release keystore is missing. Restore the existing key; do not replace it." }
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
