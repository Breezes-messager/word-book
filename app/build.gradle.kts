plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.wordbook"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wordbook"
        minSdk = 26
        targetSdk = 35
        // 发新版本时：versionCode 每次 +1（商店 / 安装升级用），versionName 用语义化版本号
        versionCode = 15
        versionName = "1.4.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 词库导入时需要较大的游标窗口
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    signingConfigs {
        // 个人自用：release 直接用调试签名。
        // 好处是能和 debug 包**互相覆盖安装**（同一个签名），换包不用卸载、数据不丢；
        // 而且 R8 优化后的包启动更快、体积小 39%（25.2MB → 15.4MB）。
        create("selfSigned") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // 个人自用，直接装 APK；开启混淆以压缩体积（目标 < 50MB）
            signingConfig = signingConfigs.getByName("selfSigned")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        // 设置页要显示真实版本号；gradle.properties 里全局默认关掉了 buildConfig，这里单独打开
        buildConfig = true
    }

    // assets 里的 .db 必须保持未压缩：AssetImporter 用 openFd() 读取体积，
    // 被压缩的 assets 无法作为文件描述符打开，会在首次启动时抛异常
    androidResources {
        noCompress += "db"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric 集成测试需要访问 assets（词库）与资源
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = false
    }
}

// APK 文件名带上版本号，一眼就能看出是哪个版本：
//   debug   -> app/build/outputs/apk/debug/背单词-v1.0.4-debug.apk
//   release -> app/build/outputs/apk/release/背单词-v1.0.4-release.apk
//
// 说明：AGP 8.7 的 VariantOutput 只公开了 versionName，没有公开改名用的
// outputFileName（只有内部的 BaseVariantOutputImpl 有），所以这里用经典的
// applicationVariants + outputs.all 来改名。AGP 9 若移除该 API 需改用复制任务。
@Suppress("DEPRECATION")
android {
    applicationVariants.all {
        val variant = this
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "背单词-v" + variant.versionName + "-" + variant.buildType.name + ".apk"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.kotlinx.serialization.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
