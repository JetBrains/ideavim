/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

import com.automation.remarks.junit5.Video
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.steps.CommonSteps
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import org.assertj.swing.core.MouseButton
import org.junit.jupiter.api.Test
import ui.pages.Editor
import ui.pages.IdeaFrame
import ui.pages.actionMenu
import ui.pages.actionMenuItem
import ui.pages.editor
import ui.pages.idea
import ui.pages.welcomeFrame
import ui.utils.StepsLogger
import ui.utils.uiTest
import java.time.Duration
import kotlin.test.assertEquals

class RiderUiTest {
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

      createFile("1.txt", this@uiTest)
      val editor = editor("1.txt") {
        step("Write a text") {
          injectText(
            """
            |One Two
            |Three Four
          """.trimMargin()
          )
        }
      }
      waitFor(Duration.ofMinutes(1)) { editor.findAllText("One").isNotEmpty() }

      testEnterWorksInNormalMode(editor)
    }
  }

  private fun IdeaFrame.testEnterWorksInNormalMode(editor: Editor) {
    editor.findText("Two").click()
    keyboard {
      enter()
    }

    assertEquals(
      """
      |One Two
      |Three Four
    """.trimMargin(), editor.text
    )

    assertEquals(8, editor.caretOffset)
  }

  private fun RemoteRobot.startNewProject() {
    welcomeFrame {
      createNewProjectLink.click()
      button("Create").click()
    }
  }
}

private fun IdeaFrame.createFile(fileName: String, remoteRobot: RemoteRobot) {
  step("Create $fileName file") {
    with(projectViewTree) {
      setExpandTimeout(30_000)
      expand(projectName, "src")
      findText("src").click(MouseButton.RIGHT_BUTTON)
    }
    remoteRobot.actionMenu("New").click()
    remoteRobot.actionMenuItem("File").click()
    keyboard { enterText(fileName); enter() }
  }
}

