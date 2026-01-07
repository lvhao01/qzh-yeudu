plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "uni.lv.yuedu"
    compileSdk = 34

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        
        // 确保AAR中的native库被正确传递
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // 添加本地AAR依赖
    implementation(files("libs/basesdk_release_v2.15.8_202512301622.aar"))
    implementation(files("libs/vtbrsdk_release_v2.15.8_202512301622.aar"))
    implementation(files("libs/vtloginsdk_release_v2.15.8_202512301707.aar"))
    implementation(files("libs/offlinefinger_release_v2.15.6_202205101903.aar"))

    // 网络库依赖（basesdk需要）
    implementation("com.squareup.okhttp3:okhttp:3.12.13")
    implementation("com.squareup.retrofit2:retrofit:2.5.0")
    implementation("com.squareup.retrofit2:converter-gson:2.5.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.5.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.8")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("com.google.code.gson:gson:2.8.9")

    // ExoPlayer依赖（vtbrsdk需要，用于音频播放）
    implementation("com.google.android.exoplayer:exoplayer-core:2.13.3")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// 发布配置：将主 AAR 与 libs 下的 AAR 一起作为附件发布
// 说明：
// - publishing：配置要生成的 Maven publication
// - signing：对生成的 publication 进行 PGP 签名，Maven Central 必须签名
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                // groupId/artifactId/version：Maven 坐标，需换成你自己的
                groupId = "io.github.lvhao01"
                artifactId = "lv-yuedu-plugin"
                version = "1.0.0"

                // from(components["release"])：把 Android library 的 release 变体产物作为主 artifact（主 AAR）
                from(components["release"])

                // 附加额外 AAR，使用 classifier 区分，外部拿到同一坐标下的多个附件
                artifact("libs/basesdk_release_v2.15.8_202512301622.aar") {
                    classifier = "basesdk"
                    extension = "aar"
                }
                artifact("libs/vtbrsdk_release_v2.15.8_202512301622.aar") {
                    classifier = "vtbrsdk"
                    extension = "aar"
                }
                artifact("libs/vtloginsdk_release_v2.15.8_202512301707.aar") {
                    classifier = "vtloginsdk"
                    extension = "aar"
                }
                artifact("libs/offlinefinger_release_v2.15.6_202205101903.aar") {
                    classifier = "offlinefinger"
                    extension = "aar"
                }

                pom {
                    // POM 元信息：中央仓库必需
                    name.set("yuedu")
                    description.set("Yuedu SDK")
                    url.set("https://example.com/yuedu")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("dev")
                            name.set("Dev")
                            email.set("dev@example.com")
                        }
                    }
                    scm {
                        url.set("https://example.com/yuedu.git")
                        connection.set("scm:git:https://example.com/yuedu.git")
                        developerConnection.set("scm:git:ssh://example.com/yuedu.git")
                    }
                }
            }
        }
        repositories {
            // 手动上传场景：先输出到本地目录，检查产物后再手动上传到 Sonatype
            maven {
                name = "localStaging"
                url = uri(layout.buildDirectory.dir("maven-local"))
            }
            // 如需直接推送 Sonatype，配置凭证后打开下面的仓库配置
            // maven {
            //     name = "sonatype"
            //     url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            //     credentials {
            //         username = findProperty("mavenCentralUsername") as String?
            //         password = findProperty("mavenCentralPassword") as String?
            //     }
            // }
        }
    }

    signing {
        // 使用本地 Gradle 属性中的 GPG 密钥进行签名；Maven Central 必须签名
        val publication = publishing.publications["release"]
        sign(publication)
        // 如需内存密钥方式：
        // useInMemoryPgpKeys(
        //     findProperty("signing.keyId") as String?,
        //     findProperty("signing.privateKey") as String?,
        //     findProperty("signing.password") as String?
        // )
    }
}