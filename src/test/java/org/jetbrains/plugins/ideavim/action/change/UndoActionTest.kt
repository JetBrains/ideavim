/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.services.IjVimOptionService
import org.jetbrains.plugins.ideavim.VimTestCase

class UndoActionTest : VimTestCase() {
  fun `test simple undo`() {
    val keys = listOf("dw", "u")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = before
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    val editor = myFixture.editor
    assertFalse(editor.caretModel.primaryCaret.hasSelection())
  }

  // Not yet supported
  fun `undo after selection`() {
    val keys = listOf("v3eld", "u")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = before
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    assertFalse(hasSelection())
  }

  fun `test undo with count`() {
    val keys = listOf("dwdwdw", "2u")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
                A Discovery

                ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    assertFalse(hasSelection())
  }

  fun `test cursor movements do not require additional undo`() {
    if (!injector.optionService.isSet(OptionScope.GLOBAL, IjVimOptionService.oldUndo)) {
      val keys = listOf("a1<Esc>ea2<Esc>ea3<Esc>", "uu")
      val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent()
      val after = """
                A Discovery

                I1 found$c it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent()
      doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
      assertFalse(hasSelection())
    }
  }

  private fun hasSelection(): Boolean {
    val editor = myFixture.editor
    return editor.caretModel.primaryCaret.hasSelection()
  }
}
