/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package _Self.buildTypes

import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

object CreateNewReleaseBranchFromMaster : IdeaVimBuildType({
  name = "EXP: Create new release branch from master"

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    script {
      name = "Calculate next potential release version"
      scriptContent = """
        #!/bin/bash

        # Fetch all remote branches
        git fetch --all

        # Get a list of all branches matching the pattern releases/x.y.z
        branches=${'$'}(git branch -r | grep -oE 'releases/[0-9]+\.[0-9]+\.x')

        # If no matching branches are found, print a message and exit
        if [[ -z "${'$'}branches" ]]; then
            echo "No release branches found"
            exit 1
        fi

        # Find the largest release version
        largest_release=${'$'}(echo "${'$'}branches" | sort -V | tail -n 1)

        # Print the largest release
        echo "Largest release branch: ${'$'}largest_release"
        echo "##teamcity[setParameter name='env.POTENTIAL_VERSION' value='${'$'}largest_release']"
      """.trimIndent()
    }

    script {
      name = "Show potential release version"
      scriptContent = """
                #!/bin/bash
                echo "Calculated or user-provided parameter value is: %env.POTENTIAL_VERSION%"
            """.trimIndent()
    }
  }

  params {
    param("env.POTENTIAL_VERSION", "")
  }

  features {
    sshAgent {
      teamcitySshKey = "IdeaVim ssh keys"
    }
  }
})