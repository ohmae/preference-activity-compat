package build.internal

import build.Properties
import org.gradle.api.Project
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.Upload
import org.gradle.kotlin.dsl.getPluginByName
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withGroovyBuilder

private val Project.base: BasePluginConvention
    get() = ((this as? Project)?.convention
        ?: (this as HasConvention).convention).getPluginByName("base")

internal fun Project.uploadArchivesSettings() {
    tasks.named<Upload>("uploadArchives") {
        repositories.withGroovyBuilder {
            "mavenDeployer" {
                "repository"("url" to "file:$buildDir/maven")
                "pom" {
                    "project" {
                        setProperty("name", base.archivesBaseName)
                        setProperty("url", Properties.Url.site)
                        setProperty("groupId", Properties.groupId)
                        setProperty("artifactId", base.archivesBaseName)
                        setProperty("version", Properties.versionName)
                        "licenses" {
                            "license" {
                                setProperty("name", "The MIT License")
                                setProperty("url", "https://opensource.org/licenses/MIT")
                                setProperty("distribution", "repo")
                            }
                        }
                        "scm" {
                            setProperty("connection", Properties.Url.scm)
                            setProperty("url", Properties.Url.github)
                        }
                    }
                }
            }
        }
    }
}
