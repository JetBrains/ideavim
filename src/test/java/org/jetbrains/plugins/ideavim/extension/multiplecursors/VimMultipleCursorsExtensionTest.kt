/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.extension.multiplecursors

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class VimMultipleCursorsExtensionTest : VimTestCase() {

  override fun setUp() {
    super.setUp()
    enableExtensions("multiple-cursors")
  }

  fun testNextOccurrence() {
    val before = """public class ChangeLineAction extends EditorAction {
  public ChangeLineAction() {
    super(new ChangeEditorActionHandler(true, CaretOrder.DECREASING_OFFSET) {
      @Override
      public boolean execute(@Not${c}Null Editor editor,
                             @NotNull Caret c,
                             @NotNull DataContext context,
                             int count,
                             int rawCount,
                             @NotNull Argument argument) {
        return VimPlugin.getChange().changeLine(editor, c, count);
      }
    });
  }
}"""
    configureByJavaText(before)

    typeText(injector.parser.parseKeys("<A-n>".repeat(3)))

    val after = """public class ChangeLineAction extends EditorAction {
  public ChangeLineAction() {
    super(new ChangeEditorActionHandler(true, CaretOrder.DECREASING_OFFSET) {
      @Override
      public boolean execute(@${s}NotNull$se Editor editor,
                             @${s}NotNull$se Caret c,
                             @${s}NotNull$se DataContext context,
                             int count,
                             int rawCount,
                             @NotNull Argument argument) {
        return VimPlugin.getChange().changeLine(editor, c, count);
      }
    });
  }
}"""

    assertState(after)
  }

  fun testAllOccurrencesIterative() {
    val before = """public class ChangeLineAction extends EditorAction {
  public ChangeLineAction() {
    super(new ChangeEditorActionHandler(true, CaretOrder.DECREASING_OFFSET) {
      @Override
      public boolean execute(@Not${c}Null Editor editor,
                             @NotNull Caret c,
                             @NotNull DataContext context,
                             int count,
                             int rawCount,
                             @NotNull Argument argument) {
        return VimPlugin.getChange().changeLine(editor, c, count);
      }
    });
  }
}"""
    configureByJavaText(before)

    typeText(injector.parser.parseKeys("<A-n>".repeat(6)))

    val after = """public class ChangeLineAction extends EditorAction {
  public ChangeLineAction() {
    super(new ChangeEditorActionHandler(true, CaretOrder.DECREASING_OFFSET) {
      @Override
      public boolean execute(@${s}NotNull$se Editor editor,
                             @${s}NotNull$se Caret c,
                             @${s}NotNull$se DataContext context,
                             int count,
                             int rawCount,
                             @${s}NotNull$se Argument argument) {
        return VimPlugin.getChange().changeLine(editor, c, count);
      }
    });
  }
}"""

    assertState(after)
  }

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
      "so IdeaVim will remove arbitrary selections, while vim-multiple-cursors do not."
  )
  fun testRemoveSelectionVisualMode() {
    val before = """q${s}we
      |dsgkldfjs ldfl gkjsdsl kj
      |dsfg dhjs${se}dafkljgh
      |dfkjsg
    """.trimMargin()
    val editor = configureByText(before)
    editor.vim.vimStateMachine.pushModes(VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER)

    typeText(injector.parser.parseKeys("<A-p>"))

    val after = """qwe
      |dsgkldfjs ldfl gkjsdsl kj
      |dsfg dhjsdafkljgh
      |dfkjsg
    """.trimMargin()
    assertState(after)
  }

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

  fun testRemoveOccurrence() {
    val before = """private i${c}nt a = 0;
      |private int b = 1;
      |private int c = 2;
      |private int d = 3;
      |private int e = 4;
    """.trimMargin()
    configureByJavaText(before)

    typeText(injector.parser.parseKeys("<A-n>" + "<A-n>".repeat(3) + "<A-p>"))

    val after = """private ${s}int$se a = 0;
      |private ${s}int$se b = 1;
      |private ${s}int$se c = 2;
      |private int d = 3;
      |private int e = 4;
    """.trimMargin()
    assertState(after)
  }

  fun testSkipOccurrence() {
    val before = """pr${c}ivate int a = 0;
      |private int b = 1;
      |private int c = 2;
    """.trimMargin()
    configureByJavaText(before)

    typeText(injector.parser.parseKeys("<A-n>" + "<A-x>" + "<A-n>"))

    val after = """private int a = 0;
      |${s}private$se int b = 1;
      |${s}private$se int c = 2;
    """.trimMargin()

    assertState(after)
  }

  fun testSkipAndThenSelectAllOccurrences() {
    val before = """pr${c}ivate int a = 0;
      |private int b = 1;
      |private int c = 2;
    """.trimMargin()
    configureByJavaText(before)

    typeText(injector.parser.parseKeys("<A-n>" + "<A-x>" + "<A-n>".repeat(3)))

    val after = """${s}private$se int a = 0;
      |${s}private$se int b = 1;
      |${s}private$se int c = 2;
    """.trimMargin()

    assertState(after)
  }

  fun testSeveralActions() {
    val before = """public class Main {
      |  public static void main(String[] args) {
      |    f${c}inal Integer a = 0;
      |    final Integer b = 1;
      |    final Integer c = 2;
      |    final Integer d = 3;
      |    final Integer e = 5;
      |    final Integer f = 6;
      |    final Integer g = 7;
      |  }
      |}
    """.trimMargin()
    configureByJavaText(before)

    typeText(injector.parser.parseKeys("<A-n>" + "<A-x>" + "<A-n>" + "<A-n>" + "<A-n>" + "<A-p>" + "<A-n>" + "<A-x>"))

    val after = """public class Main {
      |  public static void main(String[] args) {
      |    final Integer a = 0;
      |    ${s}final$se Integer b = 1;
      |    ${s}final$se Integer c = 2;
      |    ${s}final$se Integer d = 3;
      |    final Integer e = 5;
      |    ${s}final$se Integer f = 6;
      |    final Integer g = 7;
      |  }
      |}
    """.trimMargin()

    assertState(after)

    typeText(injector.parser.parseKeys("<Esc>"))
    val afterEscape = """public class Main {
      |  public static void main(String[] args) {
      |    final Integer a = 0;
      |    fina${c}l Integer b = 1;
      |    fina${c}l Integer c = 2;
      |    fina${c}l Integer d = 3;
      |    final Integer e = 5;
      |    fina${c}l Integer f = 6;
      |    final Integer g = 7;
      |  }
      |}
    """.trimMargin()

    assertMode(VimStateMachine.Mode.COMMAND)
    assertState(afterEscape)

    typeText(injector.parser.parseKeys("I" + "@NotNull " + "<Esc>"))
    assertMode(VimStateMachine.Mode.COMMAND)
    val afterInsert = """public class Main {
      |  public static void main(String[] args) {
      |    final Integer a = 0;
      |    @NotNull final Integer b = 1;
      |    @NotNull final Integer c = 2;
      |    @NotNull final Integer d = 3;
      |    final Integer e = 5;
      |    @NotNull final Integer f = 6;
      |    final Integer g = 7;
      |  }
      |}
    """.trimMargin()

    assertMode(VimStateMachine.Mode.COMMAND)
    assertState(afterInsert)
  }

  fun testSelectTwice() {
    val before = """qwe
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("<A-n>".repeat(3)))
    assertMode(VimStateMachine.Mode.VISUAL)
    typeText(injector.parser.parseKeys("<A-p>".repeat(3)))
    assertMode(VimStateMachine.Mode.COMMAND)
    typeText(injector.parser.parseKeys("<A-n>".repeat(2)))

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

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

  fun testSkipSelectionVisualMode() {
    val before = """q${s}we
      |dsgkldfjs ldfl gkjsdsl kj
      |dsfg dhjs${se}dafkljgh
      |dfkjsg
    """.trimMargin()
    val editor = configureByText(before)
    editor.vim.vimStateMachine.pushModes(VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER)

    typeText(injector.parser.parseKeys("<A-x>"))
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState(before)
  }

  fun testAddSelectionVisualMode() {
    val before = """jdfsg sdf${c}dfkgjhfkgkldfjsg
      |dfkjghdfsgs
      |dflsgsdfgh
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("vjl" + "<A-n>"))
    val after = """jdfsg sdf${c}dfkgjhfkgkldfjsg
                        |dfkjghdfs${c}gs
                        |dflsgsdfgh""".trimMargin()
    assertState(after)
  }

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
    doTest(keys, before, after, VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER)
  }

  fun `test pattern is always case sensitive`() {
    val before = """test ${c}Test tEst TeSt tEST Test test Test test"""
    configureByText(before)

    typeText(commandToKeys("set ignorecase"))
    typeText(injector.parser.parseKeys("<A-n><A-n><A-n><A-n>"))
    val after = """test ${s}Test$se tEst TeSt tEST ${s}Test$se test ${s}Test$se test"""
    assertState(after)
  }

  fun `test ignores regex in search pattern`() {
    val before = "test ${s}t.*st${c}$se toast tallest t.*st"
    val editor = configureByText(before)
    editor.vim.vimStateMachine.pushModes(VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER)

    typeText(injector.parser.parseKeys("<A-n><A-n>"))
    val after = "test ${s}t.*st$se toast tallest ${s}t.*st$se"
    assertState(after)
  }
}
