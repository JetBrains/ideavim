package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object TestsForIntelliJ20202_2 : BuildType({
    name = "Tests for IntelliJ 2020.2"
    description = "branch 202"

    params {
        param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
        param("env.ORG_GRADLE_PROJECT_ideaVersion", "2020.2")
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
