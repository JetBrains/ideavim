package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle

object Release : BuildType({
  name = "Publish Release"
  description = "Build and publish IdeaVim plugin"

  artifactRules = "build/distributions/*"
  buildNumberPattern = "0.60"

  params {
    param("env.ORG_GRADLE_PROJECT_ideaVersion", "2020.2")
    password(
      "env.ORG_GRADLE_PROJECT_publishToken",
      "credentialsJSON:ec1dc748-e289-47e1-88b6-f193d7999bf4",
      label = "Password"
    )
    param("env.ORG_GRADLE_PROJECT_publishUsername", "vlan")
    param("env.ORG_GRADLE_PROJECT_version", "%build.number%")
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_publishChannels", "default,eap")
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
