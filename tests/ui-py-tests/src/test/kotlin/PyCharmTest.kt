/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

import com.automation.remarks.junit5.Video
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.steps.CommonSteps
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.Test
import ui.pages.IdeaFrame
import ui.pages.idea
import ui.pages.welcomeFrame

class PyCharmTest {
  init {
    StepsLogger.init()
  }

  private lateinit var commonSteps: CommonSteps

  @Test
  @Video
  fun run() = uiTest("ideaVimTest") {
    commonSteps = CommonSteps(this)

    startNewProject()
    Thread.sleep(1000)

    idea {
      waitSmartMode()

      testEnterWorksInPyConsole()
    }
  }

  private fun IdeaFrame.testEnterWorksInPyConsole() {
    findText("Python Console").click()

    Thread.sleep(1000)

    keyboard {
      enterText("print(123 + 321)")
      enter()
    }

    waitFor {
      findAllText("444").isNotEmpty()
    }
  }

  private fun RemoteRobot.startNewProject() {
    welcomeFrame {
      createNewProjectLink.click()
      button("Create").click()
    }
  }
}