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
class InsertWordUnderCaretActionTest : VimExTestCase() {
  @Test
  fun `test render quote prompt after c_CTRL-R`() {
    configureByText("lorem ipsum")
    typeText(":set <C-R>")
    assertRenderedExText("set \"")
    typeText("<C-W>")
    assertRenderedExText("set lorem")
  }

  @Test
  fun `test render quote prompt after c_CTRL-R_CTRL-R`() {
    configureByText("lorem ipsum")
    typeText(":set <C-R><C-R>")
    assertRenderedExText("set \"")
    typeText("<C-W>")
    assertRenderedExText("set lorem")
  }

  @Test
  fun `test render quote prompt after c_CTRL-R_CTRL-O`() {
    configureByText("lorem ipsum")
    typeText(":set <C-R><C-O>")
    assertRenderedExText("set \"")
    typeText("<C-W>")
    assertRenderedExText("set lorem")
  }

  @Test
  fun `test insert word under caret`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":<C-R><C-W>")
    assertExText("adipiscing")
  }

  @Test
  fun `test insert word under caret literally with c_CTRL-R_CTRL-R`() {
    // How do you insert a word "literally"? It can't contain control characters/shortcuts, and we don't apply maps
    // while replaying/inserting, so a word can only ever be literal text, right?
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":<C-R><C-R><C-W>")
    assertExText("adipiscing")
  }

  @Test
  fun `test insert word under caret literally with c_CTRL-R_CTRL-O`() {
    // How do you insert a word "literally"? It can't contain control characters/shortcuts, and we don't apply maps
    // while replaying/inserting, so a word can only ever be literal text, right?
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":<C-R><C-O><C-W>")
    assertExText("adipiscing")
  }

  @Test
  fun `test insert word under caret after existing command line text`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":set ", "<C-R><C-W>")
    assertExText("set adipiscing")
  }

  @Test
  fun `test insert word under caret before existing command line text`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":set <Home>", "<C-R><C-W>")
    assertExText("adipiscingset ")
  }

  @Test
  fun `test insert word under caret does not include word boundaries - punctuation`() {
    configureByText("""
      |  Lorem ipsum, dol${c}or, sit amet,
      |  consectetur adipiscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":<C-R><C-W>")
    assertExText("dolor")
  }

  @VimBehaviorDiffers("conse")
  @Test
  fun `test insert word under caret does not include word boundaries - emoji`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  con${c}se😁ctetur adipiscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":<C-R><C-W>")
//    assertExText("conse")
    assertExText("""conse😁ctetur""")  // TODO: Why does word match this, but WORD does not?!
  }

  @Test
  fun `test insert word under caret on leading whitespace inserts next word`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |${c}  consectetur adipiscing elit
      |  Sed in orci mauris.
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":<C-R><C-W>")
    assertExText("consectetur")
  }

  @Test
  fun `test insert word under caret ignores intermediate punctuation`() {
    configureByText("""|
      |  fun something${c}(value: Int) {
      |  }
      """.trimMargin()
    )
    typeText(":", "<C-R><C-W>")
    assertExText("value")
  }

  @VimBehaviorDiffers("()")
  @Test
  fun `test insert word under caret with no following word uses current punctuation`() {
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
  fun `test insert word under caret with no following word uses following punctuation after leading punctuation`() {
    configureByText("""|
      |  fun something  $c  () {
      |  }
      """.trimMargin()
    )
    typeText(":", "<C-R><C-W>")
    assertExText("(")
  }

  @Test
  fun `test insert word under caret on trailing whitespace causes error`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adipiscing elit....${c}..
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace())
    typeText(":<C-R><C-W>")
    assertPluginError(true)
    assertPluginErrorMessageContains("E348: No string under cursor")
  }

  @Test
  fun `test insert word under caret on blank line causes error`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adipiscing elit......
      |  Sed in orci mauris.
      |$c
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace())
    typeText(":<C-R><C-W>")
    assertPluginError(true)
    assertPluginErrorMessageContains("E348: No string under cursor")
  }

  @Test
  fun `test insert word under caret replaces matching prefix in command line`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":adip<C-R><C-W>")
    assertExText("adipiscing")
  }

  @Test
  fun `test insert word under caret maintains matching prefix in command line regardless of case`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":ADIP<C-R><C-W>")
    assertExText("ADIPiscing")
  }

  @Test
  fun `test insert word under caret matches word boundary in prefix`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":set something=foo,adip<C-R><C-W>")
    assertExText("set something=foo,adipiscing")
  }

  @Test
  fun `test insert word under caret appends to partial matching prefix in command line 1`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":dip<C-R><C-W>")
    assertExText("dipadipiscing")
  }

  @Test
  fun `test insert word under caret appends to partial matching prefix in command line 2`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText(":xadip<C-R><C-W>")
    assertExText("xadipadipiscing")
  }

  @Test
  fun `test inserts word under offset of end of incsearch range`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommand("set incsearch")
    typeText("/orci ma<C-R><C-W>")
    assertExText("orci mauris")
  }

  @Test
  fun `test inserts word literally under offset of end of incsearch range`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommand("set incsearch")
    typeText("/orci ma<C-R><C-R><C-W>")
    assertExText("orci mauris")
  }

  @Test
  fun `test inserts word following offset of end of incsearch range when incsearch is at end of word`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommand("set incsearch")
    typeText("/in orci<C-R><C-W>")
    assertExText("in orcimauris")
  }

  @VimBehaviorDiffers("()")
  @Test
  fun `test inserts punctuation following offset of end of incsearch range when incsearch is at end of word`() {
    configureByText("""|
      |${c}  fun something() {
      |  }
      """.trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/something<C-R><C-W>")
    assertExText("something(")
  }
}
