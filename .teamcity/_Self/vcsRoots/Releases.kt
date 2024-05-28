package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object ReleasesVcsRoot : GitVcsRoot({
  name = "IdeaVim Releases"
  url = "git@github.com:JetBrains/ideavim.git"
  branchSpec = "+:refs/head/releases/*"
  authMethod = uploadedKey {
    uploadedKey = "IdeaVim ssh keys"
  }
})
