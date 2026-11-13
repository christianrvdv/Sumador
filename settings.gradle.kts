pluginManagement {
    repositories {
        // Mirror de Google Maven (artefactos y plugin markers de AGP).
        // Se limita por grupo: el mirror de Aliyun devuelve 502 en la ruta del
        // plugin marker de KSP y eso aborta la resolución aunque haya repos
        // oficiales después. Por eso NO se usa repository/gradle-plugin.
        maven("https://maven.aliyun.com/repository/google") {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.google\\.android.*")
                includeGroupByRegex("com\\.google\\.firebase.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/google") {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.google\\.android.*")
                includeGroupByRegex("com\\.google\\.firebase.*")
            }
        }
        mavenCentral()
        google()
        maven("https://maven.aliyun.com/repository/public")
    }
}

rootProject.name = "Sumador"
include(":app")
