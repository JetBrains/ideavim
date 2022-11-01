/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import org.jetbrains.plugins.ideavim.OptionValueType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimOptionDefaultAll
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption

class DeleteVisualLinesEndActionTest : VimOptionTestCase(OptionConstants.virtualeditName) {
  @VimOptionDefaultAll
  fun `test simple deletion`() {
    val keys = listOf("v", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore))
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore))
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore))
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @VimOptionDefaultAll
  fun `test simple deletion with indent`() {
    val keys = listOf("v", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

                ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test simple deletion with indent and nostartofline`() {
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startoflineName)
    val keys = listOf("v", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

              ${c}  all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test simple deletion empty line`() {
    val keys = listOf("v", "D")
    val before = """
            A Discovery
            ${c}
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery
            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test simple deletion last line`() {
    val keys = listOf("v", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.

    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test simple deletion first line`() {
    val keys = listOf("v", "D")
    val before = """
            A ${c}Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            ${c}
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test simple deletion before empty`() {
    val keys = listOf("v", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,

            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            ${c}
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test simple deletion last line without empty line`() {
    val keys = listOf("v", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            ${c}where it was settled on some sodden sand
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test simple deletion multiline`() {
    val keys = listOf("vj", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            ${c}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test simple deletion multiline motion up`() {
    val keys = listOf("vk", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            ${c}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test delete visual lines end action`() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "2j" + "D"),
      """
                    a${c}bcde
                    abcde
                    abcde
                    abcde
                    abcd${c}e
                    abcde
                    abcde

      """.trimIndent()
    )
    assertState("${c}abcde\n${c}")
  }

  @VimOptionDefaultAll
  fun `test line simple deletion`() {
    val keys = listOf("V", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test line deletion with indent`() {
    val keys = listOf("V", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

                ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test line deletion with indent and nostartofline`() {
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startoflineName)
    val keys = listOf("V", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

              ${c}  all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test line deletion empty line`() {
    val keys = listOf("V", "D")
    val before = """
            A Discovery
            ${c}
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery
            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test line deletion last line`() {
    val keys = listOf("V", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.

    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test line deletion last line without empty line`() {
    val keys = listOf("V", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            ${c}where it was settled on some sodden sand
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test line deletion multiline`() {
    val keys = listOf("Vj", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            ${c}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test line deletion multiline motion up`() {
    val keys = listOf("Vk", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            ${c}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test line delete visual lines end action`() {
    typeTextInFile(
      injector.parser.parseKeys("V" + "2j" + "D"),
      """
                    a${c}bcde
                    abcde
                    abcde
                    abcde
                    abcd${c}e
                    abcde
                    abcde

      """.trimIndent()
    )
    assertState("${c}abcde\n${c}")
  }

  @VimOptionDefaultAll
  fun `test block simple deletion`() {
    val keys = listOf("<C-V>", "D")
    val before = """
            A Discovery

            I${c} found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test block deletion empty line`() {
    val keys = listOf("<C-V>", "D")
    val before = """
            A Discovery
            ${c}
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery
            ${c}
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test block deletion last line`() {
    val keys = listOf("<C-V>", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the${c} torrent of a mountain pass.

    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the

    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test block deletion last line without empty line`() {
    val keys = listOf("<C-V>", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the${c} torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test block deletion multiline`() {
    val keys = listOf("<C-V>j", "D")
    val before = """
            A Discovery

            I${c} found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I
            a
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
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
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test delete visual block line end action`() {
    typeTextInFile(
      injector.parser.parseKeys("<C-V>" + "2j" + "2l" + "D"),
      """
                    abcde
                    a${c}bcde
                    abcde
                    abcde
                    abcde

      """.trimIndent()
    )
    assertState(
      """
    abcde
    ${c}a
    a
    a
    abcde

      """.trimIndent()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore))
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
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }
}
