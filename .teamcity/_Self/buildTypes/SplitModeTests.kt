/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package _Self.buildTypes

import _Self.AgentSize
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object SplitModeTests : IdeaVimBuildType({
  name = "Split mode tests"
  description = "Tests for IdeaVim in Remote Development split mode (backend + frontend)"

  artifactRules = """
        +:tests/split-mode-tests/build/reports => split-mode-tests/build/reports
        +:out/ide-tests/tests/**/log => out/ide-tests/log
        +:out/ide-tests/tests/**/frontend/log => out/ide-tests/frontend-log
    """.trimIndent()

  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
    param("env.DISPLAY", ":99")
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    script {
      name = "Run split mode tests"
      scriptContent = "./gradlew :tests:split-mode-tests:testSplitMode --console=plain --build-cache --configuration-cache --stacktrace"
    }
  }

  triggers {
    vcs {
      branchFilter = "+:<default>"
    }
  }

  requirements {
    // Use a larger agent for split-mode tests — they launch two full IDE instances
    equals("teamcity.agent.hardware.cpuCount", AgentSize.XLARGE)
    equals("teamcity.agent.os.family", "Linux")
  }
})
