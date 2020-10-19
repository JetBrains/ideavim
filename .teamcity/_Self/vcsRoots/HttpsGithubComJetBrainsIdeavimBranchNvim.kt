package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object HttpsGithubComJetBrainsIdeavimBranchNvim : GitVcsRoot({
    name = "https://github.com/JetBrains/ideavim (branch nvim)"
    url = "https://github.com/JetBrains/ideavim.git"
    branch = "neovim"
})
