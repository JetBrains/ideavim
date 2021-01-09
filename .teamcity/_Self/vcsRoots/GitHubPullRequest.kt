package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object GitHubPullRequest : GitVcsRoot({
  name = "IdeaVim Pull Requests"
  url = "git@github.com:JetBrains/ideavim.git"
  branchSpec = "+:refs/(pull/*)/head"
  authMethod = uploadedKey {
    uploadedKey = "Alex Plate TeamCity key"
  }
})
