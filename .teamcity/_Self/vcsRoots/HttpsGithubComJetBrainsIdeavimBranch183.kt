package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object HttpsGithubComJetBrainsIdeavimBranch183 : GitVcsRoot({
    name = "https://github.com/JetBrains/ideavim (branch 183)"
    url = "https://github.com/JetBrains/ideavim.git"
    branch = "183"
    useMirrors = false
})
