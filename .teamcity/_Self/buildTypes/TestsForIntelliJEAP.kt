package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object TestsForIntelliJEAP : BuildType({
    name = "Tests for IntelliJ Latest EAP"
    description = "branch EAP"

    params {
        param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
        param("env.ORG_GRADLE_PROJECT_ideaVersion", "LATEST-EAP-SNAPSHOT")
        param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
    }

    vcs {
        root(DslContext.settingsRoot)

        checkoutMode = CheckoutMode.ON_SERVER
    }

    steps {
        gradle {
            tasks = "clean test"
            buildFile = ""
            enableStacktrace = true
            param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
        }
    }

    triggers {
        vcs {
            branchFilter = ""
        }
    }

    requirements {
        noLessThanVer("teamcity.agent.jvm.version", "1.8")
    }
})
