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

    // Wait for the IDE frame to appear after project creation
    // The new Rider startup behavior requires waiting for the welcome dialog to close
    // and the project to fully open before we can access the IdeFrameImpl
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
    // Handle license activation if the dialog appears
    try {
      manageLicensesFrame {
        enableFreeTier()
      }
    } catch (e: Exception) {
      // License frame not found, likely already activated
    }

    welcomeFrame {
      createNewSolutionLink.click()

      // Handle .NET SDK installation if needed
      // The "Install" button appears when .NET SDK is not detected
      try {
        val installButton = button("Install")
        if (installButton.isShowing && installButton.isEnabled()) {
          step("Install .NET SDK") {
            installButton.click()
            // Wait for SDK installation to complete and Create button to enable
            Thread.sleep(10000)
          }
        }
      } catch (e: Exception) {
        // Install button not found, SDK likely already installed
      }

      // Wait a bit more to ensure Create button is ready
      Thread.sleep(2000)
      button("Create").click()

      // Wait for the New Solution dialog to close after clicking Create
      // This is necessary due to new Rider startup behavior
      step("Wait for dialog to close") {
        Thread.sleep(15000)
      }
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

