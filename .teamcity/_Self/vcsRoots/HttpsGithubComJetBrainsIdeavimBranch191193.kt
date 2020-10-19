package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object HttpsGithubComJetBrainsIdeavimBranch191193 : GitVcsRoot({
    name = "https://github.com/JetBrains/ideavim (branch 191-193)"
    url = "https://github.com/JetBrains/ideavim.git"
    branch = "191-193"
    useMirrors = false
})
