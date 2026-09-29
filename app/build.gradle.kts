plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.haddadian.universalremote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.haddadian.universalremote"
        minSdk = 26
        targetSdk = 35
        versionCode = 63
        versionName = "63"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val protocolRepositoryUrl = providers.gradleProperty("protocolRepositoryUrl").orElse("").get()
        val protocolRepositoryPublicKey = providers.gradleProperty("protocolRepositoryPublicKey")
            .orElse("")
            .get()
        buildConfigField("String", "PROTOCOL_REPOSITORY_URL", "\"$protocolRepositoryUrl\"")
        buildConfigField("String", "PROTOCOL_REPOSITORY_PUBLIC_KEY", "\"$protocolRepositoryPublicKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        val releaseStoreFile = providers.gradleProperty("releaseStoreFile").orElse("").get()
        val releaseStorePassword = providers.gradleProperty("releaseStorePassword").orElse("").get()
        val releaseKeyAlias = providers.gradleProperty("releaseKeyAlias").orElse("").get()
        val releaseKeyPassword = providers.gradleProperty("releaseKeyPassword").orElse("").get()
        if (releaseStoreFile.isNotBlank() && releaseStorePassword.isNotBlank() &&
            releaseKeyAlias.isNotBlank() && releaseKeyPassword.isNotBlank()
        ) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigningConfigured = providers.gradleProperty("releaseStoreFile").orElse("").get().isNotBlank() &&
                providers.gradleProperty("releaseStorePassword").orElse("").get().isNotBlank() &&
                providers.gradleProperty("releaseKeyAlias").orElse("").get().isNotBlank() &&
                providers.gradleProperty("releaseKeyPassword").orElse("").get().isNotBlank()
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Bouncy Castle modules package the same OSGi manifest path.
            // Android resource merging must keep a single copy of this Java resource.
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.compose.ui)
    implementation(libs.compose.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    implementation(libs.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.protobuf.javalite)
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.80")

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation("org.json:json:20240303")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:4.28.3" }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") { option("lite") }
            }
        }
    }
}

kotlin { jvmToolchain(17) }
