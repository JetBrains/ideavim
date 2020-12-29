package _Self.buildTypes

import _Self.Constants.DEFAULT
import _Self.Constants.DEV
import _Self.Constants.EAP
import _Self.Constants.VERSION
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle

object Release_201 : BuildType({
  name = "Publish Release 2020.1"
  description = "Build and publish IdeaVim plugin"

  artifactRules = "build/distributions/*"
  buildNumberPattern = "$VERSION-2020.1"

  params {
    param("env.ORG_GRADLE_PROJECT_ideaVersion", "2020.1")
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
    root(_Self.vcsRoots.Branch_201)

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
