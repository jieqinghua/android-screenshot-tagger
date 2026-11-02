import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 固定签名密钥：每次编译都用同一把，新版 APK 才能直接覆盖安装。
// 在线编译从 GitHub Secret RELEASE_KEYSTORE_B64 读取；本地编译读 signing/release.p12.b64（不入库）。
val releaseKeystoreB64: String? = System.getenv("RELEASE_KEYSTORE_B64")?.takeIf { it.isNotBlank() }
    ?: rootProject.file("signing/release.p12.b64").takeIf { it.exists() }?.readText()
if (releaseKeystoreB64 == null && System.getenv("GITHUB_ACTIONS") == "true") {
    throw GradleException("缺少 RELEASE_KEYSTORE_B64 secret，拒绝产出签名不一致的 APK")
}
val releaseKeystore: File? = releaseKeystoreB64?.let { b64 ->
    layout.buildDirectory.file("signing/release.p12").get().asFile.apply {
        parentFile.mkdirs()
        writeBytes(Base64.getMimeDecoder().decode(b64))
    }
}

android {
    namespace = "com.example.screenshottagger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.screenshottagger"
        minSdk = 30
        targetSdk = 35
        // 在线编译时用构建序号，系统安装界面会显示为"更新"
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 1
        versionName = "1.0"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storeType = "pkcs12"
                storePassword = "screenshottagger"
                keyAlias = "tagger"
                keyPassword = "screenshottagger"
            }
        }
    }

    buildTypes {
        release {
            // R8 压缩 + 资源裁剪：APK 更小，常驻进程加载的 dex 更少
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
