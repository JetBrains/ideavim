package _Self.buildTypes

import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.golang
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule

object Compatibility : IdeaVimBuildType({
  id("IdeaVimCompatibility")
  name = "IdeaVim compatibility with external plugins"

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"
  }

  steps {
    script {
      name = "Load Verifier"
      scriptContent = """
        mkdir verifier1
        curl -f -L -o verifier1/verifier-cli-dev-all-2.jar "https://packages.jetbrains.team/files/p/ideavim/plugin-verifier/verifier-cli-dev-all-2.jar"
      """.trimIndent()
    }
    script {
      name = "Check"
      scriptContent = """
              # We use a custom build of verifier that downloads IdeaVim from dev channel
              # To create a custom build: Download plugin verifier repo, add an if that switches to dev channel for IdeaVim repo
              # At the moment it's com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository#getLastCompatibleVersionOfPlugin
              # Build using gradlew :intellij-plugin-verifier:verifier-cli:shadowJar
              # Upload verifier-cli-dev-all.jar artifact to the repo in IdeaVim space repo
              
              java --version
              java -jar verifier1/verifier-cli-dev-all-2.jar check-plugin '${'$'}org.jetbrains.IdeaVim-EasyMotion' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-dev-all-2.jar check-plugin '${'$'}eu.theblob42.idea.whichkey' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-dev-all-2.jar check-plugin '${'$'}IdeaVimExtension' [latest-IU] -team-city
              # Outdated java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}github.zgqq.intellij-enhance' [latest-IU] -team-city
              # java -jar verifier1/verifier-cli-dev-all-2.jar check-plugin '${'$'}com.github.copilot' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-dev-all-2.jar check-plugin '${'$'}com.github.dankinsoid.multicursor' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-dev-all-2.jar check-plugin '${'$'}com.joshestein.ideavim-quickscope' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-dev-all-2.jar check-plugin '${'$'}com.julienphalip.ideavim.peekaboo' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-dev-all-2.jar check-plugin '${'$'}com.julienphalip.ideavim.switch' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-dev-all-2.jar check-plugin '${'$'}com.julienphalip.ideavim.functiontextobj' [latest-IU] -team-city
              java -jar verifier1/verifier-cli-dev-all-2.jar check-plugin '${'$'}com.miksuki.HighlightCursor' [latest-IU] -team-city
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