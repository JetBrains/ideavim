/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

import com.intellij.remoterobot.steps.CommonSteps
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.Test
import ui.pages.IdeaFrame
import ui.pages.idea
import ui.utils.StepsLogger
import ui.utils.uiTest

class PyCharmTest {
  init {
    StepsLogger.init()
  }

  private lateinit var commonSteps: CommonSteps

  @Test
  fun run() = uiTest("ideaVimTest") {
    commonSteps = CommonSteps(this)

    // PyCharm now opens with a project directly, no need to create one
    idea {
      waitSmartMode()

      testEnterWorksInPyConsole()
    }
  }

  private fun IdeaFrame.testEnterWorksInPyConsole() {
//    waitFor(duration = Duration.ofMinutes(5)) {
//      findAllText("Python Console").isNotEmpty() &&
//        findAllText("Version Control").isNotEmpty() &&
//        findAllText("Python Packages").isNotEmpty() &&
//        isSmartMode()
//    }

    // Open tool window by id.
    // id taken from PythonConsoleToolWindowFactory.ID but it's not resolved in robot by some reason
    // the last 'x' is just to return some serializable value
    callJs<String>("com.intellij.openapi.wm.ToolWindowManager.getInstance(component.project).getToolWindow('Python Console').activate(null, true); 'x'", true)

    Thread.sleep(10_000)

    keyboard {
      enterText("print(123 + 321)")
      enter()
    }

    waitFor {
      findAllText("444").isNotEmpty()
    }
  }

}