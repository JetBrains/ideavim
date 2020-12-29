package _Self.buildTypes

import _Self.Constants.DEFAULT
import _Self.Constants.DEV
import _Self.Constants.EAP
import _Self.Constants.VERSION
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle

object Release : BuildType({
  name = "Publish Release"
  description = "Build and publish IdeaVim plugin"

  artifactRules = "build/distributions/*"
  buildNumberPattern = VERSION

  params {
    param("env.ORG_GRADLE_PROJECT_ideaVersion", "2020.2")
    password(
      "env.ORG_GRADLE_PROJECT_publishToken",
      "credentialsJSON:61a36031-4da1-4226-a876-b8148bf32bde",
      label = "Password"
    )
    param("env.ORG_GRADLE_PROJECT_publishUsername", "Aleksei.Plate")
    param("env.ORG_GRADLE_PROJECT_version", "%build.number%")
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_publishChannels", "$DEFAULT,$EAP,$DEV")
  }

  vcs {
    root(DslContext.settingsRoot)

    checkoutMode = CheckoutMode.ON_SERVER
  }

  steps {
    gradle {
      tasks = "clean publishPlugin"
      buildFile = ""
      enableStacktrace = true
      param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
    }
  }
})
