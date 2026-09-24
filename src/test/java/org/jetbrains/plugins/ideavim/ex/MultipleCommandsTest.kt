/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for `|` as a command separator on the Ex command line (VIM-748).
 *
 * From `:help :bar` (neovim:runtime/doc/cmdline.txt:603):
 * ```
 * '|' can be used to separate commands, so you can give multiple commands in one
 * line.  If you want to use '|' in an argument, precede it with '\'.
 *
 * These commands see the '|' as their argument, and can therefore not be
 * followed by another Vim command:
 *     ... :global ... :normal ... :vglobal ... :[range]! ...
 *
 * Note that this is confusing (inherited from Vi): With ":g" the '|' is included
 * in the command, with ":s" it is not.
 * ```
 * (neovim:runtime/doc/cmdline.txt:643)
 *
 * All expectations below were captured from `nvim --clean --headless` (NVIM v0.12.5).
 */
class MultipleCommandsTest : VimTestCase() {
  
  @Test
  fun `test two substitute commands separated by bar`() {
    doTest(
      exCommand("s/lorem/LOREM/ | s/ipsum/IPSUM/"),
      """
        ${c}lorem ipsum dolor sit amet
        consectetur adipiscing elit
      """.trimIndent(),
      """
        ${c}LOREM IPSUM dolor sit amet
        consectetur adipiscing elit
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test two substitute commands separated by bar without whitespace`() {
    doTest(
      exCommand("s/lorem/LOREM/|s/ipsum/IPSUM/"),
      """
        ${c}lorem ipsum dolor sit amet
        consectetur adipiscing elit
      """.trimIndent(),
      """
        ${c}LOREM IPSUM dolor sit amet
        consectetur adipiscing elit
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test three substitute commands separated by bars`() {
    doTest(
      exCommand("%s/lorem/LOREM/|%s/ipsum/IPSUM/|%s/dolor/DOLOR/"),
      """
        ${c}lorem ipsum dolor sit amet
        consectetur adipiscing elit
        lorem ipsum dolor sit amet
      """.trimIndent(),
      """
        LOREM IPSUM DOLOR sit amet
        consectetur adipiscing elit
        ${c}LOREM IPSUM DOLOR sit amet
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test substitute commands with ranges separated by bar`() {
    doTest(
      exCommand("1s/lorem/LOREM/|3s/lorem/LOREM/"),
      """
        ${c}lorem ipsum dolor sit amet
        consectetur adipiscing elit
        lorem ipsum dolor sit amet
      """.trimIndent(),
      """
        LOREM ipsum dolor sit amet
        consectetur adipiscing elit
        ${c}LOREM ipsum dolor sit amet
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test trailing bar does not break the command`() {
    doTest(
      exCommand("s/lorem/LOREM/|"),
      """
        ${c}lorem ipsum dolor sit amet
        consectetur adipiscing elit
      """.trimIndent(),
      """
        ${c}LOREM ipsum dolor sit amet
        consectetur adipiscing elit
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test escaped bar in substitute pattern is not a separator`() {
    // `\|` is regex alternation here, not a command separator
    doTest(
      exCommand("s/lorem\\|ipsum/X/g"),
      """
        ${c}lorem ipsum dolor sit amet
        consectetur adipiscing elit
      """.trimIndent(),
      """
        ${c}X X dolor sit amet
        consectetur adipiscing elit
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test escaped bar in substitute replacement is not a separator`() {
    doTest(
      exCommand("s/ipsum/a\\|b/"),
      """
        ${c}lorem ipsum dolor sit amet
        consectetur adipiscing elit
      """.trimIndent(),
      """
        ${c}lorem a|b dolor sit amet
        consectetur adipiscing elit
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test delete commands separated by bar`() {
    doTest(
      exCommand("1d|1d"),
      """
        ${c}lorem ipsum dolor sit amet
        consectetur adipiscing elit
        sed do eiusmod tempor
      """.trimIndent(),
      """
        ${c}sed do eiusmod tempor
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test put commands with addresses separated by bars`() {
    configureByText(
      """
        ${c}lorem ipsum dolor sit amet
        consectetur adipiscing elit
        sed do eiusmod tempor
        incididunt ut labore
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("yy"))
    enterCommand("1pu|3pu")
    assertState(
      """
        lorem ipsum dolor sit amet
        lorem ipsum dolor sit amet
        consectetur adipiscing elit
        ${c}lorem ipsum dolor sit amet
        sed do eiusmod tempor
        incididunt ut labore
      """.trimIndent(),
    )
  }
  
  @TestWithoutNeovim(
    reason = SkipNeovimReason.OPTION,
    description = "Neovim indents with a tab ('noexpandtab'), the IntelliJ editor indents with spaces",
  )
  @Test
  fun `test shift command after substitute separated by bar`() {
    // `:help :bar` example: `:%s/foo/bar/|>` moves one line one shiftwidth
    doTest(
      exCommand("%s/lorem/LOREM/|>"),
      """
        ${c}lorem ipsum dolor sit amet
        lorem ipsum dolor sit amet
      """.trimIndent(),
      """
        LOREM ipsum dolor sit amet
            ${c}LOREM ipsum dolor sit amet
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test global command consumes the bar`() {
    // `:global` is in the `:bar` exception list, so `|s/dolor/DOLOR/` is part of the :g command
    // and runs on every matching line, not once at the end
    doTest(
      exCommand("g/lorem/s/ipsum/IPSUM/|s/dolor/DOLOR/"),
      """
        ${c}lorem ipsum dolor sit amet
        consectetur adipiscing elit
        lorem ipsum dolor sit amet
      """.trimIndent(),
      """
        lorem IPSUM DOLOR sit amet
        consectetur adipiscing elit
        ${c}lorem IPSUM DOLOR sit amet
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test bar in sort pattern is not a separator`() {
    // The lines are sorted on what follows the match, which is only possible if the whole pattern arrived. Cut at
    // the bar, it would be `/a`
    doTest(
      exCommand("%sort /a|b/"),
      """
        ${c}x a|b 3
        y a|b 1
        z a|b 2
      """.trimIndent(),
      """
        ${c}y a|b 1
        z a|b 2
        x a|b 3
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test bar after sort flags separates`() {
    doTest(
      exCommand("%sort u|1d"),
      """
        ${c}b
        a
        b
      """.trimIndent(),
      """
        ${c}b
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test bar separates a command that the parser does not know by name`() {
    // `:stopinsert` is not in the parser's list of command names, so the catch-all rule hands it the rest of the
    // line. It is a `:bar` command in Vim, so `|1d` is the next command, not its argument
    doTest(
      exCommand("stopinsert|1d"),
      """
        ${c}lorem ipsum dolor sit amet
        consectetur adipiscing elit
      """.trimIndent(),
      """
        ${c}consectetur adipiscing elit
      """.trimIndent(),
    )
  }
  
  @Test
  fun `test normal command consumes the bar`() {
    // `:normal` is in the `:bar` exception list - `|y` is typed as keys, not run as `:yank`
    doTest(
      exCommand("normal Ax|y"),
      """
        ${c}lorem ipsum dolor sit amet
        consectetur adipiscing elit
      """.trimIndent(),
      """
        lorem ipsum dolor sit ametx|${c}y
        consectetur adipiscing elit
      """.trimIndent(),
    )
  }
}
