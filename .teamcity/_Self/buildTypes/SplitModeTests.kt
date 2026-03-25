package _Self.buildTypes

import _Self.AgentSize
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule

object SplitModeTests : IdeaVimBuildType({
  name = "Split mode tests"
  description = "End-to-end tests running IdeaVim in Remote Development split mode (backend + frontend)"

  artifactRules = """
    +:tests/split-mode-tests/build/reports => split-mode-tests/build/reports
    +:out/ide-tests/tests/**/log => split-mode-tests/ide-logs
    +:out/ide-tests/tests/**/frontend/log => split-mode-tests/frontend-logs
  """.trimIndent()

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
      clearConditions()
      tasks = ":tests:split-mode-tests:testSplitMode"
      buildFile = ""
      enableStacktrace = true
      gradleParams = "--build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
  }

  triggers {
    schedule {
      enabled = true
      schedulingPolicy = daily {
        hour = 4
      }
    }
  }

  requirements {
    // Split mode tests need more resources (launches 2 IDE processes)
    equals("teamcity.agent.hardware.cpuCount", AgentSize.XLARGE)
    equals("teamcity.agent.os.family", "Linux")
  }
})
