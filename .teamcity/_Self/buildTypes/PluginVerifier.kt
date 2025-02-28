package _Self.buildTypes

import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object PluginVerifier : IdeaVimBuildType({
  name = "Plugin verification"
  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    gradle {
      tasks = "clean verifyPlugin"
      buildFile = ""
      enableStacktrace = true
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
  }

  triggers {
    vcs {
      branchFilter = "+:<default>"
    }
  }
})
