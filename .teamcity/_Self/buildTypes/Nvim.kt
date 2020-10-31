package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object Nvim : BuildType({
  name = "NVIM"
  description = "branch EAP"

  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_ideaVersion", "LATEST-EAP-SNAPSHOT")
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
  }

  vcs {
    root(DslContext.settingsRoot)

    checkoutMode = CheckoutMode.ON_SERVER
  }

  steps {
    script {
      name = "Download NeoVim"
      scriptContent = """
              wget https://github.com/neovim/neovim/releases/download/v0.4.4/nvim-linux64.tar.gz
              tar xzf nvim-linux64.tar.gz
              cd nvim-linux64/bin
              chmod +x nvim
              export IDEAVIM_NVIM_PATH=${'$'}(pwd)/nvim
              """.trimIndent()
    }
    gradle {
      tasks = "clean testWithNeovim"
      gradleParams = "-Dideavim.nvim.path=./nvim-linux64/bin/nvim"
      buildFile = ""
      enableStacktrace = true
      param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
    }
  }

  triggers {
    vcs {
      branchFilter = ""
    }
  }

  requirements {
    noLessThanVer("teamcity.agent.jvm.version", "1.8")
  }
})
