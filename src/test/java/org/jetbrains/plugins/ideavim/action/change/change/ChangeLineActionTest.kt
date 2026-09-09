/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ChangeLineActionTest : VimTestCase() {
  @Test
  fun `test on empty file`() {
    setupChecks {
      this.neoVim.ignoredRegisters = setOf('1', '"')
    }
    doTest("cc", "", "", Mode.INSERT)
  }

  @Test
  fun `test on empty file with S`() {
    setupChecks {
      this.neoVim.ignoredRegisters = setOf('1', '"')
    }
    doTest("S", "", "", Mode.INSERT)
  }

  @Test
  fun `test on last line with S`() {
    doTest(
      "S",
      """
            Lorem ipsum dolor sit amet,
            all ${c}rocks and lavender and tufted grass,
      """.trimIndent(),
      """
            Lorem ipsum dolor sit amet,
            $c
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @Test
  fun `test on last line with new line with S`() {
    doTest(
      "S",
      """
            Lorem ipsum dolor sit amet,
            all ${c}rocks and lavender and tufted grass,
            
      """.trimIndent(),
      """
            Lorem ipsum dolor sit amet,
            $c
            
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @Test
  fun `test on very last line with new line with S`() {
    doTest(
      "S",
      """
            Lorem ipsum dolor sit amet,
            all ${c}rocks and lavender and tufted grass,
      """.trimIndent(),
      """
            Lorem ipsum dolor sit amet,
            $c
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @Test
  fun `test on very last line with new line with S2`() {
    doTest(
      "S",
      """
            Lorem ipsum dolor sit amet,
            all ${c}rocks and lavender and tufted grass,
            
      """.trimIndent(),
      """
            Lorem ipsum dolor sit amet,
            $c
            
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @Test
  fun `test on first line with new line with S`() {
    doTest(
      "S",
      """
            I ${c}found it in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
      """
            $c
            consectetur adipiscing elit
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @Test
  fun `test on last line with new line with cc`() {
    doTest(
      "cc",
      """
            Lorem ipsum dolor sit amet,
            all ${c}rocks and lavender and tufted grass,
            
      """.trimIndent(),
      """
            Lorem ipsum dolor sit amet,
            $c
            
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @Test
  fun `test on last line`() {
    doTest(
      "cc",
      """
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            $c
      """.trimIndent(),
      """
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            $c
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @Test
  fun `test S with count`() {
    doTest(
      "3S",
      """
            ${c}Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.

            My needles have teased out its sculpted sex;
            corroded tissues could no longer hide
            that priceless mote now dimpling the convex
            and limpid teardrop on a lighted slide.
      """.trimIndent(),
      """
            $c
            Cras id tellus in ex imperdiet egestas.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.

            My needles have teased out its sculpted sex;
            corroded tissues could no longer hide
            that priceless mote now dimpling the convex
            and limpid teardrop on a lighted slide.
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  // The indent kept by `cc`/`S` is auto-indent (Vim's `did_ai`). If nothing is typed before leaving Insert mode, Vim
  // deletes it again, leaving a completely empty line. See `:help 'autoindent'` and `stop_insert()` in Vim's edit.c
  @Test
  fun `test auto indent is removed when nothing is typed`() {
    doTest(
      "cc<Esc>",
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
      """
            fun main() {
            $c
            }
      """.trimIndent(),
    )
  }

  @Test
  fun `test auto indent is removed when nothing is typed with S`() {
    doTest(
      "S<Esc>",
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
      """
            fun main() {
            $c
            }
      """.trimIndent(),
    )
  }

  @Test
  fun `test auto indent is removed when nothing is typed with count`() {
    doTest(
      "2cc<Esc>",
      """
            fun main() {
                ${c}println("hi")
                println("there")
            }
      """.trimIndent(),
      """
            fun main() {
            $c
            }
      """.trimIndent(),
    )
  }

  // Typing a character clears `did_ai`, so whitespace typed by the user is kept as-is
  @Test
  fun `test trailing whitespace typed by user is not removed`() {
    doTest(
      "ccfoo   <Esc>",
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
      """
            fun main() {
            ....foo..${c}.
            }
      """.trimIndent().dotToSpace(),
    )
  }

  // `did_ai` is cleared by typing, not by the resulting text: erasing the typed text again does not bring back the
  // auto-indent removal
  @Test
  fun `test auto indent is not removed when typed text is deleted again`() {
    doTest(
      "ccfoo<BS><BS><BS><Esc>",
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
      """
            fun main() {
            ...${c}.
            }
      """.trimIndent().dotToSpace(),
    )
  }

  // Removing the auto-indent is part of the change, not a separate edit: a single undo restores the original line
  @Test
  fun `test undo after cc and immediate escape`() {
    configureByText(
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
    )
    typeText("cc<Esc>")
    assertState(
      """
            fun main() {
            $c
            }
      """.trimIndent(),
    )
    typeText("u")
    assertState(
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
    )
  }
}
