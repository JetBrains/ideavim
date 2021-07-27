package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object PluginVerifier : BuildType({
  name = "Plugin verification"
  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
  }

  vcs {
    root(DslContext.settingsRoot)

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    gradle {
      tasks = "clean runPluginVerifier"
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
