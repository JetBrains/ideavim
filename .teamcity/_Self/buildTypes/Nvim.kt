package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object Nvim : BuildType({
    name = "NVIM"
    description = "branch EAP"

    params {
        param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
        param("env.ORG_GRADLE_PROJECT_ideaVersion", "LATEST-EAP-SNAPSHOT")
        param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
    }

    vcs {
        root(_Self.vcsRoots.Branch_Nvim)

        checkoutMode = CheckoutMode.ON_SERVER
    }

    steps {
        script {
            scriptContent = "apt-get install neovim"
        }
        gradle {
            tasks = "--version"
            param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
        }
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
