package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object ReleasesVcsRoot : GitVcsRoot({
  name = "IdeaVim Releases"
  url = "git@github.com:JetBrains/ideavim.git"
  branch = "refs/heads/master"
  branchSpec = "+:refs/(*)"
  authMethod = uploadedKey {
    uploadedKey = "IdeaVim ssh keys"
  }
})
