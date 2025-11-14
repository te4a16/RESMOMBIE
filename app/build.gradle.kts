plugins {
    id("com.android.application")
    //id("org.jetbrains.kotlin.android")
}

android {
    namespace="org.example"
    compileSdk = 34

    defaultConfig {
        applicationId="org.example" // アプリケーションID
        minSdk=21
        targetSdk=34
        versionCode=1
        versionName="1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    
    // Javaのバージョン設定
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    /*
    // ViewBindingを有効にする（便利）
    buildFeatures {
        viewBinding = true
    }
    */
}

dependencies {
    // Android標準ライブラリ（通常は自動で含まれる）
    implementation("androidx.core:core:1.9.0")

    // 必須：AppCompatActivityと基本的なUIサポート
    implementation("androidx.appcompat:appcompat:1.6.1")

    // 必須：Fragment と FragmentTransaction (App.java および CameraFragment.java で使用)
    implementation("androidx.fragment:fragment:1.6.2")

    // 必須：Permissions (ActivityCompat, ContextCompat)
    implementation("androidx.core:core-ktx:1.12.0")

    // 必須：UI要素 (ToastなどのためのActivity/Fragment Context)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // CameraX Core, Camera2, and Lifecycle libraries
    val cameraXVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")

    // CameraX View (PreviewView を含む) と Extensions
    implementation("androidx.camera:camera-view:$cameraXVersion")
    implementation("androidx.camera:camera-extensions:$cameraXVersion")
}

//kotlinのバージョンを統一させる
configurations.all {
    resolutionStrategy {
        // すべての依存関係において、KotlinのStdlibを統一する
        // ここでは最新の1.8.22（エラーメッセージで確認できた最新版）に強制的に合わせる
        force("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22")
        // 他の競合している古いkotlin-stdlib-jdk*があれば、1.8.22に合わせる
    }
}