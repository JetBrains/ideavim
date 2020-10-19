package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object HttpsGithubComJetBrainsIdeavim_181 : GitVcsRoot({
    name = "https://github.com/JetBrains/ideavim (branch 181)"
    url = "https://github.com/JetBrains/ideavim.git"
    branch = "181"
    useMirrors = false
})
