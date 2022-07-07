package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.golang
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule

object Compatibility : BuildType({
  id("IdeaVimCompatibility")
  name = "IdeaVim compatibility with external plugins"

  vcs {
    root(DslContext.settingsRoot)
  }

  steps {
    script {
      name = "Check"
      scriptContent = """
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}org.jetbrains.IdeaVim-EasyMotion' [latest-IU] -team-city
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}io.github.mishkun.ideavimsneak' [latest-IU] -team-city
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}eu.theblob42.idea.whichkey' [latest-IU] -team-city
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}IdeaVimExtension' [latest-IU] -team-city
                # Outdated java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}github.zgqq.intellij-enhance' [latest-IU] -team-city
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}com.github.copilot' [latest-IU] -team-city
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}com.github.dankinsoid.multicursor' [latest-IU] -team-city
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}com.joshestein.ideavim-quickscope' [latest-IU] -team-city
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
})