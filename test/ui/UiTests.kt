/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import org.assertj.swing.core.MouseButton
import org.intellij.examples.simple.plugin.steps.JavaExampleSteps
import org.junit.Ignore
import org.junit.Test
import ui.pages.Editor
import ui.pages.actionMenu
import ui.pages.actionMenuItem
import ui.pages.dialog
import ui.pages.editor
import ui.pages.gutter
import ui.pages.idea
import ui.pages.welcomeFrame
import ui.utils.StepsLogger
import ui.utils.doubleClickOnRight
import ui.utils.moveMouseForthAndBack
import ui.utils.moveMouseInGutterTo
import ui.utils.moveMouseTo
import ui.utils.tripleClickOnRight
import ui.utils.uiTest
import ui.utils.vimExit
import java.awt.event.KeyEvent
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UiTests {
  init {
    StepsLogger.init()
  }

  @Test
  @Ignore("Please start it manually")
  fun ideaVimTest() = uiTest {
    val sharedSteps = JavaExampleSteps(this)

    welcomeFrame {
      createNewProjectLink.click()
      dialog("New Project") {
        findText("Java").click()
        find(
          ComponentFixture::class.java,
          byXpath("//div[@class='FrameworksTree']")
        ).findText("Kotlin/JVM").click()
        runJs("robot.pressAndReleaseKey(${KeyEvent.VK_SPACE})")
        button("Next").click()
        button("Finish").click()
      }
    }
    sharedSteps.closeTipOfTheDay()
    idea {
      step("Create App file") {
        with(projectViewTree) {
          findText(projectName).doubleClick()
          waitFor { hasText("src") }
          findText("src").click(MouseButton.RIGHT_BUTTON)
        }
        actionMenu("New").click()
        actionMenuItem("File").click()
        keyboard { enterText("MyDoc.txt"); enter() }
      }
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

      testSelectTextWithMouseInGutter(editor)
      testSelectForthAndBack(editor)
      testSelectTextUsingMouse(editor)
      testTripleClickRightFromLineEnd(editor)
      testClickRightFromLineEnd(editor)
      testClickOnWord(editor)
      testGutterClick(editor)

    }
  }

  private fun ContainerFixture.testSelectTextWithMouseInGutter(editor: Editor) {
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

  private fun ContainerFixture.testSelectTextUsingMouse(editor: Editor) {
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

  private fun ContainerFixture.testSelectForthAndBack(editor: Editor) {
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
    editor.findText("Two").doubleClickOnRight(40, editor)

    assertEquals("Two", editor.selectedText)
    assertEquals(6, editor.caretOffset)

    keyboard { enterText("h") }

    assertEquals("Tw", editor.selectedText)
    assertEquals(5, editor.caretOffset)

    vimExit()
  }

  private fun ContainerFixture.testClickOnWord(editor: Editor) {
    editor.findText("One").doubleClick(MouseButton.LEFT_BUTTON)

    assertEquals("One", editor.selectedText)
    assertEquals(2, editor.caretOffset)

    keyboard { enterText("h") }

    assertEquals("On", editor.selectedText)
    assertEquals(1, editor.caretOffset)

    vimExit()
  }

  private fun ContainerFixture.testGutterClick(editor: Editor) {
    gutter {
      findText("2").click()
    }

    assertEquals("Three Four\n", editor.selectedText)
    assertEquals(8, editor.caretOffset)

    keyboard {
      enterText("k")
    }

    assertEquals("One Two\nThree Four\n", editor.selectedText)
    assertEquals(0, editor.caretOffset)

    vimExit()
  }
}
