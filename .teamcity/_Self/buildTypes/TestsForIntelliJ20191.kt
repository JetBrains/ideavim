package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object TestsForIntelliJ20191 : BuildType({
    name = "Tests for IntelliJ 2019.1"
    description = "branch 191"

    params {
        param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
        param("env.ORG_GRADLE_PROJECT_legacyNoJavaPlugin", "true")
        param("env.ORG_GRADLE_PROJECT_ideaVersion", "IC-2019.1")
        param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
        param("env.ORG_GRADLE_PROJECT_javaVersion", "1.8")
    }

    vcs {
        root(_Self.vcsRoots.Branch_191_193)

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
