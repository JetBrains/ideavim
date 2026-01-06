/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.steps.CommonSteps
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
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
import kotlin.test.assertEquals

class RiderUiTest {
  init {
    StepsLogger.init()
  }

  private lateinit var commonSteps: CommonSteps

  @Test
  fun run() = uiTest("ideaVimTest") {
    commonSteps = CommonSteps(this)

    startNewProject()
    Thread.sleep(1000)

    idea {
      waitSmartMode()

      val editor = editor("Program.cs")

      testEnterWorksInNormalMode(editor)
      testReformatCodeUsingMappingWithSpace(editor)
      testEnterInInsertMode(editor)
    }
  }

  private fun IdeaFrame.testEnterInInsertMode(editor: Editor) {
    editor.findText(" for more information").click()
    keyboard {
      enterText("A")
      enter()
    }
    Thread.sleep(1000)
    assertEquals(
      """
      |// See https://aka.ms/new-console-template for more information
      |
      |
      |Console.WriteLine("Hello, World!");
    """.trimMargin(), editor.text
    )

    keyboard {
      escape()
      enterText("dd")
    }
  }

  private fun IdeaFrame.testReformatCodeUsingMappingWithSpace(editor: Editor) {
    editor.findText(" for more information").click()
    keyboard {
      enterText("jj>>")

      enterText(":map <Space>x <Action>(ReformatCode)")
      enter()

      enterText(" x")
    }

    Thread.sleep(1000)
    assertEquals(
      """
      |// See https://aka.ms/new-console-template for more information
      |
      |Console.WriteLine("Hello, World!");
    """.trimMargin(), editor.text
    )
  }

  private fun IdeaFrame.testEnterWorksInNormalMode(editor: Editor) {
    editor.findText(" for more information").click()
    keyboard {
      enter()
    }

    assertEquals(
      """
      |// See https://aka.ms/new-console-template for more information
      |
      |Console.WriteLine("Hello, World!");
    """.trimMargin(), editor.text
    )

    assertEquals(64, editor.caretOffset)
  }

  private fun RemoteRobot.startNewProject() {
    manageLicensesFrame {
      enableFreeTier()
    }
    welcomeFrame {
      createNewSolutionLink.click()
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

