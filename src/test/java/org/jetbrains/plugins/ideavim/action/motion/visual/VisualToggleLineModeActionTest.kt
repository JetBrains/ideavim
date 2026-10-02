/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.visual

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.codeInsight.folding.impl.FoldingUtil
import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class VisualToggleLineModeActionTest : VimTestCase() {
  @Test
  fun `test enter visual with count`() {
    doTest(
      "1V",
      """
                    Lorem Ipsum

                    I ${c}found it in a legendary land
                    consectetur adipiscing elit
                    Sed in orci mauris.
                    Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                    Lorem Ipsum

                    ${s}I ${c}found it in a legendary land
                    ${se}consectetur adipiscing elit
                    Sed in orci mauris.
                    Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test enter visual with count multicaret`() {
    doTest(
      "1V",
      """
                    Lorem Ipsum

                    I ${c}found it in a legendary land
                    consectetur adipiscing elit
                    where it ${c}was settled on some sodden sand
                    Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                    Lorem Ipsum

                    ${s}I ${c}found it in a legendary land
                    ${se}consectetur adipiscing elit
                    ${s}where it ${c}was settled on some sodden sand
                    ${se}Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test enter visual with 3 count`() {
    doTest(
      "3V",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                    A Discovery

                    ${s}I found it in a legendary land
                    all rocks and lavender and tufted grass,
                    wh${c}ere it was settled on some sodden sand
                    ${se}hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test enter visual with 100 count`() {
    doTest(
      "100V",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                    A Discovery

                    ${s}I found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    ha${c}rd by the torrent of a mountain pass.${se}
      """.trimIndent(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test selectmode option`() {
    configureByText(
      """
                    Lorem Ipsum

                    I${c} found it in a legendary land
                    consectetur adipiscing elit
                    Sed in orci mauris.[long line]
                    Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    enterCommand("set selectmode=cmd")
    typeText("V")
    assertMode(Mode.SELECT(SelectionType.LINE_WISE))
  }

  @Test
  fun `enter visual line from visual block with motion up`() {
    doTest(
      "<C-V>khV",
      """
        Lorem Ipsum

        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in${c} orci mauris.
        Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
        Lorem Ipsum

        Lorem ipsum dolor sit amet,
        ${s}conse${c}ctetur adipiscing elit
        Sed in orci mauris.
        ${se}Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.LINE_WISE)
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test enter visual line on collapsed html tag selects whole tag`() {
    configureByText(
      HtmlFileType.INSTANCE,
      """
        ${c}<div>
          asdfasdf
        </div>
      """.trimIndent(),
    )
    collapseFoldAtLine(0)

    typeText("V")

    assertState(
      """
        ${s}${c}<div>
          asdfasdf
        </div>${se}
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test delete visual line on collapsed html tag deletes whole tag`() {
    configureByText(
      HtmlFileType.INSTANCE,
      """
        ${c}<div>
          asdfasdf
        </div>
        <p></p>
      """.trimIndent(),
    )
    collapseFoldAtLine(0)

    typeText("Vd")

    assertState("${c}<p></p>")
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test extend visual line down onto collapsed html tag selects whole tag`() {
    configureByText(
      HtmlFileType.INSTANCE,
      """
        ${c}<p></p>
        <div>
          asdfasdf
        </div>
        <p></p>
      """.trimIndent(),
    )
    collapseFoldAtLine(1)

    typeText("Vj")

    assertState(
      """
        ${s}<p></p>
        ${c}<div>
          asdfasdf
        </div>
        ${se}<p></p>
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
  }

  private fun collapseFoldAtLine(line: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.foldingModel.runBatchFoldingOperation {
        CodeFoldingManager.getInstance(fixture.project).updateFoldRegions(fixture.editor)
        val foldRegion = FoldingUtil.findFoldRegionStartingAtLine(fixture.editor, line)
          ?: error("Expected fold region at line $line")
        foldRegion.isExpanded = false
      }
    }
  }
}
