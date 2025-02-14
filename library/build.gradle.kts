import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
    id("com.vanniktech.maven.publish") version "0.28.0"
}

android {
    namespace = "android.boot.ble.common"
    compileSdk = 34

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    api(libs.androidx.bluetooth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.permissionx)

}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    configure(
        AndroidSingleVariantLibrary(
            // the published variant
            variant = "release",
            // whether to publish a sources jar
            sourcesJar = true,
            // whether to publish a javadoc jar
            publishJavadocJar = true,
        )
    )

    coordinates("io.github.zhangwenxue", "ble-common", "1.0.0-alpha5")

    pom {
        name.set("Android-Ble-Common-lib")
        description.set("An Android Ble common library")
        inceptionYear.set("2024")
        url.set("https://github.com/zhangwenxue/ble-common/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("zwx")
                name.set("zhangwenxue")
                url.set("https://github.com/zhangwenxue/")
            }
        }
        scm {
            url.set("https://github.com/zhangwenxue/ble-common/")
            connection.set("scm:git:git://github.com/zhangwenxue/ble-common.git")
            developerConnection.set("scm:git:ssh://git@github.com/zhangwenxue/ble-common.git")
        }
    }
    signAllPublications()
}
//publishReleasePublicationToMavenCentralRepository

///////////////AliYun Maven Publish////////////////
val gid = "io.github.zhangwenxue"
val ver = "1.0.0-alpha3"
val aid = "ble-common"

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = gid
                artifactId = aid
                version = ver
            }
        }
        repositories {
            maven {
                url = uri("https://packages.aliyun.com/maven/repository/2405228-release-Q427gT")
                credentials {
                    username = project.findProperty("aliyun.wwk.username") as String?
                        ?: System.getenv("USERNAME")
                    password = project.findProperty("aliyun.wwk.token") as String? ?: System.getenv(
                        "TOKEN"
                    )
                }
            }
        }
    }
}
// ./gradlew :library:publishReleasePublicationToMavenRepository