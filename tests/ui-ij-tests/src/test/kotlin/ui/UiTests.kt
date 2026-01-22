/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.search.locators.byXpath
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
import java.awt.event.KeyEvent
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UiTests {
  init {
    StepsLogger.init()
  }

  private lateinit var commonSteps: CommonSteps

  private val testTextForEditor = """
                  |One Two
                  |Three Four
                  |Five
              """.trimMargin()

  @Test
  fun ideaVimTest() = uiTest("ideaVimTest") {
    val sharedSteps = JavaExampleSteps(this)
    commonSteps = CommonSteps(this)

    startNewProject()
    Thread.sleep(1000)

    closeUnrelated(sharedSteps)
    Thread.sleep(1000)

    idea {
      waitSmartMode()
      createFile("MyDoc.txt", this@uiTest)
      val editor = editor("MyDoc.txt") {
        step("Write a text") {
          injectText(testTextForEditor)
        }
      }
      waitFor(Duration.ofMinutes(1)) { editor.findAllText("One").isNotEmpty() }
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
//      testTripleClickRightFromLineEnd(editor)
      testClickRightFromLineEnd(editor)
      testClickOnWord(editor)
      testAddNewLineInNormalMode(editor)
      testMappingToCtrlOrAltEnter(editor)
      `simple enter in insert mode`(editor)
      testMilticaretEnter(editor)
      `simple enter in select mode`(editor)
      testMilticaretEnterInSelectMode(editor)
      reenableIdeaVim(editor)
      reenablingIdeaVimKeepsMappings(editor)

      createFile("MyTest.java", this@uiTest)
      val javaEditor = editor("MyTest.java") {
        step("Write a text") {
          injectText(
            """
                |class MyTest {
                |  public static void main() {
                |    System.out.println("Hello");
                |  }
                |}
            """.trimMargin(),
          )
        }
      }

      // This is a hack to wait till inline hints will appear
      Thread.sleep(5000)
      wrapWithIf(javaEditor)
      testTrackActionId(javaEditor)
      testActionGenerate(javaEditor)
      testActionNewElementSamePlace(javaEditor)
      testActionCopy(javaEditor)
    }
  }

  private fun closeUnrelated(sharedSteps: JavaExampleSteps) {
    with(sharedSteps) {
      closeTipOfTheDay()
      closeAllTabs()
    }
  }

  private fun RemoteRobot.startNewProject() {
    welcomeFrame {
      createNewProjectLink.click()
      dialog("New Project") {
        findText("Java").click()
        checkBox("Add sample code").unselect()
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
      editor.text,
    )

    editor.injectText(
      """
                |One Two
                |Three Four
                |Five
      """.trimMargin(),
    )
    keyboard {
      enterText(":set clipboard-=unnamed")
      enter()
    }
  }

  private fun IdeaFrame.wrapWithIf(editor: Editor) {
    editor.findText("System").click()
    remoteRobot.invokeActionJs("SurroundWith")
    Thread.sleep(1000)
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

  private fun IdeaFrame.testTrackActionId(editor: Editor) {
    commonSteps.invokeAction("VimFindActionIdAction")

    keyboard {
      escape()
    }

    findText("Copy Action Id").click()

    // Wait for confirmation notification - proves the clipboard was updated
    // and the new notification is visible (old notification has expired)
    waitFor(duration = Duration.ofSeconds(10)) {
      try {
        findText("Action id copied")
        true
      } catch (e: Throwable) {
        false
      }
    }

    findText("Stop Tracking").click()

    editor.findText("class").click()
    remoteRobot.invokeActionJs("EditorPaste")

    waitFor {
      """
                |EditorEscapeclass MyTest {
                |  public static void main() {
                |      if (true) {
                |          System.out.println("Hello");
                |      }
                |  }
                |}
      """.trimMargin() == editor.text
    }

    // Explicitly dismiss any visible notifications
    keyboard { escape() }

    // Wait briefly for notification to dismiss
    waitFor(duration = Duration.ofSeconds(3)) {
      !hasText("Copy Action Id")
    }

    vimExit()
  }

  private fun IdeaFrame.testActionGenerate(editor: Editor) {
    val label = findAll<ComponentFixture>(byXpath("//div[@class='EngravedLabel']"))
    assertTrue(label.isEmpty())

    keyboard {
      enterText(":action Generate")
      enter()
    }

    waitFor {
      val generateDialog = findAll<ComponentFixture>(byXpath("//div[@class='EngravedLabel']"))
      if (generateDialog.size == 1) {
        return@waitFor generateDialog.single().hasText("Generate")
      }
      return@waitFor false
    }

    keyboard { escape() }
  }

  private fun IdeaFrame.testActionNewElementSamePlace(editor: Editor) {
    val label = findAll<ComponentFixture>(byXpath("//div[@class='EngravedLabel']"))
    assertTrue(label.isEmpty())

    keyboard {
      enterText(":action NewElementSamePlace")
      enter()
    }

    waitFor {
      val generateDialog = findAll<ComponentFixture>(byXpath("//div[@class='EngravedLabel']"))
      if (generateDialog.size == 1) {
        return@waitFor generateDialog.single().hasText("New in This Directory")
      }
      return@waitFor false
    }

    keyboard { escape() }
  }

  private fun IdeaFrame.testActionCopy(editor: Editor) {
    val label = findAll<ComponentFixture>(byXpath("//div[@class='EngravedLabel']"))
    assertTrue(label.isEmpty())

    keyboard {
      enterText(":action CopyReferencePopupGroup")
      enter()
    }

    waitFor {
      val generateDialog = findAll<ComponentFixture>(byXpath("//div[@class='EngravedLabel']"))
      if (generateDialog.size == 1) {
        return@waitFor generateDialog.single().hasText("Copy")
      }
      return@waitFor false
    }

    keyboard { escape() }
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
      editor.text,
    )
  }

  // Test for VIM-3418
  private fun IdeaFrame.reenablingIdeaVimKeepsMappings(editor: Editor) {
    println("Run reenablingIdeaVimKeepsMappings...")

    keyboard {
      enterText(":imap <A-Q> HEYHEY<ESC>")
      enter()
      enterText("i")
      pressing(KeyEvent.VK_ALT) { enterText("q") }
    }

    assertEquals(
      """
      HEYHEYOne Two
      Three Four
      Five
      """.trimIndent(),
      editor.text,
    )

    toggleIdeaVim()
    keyboard {
      key(KeyEvent.VK_RIGHT)
      enterText("-CHECK-")
    }

    toggleIdeaVim()

    keyboard {
      enterText("i")
      pressing(KeyEvent.VK_ALT) { enterText("q") }
    }

    assertEquals(
      """
      HEYHEY-CHECK-HEYHEYOne Two
      Three Four
      Five
      """.trimIndent(),
      editor.text,
    )

    vimExit()
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

    Thread.sleep(1000)
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

  // For VIM-3159
  private fun ContainerFixture.testAddNewLineInNormalMode(editor: Editor) {
    println("Run testAddNewLineInNormalMode...")

    commonSteps.invokeAction("EditorStartNewLineBefore")

    waitFor {
      """
      |
      |One Two
      |Three Four
      |Five
    """.trimMargin() == editor.text
    }

    editor.injectText(testTextForEditor)

    commonSteps.invokeAction("EditorStartNewLine")

    waitFor {
      """
      |One Two
      |
      |Three Four
      |Five
    """.trimMargin() == editor.text
    }

    editor.injectText(testTextForEditor)

    vimExit()
  }

  // For VIM-3190
  private fun ContainerFixture.testMappingToCtrlOrAltEnter(editor: Editor) {
    println("Run testMappingToCtrlOrAltEnter...")

    keyboard {
      enterText(":nmap <C-Enter> k")
      enter()
      enterText(":nmap <A-Enter> G")
      enter()
    }

    // Set up initial position
    keyboard {
      enterText("jll")
    }
    assertEquals(10, editor.caretOffset)

    // Checking C-ENTER
    keyboard {
      pressing(KeyEvent.VK_CONTROL) { enter() }
    }
    assertEquals(2, editor.caretOffset)

    // Checking A-ENTER
    keyboard {
      pressing(KeyEvent.VK_ALT) { enter() }
    }
    assertEquals(19, editor.caretOffset)

    vimExit()
  }

  // For VIM-3186
  private fun ContainerFixture.testMilticaretEnter(editor: Editor) {
    println("Run testMilticaretEnter...")

    keyboard {
      pressing(KeyEvent.VK_ALT) {
        pressing(KeyEvent.VK_SHIFT) {
          findText("One").click()
          findText("Three").click()
          findText("Five").click()
        }
      }

      enterText("A")
      enter()
    }

    assertEquals(3, editor.caretCount)

    assertEquals(
      """
      |One Two
      |
      |Three Four
      |
      |Five
      |
    """.trimMargin(), editor.text
    )

    // Reset state
    keyboard {
      escape()
      escape()
    }
    assertEquals(1, editor.caretCount)
    editor.injectText(testTextForEditor)
    vimExit()
  }

  private fun ContainerFixture.`simple enter in insert mode`(editor: Editor) {
    println("Run test 'simple enter in insert mode'...")

    // Start of file
    keyboard {
      enterText("i")
      enter()
    }
    assertEquals(
      """
      |
      |One Two
      |Three Four
      |Five
    """.trimMargin(),
      editor.text
    )

    // Middle of file
    findText("Four").click()
    keyboard { enter() }
    assertEquals(
      """
      |
      |One Two
      |Three 
      |Four
      |Five
    """.trimMargin(),
      editor.text
    )

    // End of file
    val fivePoint = findText("Five").point
    val endOfLine = Point(fivePoint.x + 50, fivePoint.y)
    click(endOfLine)
    keyboard { enter() }
    assertEquals(
      """
      |
      |One Two
      |Three 
      |Four
      |Five
      |
    """.trimMargin(),
      editor.text
    )

    editor.injectText(testTextForEditor)
    vimExit()
  }


  private fun ContainerFixture.`simple enter in select mode`(editor: Editor) {
    println("Run test 'simple enter in select mode'...")

    findText("Four").doubleClick()

    keyboard {
      pressing(KeyEvent.VK_CONTROL) { enterText("g") }
      enter()
    }

    assertEquals(
      """
      |One Two
      |Three 
      |
      |Five
    """.trimMargin(),
      editor.text
    )

    editor.injectText(testTextForEditor)
    vimExit()
  }

  // For VIM-3186
  private fun ContainerFixture.testMilticaretEnterInSelectMode(editor: Editor) {
    println("Run testMilticaretEnter...")

    keyboard {
      pressing(KeyEvent.VK_ALT) {
        pressing(KeyEvent.VK_SHIFT) {
          findText("One").click()
          findText("Three").click()
          findText("Five").click()
        }
      }

      enterText("$")
      enterText("v")
      pressing(KeyEvent.VK_CONTROL) { enterText("g") }
      enter()
    }

    assertEquals(3, editor.caretCount)

    assertEquals(
      """
      |One Tw
      |
      |Three Fou
      |
      |Fiv
      |
    """.trimMargin(), editor.text
    )

    // Reset state
    keyboard {
      escape()
      escape()
    }
    assertEquals(1, editor.caretCount)
    editor.injectText(testTextForEditor)
    vimExit()
  }
}
