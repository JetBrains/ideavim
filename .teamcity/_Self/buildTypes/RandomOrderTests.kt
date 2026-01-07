package _Self.buildTypes

import _Self.AgentSize
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object RandomOrderTests : IdeaVimBuildType({
  name = "Random order tests"
  description = "Running tests with random order on each run. This way we can catch order-dependent bugs."
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
      tasks = """
        clean test
        -x :tests:property-tests:test
        -x :tests:long-running-tests:test
        -Djunit.jupiter.execution.order.random.seed=default
        -Djunit.jupiter.testmethod.order.default=random
      """.trimIndent().replace("\n", " ")
      buildFile = ""
      enableStacktrace = true
      gradleParams = "--build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
  }

  triggers {
    vcs {
      branchFilter = "+:<default>"
    }
  }

  requirements {
    equals("teamcity.agent.hardware.cpuCount", AgentSize.MEDIUM)
    equals("teamcity.agent.os.family", "Linux")
  }
})
