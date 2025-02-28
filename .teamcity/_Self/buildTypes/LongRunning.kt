package _Self.buildTypes

import _Self.Constants.LONG_RUNNING_TESTS
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object LongRunning : IdeaVimBuildType({
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
      tasks = "clean :tests:long-running-tests:testLongRunning"
      buildFile = ""
      enableStacktrace = true
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
  }

  triggers {
    vcs {
      enabled = false
      branchFilter = "+:<default>"
    }
    schedule {
      enabled = true
      schedulingPolicy = daily {
        hour = 5
      }
    }
  }
})