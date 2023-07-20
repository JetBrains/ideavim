/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

/**
 * JetBrains Space Automation
 * This Kotlin script file lets you automate build activities
 * For more info, see https://www.jetbrains.com/help/space/automation.html
 */

job("Deploy vim-engine library to intellij-dependencies") {
  parameters {
    text("spaceUsername", value = "{{ project:spaceUsername }}")
    secret("spacePassword", value = "{{ project:spacePassword }}")
    text("uploadUrl", value = "{{ project:uploadUrl }}")
  }
  container(displayName = "Publish Artifact", image = "amazoncorretto:17-alpine") {
    kotlinScript { api ->
      api.parameters["engineVersion"] = "0.0." + api.executionNumber()
      api.gradlew(":vim-engine:publish")
    }
  }
}