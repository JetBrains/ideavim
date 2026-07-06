/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package _Self.buildTypes

import _Self.AgentSize
import _Self.Constants.DEFAULT_CHANNEL
import _Self.Constants.DEV_CHANNEL
import _Self.Constants.EAP_CHANNEL
import _Self.Constants.RELEASE
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

object ReleaseMajor : ReleasePlugin("major")
object ReleaseMinor : ReleasePlugin("minor")
object ReleasePatch : ReleasePlugin("patch")

sealed class ReleasePlugin(private val releaseType: String) : IdeaVimBuildType({
  name = "Publish $releaseType release"
  description = "Build and publish IdeaVim plugin"

  artifactRules = """
        build/distributions/*
        build/reports/*
    """.trimIndent()

  params {
    param("env.ORG_GRADLE_PROJECT_ideaVersion", RELEASE)
    password(
      "env.ORG_GRADLE_PROJECT_publishToken",
      "credentialsJSON:61a36031-4da1-4226-a876-b8148bf32bde",
      label = "Password"
    )
    param("env.ORG_GRADLE_PROJECT_publishChannels", "$DEFAULT_CHANNEL,$EAP_CHANNEL,$DEV_CHANNEL")
    password(
      "env.ORG_GRADLE_PROJECT_slackUrl",
      "credentialsJSON:a8ab8150-e6f8-4eaf-987c-bcd65eac50b5",
      label = "Slack URL"
    )
    password(
      "env.ORG_GRADLE_PROJECT_youtrackToken",
      "credentialsJSON:7bc0eb3a-b86a-4ebd-b622-d4ef12d7e1d3",
      display = ParameterDisplay.HIDDEN
    )
    param("env.ORG_GRADLE_PROJECT_releaseType", releaseType)
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    script {
      name = "Pull git tags"
      scriptContent = """
        mkdir -p ~/.ssh && chmod 700 ~/.ssh
        ssh-keyscan -H github.com >> ~/.ssh/known_hosts
        git fetch --tags --force origin
      """.trimIndent()
    }
    script {
      name = "Pull git history"
      scriptContent = "git fetch --unshallow"
    }
    script {
      name = "Reset release branch"
      scriptContent = """
        if [ "major" = "$releaseType" ] || [ "minor" = "$releaseType" ]
        then
          echo Resetting the release branch to the latest master because the release type is $releaseType
          git checkout master
          master_commit=${'$'}(git rev-parse HEAD)
          echo Master commit: ${'$'}master_commit
          git checkout release
          git reset --hard ${'$'}master_commit
        else
          git checkout release
          echo Do not reset the release branch because the release type is $releaseType
        fi
        echo Checked out release branch
      """.trimIndent()
    }
    gradle {
      name = "Calculate new version"
      tasks = "scripts:calculateNewVersion"
      gradleParams = "--build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    script {
      name = "Set TeamCity build number"
      scriptContent = """
        set -e
        cd scripts-ts
        npm ci --silent --no-fund --no-audit
        npx tsx src/setTeamCityBuildNumber.ts
      """.trimIndent()
    }
    script {
      name = "Update change log"
      scriptContent = """
        set -e
        cd scripts-ts
        npm ci --silent --no-fund --no-audit
        npx tsx src/promoteChangelog.ts "%build.number%" "%env.ORG_GRADLE_PROJECT_releaseType%" ..
      """.trimIndent()
    }
    script {
      name = "Update what's new"
      scriptContent = """
        set -e
        version=${'$'}(echo "%build.number%" | tr '[:upper:]' '[:lower:]')
        tbr="src/main/resources/whatsnew-tbr.html"
        target="src/main/resources/whatsnew-${'$'}version.html"
        if [ -f "${'$'}tbr" ]; then
          cp "${'$'}tbr" "${'$'}target"
          git add "${'$'}target"
          echo "Promoted whatsnew-tbr.html to whatsnew-${'$'}version.html"
        else
          echo "WARN: ${'$'}tbr not found; skipping What's New promotion"
        fi
      """.trimIndent()
    }
    script {
      name = "Commit preparation changes"
      scriptContent = """
        set -e
        git config user.name "IdeaVim Bot"
        git config user.email "maintainers@ideavim.dev"
        if git diff --quiet && git diff --cached --quiet; then
          echo "No preparation changes to commit"
        else
          git commit -am "Preparation to %build.number% release"
        fi
      """.trimIndent()
    }
    gradle {
      name = "Add release tag"
      tasks = "scripts:addReleaseTag"
      gradleParams = "--build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    script {
      name = "Run tests"
      scriptContent = """
        export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
        export PATH="${'$'}JAVA_HOME/bin:${'$'}PATH"
        ./gradlew test -x :tests:property-tests:test -x :tests:long-running-tests:test --build-cache --configuration-cache
      """.trimIndent()
    }
    gradle {
      name = "Publish release"
      tasks = "publishPlugin"
      gradleParams = "--build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    script {
      name = "Sync changelog and what's new to master"
      scriptContent = """
        set -e
        git checkout master
        cd scripts-ts
        npx tsx src/promoteChangelog.ts "%build.number%" "%env.ORG_GRADLE_PROJECT_releaseType%" ..
        cd ..
        version=${'$'}(echo "%build.number%" | tr '[:upper:]' '[:lower:]')
        tbr="src/main/resources/whatsnew-tbr.html"
        target="src/main/resources/whatsnew-${'$'}version.html"
        if [ -f "${'$'}tbr" ] && [ ! -f "${'$'}target" ]; then
          cp "${'$'}tbr" "${'$'}target"
          git add "${'$'}target"
          echo "Promoted whatsnew-tbr.html to whatsnew-${'$'}version.html on master"
        fi
        git config user.name "IdeaVim Bot"
        git config user.email "maintainers@ideavim.dev"
        if git diff --quiet && git diff --cached --quiet; then
          echo "No changes to commit on master"
        else
          git commit -am "Preparation to %build.number% release"
        fi
      """.trimIndent()
    }
    script {
      name = "Push changes to the repo"
      scriptContent = """
      set -e
      # Push master if it has the changelog promotion commit (soft-fail: a stale
      # master shouldn't block the marketplace release, but log loudly).
      git push origin master || echo "WARN: master push failed; sync manually"
      git checkout release
      echo checkout release branch
      git branch --set-upstream-to=origin/release release
      git push origin --force
      # Push tag
      git push origin %build.number%
      """.trimIndent()
    }
    gradle {
      name = "Run Integrations"
      tasks = "releaseActions"
      gradleParams = "--build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
  }

  features {
    sshAgent {
      teamcitySshKey = "IdeaVim ssh keys"
    }
  }

  requirements {
    equals("teamcity.agent.hardware.cpuCount", AgentSize.MEDIUM)
    equals("teamcity.agent.os.family", "Linux")
  }
})
