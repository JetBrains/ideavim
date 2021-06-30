package _Self.buildTypes

import _Self.Constants.GITHUB_TESTS
import _Self.vcsRoots.GitHubPullRequest
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object GithubTests : Github("clean test", "Tests")

sealed class Github(command: String, desc: String) : BuildType({
  name = "GitHub Pull Requests $desc"
  description = "Test GitHub pull requests $desc"

  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_ideaVersion", GITHUB_TESTS)
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
      param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
    }
  }

  triggers {
    vcs {
      quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
      branchFilter = ""
    }
  }

  features {
    pullRequests {
      provider = github {
        authType = token {
          token = "credentialsJSON:43afd6e5-6ad5-4d12-a218-cf1547717a7f"
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
          token = "credentialsJSON:43afd6e5-6ad5-4d12-a218-cf1547717a7f"
        }
      }
      param("github_oauth_user", "AlexPl292")
    }
  }

  requirements {
    noLessThanVer("teamcity.agent.jvm.version", "1.8")
  }
})
