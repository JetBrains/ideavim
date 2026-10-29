/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.rider

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/**
 * IDE actions run by IdeaVim (`:action`, `<Action>` mappings) in Rider. Many of them are implemented on the ReSharper
 * backend, and the frontend only delegates to it
 */
class RiderActionTest : IdeaVimRiderTestBase() {

  override fun ideaVimRcContent(): String = """
    |set NERDTree
    |nmap \m <Action>(MoveElementRight)
  """.trimMargin()

  @AfterEach
  fun resetMode() {
    esc()
  }

  // Control for the tests below: Rider implements MoveElementRight on its backend
  @Test
  fun `move element right with the IDE shortcut`() {
    setText(MOVE_ELEMENT_TEXT, caretAt = "1, 2")

    actionShortcut("MoveElementRight")

    assertText(MOVE_ELEMENT_TEXT.replace("1, 2", "2, 1"))
  }

  // VIM-3968
  @Test
  fun `move element right with an ex command`() {
    setText(MOVE_ELEMENT_TEXT, caretAt = "1, 2")

    exCommand("action MoveElementRight")

    assertText(MOVE_ELEMENT_TEXT.replace("1, 2", "2, 1"))
  }

  // VIM-3968
  @Test
  fun `move element right with an Action mapping`() {
    setText(MOVE_ELEMENT_TEXT, caretAt = "1, 2")

    typeVim("\\m")

    assertText(MOVE_ELEMENT_TEXT.replace("1, 2", "2, 1"))
  }

  // VIM-3376
  @Test
  fun `ex command opens the Generate popup`() {
    setText(CLASS_TEXT, caretAt = "private")

    exCommand("action Generate")

    assertPopup("Generate")
  }

  // VIM-3376
  @Test
  fun `ex command opens the New in This Directory popup`() {
    setText(CLASS_TEXT)

    exCommand("action NewElementSamePlace")

    assertPopup("New in This Directory")
  }

  // VIM-3376
  @Test
  fun `ex command opens the Copy reference popup`() {
    setText(CLASS_TEXT, caretAt = "Foo")

    exCommand("action CopyReferencePopupGroup")

    assertPopup("Copy")
  }

  // VIM-4336: Rider comments the line on its backend, after IdeaVim has already seen the action finish
  @Test
  fun `comment line with the IDE shortcut moves the caret to the next line`() {
    setText(COMMENT_TEXT, caretAt = "class")

    actionShortcut("CommentByLineComment")
    assertText(COMMENT_TEXT.replaceFirst("public", "// public"))
    assertCaretLine(1)

    actionShortcut("CommentByLineComment")
    assertText(COMMENT_TEXT.replaceFirst("public", "// public").replaceFirst("{", "// {"))
    assertCaretLine(2)
  }

  @Test
  fun `comment line with an ex command`() {
    setText(COMMENT_TEXT, caretAt = "class")

    exCommand("action CommentByLineComment")

    assertText(COMMENT_TEXT.replaceFirst("public", "// public"))
  }

  // VIM-3881
  @Test
  fun `undo and redo a change`() {
    setText(PROGRAM_TEXT, caretAt = "World")

    typeVim("ciwVim")
    esc()
    assertText(PROGRAM_TEXT.replace("World", "Vim"))

    typeVim("u")
    assertText(PROGRAM_TEXT)

    ctrl(KeyEvent.VK_R)
    assertText(PROGRAM_TEXT.replace("World", "Vim"))
  }

  // VIM-3861
  @Test
  fun `ex command stops the running program`() {
    setText(ENDLESS_PROGRAM_TEXT)
    exCommand("w")

    exCommand("action Run")
    val started = waitUntil(timeoutMs = 180_000, pollMs = 1000) { runningProcesses() > 0 }
    assertTrue(started) { "The program didn't start after :action Run" }

    exCommand("action Stop")
    val stopped = waitUntil(timeoutMs = 30_000, pollMs = 500) { runningProcesses() == 0 }
    assertTrue(stopped) { "The program is still running after :action Stop" }
  }

  // NERDTree moves in the project view with Swing actions, without the IDE actions that may need the backend
  @Test
  fun `NERDTree moves the selection in the solution view`() {
    setText(PROGRAM_TEXT)

    exCommand("NERDTreeFocus")
    var row: Int? = null
    val focused = waitUntil { row = focusedTreeRow(); row != null }
    assertTrue(focused) { "The focus isn't in a tree after :NERDTreeFocus" }
    val before = assertNotNull(row)

    typeInFocused("j")

    var after: Int? = before
    waitUntil { after = focusedTreeRow(); after != before }
    assertNotEquals(before, after, "j didn't move the selection")
  }

  private fun assertCaretLine(expected: Int) {
    var line = -1
    waitUntil { line = caretLine(); line == expected }
    assertEquals(expected, line, "Caret line")
  }

  companion object {
    private val PROGRAM_TEXT = """
      |// See https://aka.ms/new-console-template for more information
      |
      |Console.WriteLine("Hello, World!");
    """.trimMargin()

    private val MOVE_ELEMENT_TEXT = """
      |Foo(1, 2);
      |
      |static void Foo(int a, int b) { }
    """.trimMargin()

    private val CLASS_TEXT = """
      |Console.WriteLine(new Foo());
      |
      |class Foo
      |{
      |    private int bar;
      |    public Foo() { }
      |}
    """.trimMargin()

    // The class from VIM-4336
    private val COMMENT_TEXT = """
      |public sealed class ExternalPaymentResult
      |{
      |}
    """.trimMargin()

    private val ENDLESS_PROGRAM_TEXT = """
      |while (true) Thread.Sleep(100);
      |
    """.trimMargin()
  }
}
