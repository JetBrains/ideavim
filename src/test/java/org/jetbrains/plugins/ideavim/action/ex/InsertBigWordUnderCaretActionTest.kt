/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class InsertBigWordUnderCaretActionTest : VimExTestCase() {
  @Test
  fun `test render quote prompt after c_CTRL-R`() {
    configureByText("lorem ipsum")
    typeText(":set <C-R>")
    assertRenderedExText("set \"")
    typeText("<C-A>")
    assertRenderedExText("set lorem")
  }

  @Test
  fun `test render quote prompt after c_CTRL-R_CTRL-R`() {
    configureByText("lorem ipsum")
    typeText(":set <C-R><C-R>")
    assertRenderedExText("set \"")
    typeText("<C-A>")
    assertRenderedExText("set lorem")
  }

  @Test
  fun `test render quote prompt after c_CTRL-R_CTRL-O`() {
    configureByText("lorem ipsum")
    typeText(":set <C-R><C-O>")
    assertRenderedExText("set \"")
    typeText("<C-A>")
    assertRenderedExText("set lorem")
  }

  @Test
  fun `test insert WORD under caret`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":<C-R><C-A>")
    assertExText("adipiscing")
  }

  @Test
  fun `test insert WORD under caret literally with c_CTRL-R_CTRL-R`() {
    // How do you insert a WORD "literally"? It can't contain control characters/shortcuts, and we don't apply maps
    // while replaying/inserting, so a WORD can only ever be literal text, right?
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":<C-R><C-R><C-A>")
    assertExText("adipiscing")
  }

  @Test
  fun `test insert WORD under caret literally with c_CTRL-R_CTRL-O`() {
    // How do you insert a WORD "literally"? It can't contain control characters/shortcuts, and we don't apply maps
    // while replaying/inserting, so a WORD can only ever be literal text, right?
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":<C-R><C-O><C-A>")
    assertExText("adipiscing")
  }

  @Test
  fun `test insert WORD under caret after existing command line text`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":set ", "<C-R><C-A>")
    assertExText("set adipiscing")
  }

  @Test
  fun `test insert WORD under caret before existing command line text`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":set <Home>", "<C-R><C-A>")
    assertExText("adipiscingset ")
  }

  @Test
  fun `test insert WORD under caret includes non-keyword characters - punctuation`() {
    configureByText("""
      |  Lorem ipsum, dol${c}or, sit amet,
      |  consectetur adipiscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":<C-R><C-A>")
    assertExText("dolor,")
  }

  @Test
  fun `test insert WORD under caret includes non-keyword characters - emoji`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  con${c}se😁ctetur adipiscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":<C-R><C-A>")
    assertExText("""conse😁ctetur""")
  }

  @Test
  fun `test insert WORD under caret on leading whitespace inserts next WORD`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |${c}  consectetur adipiscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText("<Home>", ":<C-R><C-A>")
    assertExText("consectetur")
  }

  @Test
  fun `test insert WORD under caret includes intermediate punctuation`() {
    configureByText("""|
      |  fun something${c}(value: Int) {
      |  }
      """.trimMargin()
    )
    typeText(":", "<C-R><C-A>")
    assertExText("something(value:")
  }

  @VimBehaviorDiffers("()")
  @Test
  fun `test insert WORD under caret with no following WORD uses current punctuation`() {
    configureByText("""|
      |  fun something${c}() {
      |  }
      """.trimMargin()
    )
    typeText(":", "<C-R><C-W>")
    assertExText("(")
  }

  @VimBehaviorDiffers("()")
  @Test
  fun `test insert WORD under caret with no following WORD uses following punctuation after leading punctuation`() {
    configureByText("""|
      |  fun something  $c  () {
      |  }
      """.trimMargin()
    )
    typeText(":", "<C-R><C-A>")
    assertExText("()")
  }

  @Test
  fun `test insert WORD under caret on trailing whitespace causes error`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adipiscing elit...${c}...
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace())
    typeText(":<C-R><C-A>")
    assertPluginError(true)
    assertPluginErrorMessage("E348: No string under cursor")
  }

  @Test
  fun `test insert WORD under caret on blank line causes error`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adipiscing elit......
      |  Sed in orci mauris.
      |$c
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace())
    typeText(":<C-R><C-A>")
    assertPluginError(true)
    assertPluginErrorMessage("E348: No string under cursor")
  }

  @Test
  fun `test insert WORD under caret appends to matching prefix in command line`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":adip<C-R><C-A>")
    assertExText("adipadipiscing")
  }

  @Test
  fun `test insert WORD under caret appends to matching prefix in command line regardless of case`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":ADIP<C-R><C-A>")
    assertExText("ADIPadipiscing")
  }

  @Test
  fun `test insert WORD under caret appends to matching WORD prefix including non-word characters`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur --adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":--adip<C-R><C-A>")

    // This is perhaps surprising behaviour - the WORD prefixes match. But it is what Vim does
    assertExText("--adip--adipiscing")
  }

  @Test
  fun `test insert WORD under caret appends to partial matching prefix in command line 1`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":dip<C-R><C-A>")
    assertExText("dipadipiscing")
  }

  @Test
  fun `test insert WORD under caret appends to partial matching prefix in command line 2`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":xadip<C-R><C-A>")
    assertExText("xadipadipiscing")
  }

  @Test
  fun `test insert WORD under caret appends to partial matching prefix including non-word characters`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur --adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":adip<C-R><C-A>")
    assertExText("adip--adipiscing")
  }

  @Test
  fun `test inserts WORD under offset of end of incsearch range`() {
    configureByText(
      """
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/orci ma<C-R><C-A>")
    assertExText("orci mamauris.")
  }

  @Test
  fun `test inserts WORD literally under offset of end of incsearch range`() {
    configureByText(
      """
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/orci ma<C-R><C-R><C-A>")
    assertExText("orci mamauris.")
  }

  @Test
  fun `test inserts WORD following offset of end of incsearch range when incsearch is at end of word`() {
    configureByText(
      """
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/in orci<C-R><C-A>")
    assertExText("in orcimauris.")
  }

  @Test
  fun `test inserts punctuation following offset of end of incsearch range when incsearch is at end of word`() {
    configureByText(
      """|
      |${c}  fun something() {
      |  }
      """.trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/something<C-R><C-A>")
    assertExText("somethingsomething()")
  }
}
