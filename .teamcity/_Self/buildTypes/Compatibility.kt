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
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.golang
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule

object Compatibility : IdeaVimBuildType({
  id("IdeaVimCompatibility")
  name = "IdeaVim compatibility with external plugins"

  failureConditions {
    executionTimeoutMin = 180
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"
  }

  steps {
    script {
      name = "Load Verifier"
      scriptContent = """
        mkdir verifier1
        curl -f -L -o verifier1/verifier-cli-ideavim.jar "https://github.com/AlexPl292/intellij-plugin-verifier/releases/download/cli-3/verifier-cli-1.403-ideavim-3-all.jar"
      """.trimIndent()
    }
    script {
      name = "Check"
      scriptContent = """
              # We use a custom build of plugin-verifier that resolves IdeaVim from the dev channel.
              # The fork lives at https://github.com/AlexPl292/intellij-plugin-verifier — the patch is in
              # com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository#getLastCompatibleVersionOfPlugin
              # (switches the marketplace channel to "dev" when pluginId is org.jetbrains.IdeaVim).
              #
              # To refresh against upstream:
              #   1. In the fork, pull from upstream and re-apply the dev-channel patch.
              #   2. Run the "Publish verifier-cli" workflow:
              #      https://github.com/AlexPl292/intellij-plugin-verifier/actions/workflows/publish-verifier-cli.yml
              #      It builds the shadow jar and attaches it to a new GitHub Release.
              #   3. Update the release URL in the "Load Verifier" step above to point at the new jar.

              java --version
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}org.jetbrains.IdeaVim-EasyMotion' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}eu.theblob42.idea.whichkey' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}IdeaVimExtension' [latest-IU] -team-city
              # Outdated java -jar verifier/verifier-cli-ideavim.jar check-plugin '${'$'}github.zgqq.intellij-enhance' [latest-IU] -team-city
              # java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.github.copilot' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.github.dankinsoid.multicursor' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.joshestein.ideavim-quickscope' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.julienphalip.ideavim.peekaboo' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.julienphalip.ideavim.switch' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.julienphalip.ideavim.functiontextobj' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.miksuki.HighlightCursor' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.ugarosa.idea.edgemotion' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}cn.mumukehao.plugin' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.magidc.ideavim.dial' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}dev.ghostflyby.ideavim.toggleIME' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.magidc.ideavim.anyObject' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.yelog.ideavim.cmdfloat' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}gg.ninetyfive' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.github.pooryam92.vimcoach' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}lazyideavim.whichkeylazy' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}com.github.vimkeysuggest' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-ideavim.jar check-plugin '${'$'}dev.ckob.lazygit' [latest-IU] -team-city
            """.trimIndent()
    }
  }

  triggers {
    schedule {
      schedulingPolicy = daily {
        hour = 4
      }
      branchFilter = ""
      triggerBuild = always()
      withPendingChangesOnly = false
    }
  }

  features {
    golang {
      testFormat = "json"
    }
  }

  requirements {
    equals("teamcity.agent.hardware.cpuCount", AgentSize.MEDIUM)
    equals("teamcity.agent.os.family", "Linux")
  }
})