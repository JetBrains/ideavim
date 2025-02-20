/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.multiplecursors

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class VimMultipleCursorsExtensionTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("multiple-cursors")
  }

  @Test
  fun testNotWholeOccurrence() {
    val before = """Int
      |Integer
      |I${c}nt
      |Integer
      |Integer
      |Int
      |Intger
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("g<A-n>" + "<A-n>".repeat(before.count { it == '\n' } - 1)))

    val after = """${s}Int$se
      |Integer
      |${s}Int$se
      |${s}Int${se}eger
      |${s}Int${se}eger
      |${s}Int$se
      |${s}Int${se}ger
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun testSelectSubstring() {
    val before = """q${c}we
      |asdqweasd
      |qwe
      |asd
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("g<A-n>".repeat(3)))

    val after = """${s}qwe$se
      |asd${s}qwe${se}asd
      |${s}qwe$se
      |asd
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun testSelectSingleOccurrence() {
    val before = """q${c}we
      |asd
      |zxc
      |cvb
      |dfg
      |rty
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("<A-n>".repeat(4)))

    val after = """${s}qwe$se
      |asd
      |zxc
      |cvb
      |dfg
      |rty
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun testSelectionWithMultipleCarets() {
    val before = """qwe
      |sdfgdfs${c}fdasfg
      |${c}dfkjsghdfs
      |gkj dhfs
      |dfsgdf${c}dfkgh dfs
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("<A-n>"))
    assertState(before)
  }

  @Test
  fun testSelectAll() {
    val before = """qwe
      |asd
      |q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("<Plug>AllWholeOccurrences"))

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun testSelectAllNotWhole() {
    val before = """Int
      |Integer
      |I${c}nt
      |Integer
      |Integer
      |Int
      |Intger
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("<Plug>AllOccurrences"))
    val after = """${s}Int$se
      |${s}Int${se}eger
      |${s}Int$se
      |${s}Int${se}eger
      |${s}Int${se}eger
      |${s}Int$se
      |${s}Int${se}ger
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun testSelectAllSingleOccurrence() {
    val before = """qwe
      |asd
      |z${c}xc
      |adgf
      |dfgh
      |awe
      |td
      |gfhsd
      |fg
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("<Plug>AllOccurrences"))

    val after = before.replace("z${c}xc", "${s}zxc$se")
    assertState(after)
  }

  @VimBehaviorDiffers(
    shouldBeFixed = false,
    description = "Vim does not have native support for multiple cursors, so vim-multiple-cursors fakes it and " +
      "keeps a track of selections added as part of additional cursors. It will only remove selections from these " +
      "additional cursors. IdeaVim has native support, so doesn't track if a selection is due to an additional cursor " +
      "so IdeaVim will remove arbitrary selections, while vim-multiple-cursors do not.",
  )
  @Test
  fun testRemoveSelectionVisualMode() {
    val before = """q${s}we
      |dsgkldfjs ldfl gkjsdsl kj
      |dsfg dhjs${se}dafkljgh
      |dfkjsg
    """.trimMargin()
    val editor = configureByText(before)
    ApplicationManager.getApplication().invokeAndWait {
      editor.vim.mode = Mode.VISUAL(SelectionType.CHARACTER_WISE)
    }

    typeText(injector.parser.parseKeys("<A-p>"))

    val after = """qwe
      |dsgkldfjs ldfl gkjsdsl kj
      |dsfg dhjsdafkljgh
      |dfkjsg
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun testRemoveSubSelection() {
    val before = """Int
      |kekInteger
      |lolInteger
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("g<A-n>" + "<A-n>".repeat(2) + "<A-p>"))

    val after = """${s}Int$se
      |kek${s}Int${se}eger
      |lolInteger
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun testSelectTwice() {
    val before = """qwe
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("<A-n>".repeat(3)))
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    typeText(injector.parser.parseKeys("<A-p>".repeat(3)))
    assertMode(Mode.NORMAL())
    typeText(injector.parser.parseKeys("<A-n>".repeat(2)))

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun testSkipSelectionSubstring() {
    val before = """qw${c}e
      |asdqweasd
      |ads
      |asdqweasd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("g<A-n>" + "<A-x>" + "<A-n>".repeat(2)))

    val after = """qwe
      |asd${s}qwe${se}asd
      |ads
      |asd${s}qwe${se}asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun testSkipSelectionVisualMode() {
    val before = """q${s}we
      |dsgkldfjs ldfl gkjsdsl kj
      |dsfg dhjs${se}dafkljgh
      |dfkjsg
    """.trimMargin()
    val editor = configureByText(before)
    ApplicationManager.getApplication().invokeAndWait {
      editor.vim.mode = Mode.VISUAL(SelectionType.CHARACTER_WISE)
    }

    typeText(injector.parser.parseKeys("<A-x>"))
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState(before)
  }

  @Test
  fun testAddSelectionVisualMode() {
    val before = """jdfsg sdf${c}dfkgjhfkgkldfjsg
      |dfkjghdfsgs
      |dflsgsdfgh
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("vjl" + "<A-n>"))
    val after = """jdfsg sdf${c}dfkgjhfkgkldfjsg
                        |dfkjghdfs${c}gs
                        |dflsgsdfgh
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun testNextOccurrenceCaseSensitive() {
    val before = """@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
fun getCellType(${c}pos: VisualPosition): CellType {
    if (pos in snakeCells) {
      return CellType.SNAKE
    }
    val char = getCharAt(pos)
    return when {
      char.isWhitespace() || pos in eatenCells -> CellType.EMPTY
      char in ANTI_PYTHON_CHARS -> CellType.FOOD
      else -> CellType.WALL
    }
    }"""
    configureByText(before)

    typeText(commandToKeys("set ignorecase"))
    typeText(injector.parser.parseKeys("g<A-n><A-n><A-n>"))
    val after = """@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
fun getCellType(${s}pos$se: VisualPosition): CellType {
    if (${s}pos$se in snakeCells) {
      return CellType.SNAKE
    }
    val char = getCharAt(${s}pos$se)
    return when {
      char.isWhitespace() || pos in eatenCells -> CellType.EMPTY
      char in ANTI_PYTHON_CHARS -> CellType.FOOD
      else -> CellType.WALL
    }
    }"""
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test with tabs`() {
    val before = """
  I found it in a legendary land
  ...${c}all rocks and lavender and tufted grass,
  ...all it was settled on some sodden sand
  ...all by the torrent of a mountain pass
    """.trimIndent().dotToTab()
    val keys = listOf("vll", "<A-N>", "<A-N>")
    val after = """
  I found it in a legendary land
  ...${s}al${c}l$se rocks and lavender and tufted grass,
  ...${s}al${c}l$se it was settled on some sodden sand
  ...${s}al${c}l$se by the torrent of a mountain pass
    """.trimIndent().dotToTab()
    doTest(keys, before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test pattern is always case sensitive`() {
    val before = """test ${c}Test tEst TeSt tEST Test test Test test"""
    configureByText(before)

    typeText(commandToKeys("set ignorecase"))
    typeText(injector.parser.parseKeys("<A-n><A-n><A-n><A-n>"))
    val after = """test ${s}Test$se tEst TeSt tEST ${s}Test$se test ${s}Test$se test"""
    assertState(after)
  }

  @Test
  fun `test ignores regex in search pattern`() {
    val before = "test ${s}t.*st${c}$se toast tallest t.*st"
    val editor = configureByText(before)
    ApplicationManager.getApplication().invokeAndWait {
      editor.vim.mode = Mode.VISUAL(SelectionType.CHARACTER_WISE)
    }

    typeText(injector.parser.parseKeys("<A-n><A-n>"))
    val after = "test ${s}t.*st$se toast tallest ${s}t.*st$se"
    assertState(after)
  }

  @Test
  fun `test adding multicaret after linewise selection`() {
    val before = """
      ${c}Lorem ipsum dolor sit amet,
      consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas. 
    """.trimIndent()

    val after = """
      ${c}Lorem ipsum dolor sit amet,
      ${c}consectetur adipiscing elit
      ${c}Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas. 
    """.trimIndent()
    doTest("Vjj<A-n>", before, after)
  }

  @Test
  fun `test adding multicaret after linewise selection till the last line`() {
    val before = """
      ${c}Lorem ipsum dolor sit amet,
      consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas. 
    """.trimIndent()
    val after = """
      ${c}Lorem ipsum dolor sit amet,
      ${c}consectetur adipiscing elit
      ${c}Sed in orci mauris.
      ${c}Cras id tellus in ex imperdiet egestas. 
    """.trimIndent()
    doTest("Vjjj<A-n>", before, after)
  }

  @Test
  fun `test adding multicaret after linewise selection moving up`() {
    val before = """
      Lorem ipsum dolor sit amet,
      consectetur adipiscing elit
      ${c}Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()

    val after = """
      ${c}Lorem ipsum dolor sit amet,
      ${c}consectetur adipiscing elit
      ${c}Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest("Vkk<A-n>", before, after)
  }

  @Test
  fun `test adding multicaret after linewise selection moving up from down`() {
    val before = """
      Lorem ipsum dolor sit amet,
      consectetur adipiscing elit
      Sed in orci mauris.
      ${c}Cras id tellus in ex imperdiet egestas.
    """.trimIndent()

    val after = """
      Lorem ipsum dolor sit amet,
      ${c}consectetur adipiscing elit
      ${c}Sed in orci mauris.
      ${c}Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest("Vkk<A-n>", before, after)
  }

  @Test
  fun `test adding multicaret after linewise selection moving up to empty lines`() {
    val before = """
      Lorem ipsum dolor sit amet,
      consectetur adipiscing elit
      
      
      
      Sed in orci mauris.
      ${c}Cras id tellus in ex imperdiet egestas.
    """.trimIndent()

    val after = """
      Lorem ipsum dolor sit amet,
      consectetur adipiscing elit
      
      $c
      $c
      ${c}Sed in orci mauris.
      ${c}Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest("Vkkk<A-n>", before, after)
  }

  @Test
  fun `test adding multicaret after linewise selection moving down to empty lines`() {
    val before = """
      ${c}Lorem ipsum dolor sit amet,
      consectetur adipiscing elit
      
      
      
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()

    val after = """
      ${c}Lorem ipsum dolor sit amet,
      ${c}consectetur adipiscing elit
      $c
      $c
      
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest("Vjjj<A-n>", before, after)
  }
}
