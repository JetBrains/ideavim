/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.text

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionParagraphNextActionTest : VimTestCase() {
  @Test
  fun `test delete paragraph`() {
    doTest(
      "d}",
      """
        void foo() {
        }
        $c
        void bar() {
        }

        void baz() {
        }
      """.trimIndent(),
      """
        void foo() {
        }
        $c
        void baz() {
        }
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // VIM-4287: an exclusive characterwise delete promoted to linewise must remove the whole first line, including its
  // leading whitespace, without swallowing the blank line the exclusive motion stops on.
  @Test
  fun `test delete paragraph with indented start is linewise`() {
    doTest(
      "d}",
      "<a>\n" +
        "  ${c}foo\n" +
        "  bar\n" +
        "\n" +
        "baz\n",
      "<a>\n" +
        "$c\n" +
        "baz\n",
      Mode.NORMAL(),
    )
  }
}
