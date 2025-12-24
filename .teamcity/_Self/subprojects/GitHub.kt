package _Self.subprojects

import _Self.AgentSize
import _Self.IdeaVimBuildType
import _Self.vcsRoots.GitHubPullRequest
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object GitHub : Project({
  name = "Pull Requests checks"
  description = "Automatic checking of GitHub Pull Requests"

  buildType(GithubBuildType("clean test -x :tests:property-tests:test -x :tests:long-running-tests:test", "Tests"))
})

class GithubBuildType(command: String, desc: String) : IdeaVimBuildType({
  name = "GitHub Pull Requests $desc"
  description = "Test GitHub pull requests $desc"

  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
  }

  vcs {
    root(GitHubPullRequest)

    checkoutMode = CheckoutMode.AUTO
    branchFilter = """
            +:*
            -:<default>
        """.trimIndent()
  }

  steps {
    gradle {
      tasks = command
      buildFile = ""
      enableStacktrace = true
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
  }

  triggers {
    vcs {
      enabled = false
      quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
      branchFilter = ""
    }
  }

  features {
    pullRequests {
      provider = github {
        authType = token {
          token = "credentialsJSON:90f3b439-6e91-40f7-a086-d4dd8e0ea9b8"
        }
        filterTargetBranch = "refs/heads/master"
        filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
      }
    }
    commitStatusPublisher {
      vcsRootExtId = "${GitHubPullRequest.id}"
      publisher = github {
        githubUrl = "https://api.github.com"
        authType = personalToken {
          token = "credentialsJSON:90f3b439-6e91-40f7-a086-d4dd8e0ea9b8"
        }
      }
      param("github_oauth_user", "AlexPl292")
    }
  }

  requirements {
    equals("teamcity.agent.hardware.cpuCount", AgentSize.MEDIUM)
    equals("teamcity.agent.os.family", "Linux")
  }
})
