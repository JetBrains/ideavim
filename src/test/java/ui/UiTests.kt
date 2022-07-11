/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ui

import com.automation.remarks.junit.VideoRule
import com.automation.remarks.video.annotations.Video
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import org.assertj.swing.core.MouseButton
import org.junit.Rule
import org.junit.Test
import ui.pages.Editor
import ui.pages.IdeaFrame
import ui.pages.actionMenu
import ui.pages.actionMenuItem
import ui.pages.dialog
import ui.pages.editor
import ui.pages.gutter
import ui.pages.idea
import ui.pages.welcomeFrame
import ui.utils.JavaExampleSteps
import ui.utils.StepsLogger
import ui.utils.doubleClickOnRight
import ui.utils.invokeActionJs
import ui.utils.moveMouseForthAndBack
import ui.utils.moveMouseInGutterTo
import ui.utils.moveMouseTo
import ui.utils.moveMouseWithDelayTo
import ui.utils.tripleClickOnRight
import ui.utils.uiTest
import ui.utils.vimExit
import java.awt.Point
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UiTests {
  init {
    StepsLogger.init()
  }

  @Rule
  @JvmField
  var videoRule = VideoRule()

  @Test
  @Video
  fun ideaVimTest() = uiTest("ideaVimTest") {
    val sharedSteps = JavaExampleSteps(this)

    startNewProject()
    Thread.sleep(1000)

    closeUnrelated(sharedSteps)
    Thread.sleep(1000)

    idea {
      createFile("MyDoc.txt", this@uiTest)
      val editor = editor("MyDoc.txt") {
        step("Write a text") {
          injectText(
            """
                |One Two
                |Three Four
                |Five
            """.trimMargin()
          )
        }
      }
      testSelectTextWithDelay(editor)
      testExtendSelection(editor)
      testLargerDragSelection(editor)
      testSelectLastCharacter(editor)
      testMicrodragSelection(editor)
      testUnnamedClipboard(editor)
      testSelectAndRightClick(editor)
      testSelectTextWithMouseInGutter(editor)
      testSelectForthAndBack(editor)
      testSelectTextUsingMouse(editor)
      testTripleClickRightFromLineEnd(editor)
      testClickRightFromLineEnd(editor)
      testClickOnWord(editor)
      testGutterClick(editor)
      reenableIdeaVim(editor)

      createFile("MyTest.java", this@uiTest)
      val javaEditor = editor("MyTest.java") {
        step("Write a text") {
          injectText(
            """
                |class Main {
                |  public static void main() {
                |    System.out.println("Hello");
                |  }
                |}
            """.trimMargin()
          )
        }
      }

      wrapWithIf(javaEditor)
    }
  }

  private fun closeUnrelated(sharedSteps: JavaExampleSteps) {
    with(sharedSteps) {
      closeIdeaVimDialog()
      closeTipOfTheDay()
      closeAllTabs()
    }
  }

  private fun RemoteRobot.startNewProject() {
    welcomeFrame {
      createNewProjectLink.click()
      dialog("New Project") {
        findText("Java").click()
        checkBox("Add sample code").select()
        button("Create").click()
      }
    }
  }

  private fun IdeaFrame.testUnnamedClipboard(editor: Editor) {
    keyboard {
      enterText(":set clipboard+=unnamed")
      enter()

      enterText("gg")
      enterText("yy")
      enterText("jyy")
      enterText("jyy")
      enterText("p")
      enterText("p")
      enterText("p")
    }

    assertEquals(
      """
      One Two
      Three Four
      Five
      Five
      Five
      Five
      
      """.trimIndent(),
      editor.text
    )

    editor.injectText(
      """
                |One Two
                |Three Four
                |Five
            """.trimMargin()
    )
    keyboard {
      enterText(":set clipboard-=unnamed")
      enter()
    }
  }

  private fun IdeaFrame.wrapWithIf(editor: Editor) {
    editor.findText("System").click()
    remoteRobot.invokeActionJs("SurroundWith")
    editor.keyboard { enter() }

//    assertFalse(editor.isBlockCursor)

    editor.keyboard {
      enterText("true")
      escape()
    }
//    assertTrue(editor.isBlockCursor)
    editor.keyboard {
      enterText("h")
      enterText("v")
    }
    assertEquals("u", editor.selectedText)

    vimExit()
  }

  private fun IdeaFrame.createFile(fileName: String, remoteRobot: RemoteRobot) {
    step("Create $fileName file") {
      with(projectViewTree) {
        expand(projectName, "src")
        findText("src").click(MouseButton.RIGHT_BUTTON)
      }
      remoteRobot.actionMenu("New").click()
      remoteRobot.actionMenuItem("File").click()
      keyboard { enterText(fileName); enter() }
    }
  }

  private fun IdeaFrame.reenableIdeaVim(editor: Editor) {
    println("Run reenableIdeaVim...")
    toggleIdeaVim()

    val from = editor.findText("One")
    from.doubleClick()

    editor.click()

    toggleIdeaVim()

    from.click()

    editor.keyboard {
      enterText("i")
      enterText("Hello")
      escape()
      enterText("4h")
      enterText("5x")
    }
    assertEquals(
      """
      One Two
      Three Four
      Five
      """.trimIndent(),
      editor.text
    )
  }

  private fun IdeaFrame.toggleIdeaVim() {
    this.remoteRobot.invokeActionJs("VimPluginToggle")
  }

  private fun ContainerFixture.testSelectTextWithMouseInGutter(editor: Editor) {
    println("Run testSelectTextWithMouseInGutter...")
    gutter {
      val from = findText("1")
      val to = findText("2")

      from.moveMouseInGutterTo(to, this)
    }

    Thread.sleep(1000)

    assertEquals("One Two\nThree Four\n", editor.selectedText)

    keyboard { enterText("j") }
    assertEquals("One Two\nThree Four\nFive", editor.selectedText)

    vimExit()
  }

  private fun ContainerFixture.testMicrodragSelection(editor: Editor) {
    println("Run testMicrodragSelection...")

    val point = editor.findText("Four").point
    val startPoint = Point(point.x + 50, point.y)
    val endPoint = Point(point.x + 49, point.y)

    startPoint.moveMouseTo(endPoint, editor)

    // Assert there was no selection
    keyboard {
      enterText("v")
    }
    assertEquals("r", editor.selectedText)

    vimExit()
  }

  private fun ContainerFixture.testLargerDragSelection(editor: Editor) {
    println("Run testMicrodragSelection...")

    val point = editor.findText("Four").point
    val startPoint = Point(point.x + 50, point.y)
    val endPoint = Point(point.x + 40, point.y)

    startPoint.moveMouseTo(endPoint, editor)

    // Assert there was no selection
    keyboard {
      enterText("v")
    }
    assertEquals("r", editor.selectedText)

    vimExit()
  }

  private fun ContainerFixture.testExtendSelection(editor: Editor) {
    println("Run testExtendSelection...")

    // Preparations
    val from = editor.findText("One")
    val to = editor.findText("Four")
    from.moveMouseTo(to, editor)
    vimExit()

    keyboard {
      enterText(":action EditorSelectWord")
      enter()
    }

    assertEquals("One", editor.selectedText)

    keyboard {
      enterText("l")
    }

    assertEquals("ne", editor.selectedText)

    vimExit()
  }

  private fun ContainerFixture.testSelectTextWithDelay(editor: Editor) {
    println("Run testSelectTextUsingMouse...")
    val from = editor.findText("One")
    val to = editor.findText("Four")

    val caretIsBlockWhileDragging = from.moveMouseWithDelayTo(to, editor)
    assertFalse(caretIsBlockWhileDragging)

    Thread.sleep(1000)

    assertEquals("One Two\nThree ", editor.selectedText)

    keyboard { enterText("l") }
    assertEquals("One Two\nThree F", editor.selectedText)

    vimExit()
  }

  private fun ContainerFixture.testSelectLastCharacter(editor: Editor) {
    println("Run testSelectLastCharacter...")

    val point = editor.findText("Four").point
    val startPoint = Point(point.x + 50, point.y)

    startPoint.moveMouseTo(point, editor)

    assertEquals("Four", editor.selectedText)

    vimExit()
  }

  private fun ContainerFixture.testSelectTextUsingMouse(editor: Editor) {
    println("Run testSelectTextUsingMouse...")
    val from = editor.findText("One")
    val to = editor.findText("Four")

    val caretIsBlockWhileDragging = from.moveMouseTo(to, editor)
    assertFalse(caretIsBlockWhileDragging)

    Thread.sleep(1000)

    assertEquals("One Two\nThree ", editor.selectedText)

    keyboard { enterText("l") }
    assertEquals("One Two\nThree F", editor.selectedText)

    vimExit()
  }

  private fun ContainerFixture.testSelectAndRightClick(editor: Editor) {
    println("Run testSelectTextUsingMouse...")
    val from = editor.findText("One")
    val to = editor.findText("Five")

    val caretIsBlockWhileDragging = from.moveMouseTo(to, editor)
    assertFalse(caretIsBlockWhileDragging)

    Thread.sleep(1000)

    // Right click
    editor.findText("Two").click(MouseButton.RIGHT_BUTTON)

    Thread.sleep(1000)

    assertTrue(editor.selectedText.isNotEmpty())

    // Reset state
    editor.findText("One").click()
    vimExit()
  }

  private fun ContainerFixture.testSelectForthAndBack(editor: Editor) {
    println("Run testSelectForthAndBack...")
    val from = editor.findText("Two")
    val to = editor.findText("Four")

    from.moveMouseForthAndBack(to, editor)
    Thread.sleep(1000)

    // Currently null can't be serialized, so we cant get empty string as a selected text. So we move caret a bit,
    //   enter visual mode and check that only the char under the caret is selected.
    keyboard { enterText("l") }
    keyboard { enterText("v") }
    assertEquals("w", editor.selectedText)

    vimExit()
  }

  private fun ContainerFixture.testTripleClickRightFromLineEnd(editor: Editor) {
    println("Run testTripleClickRightFromLineEnd...")
    editor.findText("Two").tripleClickOnRight(40, editor)

    assertEquals("One Two\n", editor.selectedText)
    assertEquals(7, editor.caretOffset)

    keyboard { enterText("h") }

    assertEquals("One Two\n", editor.selectedText)
    assertEquals(6, editor.caretOffset)

    keyboard { enterText("j") }

    assertEquals("One Two\nThree Four\n", editor.selectedText)
    assertEquals(14, editor.caretOffset)

    vimExit()
  }

  private fun ContainerFixture.testClickRightFromLineEnd(editor: Editor) {
    println("Run testClickRightFromLineEnd...")
    editor.findText("Two").doubleClickOnRight(40, editor)

    assertEquals("Two", editor.selectedText)
    assertEquals(6, editor.caretOffset)

    keyboard { enterText("h") }

    assertEquals("Tw", editor.selectedText)
    assertEquals(5, editor.caretOffset)

    vimExit()
  }

  private fun ContainerFixture.testClickOnWord(editor: Editor) {
    println("Run testClickOnWord...")
    editor.findText("One").doubleClick(MouseButton.LEFT_BUTTON)

    assertEquals("One", editor.selectedText)
    assertEquals(2, editor.caretOffset)

    keyboard { enterText("h") }

    assertEquals("On", editor.selectedText)
    assertEquals(1, editor.caretOffset)

    vimExit()
  }

  private fun ContainerFixture.testGutterClick(editor: Editor) {
    println("Run testGutterClick...")
    gutter {
      findText("2").click()
    }

    assertEquals("Three Four\n", editor.selectedText)

    keyboard {
      enterText("k")
    }

    assertEquals("One Two\nThree Four\n", editor.selectedText)

    vimExit()
  }
}
