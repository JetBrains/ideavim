package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object HttpsGithubComJetBrainsIdeavimPullRequests : GitVcsRoot({
    name = "IdeaVim Pull Requests"
    url = "git@github.com:JetBrains/ideavim.git"
    branchSpec = "+:refs/(pull/*/merge)"
    authMethod = uploadedKey {
      uploadedKey = "Alex Plate TeamCity key"
    }
})
