/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.fixtures.JTreeFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.waitFor
import java.time.Duration

fun RemoteRobot.idea(function: IdeaFrame.() -> Unit) {
  find<IdeaFrame>(Duration.ofSeconds(60)).apply(function)
}

@FixtureName("Idea frame")
@DefaultXpath("IdeFrameImpl type", "//div[@class='IdeFrameImpl']")
class IdeaFrame(
  remoteRobot: RemoteRobot,
  remoteComponent: RemoteComponent,
) : CommonContainerFixture(remoteRobot, remoteComponent) {

  val projectViewTree: JTreeFixture
    get() {
      return find<JTreeFixture>(byXpath("MyProjectViewTree", "//div[@class='MyProjectViewTree']"), Duration.ofSeconds(30))
    }

  val projectName
    get() = step("Get project name") { return@step callJs<String>("component.getProject().getName()") }

  @JvmOverloads
  fun dumbAware(timeout: Duration = Duration.ofMinutes(5), function: () -> Unit) {
    step("Wait for smart mode") {
      waitFor(duration = timeout, interval = Duration.ofSeconds(5)) {
        runCatching { isDumbMode().not() }.getOrDefault(false)
      }
      function()
      step("..wait for smart mode again") {
        waitFor(duration = timeout, interval = Duration.ofSeconds(5)) {
          isDumbMode().not()
        }
      }
    }
  }

  fun waitSmartMode(timeout: Duration = Duration.ofMinutes(5)) {
    step("Wait for smart mode") {
      waitFor(duration = timeout, interval = Duration.ofSeconds(5)) {
        isSmartMode()
      }
    }
  }

  private fun isDumbMode(): Boolean {
    return callJs("com.intellij.openapi. project.DumbService.isDumb(component.project);", true)
  }

  fun isSmartMode(): Boolean {
    return callJs(
      "component.project == null ? false : !com.intellij.openapi. project.DumbService.isDumb(component.project);",
      true
    )
  }
}
