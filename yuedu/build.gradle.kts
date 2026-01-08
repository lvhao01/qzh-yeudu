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

// 注意：repositories 应该在 settings.gradle.kts 或项目级别配置
// 这里不再单独配置 repositories

dependencies {
    // 原本引用的本地依赖
    // implementation files('libs/basesdk_release_v2.15.8_202512301622.aar')
    // implementation files('libs/vtbrsdk_release_v2.15.8_202512301622.aar')
    // implementation files('libs/offlinefinger_release_v2.15.6_202205101903.aar')
    // implementation files('libs/vtloginsdk_release_v2.15.8_202512301707.aar')

    // 使用本地Maven仓库中的AAR依赖（需要先运行 publishAarDepsToMavenLocal）
    // 如果本地Maven中没有，则从libs目录加载（构建时会自动处理）
    implementation("io.github.lvhao01:lv-yuedu-basesdk:1.0.1")
    implementation("io.github.lvhao01:lv-yuedu-vtbrsdk:1.0.1")
    implementation("io.github.lvhao01:lv-yuedu-vtloginsdk:1.0.1")
    implementation("io.github.lvhao01:lv-yuedu-offlinefinger:1.0.1")

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

// 发布配置：为 yuedu 插件和 4 个 AAR 分别创建独立的 Maven publication
// 说明：
// - publishing：配置要生成的 Maven publication
// - signing：对生成的 publication 进行 PGP 签名，Maven Central 必须签名
afterEvaluate {
    publishing {
        publications {
            // 主 publication：yuedu 插件本身
            create<MavenPublication>("release") {
                groupId = "io.github.lvhao01"
                artifactId = "lv-yuedu-plugin"
                version = "1.0.1"

                from(components["release"])

                pom {
                    name.set("Yuedu Plugin")
                    description.set("Reading children's picture books SDK Plugin")
                    url.set("https://github.com/lvhao01/qzh-yeudu")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("lvhao01")
                            name.set("lvhao01")
                            email.set("1274714546@qq.com")
                        }
                    }
                    scm {
                        url.set("https://github.com/lvhao01/qzh-yeudu.git")
                        connection.set("scm:git:https://github.com/lvhao01/qzh-yeudu.git")
                        developerConnection.set("scm:git:ssh://github.com/lvhao01/qzh-yeudu.git")
                    }
                }
            }

            // basesdk AAR 的独立 publication
            create<MavenPublication>("basesdk") {
                groupId = "io.github.lvhao01"
                artifactId = "lv-yuedu-basesdk"
                version = "1.0.1"

                artifact("libs/basesdk_release_v2.15.8_202512301622.aar") {
                    extension = "aar"
                }

                pom {
                    name.set("Base SDK")
                    description.set("Base SDK for Reading children's picture books SDK Plugin of lv-yuedu-plugin")
                    url.set("https://github.com/lvhao01/qzh-yeudu")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("lvhao01")
                            name.set("lvhao01")
                            email.set("1274714546@qq.com")
                        }
                    }
                    scm {
                        url.set("https://github.com/lvhao01/qzh-yeudu.git")
                        connection.set("scm:git:https://github.com/lvhao01/qzh-yeudu.git")
                        developerConnection.set("scm:git:ssh://github.com/lvhao01/qzh-yeudu.git")
                    }
                }
            }

            // vtbrsdk AAR 的独立 publication
            create<MavenPublication>("vtbrsdk") {
                groupId = "io.github.lvhao01"
                artifactId = "lv-yuedu-vtbrsdk"
                version = "1.0.1"

                artifact("libs/vtbrsdk_release_v2.15.8_202512301622.aar") {
                    extension = "aar"
                }

                pom {
                    name.set("VTBR SDK")
                    description.set("VTBR SDK for Reading children's picture books SDK Plugin of lv-yuedu-plugin")
                    url.set("https://github.com/lvhao01/qzh-yeudu")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("lvhao01")
                            name.set("lvhao01")
                            email.set("1274714546@qq.com")
                        }
                    }
                    scm {
                        url.set("https://github.com/lvhao01/qzh-yeudu.git")
                        connection.set("scm:git:https://github.com/lvhao01/qzh-yeudu.git")
                        developerConnection.set("scm:git:ssh://github.com/lvhao01/qzh-yeudu.git")
                    }
                }
            }

            // vtloginsdk AAR 的独立 publication
            create<MavenPublication>("vtloginsdk") {
                groupId = "io.github.lvhao01"
                artifactId = "lv-yuedu-vtloginsdk"
                version = "1.0.1"

                artifact("libs/vtloginsdk_release_v2.15.8_202512301707.aar") {
                    extension = "aar"
                }

                pom {
                    name.set("VTLogin SDK")
                    description.set("VTLogin SDK for Reading children's picture books SDK Plugin of lv-yuedu-plugin")
                    url.set("https://github.com/lvhao01/qzh-yeudu")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("lvhao01")
                            name.set("lvhao01")
                            email.set("1274714546@qq.com")
                        }
                    }
                    scm {
                        url.set("https://github.com/lvhao01/qzh-yeudu.git")
                        connection.set("scm:git:https://github.com/lvhao01/qzh-yeudu.git")
                        developerConnection.set("scm:git:ssh://github.com/lvhao01/qzh-yeudu.git")
                    }
                }
            }

            // offlinefinger AAR 的独立 publication
            create<MavenPublication>("offlinefinger") {
                groupId = "io.github.lvhao01"
                artifactId = "lv-yuedu-offlinefinger"
                version = "1.0.1"

                artifact("libs/offlinefinger_release_v2.15.6_202205101903.aar") {
                    extension = "aar"
                }

                pom {
                    name.set("Offline Finger SDK")
                    description.set("Offline SDK for Reading children's picture books SDK Plugin of lv-yuedu-plugin")
                    url.set("https://github.com/lvhao01/qzh-yeudu")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("lvhao01")
                            name.set("lvhao01")
                            email.set("1274714546@qq.com")
                        }
                    }
                    scm {
                        url.set("https://github.com/lvhao01/qzh-yeudu.git")
                        connection.set("scm:git:https://github.com/lvhao01/qzh-yeudu.git")
                        developerConnection.set("scm:git:ssh://github.com/lvhao01/qzh-yeudu.git")
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
    
    // 创建一个任务，先将4个AAR发布到本地Maven仓库
    // 主插件构建时需要使用这些依赖
    tasks.register("publishAarDepsToMavenLocal") {
        group = "publishing"
        description = "Publish 4 AAR dependencies to local Maven repository"
        dependsOn(
            "publishBasesdkPublicationToMavenLocal",
            "publishVtbrsdkPublicationToMavenLocal",
            "publishVtloginsdkPublicationToMavenLocal",
            "publishOfflinefingerPublicationToMavenLocal"
        )
    }
    
    // 确保在构建主插件AAR之前，先发布4个AAR到本地Maven
    tasks.named("bundleReleaseAar") {
        mustRunAfter("publishAarDepsToMavenLocal")
    }
    
    tasks.named("assembleRelease") {
        dependsOn("publishAarDepsToMavenLocal")
    }
    
    signing {
        // 使用本地 GPG 命令进行签名（无需在属性中保存私钥明文）
        // 需要配置 signing.keyId 在 gradle.properties 中，私钥由系统 GPG 管理
        useGpgCmd()
        
        // 对所有 publication 进行签名
        publishing.publications.forEach { publication ->
            sign(publication)
        }
    }
}