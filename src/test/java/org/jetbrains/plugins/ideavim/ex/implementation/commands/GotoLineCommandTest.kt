/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("SpellCheckingInspection")
class GotoLineCommandTest : VimTestCase() {
  @Test
  fun `test goto explicit line`() {
    val before = """
      A Discovery

      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("3")
    val after = """
      A Discovery

      ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `test goto first non-whitespace character of line`() {
    doTest(
      exCommand("3"),
      """
        |    Lorem ipsum dolor ${c}sit amet, consectetur adipiscing elit.
        |    Morbi nec luctus tortor, id venenatis lacus.
        |    Nunc sit amet tellus vel purus cursus posuere et at purus.
        |    Ut id dapibus augue.
        |    Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |    Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |    Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |    Morbi nec luctus tortor, id venenatis lacus.
        |    ${c}Nunc sit amet tellus vel purus cursus posuere et at purus.
        |    Ut id dapibus augue.
        |    Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |    Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
  }

  @Test
  fun `test goto line maintains column with nostartofline`() {
    doTest(
      exCommand("3"),
      """
        |    Lorem ipsum dolor ${c}sit amet, consectetur adipiscing elit.
        |    Morbi nec luctus tortor, id venenatis lacus.
        |    Nunc sit amet tellus vel purus cursus posuere et at purus.
        |    Ut id dapibus augue.
        |    Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |    Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |    Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |    Morbi nec luctus tortor, id venenatis lacus.
        |    Nunc sit amet tell${c}us vel purus cursus posuere et at purus.
        |    Ut id dapibus augue.
        |    Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |    Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    ) {
      enterCommand("set nostartofline")
    }
  }

  @Test
  fun `test goto explicit line check history`() {
    val before = """
      A Discovery

      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("3")
    val after = """
      A Discovery

      ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)

    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val register = VimPlugin.getRegister().getRegister(vimEditor, context, ':')
    assertNotNull(register)
    assertEquals("3", register.text)
  }

  @Test
  fun `test goto explicit line beyond end of file without errors`() {
    doTest(
      exCommand("100"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |${c}Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
    assertPluginError(false)
  }

  @Test
  fun `test goto last line`() {
    doTest(
      exCommand("$"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |${c}Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
    assertPluginError(false)
  }

  @Test
  fun `test goto line 0 moves to start of file without errors`() {
    doTest(
      exCommand("0"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
    assertPluginError(false)
  }

  @Test
  fun `test goto positive relative line`() {
    val before = """
      A Discovery

      I found it ${c}in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("+2")
    val after = """
      A Discovery

      I found it in a legendary land
      all rocks and lavender and tufted grass,
      ${c}where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `test goto positive relative line stops at end of file without error`() {
    doTest(
      exCommand("+100"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |${c}Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
    assertPluginError(false)
  }

  @Test
  fun `test goto explicit line with postitive offset`() {
    doTest(
      exCommand("2+3"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
  }

  @Test
  fun `test goto explicit line with too large postitive offset moves to end of file without errors`() {
    doTest(
      exCommand("2+300"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |${c}Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
    assertPluginError(false)
  }

  @Test
  fun `test goto using forward search range`() {
    val before = """
      A Discovery

      I found it ${c}in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("/settled")
    val after = """
      A Discovery

      I found it in a legendary land
      all rocks and lavender and tufted grass,
      ${c}where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `test goto using forward search with too large negative offset moves to top of file without errors`() {
    doTest(
      exCommand("/ipsum/-100"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
      """.trimMargin(),
      """
        |${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
      """.trimMargin(),
    )
    assertPluginError(false)
  }

  @Test
  fun `test goto using complex forward search moving back to start of file`() {
    doTest(
      exCommand("/ipsum/-100,/ipsum/"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
        |${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit.
      """.trimMargin(),
    )
  }

  @Test
  fun `test goto using forward search range without results`() {
    doTest(
      exCommand("/banana"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E486: Pattern not found: banana")
  }

  @Test
  fun `test goto using forward search range without wrapscan`() {
    doTest(
      exCommand("/ipsum"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    ) {
      enterCommand("set nowrapscan")
    }
    assertPluginError(true)
    assertPluginErrorMessage("E385: Search hit BOTTOM without match for: ipsum")
  }

  @Test
  fun `test goto next line with last used search pattern`() {
    doTest(
      listOf(searchCommand("/natoque"), "1G", exCommand("\\/")),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
  }

  @Test
  fun `test goto previous line with last used search pattern`() {
    doTest(
      listOf(searchCommand("/sit"), "2G", exCommand("\\?")),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
  }

  @Test
  fun `test goto next line with last used substitution pattern`() {
    doTest(
      // This tries to subsititute on the current line and will fail, but it will remember the pattern
      listOf(exCommand("s/natoque/banana/g"), "1G", exCommand("\\&")),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
  }

  @Test
  fun `test goto using backward search range`() {
    val before = """
      A Discovery

      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("?lavender")
    val after = """
      A Discovery

      I found it in a legendary land
      ${c}all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `test goto negative relative line`() {
    val before = """
      A Discovery

      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it ${c}was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("-2")
    val after = """
      A Discovery

      ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `test goto negative relative line returns invalid range if beyond top of file`() {
    doTest(
      exCommand("-10"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E16: Invalid range")
  }

  @Test
  fun `test goto explicit line with negative offset`() {
    doTest(
      exCommand("5-3"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
  }

  @Test
  fun `test goto explicit line with too large negative offset is invalid range`() {
    doTest(
      exCommand("4-10"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E16: Invalid range")
  }

  @Test
  fun `test goto last line of complex range`() {
    doTest(
      exCommand("2,3,-2,5"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
  }

  @Test
  fun `test goto mark`() {
    doTest(
      listOf("ma", "3j", exCommand("'a")),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
  }

  @Test
  fun `test goto unset mark reports errors`() {
    doTest(
      exCommand("'a"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E20: Mark not set")
  }

  @Test
  fun `test goto line moves to first non-blank char`() {
    val before = """
      A Discovery

          I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("3")
    val after = """
      A Discovery

          ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `test goto zero relative line moves to first non-blank char on current line`() {
    val before = """
      A Discovery

          I found it ${c}in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("+0")
    val after = """
      A Discovery

          ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test goto line moves to same column with nostartofline option`() {
    val before = """
      A Discovery

          I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("set nostartofline")
    enterCommand("3")
    val after = """
      A Discovery

          I found ${c}it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test goto zero relative line with nostartofline option does not move caret`() {
    val before = """
      A Discovery

          I found it ${c}in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("set nostartofline")
    enterCommand("+0")
    val after = """
      A Discovery

          I found it ${c}in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `test goto line with scrolloff`() {
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    enterCommand("30")
    assertPosition(29, 4)
    assertTopLogicalLine(5)
  }

  @Test
  fun `test goto relative line with scrolloff`() {
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    enterCommand("+30")
    assertPosition(30, 4)
    assertTopLogicalLine(6)
  }
}
