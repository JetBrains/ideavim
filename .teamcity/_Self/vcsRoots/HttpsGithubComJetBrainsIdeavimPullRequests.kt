package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object HttpsGithubComJetBrainsIdeavimPullRequests : GitVcsRoot({
    name = "https://github.com/JetBrains/ideavim (Pull Requests)"
    url = "https://github.com/JetBrains/ideavim"
    branchSpec = "+:refs/(pull/*/merge)"
    authMethod = password {
        userName = "AlexPl292"
    }
})
