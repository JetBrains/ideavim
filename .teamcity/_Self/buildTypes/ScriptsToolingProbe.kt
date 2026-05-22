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

// Diagnostic build: verifies that node / npm / npx are available on the release-class
// Linux agent so we can wire scripts-ts CLIs (e.g., promoteChangelog) into the release
// pipeline. Run manually; once we confirm the toolchain, this build type can be removed.
object ScriptsToolingProbe : IdeaVimBuildType({
  name = "Scripts tooling probe"
  description = "Diagnostic: report node/npm/npx availability on a release-class agent"

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"
    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    script {
      name = "Print node/npm versions"
      scriptContent = """
        echo "--- which ---"
        command -v node || echo "node NOT FOUND"
        command -v npm || echo "npm NOT FOUND"
        command -v npx || echo "npx NOT FOUND"
        echo ""
        echo "--- versions ---"
        node --version 2>&1 || true
        npm --version 2>&1 || true
        npx --version 2>&1 || true
        echo ""
        echo "--- PATH ---"
        echo "${'$'}PATH"
      """.trimIndent()
    }
  }

  requirements {
    equals("teamcity.agent.hardware.cpuCount", AgentSize.MEDIUM)
    equals("teamcity.agent.os.family", "Linux")
  }
})
