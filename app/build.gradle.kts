plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.screenshottagger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.screenshottagger"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // R8 压缩 + 资源裁剪：APK 更小，常驻进程加载的 dex 更少
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // 自用侧载：直接用 debug 密钥签名，省去配置
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// 刻意不引入 AndroidX / Compose / 协程，只依赖系统框架
dependencies {
    testImplementation("junit:junit:4.13.2")
}
