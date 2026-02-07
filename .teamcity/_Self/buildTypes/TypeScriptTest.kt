package _Self.buildTypes

import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

object TypeScriptTest : IdeaVimBuildType({
  id("IdeaVimTests_TypeScript")
  name = "TypeScript Scripts Test"
  description = "Test that TypeScript scripts can run on TeamCity"

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    script {
      name = "Set up Node.js"
      scriptContent = """
        wget https://nodejs.org/dist/v20.18.1/node-v20.18.1-linux-x64.tar.xz
        tar xf node-v20.18.1-linux-x64.tar.xz
        export PATH="${"$"}PWD/node-v20.18.1-linux-x64/bin:${"$"}PATH"
        node --version
        npm --version
      """.trimIndent()
    }
    script {
      name = "Run TypeScript test"
      scriptContent = """
        export PATH="${"$"}PWD/node-v20.18.1-linux-x64/bin:${"$"}PATH"
        cd scripts-ts
        npm install
        npx tsx src/teamcityTest.ts
      """.trimIndent()
    }
  }

  requirements {
    equals("teamcity.agent.os.family", "Linux")
  }
})
