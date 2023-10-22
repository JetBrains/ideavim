/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.codeInsight.folding.impl.FoldingUtil
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class VisualBlockInsertActionTest : VimTestCase() {

  // VIM-1110 |CTRL-V| |v_b_i| |zc|
  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test block insert after folds`() {
    configureByJavaText(
      """$c/**
 * Something to fold.
 */
foo
bar
""",
    )

    fixture.editor.foldingModel.runBatchFoldingOperation {
      CodeFoldingManager.getInstance(fixture.project).updateFoldRegions(fixture.editor)
      FoldingUtil.findFoldRegionStartingAtLine(fixture.editor, 0)!!.isExpanded = false
    }

    typeText(injector.parser.parseKeys("j" + "<C-V>" + "j" + "I" + "X" + "<Esc>"))
    assertState(
      """/**
 * Something to fold.
 */
${c}Xfoo
Xbar
""",
    )
  }

  // VIM-1379 |CTRL-V| |j| |v_b_I|
  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  @Test
  fun `test insert visual block with empty line in the middle`() {
    doTest(
      listOf("ll", "<C-V>", "jjI", "_quux_", "<Esc>"),
      """
                    foo

                    bar

      """.trimIndent(),
      """
                    fo_quux_o

                    ba_quux_r

      """.trimIndent(),
    )
  }

  // VIM-632 |CTRL-V| |v_b_I|
  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  @Test
  fun `test change visual block`() {
    doTest(
      listOf("<C-V>", "j", "I", "quux ", "<Esc>"),
      """
                    foo bar
                    ${c}baz quux
                    spam eggs

      """.trimIndent(),
      (
        """
                    foo bar
                    ${c}quux baz quux
                    quux spam eggs

        """.trimIndent()
        ),
    )
  }

  @Test
  fun `test visual block insert`() {
    val before = """
            ${c}int a;
            int b;
            int c;
    """.trimIndent()
    typeTextInFile(injector.parser.parseKeys("<C-V>" + "2j" + "I" + "const " + "<Esc>"), before)
    val after = """
            ${c}const int a;
            const int b;
            const int c;
    """.trimIndent()
    assertState(after)
  }

  // VIM-1379 |CTRL-V| |j| |v_b_I|
  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  @Test
  fun `test insert visual block with shorter line in the middle`() {
    doTest(
      listOf("ll", "<C-V>", "jjI", "_quux_", "<Esc>"),
      """
                    foo
                    x
                    bar

      """.trimIndent(),
      (
        """
                    fo_quux_o
                    x
                    ba_quux_r

        """.trimIndent()
        ),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  @Test
  fun `test insert in non block mode`() {
    doTest(
      listOf("vwIHello<esc>"),
      """
                ${c}A Discovery

                ${c}I found it in a legendary land
                all rocks and ${c}lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                Hell${c}oA Discovery

                Hell${c}oI found it in a legendary land
                Hell${c}oall rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  @Test
  fun `test block mode with inlays`() {
    val before = """
                A Discovery

                I found it in ${c}a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """
    doTest(
      listOf("<C-V>" + "jjI" + " Hello " + "<ESC>"),
      before.trimIndent(),
      """
                A Discovery

                I found it in  Hello a legendary land
                all rocks and  Hello lavender and tufted grass,
                where it was s Hello ettled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
    ) {
      it.inlayModel.addInlineElement(before.indexOf("found"), HintRenderer("Hello"))
      it.inlayModel.addInlineElement(before.indexOf("l rocks"), HintRenderer("Hello"))
      it.inlayModel.addInlineElement(before.indexOf("ere it"), HintRenderer("Hello"))
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  @Test
  fun `test insert with block on one line`() {
    val before = """
                A Discovery

                I found it in ${c}a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """
    doTest(
      listOf("<C-V>", "lll", "I"),
      before.trimIndent(),
      before.trimIndent(),
      Mode.INSERT,
    )
  }
}
