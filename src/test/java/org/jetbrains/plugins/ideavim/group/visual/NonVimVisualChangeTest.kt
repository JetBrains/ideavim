/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.visual

import com.intellij.codeInsight.editorActions.BackspaceHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import com.maddyhome.idea.vim.helper.hasVisualSelection
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.selectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.assertModeDoesNotChange
import org.jetbrains.plugins.ideavim.rangeOf
import org.jetbrains.plugins.ideavim.waitAndAssert
import org.jetbrains.plugins.ideavim.waitAndAssertMode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * @author Alex Plate
 */
class NonVimVisualChangeTest : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test save mode after removing text`() {
    // PyCharm uses BackspaceHandler.deleteToTargetPosition to remove indent
    // See https://github.com/JetBrains/ideavim/pull/186#issuecomment-486656093
    configureByText(
      """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    typeText("i")
    assertMode(Mode.INSERT)
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runWriteAction {
        CommandProcessor.getInstance().runUndoTransparentAction {
          BackspaceHandler.deleteToTargetPosition(fixture.editor, LogicalPosition(2, 0))
        }
      }
    }
    assertState(
      """
            Lorem Ipsum

            found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test enable and disable selection`() {
    configureByText(
      """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    typeText("i")
    assertMode(Mode.INSERT)

    // Fast add and remove selection
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.setSelection(0, 10)
      fixture.editor.selectionModel.removeSelection()
    }

    assertModeDoesNotChange(fixture.editor, Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test enable, disable, and enable selection again`() {
    configureByText(
      """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    typeText("i")
    assertMode(Mode.INSERT)

    // Fast add and remove selection
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.setSelection(0, 10)
      fixture.editor.selectionModel.removeSelection()
      fixture.editor.selectionModel.setSelection(0, 10)
    }

    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.INSERT))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test switch from char to line visual mode`() {
    val text = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(text)
    typeText("i")
    assertMode(Mode.INSERT)

    val range = text.rangeOf("Discovery")
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.setSelection(range.startOffset, range.endOffset)
    }
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.INSERT))
    assertEquals(SelectionType.CHARACTER_WISE, fixture.editor.vim.mode.selectionType)

    val rangeLine = text.rangeOf("A Discovery\n")
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.setSelection(rangeLine.startOffset, rangeLine.endOffset)
    }
    waitAndAssert { fixture.editor.vim.mode.selectionType == SelectionType.LINE_WISE }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test mouse selection from insert mode enters Insert Visual mode and returns to Insert on deselect`() {
    configureByText(
      """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    typeText("i")
    assertMode(Mode.INSERT)

    val range = fixture.editor.document.text rangeOf "found"
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.setSelection(range.startOffset, range.endOffset)
      IdeaSelectionControl.controlNonVimSelectionChange(
        fixture.editor,
        VimListenerManager.SelectionSource.MOUSE,
      )
    }
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.INSERT))

    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.removeSelection()
      IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    }
    waitAndAssertMode(fixture, Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test mouse selection from Insert Visual via i_CTRL-O v does not nest Visual returnTo`() {
    configureByText(
      """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
    )
    typeText("i<C-O>v")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.INSERT))

    val range = fixture.editor.document.text rangeOf "found"
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.setSelection(range.startOffset, range.endOffset)
      IdeaSelectionControl.controlNonVimSelectionChange(
        fixture.editor,
        VimListenerManager.SelectionSource.MOUSE,
      )
    }
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.INSERT))
    assertFalse(fixture.editor.vim.mode.returnTo.hasVisualSelection)

    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.removeSelection()
      IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    }
    waitAndAssertMode(fixture, Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test mouse selection from Insert Normal via i_CTRL-O preserves return chain on deselect`() {
    configureByText(
      """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
    )
    typeText("i<C-O>")
    assertMode(Mode.NORMAL(Mode.INSERT))

    val range = fixture.editor.document.text rangeOf "found"
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.setSelection(range.startOffset, range.endOffset)
      IdeaSelectionControl.controlNonVimSelectionChange(
        fixture.editor,
        VimListenerManager.SelectionSource.MOUSE,
      )
    }
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.NORMAL(Mode.INSERT)))
    assertFalse(fixture.editor.vim.mode.returnTo.hasVisualSelection)

    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.removeSelection()
      IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    }
    waitAndAssertMode(fixture, Mode.INSERT)
  }
}
