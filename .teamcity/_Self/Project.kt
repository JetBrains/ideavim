package _Self

import _Self.buildTypes.GitHubPullRequests
import _Self.buildTypes.Nvim
import _Self.buildTypes.Release
import _Self.buildTypes.ReleaseEap
import _Self.buildTypes.TestsForIntelliJ20181
import _Self.buildTypes.TestsForIntelliJ20182
import _Self.buildTypes.TestsForIntelliJ20183
import _Self.buildTypes.TestsForIntelliJ20191
import _Self.buildTypes.TestsForIntelliJ20192
import _Self.buildTypes.TestsForIntelliJ20193
import _Self.buildTypes.TestsForIntelliJ20201
import _Self.buildTypes.TestsForIntelliJ20202
import _Self.buildTypes.TestsForIntelliJEAP
import _Self.vcsRoots.Branch_181
import _Self.vcsRoots.Branch_183
import _Self.vcsRoots.Branch_191_193
import _Self.vcsRoots.Branch_Nvim
import _Self.vcsRoots.GitHubPullRequest
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object Project : Project({
  description = "Vim emulation plugin for the IntelliJ platform products"

  vcsRoot(Branch_183)
  vcsRoot(Branch_181)
  vcsRoot(GitHubPullRequest)
  vcsRoot(Branch_191_193)
  vcsRoot(Branch_Nvim)

  buildType(GitHubPullRequests)
  buildType(Release)
  buildType(TestsForIntelliJ20201)
  buildType(TestsForIntelliJ20191)
  buildType(TestsForIntelliJ20181)
  buildType(TestsForIntelliJ20192)
  buildType(TestsForIntelliJ20182)
  buildType(TestsForIntelliJ20193)
  buildType(TestsForIntelliJ20183)
  buildType(Nvim)
  buildType(ReleaseEap)
  buildType(TestsForIntelliJ20202)
  buildType(TestsForIntelliJEAP)

  features {
    feature {
      id = "PROJECT_EXT_768"
      type = "CloudImage"
      param("use-spot-instances", "true")
      param("user-tags", "project=idea-vim")
      param("agent_pool_id", "41")
      param("image-instances-limit", "")
      param("subnet-id", "subnet-58839511")
      param("ebs-optimized", "false")
      param("instance-type", "c5d.large")
      param("amazon-id", "ami-0d1a6a32faa92923e")
      param("spot-instance-price", "0.1")
      param("source-id", "BuildAgentsIdeaVim")
      param("image-name-prefix", "BuildAgentsIdeaVim")
      param("key-pair-name", "teamcity-prod-pub")
      param("security-group-ids", "sg-eda08696,sg-7332cf0f,")
      param("profileId", "amazon-48")
    }
    feature {
      id = "amazon-48"
      type = "CloudProfile"
      param("profileServerUrl", "")
      param("secure:access-id", "credentialsJSON:dbcdb2a2-de5f-4bc9-9421-292b19e83947")
      param("system.cloud.profile_id", "amazon-48")
      param("total-work-time", "")
      param("description", "")
      param("cloud-code", "amazon")
      param("enabled", "true")
      param("max-running-instances", "10")
      param("agentPushPreset", "")
      param("profileId", "amazon-48")
      param("name", "Cloud Agents")
      param("next-hour", "")
      param("secure:secret-key", "credentialsJSON:65a87fe7-0977-4af9-96f1-344f2b82d269")
      param("region", "eu-west-1")
      param("terminate-idle-time", "15")
      param("not-checked", "")
    }
  }
})
