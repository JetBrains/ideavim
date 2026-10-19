/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.listener

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.fixtures.EditorMouseFixture
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.listener.VimPasteFromX11Handler
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The mouse press and the paste action are tested separately, because the mouse fixture does not trigger actions.
 */
class MiddleClickPasteTest : VimTestCase() {
  @BeforeEach
  fun assumePasteFromX11IsSupported() {
    assumeTrue(SystemInfo.isXWindow)
  }

  @Test
  fun `middle click in insert mode does not move caret`() {
    configureByText("Lorem ${c}ipsum dolor sit amet,\nconsectetur adipiscing elit")
    enterCommand("set mouse=a")
    typeText("i")

    val before = readOffset()
    middleClickAt(1, 10)
    assertEquals(before, readOffset())
  }

  @Test
  fun `middle click in normal mode moves caret`() {
    configureByText("Lorem ${c}ipsum dolor sit amet,\nconsectetur adipiscing elit")
    enterCommand("set mouse=a")

    val before = readOffset()
    middleClickAt(1, 10)
    assertNotEquals(before, readOffset())
  }

  @Test
  fun `paste in insert mode puts primary register at caret`() {
    configureByText("Lorem ${c}ipsum dolor sit amet,\nconsectetur adipiscing elit")
    storePrimary("foo ")
    typeText("i")

    pasteFromX11()

    assertState("Lorem foo ${c}ipsum dolor sit amet,\nconsectetur adipiscing elit")
    assertMode(Mode.INSERT)
  }

  @Test
  fun `paste in insert mode at end of line`() {
    configureByText("Lorem ipsum dolo${c}r\nconsectetur adipiscing elit")
    storePrimary(" sit")
    typeText("A")

    pasteFromX11()

    assertState("Lorem ipsum dolor sit${c}\nconsectetur adipiscing elit")
    assertMode(Mode.INSERT)
  }

  @Test
  fun `paste in normal mode is left to the platform`() {
    configureByText("Lorem ${c}ipsum dolor sit amet,\nconsectetur adipiscing elit")
    storePrimary("foo ")

    // The platform handler reads the system selection, which throws HeadlessException in headless tests
    var platformPasted = false
    val handler = VimPasteFromX11Handler(object : EditorActionHandler() {
      override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
        platformPasted = true
      }
    })
    ApplicationManager.getApplication().invokeAndWait {
      handler.execute(fixture.editor, null, DataContext.EMPTY_CONTEXT)
    }

    assertTrue(platformPasted)
    assertState("Lorem ${c}ipsum dolor sit amet,\nconsectetur adipiscing elit")
    assertMode(Mode.NORMAL())
  }

  private fun storePrimary(text: String) {
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', text, SelectionType.CHARACTER_WISE)
  }

  private fun pasteFromX11() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.performEditorAction("EditorPasteFromX11")
    }
  }

  private fun middleClickAt(line: Int, column: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      EditorMouseFixture(fixture.editor as EditorImpl).middle().clickAt(line, column)
    }
  }

  private fun readOffset(): Int {
    var offset = 0
    ApplicationManager.getApplication().invokeAndWait {
      offset = fixture.editor.caretModel.primaryCaret.offset
    }
    return offset
  }
}
