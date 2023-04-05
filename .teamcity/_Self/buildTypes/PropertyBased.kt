package _Self.buildTypes

import _Self.Constants.PROPERTY_TESTS
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object PropertyBased : BuildType({
  name = "Property based tests"
  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_ideaVersion", PROPERTY_TESTS)
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    gradle {
      tasks = "clean testPropertyBased"
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
