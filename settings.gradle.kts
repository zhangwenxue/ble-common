pluginManagement {
    repositories {
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        maven(url = "https://maven.aliyun.com/repository/public")
        maven(url = "https://maven.aliyun.com/repository/central")
        maven(url = "https://maven.aliyun.com/repository/google")
        mavenCentral()
        gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven(url = "https://jitpack.io")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {


        maven(url = "https://maven.aliyun.com/repository/public")
        maven(url = "https://maven.aliyun.com/repository/central")
        maven(url = "https://maven.aliyun.com/repository/google")

        mavenCentral()

        maven(url = "https://packages.aliyun.com/maven/repository/2405228-release-Q427gT") {
            credentials {
                username = "655ab864b07fd20003315288"
                password = "boQWixPrcK_y"
            }
        }

        google()
        maven(url = "https://www.jitpack.io")

    }
}

rootProject.name = "BleCommon"
include(":app")
include(":library")
