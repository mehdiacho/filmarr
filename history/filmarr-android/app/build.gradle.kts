import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.mehdiacho.filmarr"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "app.greenforrest.filmarr"
    minSdk = 24
    targetSdk = 36
    // CI passes a monotonically increasing code; local builds use the literal.
    versionCode = System.getenv("FILMARR_VERSION_CODE")?.toIntOrNull() ?: 1
    versionName = System.getenv("FILMARR_VERSION_NAME") ?: "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables { useSupportLibrary = true }
  }

  signingConfigs {
    create("release") {
      // Upload key for Play App Signing. Credentials live in keystore.properties at the repo
      // root (git-ignored — BACK IT UP together with upload-keystore.jks). Env vars override for CI.
      val props = Properties()
      val propsFile = rootProject.file("keystore.properties")
      if (propsFile.exists()) propsFile.inputStream().use { props.load(it) }
      val store = System.getenv("KEYSTORE_PATH")
        ?: props.getProperty("storeFile")?.let { "${rootDir}/$it" }
      if (store != null) {
        storeFile = file(store)
        storePassword = System.getenv("STORE_PASSWORD") ?: props.getProperty("storePassword")
        keyAlias = System.getenv("KEY_ALIAS") ?: props.getProperty("keyAlias") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD") ?: props.getProperty("keyPassword")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Only attach the release signing config when a keystore is actually configured.
      val hasKeystore = rootProject.file("keystore.properties").exists() ||
        System.getenv("KEYSTORE_PATH") != null
      if (hasKeystore) signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // No applicationId suffix: keeps a single google-services.json valid for debug + release.
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
}

// Secrets Gradle Plugin — reads .env (fallback .env.example) into BuildConfig fields.
// TMDB_READ_TOKEN lives here so it never enters source control.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices {
  missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.navigation.compose)

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.google.android.material)

  implementation(libs.coil.compose)

  // TMDB (client-side for now; proxied via backend later).
  implementation(libs.retrofit)
  implementation(libs.converter.moshi)
  implementation(libs.okhttp)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  "ksp"(libs.moshi.kotlin.codegen)

  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.accompanist.permissions)

  // Firebase — Auth, Firestore, Functions (gen2 = Cloud Run), AI (Gemini).
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.functions)
  implementation(libs.firebase.ai)

  // Google Sign-In via Credential Manager.
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
