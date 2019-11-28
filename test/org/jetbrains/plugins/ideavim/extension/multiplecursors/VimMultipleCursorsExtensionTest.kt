/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.commandState
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

    typeText(parseKeys("<A-n>".repeat(3)))

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

    myFixture.checkResult(after)
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

    typeText(parseKeys("<A-n>".repeat(6)))

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

    myFixture.checkResult(after)
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

    typeText(parseKeys("g<A-n>", "<A-n>".repeat(before.count { it == '\n' } - 1)))

    val after = """${s}Int$se
      |Integer
      |${s}Int$se
      |${s}Int${se}eger
      |${s}Int${se}eger
      |${s}Int$se
      |${s}Int${se}ger
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testSelectSubstring() {
    val before = """q${c}we
      |asdqweasd
      |qwe
      |asd
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("g<A-n>".repeat(3)))

    val after = """${s}qwe$se
      |asd${s}qwe${se}asd
      |${s}qwe$se
      |asd
    """.trimMargin()
    myFixture.checkResult(after)
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

    typeText(parseKeys("<A-n>".repeat(4)))

    val after = """${s}qwe$se
      |asd
      |zxc
      |cvb
      |dfg
      |rty
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testSelectionWithMultipleCarets() {
    val before = """qwe
      |sdfgdfs${c}fdasfg
      |${c}dfkjsghdfs
      |gkj dhfs
      |dfsgdf${c}dfkgh dfs
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("<A-n>"))
    myFixture.checkResult(before)
  }

  fun testSelectAll() {
    val before = """qwe
      |asd
      |q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("<Plug>AllWholeOccurrences"))

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    myFixture.checkResult(after)
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

    typeText(parseKeys("<Plug>AllOccurrences"))
    val after = """${s}Int$se
      |${s}Int${se}eger
      |${s}Int$se
      |${s}Int${se}eger
      |${s}Int${se}eger
      |${s}Int$se
      |${s}Int${se}ger
    """.trimMargin()
    myFixture.checkResult(after)
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

    typeText(parseKeys("<Plug>AllOccurrences"))

    val after = before.replace("z${c}xc", "${s}zxc$se")
    myFixture.checkResult(after)
  }

  fun testRemoveSelectionVisualMode() {
    val before = """q${s}we
      |dsgkldfjs ldfl gkjsdsl kj
      |dsfg dhjs${se}dafkljgh
      |dfkjsg
    """.trimMargin()
    val editor = configureByText(before)
    editor.commandState.pushState(CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER, MappingMode.VISUAL)

    typeText(parseKeys("<A-p>"))
    myFixture.checkResult(before)
  }

  fun testRemoveSubSelection() {
    val before = """Int
      |kekInteger
      |lolInteger
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("g<A-n>", "<A-n>".repeat(2), "<A-p>"))

    val after = """${s}Int$se
      |kek${s}Int${se}eger
      |lolInteger
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testRemoveOccurrence() {
    val before = """private i${c}nt a = 0;
      |private int b = 1;
      |private int c = 2;
      |private int d = 3;
      |private int e = 4;
    """.trimMargin()
    configureByJavaText(before)
  }

  fun testSkipOccurrence() {
    val before = """pr${c}ivate int a = 0;
      |private int b = 1;
      |private int c = 2;
    """.trimMargin()
    configureByJavaText(before)

    typeText(parseKeys("<A-n>", "<A-x>", "<A-n>"))

    val after = """private int a = 0;
      |${s}private$se int b = 1;
      |${s}private$se int c = 2;
    """.trimMargin()

    myFixture.checkResult(after)
  }

  fun testSkipAndThenSelectAllOccurrences() {
    val before = """pr${c}ivate int a = 0;
      |private int b = 1;
      |private int c = 2;
    """.trimMargin()
    configureByJavaText(before)

    typeText(parseKeys("<A-n>", "<A-x>", "<A-n>".repeat(3)))

    val after = """${s}private$se int a = 0;
      |${s}private$se int b = 1;
      |${s}private$se int c = 2;
    """.trimMargin()

    myFixture.checkResult(after)
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

    typeText(parseKeys("<A-n>", "<A-x>", "<A-n>", "<A-n>", "<A-n>", "<A-p>", "<A-n>", "<A-x>"))

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

    myFixture.checkResult(after)

    typeText(parseKeys("<Esc>"))
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

    assertMode(CommandState.Mode.COMMAND)
    myFixture.checkResult(afterEscape)

    typeText(parseKeys("I", "@NotNull ", "<Esc>"))
    assertMode(CommandState.Mode.COMMAND)
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

    assertMode(CommandState.Mode.COMMAND)
    myFixture.checkResult(afterInsert)
  }

  fun testSelectTwice() {
    val before = """qwe
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("<A-n>".repeat(3)))
    assertMode(CommandState.Mode.VISUAL)
    typeText(parseKeys("<A-p>".repeat(3)))
    assertMode(CommandState.Mode.COMMAND)
    typeText(parseKeys("<A-n>".repeat(2)))

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    myFixture.checkResult(after)
  }


  fun testSkipSelectionSubstring() {
    val before = """qw${c}e
      |asdqweasd
      |ads
      |asdqweasd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("g<A-n>", "<A-x>", "<A-n>".repeat(2)))

    val after = """qwe
      |asd${s}qwe${se}asd
      |ads
      |asd${s}qwe${se}asd
      |${s}qwe$se
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testSkipSelectionVisualMode() {
    val before = """q${s}we
      |dsgkldfjs ldfl gkjsdsl kj
      |dsfg dhjs${se}dafkljgh
      |dfkjsg
    """.trimMargin()
    val editor = configureByText(before)
    editor.commandState.pushState(CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER, MappingMode.VISUAL)

    typeText(parseKeys("<A-x>"))
    assertMode(CommandState.Mode.VISUAL)
    myFixture.checkResult(before)
  }

  fun testAddSelectionVisualMode() {
    val before = """jdfsg sdf${c}dfkgjhfkgkldfjsg
      |dfkjghdfsgs
      |dflsgsdfgh
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("vjl", "<A-n>"))
    val after = """jdfsg sdf${c}dfkgjhfkgkldfjsg
                        |dfkjghdfs${c}gs
                        |dflsgsdfgh""".trimMargin()
    myFixture.checkResult(after)
  }

  fun testNextOccurrenceIgnorecase() {
    val before = """fun getCellType(${c}pos: VisualPosition): CellType {
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
    typeText(parseKeys("g<A-n><A-n><A-n>"))
    val after = """fun getCellType(${s}pos$se: Visual${s}Pos${se}ition): CellType {
    if (${s}pos$se in snakeCells) {
      return CellType.SNAKE
    }
    val char = getCharAt(pos)
    return when {
      char.isWhitespace() || pos in eatenCells -> CellType.EMPTY
      char in ANTI_PYTHON_CHARS -> CellType.FOOD
      else -> CellType.WALL
    }
    }"""
    myFixture.checkResult(after)
  }

  fun `test with tabs`() {
    val before = dotToTab("""
      I found it in a legendary land
      ...${c}all rocks and lavender and tufted grass,
      ...all it was settled on some sodden sand
      ...all by the torrent of a mountain pass
    """.trimIndent())
    val keys = parseKeys("vll", "<A-N>", "<A-N>")
    val after = dotToTab("""
      I found it in a legendary land
      ...${s}al${c}l${se} rocks and lavender and tufted grass,
      ...${s}al${c}l${se} it was settled on some sodden sand
      ...${s}al${c}l${se} by the torrent of a mountain pass
    """.trimIndent())
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
  }

  fun `test multiple capitalized occurrences with ignorecase`() {
    val before = """text ${c}Test text Test text Test text Test text"""
    configureByText(before)

    typeText(commandToKeys("set ignorecase"))
    typeText(parseKeys("<A-n><A-n><A-n><A-n>"))
    val after = """text ${s}Test${se} text ${s}Test${se} text ${s}Test${se} text ${s}Test${se} text"""
    myFixture.checkResult(after)
  }

  fun `test multiple mixed case occurrences with ignorecase`() {
    val before = """text ${c}Test text tesT text TEST text test text"""
    configureByText(before)

    typeText(commandToKeys("set ignorecase"))
    typeText(parseKeys("<A-n><A-n><A-n><A-n>"))
    val after = """text ${s}Test${se} text ${s}tesT${se} text ${s}TEST${se} text ${s}test${se} text"""
    myFixture.checkResult(after)
  }
}
