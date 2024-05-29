/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package _Self.buildTypes

import _Self.IdeaVimBuildType
import _Self.vcsRoots.ReleasesVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

object PrintReleaseBranch : IdeaVimBuildType({
  name = "EXP: Print release branch"

  vcs {
    root(ReleasesVcsRoot)
    branchFilter = "+:heads/releases/*"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {

    script {
      name = "Print current branch"
      scriptContent = """
                echo "Current branch is: %teamcity.build.branch%"
            """.trimIndent()
    }
  }

  features {
    sshAgent {
      teamcitySshKey = "IdeaVim ssh keys"
    }
  }
})