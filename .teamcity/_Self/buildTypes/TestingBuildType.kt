/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("ClassName")

package _Self.buildTypes

import _Self.AgentSize
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

open class TestingBuildType(
  private val testName: String,
  private val branch: String = "<default>",
  private val version: String = testName,
  // JVM used to compile & run the tests. Must match the target platform's bytecode level:
  // 2026.1 / Latest EAP are built with JVM target 25, 2025.3 and earlier with 21.
  // 21 is the default (matches gradle.properties); newer platforms override to 25.
  private val javaVersion: String = "21",
  private val javaPlugin: Boolean = true,
) : IdeaVimBuildType({
  id("IdeaVimTests_${testName.vanish()}")
  name = "Tests for IntelliJ $testName"

  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_legacyNoJavaPlugin", javaPlugin.not().toString())
    param("env.ORG_GRADLE_PROJECT_ideaVersion", version)
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
    param("env.ORG_GRADLE_PROJECT_javaVersion", javaVersion)
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:$branch"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    gradle {
      clearConditions()
      tasks = "clean test -x :tests:property-tests:test -x :tests:long-running-tests:test"
      buildFile = ""
      enableStacktrace = true
      gradleParams = "--no-build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-$javaVersion-amazon-corretto"
    }
  }

  triggers {
    vcs {
      branchFilter = "+:$branch"
    }
  }

  failureConditions {
    failOnMetricChange {
      metric = BuildFailureOnMetric.MetricType.TEST_COUNT
      threshold = 20
      units = BuildFailureOnMetric.MetricUnit.PERCENTS
      comparison = BuildFailureOnMetric.MetricComparison.LESS
      compareTo = build {
        buildRule = lastSuccessful()
      }
    }
  }

  requirements {
    equals("teamcity.agent.hardware.cpuCount", AgentSize.MEDIUM)
    equals("teamcity.agent.os.family", "Linux")
  }
})

private fun String.vanish(): String {
  return this
    .replace(' ', '_')
    .replace('.', '_')
    .replace('-', '_')
}
