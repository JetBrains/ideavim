package _Self.buildTypes

import _Self.AgentSize
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle

object YoutrackTest : IdeaVimBuildType({
  name = "Test YouTrack Connection"
  description = "Test YouTrack API connection and token"

  params {
    password(
      "env.ORG_GRADLE_PROJECT_youtrackToken",
      "credentialsJSON:eedfa0eb-c329-462a-b7b4-bc263bda8c01",
      display = ParameterDisplay.HIDDEN
    )
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"
    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    gradle {
      name = "Test YouTrack Connection"
      tasks = "scripts:testYoutrackConnection"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
  }

  requirements {
    equals("teamcity.agent.hardware.cpuCount", AgentSize.MEDIUM)
    equals("teamcity.agent.os.family", "Linux")
  }
})
