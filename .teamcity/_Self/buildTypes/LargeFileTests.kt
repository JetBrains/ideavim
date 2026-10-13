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

object LargeFileTests : IdeaVimBuildType({
  name = "Large file tests"
  description = "Tests for IdeaVim in a full IDE with large files (search, search count)"

  // The IDE logs include the thread dumps of UI freezes, which show where a slow operation spent its time
  artifactRules = """
        +:tests/large-file-tests/build/reports => large-file-tests-reports.zip
        +:out/ide-tests/tests/**/log => ide-tests-log.zip
    """.trimIndent()

  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    script {
      name = "Run large file tests"
      scriptContent = """
        export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
        export PATH="${'$'}JAVA_HOME/bin:${'$'}PATH"
        xvfb-run -a -s '-screen 0 1920x1080x24' ./gradlew :tests:large-file-tests:testLargeFiles --console=plain --build-cache --configuration-cache --stacktrace
      """.trimIndent()
    }
  }

  triggers {
    vcs {
      branchFilter = "+:<default>"
    }
  }

  requirements {
    // Use a larger agent, the same as for split-mode tests, so searching the large files doesn't hit the timeouts
    equals("teamcity.agent.hardware.cpuCount", AgentSize.XLARGE)
    equals("teamcity.agent.os.family", "Linux")
  }
})
