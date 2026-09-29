/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.idea.TestFor
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Renaming an opening tag with `cw` makes the IDE rename the closing tag too (XmlTagNameSynchronizer, applied at
 * `beforeCommandFinished`). That is a document change the user did not type, and it happens while we are recording
 * the strokes that `.` replays. `.` must replay only `cw` + the typed text, and let the synchronizer do the closing
 * tag again on the new line - not replay the synchronizer's own edit at its recorded offset.
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "Simultaneous tag name editing is an IDE feature with no Neovim equivalent",
)
class RepeatWithTagRenameTest : VimTestCase() {

  @Test
  @TestFor(issues = ["VIM-1153"])
  fun `test repeating a tag rename renames the closing tag of the new line`() {
    doTest(
      "cwdiv<Esc>" + "j0l" + ".",
      """
        <${c}span>short line</span>
        <span>much longer line with more characters </span>
      """.trimIndent(),
      """
        <div>short line</div>
        <di${c}v>much longer line with more characters </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  @TestFor(issues = ["VIM-1153"])
  fun `test repeating a tag rename on a shorter line`() {
    // The repeat target is shorter than the line the change was recorded on, so a stroke replayed at the recorded
    // offset would land outside the tag - or outside the line
    doTest(
      "cwdiv<Esc>" + "j0l" + ".",
      """
        <${c}span>much longer line with more characters </span>
        <span>short</span>
      """.trimIndent(),
      """
        <div>much longer line with more characters </div>
        <di${c}v>short</div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  @TestFor(issues = ["VIM-1153"])
  fun `test repeating a tag rename with a longer name`() {
    doTest(
      "cwsection<Esc>" + "j0l" + ".",
      """
        <${c}span>short line</span>
        <span>much longer line with more characters </span>
      """.trimIndent(),
      """
        <section>short line</section>
        <sectio${c}n>much longer line with more characters </section>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  @TestFor(issues = ["VIM-1153"])
  fun `test repeating a tag rename when the closing tag is on its own line`() {
    doTest(
      "cwdiv<Esc>" + "3j0l" + ".",
      """
        <${c}span>
          Lorem ipsum dolor sit amet
        </span>
        <span>
          consectetur adipiscing elit
        </span>
      """.trimIndent(),
      """
        <div>
          Lorem ipsum dolor sit amet
        </div>
        <di${c}v>
          consectetur adipiscing elit
        </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }
}
