package _Self.buildTypes

import _Self.Constants.EAP
import _Self.Constants.VERSION
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule

object ReleaseEap : BuildType({
  name = "Publish EAP Build"
  description = "Build and publish EAP of IdeaVim plugin"

  artifactRules = "build/distributions/*"
  buildNumberPattern = "$VERSION.%build.counter%"

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
    param("env.ORG_GRADLE_PROJECT_publishChannels", EAP)
    password(
      "env.ORG_GRADLE_PROJECT_slackUrl",
      "credentialsJSON:a8ab8150-e6f8-4eaf-987c-bcd65eac50b5",
      label = "Slack Token"
    )
  }

  vcs {
    root(DslContext.settingsRoot)

    checkoutMode = CheckoutMode.ON_SERVER
  }

  steps {
    gradle {
      tasks = "clean publishPlugin slackEapNotification"
      buildFile = ""
      enableStacktrace = true
      param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
    }
  }

  features {
    vcsLabeling {
      vcsRootId = "${DslContext.settingsRoot.id}"
      labelingPattern = "%system.build.number%-EAP"
      successfulOnly = true
      branchFilter = ""
    }
  }
})
