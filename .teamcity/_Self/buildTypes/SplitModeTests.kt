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
      name = "Start Xvfb and run split mode tests"
      scriptContent = """
              # Install Xvfb if not present
              if ! command -v Xvfb >/dev/null 2>&1; then
                sudo apt-get update -qq && sudo apt-get install -y -qq xvfb x11-utils >/dev/null
              fi

              # Kill any leftover Xvfb from previous runs
              pkill -f 'Xvfb :99' || true

              Xvfb :99 -screen 0 1920x1080x24 -ac -nolisten tcp &
              XVFB_PID=${'$'}!

              # Wait until the display is ready
              for i in $(seq 1 30); do
                if xdpyinfo -display :99 >/dev/null 2>&1; then
                  echo "Xvfb is ready on :99"
                  break
                fi
                sleep 1
              done

              if ! xdpyinfo -display :99 >/dev/null 2>&1; then
                echo "ERROR: Xvfb failed to start on :99"
                exit 1
              fi

              ./gradlew :tests:split-mode-tests:testSplitMode --console=plain --build-cache --configuration-cache --stacktrace
              TEST_EXIT=${'$'}?

              kill ${'$'}XVFB_PID 2>/dev/null || true
              exit ${'$'}TEST_EXIT
          """.trimIndent()
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
