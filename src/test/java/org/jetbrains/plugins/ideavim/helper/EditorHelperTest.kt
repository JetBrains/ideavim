/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.helper

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.helper.EditorHelper
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals

class EditorHelperTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test scroll column to left of screen`() {
    configureByColumns(100)
    ApplicationManager.getApplication().invokeAndWait {
      EditorHelper.scrollColumnToLeftOfScreen(fixture.editor, 0, 2)
      val visibleArea = fixture.editor.scrollingModel.visibleArea
      val columnWidth = EditorHelper.getPlainSpaceWidthFloat(fixture.editor)
      assertEquals((2 * columnWidth).roundToInt(), visibleArea.x)
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test scroll column to right of screen`() {
    configureByColumns(100)
    val column = screenWidth + 2
    ApplicationManager.getApplication().invokeAndWait {
      EditorHelper.scrollColumnToRightOfScreen(fixture.editor, 0, column)
      val visibleArea = fixture.editor.scrollingModel.visibleArea
      val columnWidth = EditorHelper.getPlainSpaceWidthFloat(fixture.editor)
      assertEquals(((column - screenWidth + 1) * columnWidth).roundToInt(), visibleArea.x)
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test scroll column to middle of screen with even number of columns`() {
    configureByColumns(200)
    // For an 80 column screen, moving a column to the centre should position it in column 41 (1 based) - 40 columns on
    // the left, mid point, 39 columns on the right
    // Put column 100 into position 41 -> offset is 59 columns
    ApplicationManager.getApplication().invokeAndWait {
      EditorHelper.scrollColumnToMiddleOfScreen(fixture.editor, 0, 99)
      val visibleArea = fixture.editor.scrollingModel.visibleArea
      val columnWidth = EditorHelper.getPlainSpaceWidthFloat(fixture.editor)
      assertEquals((59 * columnWidth).roundToInt(), visibleArea.x)
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test scroll column to middle of screen with odd number of columns`() {
    configureByColumns(200)
    setEditorVisibleSize(81, 25)
    // For an 81 column screen, moving a column to the centre should position it in column 41 (1 based) - 40 columns on
    // the left, mid point, 40 columns on the right
    // Put column 100 into position 41 -> offset is 59 columns
    ApplicationManager.getApplication().invokeAndWait {
      EditorHelper.scrollColumnToMiddleOfScreen(fixture.editor, 0, 99)
      val visibleArea = fixture.editor.scrollingModel.visibleArea
      val columnWidth = EditorHelper.getPlainSpaceWidthFloat(fixture.editor)
      assertEquals((59 * columnWidth).roundToInt(), visibleArea.x)
    }
  }
}
