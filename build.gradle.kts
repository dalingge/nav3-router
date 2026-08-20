import com.vanniktech.maven.publish.MavenPublishBaseExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

// ===== Maven Central 发布配置（vanniktech 插件） =====
// 需要发布的框架模块（app / user 为示例消费者，不发布）
val publishModules = setOf("nav-annotation", "nav-runtime", "nav-compiler")

subprojects {
    val moduleName = name
    if (moduleName !in publishModules) return@subprojects

    apply(plugin = "com.vanniktech.maven.publish")

    extensions.configure<MavenPublishBaseExtension> {
        coordinates(
            project.findProperty("NAV_GROUP_ID") as String,
            moduleName,
            project.findProperty("NAV_ROUTER_VERSION") as String,
        )

        pom {
            name.set("nav3-router")
            description.set("基于 Jetpack Navigation 3 的轻量级 Android 导航框架")
            url.set(project.findProperty("NAV_PROJECT_URL") as String)
            licenses {
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set(project.findProperty("NAV_DEVELOPER_ID") as String)
                    name.set(project.findProperty("NAV_DEVELOPER_NAME") as String)
                }
            }
            scm {
                connection.set(project.findProperty("NAV_SCM_CONNECTION") as String)
                url.set(project.findProperty("NAV_PROJECT_URL") as String)
            }
        }

        // 发布到 Maven Central Portal（自动处理 sources/javadoc jar + GPG 签名）
        publishToMavenCentral()
        signAllPublications()
    }
}
