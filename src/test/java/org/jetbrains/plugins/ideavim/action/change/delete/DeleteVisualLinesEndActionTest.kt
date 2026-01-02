/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class DeleteVisualLinesEndActionTest : VimTestCase() {
  @Test
  fun `test simple deletion`() {
    val keys = listOf("v", "D")
    val before = """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            ${c}consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test virtual edit delete middle to end`() {
    doTest(
      "D",
      """
            Yesterday it w${c}orked
            Today it is not working
            The test is like that.
      """.trimIndent(),
      """
            Yesterday it w${c}
            Today it is not working
            The test is like that.
      """.trimIndent(),
    ) {
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test virtual edit delete end to end`() {
    doTest(
      "D",
      """
            Yesterday it worke${c}d
            Today it is not working
            The test is like that.
      """.trimIndent(),
      """
            Yesterday it worke${c}
            Today it is not working
            The test is like that.
      """.trimIndent(),
    ) {
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test virtual edit delete to end from virtual space`() {
    doTest(
      "D",
      """
            Yesterday it worked${c}
            Today it is not working
            The test is like that.
      """.trimIndent(),
      """
            Yesterday it worke${c}
            Today it is not working
            The test is like that.
      """.trimIndent(),
    ) {
      enterCommand("set virtualedit=onemore")
    }
  }

  @Test
  fun `test simple deletion with indent`() {
    val keys = listOf("v", "D")
    val before = """
            Lorem Ipsum

            I ${c}found it in a legendary land
                consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

                ${c}consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test simple deletion with indent and nostartofline`() {
    val keys = listOf("v", "D")
    val before = """
            Lorem Ipsum

            I ${c}found it in a legendary land
                consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

              ${c}  consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after) {
      enterCommand("set nostartofline")
    }
  }

  @Test
  fun `test simple deletion empty line`() {
    val keys = listOf("v", "D")
    val before = """
            Lorem Ipsum
            ${c}
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum
            ${c}Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test simple deletion last line`() {
    val keys = listOf("v", "D")
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            hard by the ${c}torrent of a mountain pass.

    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            ${c}
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test simple deletion first line`() {
    val keys = listOf("v", "D")
    val before = """
            A ${c}Discovery

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            ${c}
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test simple deletion before empty`() {
    val keys = listOf("v", "D")
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            all ${c}rocks and lavender and tufted grass,

            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            ${c}
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test simple deletion last line without empty line`() {
    val keys = listOf("v", "D")
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            ${c}Sed in orci mauris.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test simple deletion multiline`() {
    val keys = listOf("vj", "D")
    val before = """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            ${c}Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test simple deletion multiline motion up`() {
    val keys = listOf("vk", "D")
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            all ${c}rocks and lavender and tufted grass,
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            ${c}Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test delete visual lines end action`() {
    typeTextInFile(
      "v" + "2j" + "D",
      """
                    a${c}bcde
                    abcde
                    abcde
                    abcde
                    abcd${c}e
                    abcde
                    abcde

      """.trimIndent(),
    )
    assertState("${c}abcde\n${c}")
  }

  @Test
  fun `test line simple deletion`() {
    val keys = listOf("V", "D")
    val before = """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            ${c}consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test line deletion with indent`() {
    val keys = listOf("V", "D")
    val before = """
            Lorem Ipsum

            I ${c}found it in a legendary land
                consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

                ${c}consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test line deletion with indent and nostartofline`() {
    val keys = listOf("V", "D")
    val before = """
            Lorem Ipsum

            I ${c}found it in a legendary land
                consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

              ${c}  consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after) {
      enterCommand("set nostartofline")
    }
  }

  @Test
  fun `test line deletion empty line`() {
    val keys = listOf("V", "D")
    val before = """
            Lorem Ipsum
            ${c}
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum
            ${c}Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test line deletion last line`() {
    val keys = listOf("V", "D")
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            hard by the ${c}torrent of a mountain pass.

    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            ${c}
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test line deletion last line without empty line`() {
    val keys = listOf("V", "D")
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            ${c}Sed in orci mauris.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test line deletion multiline`() {
    val keys = listOf("Vj", "D")
    val before = """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            ${c}Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test line deletion multiline motion up`() {
    val keys = listOf("Vk", "D")
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            all ${c}rocks and lavender and tufted grass,
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            ${c}Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test line delete visual lines end action`() {
    typeTextInFile(
      "V" + "2j" + "D",
      """
                    a${c}bcde
                    abcde
                    abcde
                    abcde
                    abcd${c}e
                    abcde
                    abcde

      """.trimIndent(),
    )
    assertState("${c}abcde\n${c}")
  }

  @Test
  fun `test block simple deletion`() {
    val keys = listOf("<C-V>", "D")
    val before = """
            Lorem Ipsum

            I${c} found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test block deletion empty line`() {
    val keys = listOf("<C-V>", "D")
    val before = """
            Lorem Ipsum
            ${c}
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum
            ${c}
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test block deletion last line`() {
    val keys = listOf("<C-V>", "D")
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            hard by the${c} torrent of a mountain pass.

    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            hard by the

    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test block deletion last line without empty line`() {
    val keys = listOf("<C-V>", "D")
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            hard by the${c} torrent of a mountain pass.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            hard by the
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test block deletion multiline`() {
    val keys = listOf("<C-V>j", "D")
    val before = """
            Lorem Ipsum

            I${c} found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I
            c
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test block deletion multiline motion up`() {
    val keys = listOf("<C-V>k", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all${c} rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I f
            all
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after)
  }

  @Test
  fun `test delete visual block line end action`() {
    typeTextInFile(
      "<C-V>" + "2j" + "2l" + "D",
      """
                    abcde
                    a${c}bcde
                    abcde
                    abcde
                    abcde

      """.trimIndent(),
    )
    assertState(
      """
    abcde
    ${c}a
    a
    a
    abcde

      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test change dollar`() {
    doTest(
      "c$",
      """
            Yesterday it w${c}orked
            Today it is not working
            The test is like that.
      """.trimIndent(),
      """
            Yesterday it w${c}
            Today it is not working
            The test is like that.
      """.trimIndent(),
      Mode.INSERT,
    ) {
      enterCommand("set virtualedit=onemore")
    }
  }
}
