/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("ClassName")

package org.jetbrains.plugins.ideavim.group.motion

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.stickyLines.StickyLinesModel
import com.intellij.testFramework.EditorTestUtil
import com.maddyhome.idea.vim.api.getOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// These tests are sanity tests for scrolloff and scrolljump, with actions that move the cursor. Other actions that are
// affected by scrolloff or scrolljump should include that in the action specific tests
class MotionGroup_scrolloff_Test : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move up shows no context with scrolloff=0`() {
    configureByPages(5)
    enterCommand("set scrolloff=0")
    setPositionAndScroll(25, 25)
    typeText("k")
    assertPosition(24, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move up shows context line with scrolloff=1`() {
    configureByPages(5)
    enterCommand("set scrolloff=1")
    setPositionAndScroll(25, 26)
    typeText("k")
    assertPosition(25, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move up shows context lines with scrolloff=10`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(25, 35)
    typeText("k")
    assertPosition(34, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move up when scrolloff is slightly less than half screen height`() {
    // Screen height = 35. scrolloff=15. This gives 5 possible caret lines without scrolling (48, 49, 50, 51 + 52)
    configureByPages(5)
    enterCommand("set scrolloff=15")
    setPositionAndScroll(33, 52)

    typeText("k")
    assertPosition(51, 0)
    assertVisibleArea(33, 67)

    typeText("k")
    assertPosition(50, 0)
    assertVisibleArea(33, 67)

    typeText("k")
    assertPosition(49, 0)
    assertVisibleArea(33, 67)

    typeText("k")
    assertPosition(48, 0)
    assertVisibleArea(33, 67)

    // Scroll
    typeText("k")
    assertPosition(47, 0)
    assertVisibleArea(32, 66)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move up when scrolloff is slightly less than half screen height 2`() {
    // Screen height = 35. scrolloff=16. This gives 3 possible caret lines without scrolling (49, 50 + 51)
    configureByPages(5)
    enterCommand("set scrolloff=16")
    setPositionAndScroll(33, 51)

    typeText("k")
    assertPosition(50, 0)
    assertVisibleArea(33, 67)

    typeText("k")
    assertPosition(49, 0)
    assertVisibleArea(33, 67)

    // Scroll
    typeText("k")
    assertPosition(48, 0)
    assertVisibleArea(32, 66)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move up when scrolloff is slightly less than half screen height 3`() {
    // Screen height = 34. scrolloff=16
    // Even numbers. 2 possible caret lines without scrolling (49 + 50)
    configureByPages(5)
    enterCommand("set scrolloff=16")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(33, 50)

    typeText("k")
    assertPosition(49, 0)
    assertVisibleArea(33, 66)

    // Scroll
    typeText("k")
    assertPosition(48, 0)
    assertVisibleArea(32, 65)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  @VimBehaviorDiffers(description = "Moving up in Vim will always have 16 lines above the caret line. IdeaVim keeps 17")
  fun `test move up when scrolloff is exactly screen height`() {
    // Page height = 34. scrolloff=17
    // 2 possible caret lines without scrolling (49 + 50)
    configureByPages(5)
    enterCommand("set scrolloff=17")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(33, 50)

    typeText("k")
    assertPosition(49, 0)
    assertVisibleArea(33, 66)

    // Scroll
    typeText("k")
    assertPosition(48, 0)
    assertVisibleArea(32, 65)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move up when scrolloff is slightly greater than screen height keeps cursor in centre of screen`() {
    // Page height = 35. scrolloff=17
    configureByPages(5)
    enterCommand("set scrolloff=17")
    setPositionAndScroll(33, 50)

    typeText("k")
    assertPosition(49, 0)
    assertVisibleArea(32, 66)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move up when scrolloff is slightly greater than screen height keeps cursor in centre of screen 2`() {
    // Page height = 35. scrolloff=17
    configureByPages(5)
    enterCommand("set scrolloff=22")
    setPositionAndScroll(33, 50)

    typeText("k")
    assertPosition(49, 0)
    assertVisibleArea(32, 66)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move down shows no context with scrolloff=0`() {
    configureByPages(5)
    enterCommand("set scrolloff=0")
    setPositionAndScroll(25, 59)
    typeText("j")
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move down shows context line with scrolloff=1`() {
    configureByPages(5)
    enterCommand("set scrolloff=1")
    setPositionAndScroll(25, 58)
    typeText("j")
    assertPosition(59, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move down shows context lines with scrolloff=10`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(25, 49)
    typeText("j")
    assertPosition(50, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move down when scrolloff is slightly less than half screen height`() {
    // Screen height = 35. scrolloff=15. This gives 5 possible caret lines without scrolling (48, 49, 50, 51 + 52)
    configureByPages(5)
    enterCommand("set scrolloff=15")
    setPositionAndScroll(33, 48)

    typeText("j")
    assertPosition(49, 0)
    assertVisibleArea(33, 67)

    typeText("j")
    assertPosition(50, 0)
    assertVisibleArea(33, 67)

    typeText("j")
    assertPosition(51, 0)
    assertVisibleArea(33, 67)

    typeText("j")
    assertPosition(52, 0)
    assertVisibleArea(33, 67)

    // Scroll
    typeText("j")
    assertPosition(53, 0)
    assertVisibleArea(34, 68)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move down when scrolloff is slightly less than half screen height 2`() {
    // Screen height = 35. scrolloff=16. This gives 3 possible caret lines without scrolling (49, 50 + 51)
    configureByPages(5)
    enterCommand("set scrolloff=16")
    setPositionAndScroll(33, 49)

    typeText("j")
    assertPosition(50, 0)
    assertVisibleArea(33, 67)

    typeText("j")
    assertPosition(51, 0)
    assertVisibleArea(33, 67)

    // Scroll
    typeText("j")
    assertPosition(52, 0)
    assertVisibleArea(34, 68)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move down when scrolloff is slightly less than half screen height 3`() {
    // Screen height = 34. scrolloff=16
    // Even numbers. 2 possible caret lines without scrolling (49 + 50)
    configureByPages(5)
    enterCommand("set scrolloff=16")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(33, 49)

    typeText("j")
    assertPosition(50, 0)
    assertVisibleArea(33, 66)

    // Scroll
    typeText("j")
    assertPosition(51, 0)
    assertVisibleArea(34, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move down when scrolloff is exactly screen height`() {
    // Page height = 34. scrolloff=17
    // 2 possible caret lines without scrolling (49 + 50), but moving to line 51 will scroll 2 lines!
    configureByPages(5)
    enterCommand("set scrolloff=17")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(33, 49)

    typeText("j")
    assertPosition(50, 0)
    assertVisibleArea(33, 66)

    // Scroll. By 2 lines!
    typeText("j")
    assertPosition(51, 0)
    assertVisibleArea(35, 68)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move down when scrolloff is slightly greater than half screen height keeps cursor in centre of screen`() {
    // Page height = 35. scrolloff=17
    configureByPages(5)
    enterCommand("set scrolloff=17")
    setPositionAndScroll(33, 50)

    typeText("j")
    assertPosition(51, 0)
    assertVisibleArea(34, 68)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move down when scrolloff is slightly greater than half screen height keeps cursor in centre of screen 2`() {
    // Page height = 35. scrolloff=17
    configureByPages(5)
    enterCommand("set scrolloff=22")
    setPositionAndScroll(33, 50)

    typeText("j")
    assertPosition(51, 0)
    assertVisibleArea(34, 68)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test scrolloff=999 keeps cursor in centre of screen`() {
    configureByPages(5)
    enterCommand("set scrolloff=999")
    setPositionAndScroll(25, 42)

    typeText("j")
    assertPosition(43, 0)
    assertVisibleArea(26, 60)

    typeText("k")
    assertPosition(42, 0)
    assertVisibleArea(25, 59)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test scrolloff=999 keeps cursor in centre of screen with even screen height`() {
    configureByPages(5)
    enterCommand("set scrolloff=999")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(26, 42)

    typeText("j")
    assertPosition(43, 0)
    assertVisibleArea(27, 60)

    typeText("k")
    assertPosition(42, 0)
    assertVisibleArea(26, 59)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test reposition cursor when scrolloff is set`() {
    configureByPages(5)
    enterCommand("set scrolloff=0")
    setPositionAndScroll(50, 50)

    enterCommand("set scrolloff=999")
    assertPosition(50, 0)
    assertVisibleArea(33, 67)
  }

  // VIM-4329
  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @Test
  fun `test scrolloff=999 keeps cursor on screen when block inlays take up most of the screen`() {
    // Screen is 35 rows. Lines 20-29 (10 rows), then a 20 row inlay (e.g. rendered doc comment), then lines 30-34.
    // Only 15 lines of text are visible, which is less than half the screen height
    configureByLines(2000, "    I found it in a legendary land")
    addBlockInlay(fixture.editor.vim.getOffset(30, 0), true, 20)
    setPositionAndScroll(20, 25)

    enterCommand("set scrolloff=999")
    assertCaretOnScreen()

    typeText("j")
    assertPosition(26, 0)
    assertCaretOnScreen()

    typeText("j")
    assertPosition(27, 0)
    assertCaretOnScreen()
  }

  // VIM-4329
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test scrolloff=999 does not scroll when changing line`() {
    configureByPages(5)
    enterCommand("set scrolloff=999")
    setPositionAndScroll(43, 60)
    assertVisibleArea(43, 77)

    typeText("cc")
    assertPosition(60, 0)
    assertVisibleArea(43, 77)

    typeText("x")
    assertVisibleArea(43, 77)

    typeText("<Esc>")
    assertVisibleArea(43, 77)
  }

  private fun assertCaretOnScreen() {
    ApplicationManager.getApplication().invokeAndWait {
      val editor = fixture.editor
      val caretLine = editor.caretModel.visualPosition.line
      val topLine = EditorHelper.getVisualLineAtTopOfScreen(editor)
      val bottomLine = EditorHelper.getVisualLineAtBottomOfScreen(editor)
      assertTrue(caretLine in topLine..bottomLine, "Caret line $caretLine is outside of screen $topLine..$bottomLine")
    }
  }
}

class MotionGroup_scrolljump_Test : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move up scrolls single line with scrolljump=0`() {
    configureByPages(5)
    enterCommand("set scrolljump=0")
    setPositionAndScroll(25, 25)
    typeText("k")
    assertPosition(24, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move up scrolls single line with scrolljump=1`() {
    configureByPages(5)
    enterCommand("set scrolljump=1")
    setPositionAndScroll(25, 25)
    typeText("k")
    assertPosition(24, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move up scrolls multiple lines with scrolljump=10`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(25, 25)
    typeText("k")
    assertPosition(24, 0)
    assertVisibleArea(15, 49)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move down scrolls single line with scrolljump=0`() {
    configureByPages(5)
    enterCommand("set scrolljump=0")
    setPositionAndScroll(25, 59)
    typeText("j")
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move down scrolls single line with scrolljump=1`() {
    configureByPages(5)
    enterCommand("set scrolljump=1")
    setPositionAndScroll(25, 59)
    typeText("j")
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move down scrolls multiple lines with scrolljump=10`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(25, 59)
    typeText("j")
    assertPosition(60, 0)
    assertVisibleArea(35, 69)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test negative scrolljump treated as percentage 1`() {
    configureByPages(5)
    enterCommand("set scrolljump=-50")
    setPositionAndScroll(39, 39)
    typeText("k")
    assertPosition(38, 0)
    assertVisibleArea(22, 56)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test negative scrolljump treated as percentage 2`() {
    configureByPages(5)
    enterCommand("set scrolljump=-10")
    setPositionAndScroll(39, 39)
    typeText("k")
    assertPosition(38, 0)
    assertVisibleArea(36, 70)
  }
}

class MotionGroup_scrolloff_scrolljump_Test : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test scroll up with scrolloff and scrolljump set`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(50, 55)
    typeText("k")
    assertPosition(54, 0)
    assertVisibleArea(40, 74)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test scroll down with scrolloff and scrolljump set`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(50, 79)
    typeText("j")
    assertPosition(80, 0)
    assertVisibleArea(60, 94)
  }
}

class MotionGroup_scrolloff_stickyLines_Test : VimTestCase() {
  // VIM-4329
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test scrolloff=999 does not scroll when typing with sticky lines near top of screen`() {
    configureByLines(1000, "    I found it in a legendary land")
    enterCommand("set scrolloff=999")

    // Sticky lines panel is only created for main editors, the fixture's editor doesn't have one
    withMainEditor { editor ->
      // Two nested scopes starting just above the top line when the caret is centred. Depending on the scroll position,
      // the sticky lines panel is either 0 or 2 lines high
      addStickyLine(editor, 42, 500)
      addStickyLine(editor, 43, 500)
      ApplicationManager.getApplication().invokeAndWait {
        editor.scrollingModel.scrollVertically(30 * editor.lineHeight)
        editor.caretModel.moveToOffset(editor.document.getLineStartOffset(60))
      }

      // Let scrolloff centre the caret before we start typing
      typeText(editor, "jki")
      val scrollOffset = verticalScrollOffset(editor)
      for (c in "abcd") {
        typeText(editor, c.toString())
        assertEquals(scrollOffset, verticalScrollOffset(editor), "Editor scrolled after typing '$c'")
      }
    }
  }

  // VIM-4329
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test scrolloff=999 does not scroll when typing after a jump changes the sticky lines panel`() {
    configureByLines(1000, "    I found it in a legendary land")
    enterCommand("set scrolloff=999")

    withMainEditor(rows = 40) { editor ->
      addNestedScopes(editor)
      typeText(editor, "101G")

      // The jump is centred with the sticky lines panel of the old position. At the new position the panel has a
      // different height, so we need to recalculate the centre straight away, not on the next keystroke
      typeText(editor, "500Gi")
      val scrollOffset = verticalScrollOffset(editor)
      for (c in "abcd") {
        typeText(editor, c.toString())
        assertEquals(scrollOffset, verticalScrollOffset(editor), "Editor scrolled after typing '$c'")
      }
    }
  }

  // VIM-4329
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test scrolloff close to half screen keeps scroll offset below sticky lines panel`() {
    configureByLines(1000, "    I found it in a legendary land")
    enterCommand("set scrolloff=15")

    withMainEditor(rows = 30) { editor ->
      addNestedScopes(editor)
      typeText(editor, "101G")
      typeText(editor, "300G")

      // 30 rows with a 3 line panel leaves 27 rows, so scrolloff is effectively (27 - 1) / 2 = 13
      ApplicationManager.getApplication().invokeAndWait {
        val caretLine = editor.caretModel.visualPosition.line
        val topLine = EditorHelper.getVisualLineAtTopOfScreen(editor)
        val bottomLine = EditorHelper.getVisualLineAtBottomOfScreen(editor)
        assertEquals(3 * editor.lineHeight, (editor as EditorImpl).stickyLinesPanelHeight)
        assertTrue(caretLine - topLine >= 13, "Caret line $caretLine is too close to top line $topLine")
        assertTrue(bottomLine - caretLine >= 13, "Caret line $caretLine is too close to bottom line $bottomLine")
      }
    }
  }

  private fun withMainEditor(rows: Int = screenHeight, action: (Editor) -> Unit) {
    var editor: Editor? = null
    ApplicationManager.getApplication().invokeAndWait {
      editor = EditorFactory.getInstance()
        .createEditor(fixture.editor.document, fixture.project, EditorKind.MAIN_EDITOR)
      EditorTestUtil.setEditorVisibleSizeInPixels(
        editor,
        (screenWidth * EditorHelper.getPlainSpaceWidthFloat(editor!!)).roundToInt(),
        rows * editor.lineHeight,
      )
    }
    try {
      action(editor!!)
    } finally {
      ApplicationManager.getApplication().invokeAndWait { EditorFactory.getInstance().releaseEditor(editor!!) }
    }
  }

  private fun addStickyLine(editor: Editor, primaryLine: Int, scopeLine: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val document = editor.document
      val model = StickyLinesModel.getModel(fixture.project, document)!!
      model.addStickyLine(document.getLineStartOffset(primaryLine), document.getLineEndOffset(scopeLine), null)
      model.notifyLinesUpdate()
    }
  }

  // A class every 40 lines, with a method every 10 lines and a nested block in the first method
  private fun addNestedScopes(editor: Editor) {
    for (classLine in 0 until 960 step 40) {
      addStickyLine(editor, classLine, classLine + 39)
      for (methodLine in classLine + 2 until classLine + 38 step 10) {
        addStickyLine(editor, methodLine, methodLine + 8)
      }
      addStickyLine(editor, classLine + 3, classLine + 9)
    }
  }

  private fun typeText(editor: Editor, keys: String) =
    typeText(injector.parser.parseKeys(keys), editor, fixture.project)

  private fun verticalScrollOffset(editor: Editor): Int {
    var offset = 0
    ApplicationManager.getApplication().invokeAndWait { offset = editor.scrollingModel.verticalScrollOffset }
    return offset
  }
}
