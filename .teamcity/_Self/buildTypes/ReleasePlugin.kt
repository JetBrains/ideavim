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
  description = "Build and publish the release branch as a new IdeaVim version"

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
      name = "Checkout release branch"
      scriptContent = """
        set -e
        git checkout release
        echo Checked out release branch. It is published as is - only the EAP build moves it to master
        git fetch origin +master:refs/remotes/origin/master
      """.trimIndent()
    }
    script {
      name = "Take the changelog and what's new from master"
      scriptContent = """
        set -e
        # Both are maintained on master (the What's New PR is merged there), while master may be ahead
        # of the release branch. The changelog is taken from master without the To Be Released entries
        # for changes that are only on master; the What's New page was written for the release branch
        # by the Prepare What's New workflow, so it is taken as is. A patch release does not use it: it gets
        # the page of its minor, and the prepared page stays on master for the next minor or major release.
        cd scripts-ts
        npm ci --silent --no-fund --no-audit
        npx tsx src/prepareReleaseChangelog.ts .. origin/master
        cd ..
        if [ "%env.ORG_GRADLE_PROJECT_releaseType%" = "patch" ]; then
          echo "Patch release: What's New is the page of the minor it patches"
          exit 0
        fi
        tbr="src/main/resources/whatsnew-tbr.html"
        if ! git cat-file -e "origin/master:${'$'}tbr" 2>/dev/null; then
          echo "ERROR: master has no ${'$'}tbr. Run the Prepare What's New workflow and merge its PR first"
          exit 1
        fi
        git checkout origin/master -- "${'$'}tbr"
        # The page describes the release branch as it was when Prepare What's New ran. An EAP build or
        # a cherry-pick since then changed what the release contains, so the page may miss some of it.
        described=${'$'}(sed -n 's/^<!-- whatsnew-release-commit: \([0-9a-f]*\) -->${'$'}/\1/p' "${'$'}tbr")
        released=${'$'}(git rev-parse HEAD)
        if [ -z "${'$'}described" ]; then
          echo "ERROR: ${'$'}tbr does not say which release commit it describes. Run the Prepare What's New workflow and merge its PR first"
          exit 1
        fi
        if [ "${'$'}described" != "${'$'}released" ]; then
          echo "ERROR: ${'$'}tbr describes release commit ${'$'}described, but the release branch is at ${'$'}released."
          echo "Run the Prepare What's New workflow again and merge its PR. Commits since the page was written:"
          git log --oneline "${'$'}described..${'$'}released" || true
          exit 1
        fi
        echo "Took ${'$'}tbr from master, written for release commit ${'$'}released"
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
        if [ "%env.ORG_GRADLE_PROJECT_releaseType%" = "patch" ]; then
          # Patches roll into their parent minor, so they get the newest page of that minor. This way the IDE
          # and the What's New site have a page for every released version.
          minor=${'$'}(echo "${'$'}version" | cut -d. -f1,2)
          previous=${'$'}(ls src/main/resources/whatsnew-${'$'}minor.*.html 2>/dev/null | sort -V | tail -n 1)
          if [ -f "${'$'}target" ]; then
            echo "${'$'}target already exists"
          elif [ -n "${'$'}previous" ]; then
            cp "${'$'}previous" "${'$'}target"
            git add "${'$'}target"
            echo "Copied ${'$'}previous to ${'$'}target"
          else
            echo "WARN: no whatsnew-${'$'}minor.*.html page found; skipping What's New"
          fi
        elif [ -f "${'$'}tbr" ]; then
          git mv "${'$'}tbr" "${'$'}target"
          sed -i '/^<!-- whatsnew-release-commit: .* -->${'$'}/d' "${'$'}target"
          git add "${'$'}target"
          echo "Promoted whatsnew-tbr.html to whatsnew-${'$'}version.html"
        else
          echo "ERROR: ${'$'}tbr not found"
          exit 1
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
        # Master may be ahead of the release branch: the EAP build is the only thing that moves the
        # release branch to master, and fixes keep landing on master afterwards. So nothing here is
        # promoted from master's own To Be Released content - the released changelog section and the
        # What's New page are taken from the release branch, and master keeps the rest as unreleased.
        git checkout master
        git show release:CHANGES.md > /tmp/release-CHANGES.md
        cd scripts-ts
        npx tsx src/syncChangelogToMaster.ts "%build.number%" ../CHANGES.md /tmp/release-CHANGES.md
        cd ..
        version=${'$'}(echo "%build.number%" | tr '[:upper:]' '[:lower:]')
        tbr="src/main/resources/whatsnew-tbr.html"
        target="src/main/resources/whatsnew-${'$'}version.html"
        if git cat-file -e "release:${'$'}target" 2>/dev/null; then
          git checkout release -- "${'$'}target"
          echo "Took whatsnew-${'$'}version.html from the release branch"
          # The page is published now; the next What's New run starts a fresh one for the next release.
          # A patch release did not use it, so it stays for the next minor or major release.
          if [ "%env.ORG_GRADLE_PROJECT_releaseType%" != "patch" ] && [ -f "${'$'}tbr" ]; then
            git rm -q "${'$'}tbr"
            echo "Removed whatsnew-tbr.html on master"
          fi
        else
          echo "WARN: the release branch has no ${'$'}target; master's What's New is left as is"
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
      # A fast-forward: the release branch only got the preparation commit on top of what it already
      # had. Resetting it to master is the EAP build's job, and only it needs to force-push.
      git push origin release
      # Push tag
      git push origin %build.number%
      """.trimIndent()
    }
    script {
      name = "Run Integrations"
      scriptContent = """
        set -e
        cd scripts-ts
        npm ci --silent --no-fund --no-audit
        : "${'$'}{ORG_GRADLE_PROJECT_youtrackToken:?ORG_GRADLE_PROJECT_youtrackToken is not set}"
        export YOUTRACK_TOKEN="${'$'}ORG_GRADLE_PROJECT_youtrackToken"
        npx tsx src/releaseActions.ts "%build.number%" "%env.ORG_GRADLE_PROJECT_releaseType%" .. origin/master release
      """.trimIndent()
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
