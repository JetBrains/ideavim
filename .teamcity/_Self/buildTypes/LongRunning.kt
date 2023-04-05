package _Self.buildTypes

import _Self.Constants.LONG_RUNNING_TESTS
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object LongRunning : BuildType({
  name = "Long running tests"
  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_ideaVersion", LONG_RUNNING_TESTS)
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    gradle {
      tasks = "clean testLongRunning"
      buildFile = ""
      enableStacktrace = true
    }
  }

  triggers {
    vcs {
      branchFilter = "+:<default>"
    }
  }
})