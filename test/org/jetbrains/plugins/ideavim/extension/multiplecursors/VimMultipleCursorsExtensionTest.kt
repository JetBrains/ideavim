package org.jetbrains.plugins.ideavim.extension.multiplecursors

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
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
      public boolean execute(@Not<caret>Null Editor editor,
                             @NotNull Caret caret,
                             @NotNull DataContext context,
                             int count,
                             int rawCount,
                             @NotNull Argument argument) {
        return VimPlugin.getChange().changeLine(editor, caret, count);
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
      public boolean execute(@<selection>NotNull</selection> Editor editor,
                             @<selection>NotNull</selection> Caret caret,
                             @<selection>NotNull</selection> DataContext context,
                             int count,
                             int rawCount,
                             @NotNull Argument argument) {
        return VimPlugin.getChange().changeLine(editor, caret, count);
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
      public boolean execute(@Not<caret>Null Editor editor,
                             @NotNull Caret caret,
                             @NotNull DataContext context,
                             int count,
                             int rawCount,
                             @NotNull Argument argument) {
        return VimPlugin.getChange().changeLine(editor, caret, count);
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
      public boolean execute(@<selection>NotNull</selection> Editor editor,
                             @<selection>NotNull</selection> Caret caret,
                             @<selection>NotNull</selection> DataContext context,
                             int count,
                             int rawCount,
                             @<selection>NotNull</selection> Argument argument) {
        return VimPlugin.getChange().changeLine(editor, caret, count);
      }
    });
  }
}"""

    myFixture.checkResult(after)
  }

  fun testNotWholeOccurrence() {
    val before = """Int
      |Integer
      |I<caret>nt
      |Integer
      |Integer
      |Int
      |Intger
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("g<A-n>", "<A-n>".repeat(before.count { it == '\n' } - 1)))

    val after = """<selection>Int</selection>
      |Integer
      |<selection>Int</selection>
      |<selection>Int</selection>eger
      |<selection>Int</selection>eger
      |<selection>Int</selection>
      |<selection>Int</selection>ger
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testSelectSubstring() {
    val before = """q<caret>we
      |asdqweasd
      |qwe
      |asd
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("g<A-n>".repeat(3)))

    val after = """<selection>qwe</selection>
      |asd<selection>qwe</selection>asd
      |<selection>qwe</selection>
      |asd
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testSelectSingleOccurrence() {
    val before = """q<caret>we
      |asd
      |zxc
      |cvb
      |dfg
      |rty
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("<A-n>".repeat(4)))

    val after = """<selection>qwe</selection>
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
      |sdfgdfs<caret>fdasfg
      |<caret>dfkjsghdfs
      |gkj dhfs
      |dfsgdf<caret>dfkgh dfs
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("<A-n>"))
    myFixture.checkResult(before)
  }

  fun testSelectAll() {
    val before = """qwe
      |asd
      |q<caret>we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("<Plug>AllWholeOccurrences"))

    val after = """<selection>qwe</selection>
      |asd
      |<selection>qwe</selection>
      |asd
      |<selection>qwe</selection>
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testSelectAllNotWhole() {
    val before = """Int
      |Integer
      |I<caret>nt
      |Integer
      |Integer
      |Int
      |Intger
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("<Plug>AllOccurrences"))
    val after = """<selection>Int</selection>
      |<selection>Int</selection>eger
      |<selection>Int</selection>
      |<selection>Int</selection>eger
      |<selection>Int</selection>eger
      |<selection>Int</selection>
      |<selection>Int</selection>ger
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testSelectAllSingleOccurrence() {
    val before = """qwe
      |asd
      |z<caret>xc
      |adgf
      |dfgh
      |awe
      |td
      |gfhsd
      |fg
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("<Plug>AllOccurrences"))

    val after = before.replace("z<caret>xc", "<selection>zxc</selection>")
    myFixture.checkResult(after)
  }

  fun testRemoveSelectionVisualMode() {
    val before = """q<selection>we
      |dsgkldfjs ldfl gkjsdsl kj
      |dsfg dhjs</selection>dafkljgh
      |dfkjsg
    """.trimMargin()
    val editor = configureByText(before)
    CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER,
                                               MappingMode.VISUAL)

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

    val after = """<selection>Int</selection>
      |kek<selection>Int</selection>eger
      |lolInteger
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testRemoveOccurrence() {
    val before = """private i<caret>nt a = 0;
      |private int b = 1;
      |private int c = 2;
      |private int d = 3;
      |private int e = 4;
    """.trimMargin()
    configureByJavaText(before)
  }

  fun testSkipOccurrence() {
    val before = """pr<caret>ivate int a = 0;
      |private int b = 1;
      |private int c = 2;
    """.trimMargin()
    configureByJavaText(before)

    typeText(parseKeys("<A-n>", "<A-x>", "<A-n>"))

    val after = """private int a = 0;
      |<selection>private</selection> int b = 1;
      |<selection>private</selection> int c = 2;
    """.trimMargin()

    myFixture.checkResult(after)
  }

  fun testSkipAndThenSelectAllOccurrences() {
    val before = """pr<caret>ivate int a = 0;
      |private int b = 1;
      |private int c = 2;
    """.trimMargin()
    configureByJavaText(before)

    typeText(parseKeys("<A-n>", "<A-x>", "<A-n>".repeat(3)))

    val after = """<selection>private</selection> int a = 0;
      |<selection>private</selection> int b = 1;
      |<selection>private</selection> int c = 2;
    """.trimMargin()

    myFixture.checkResult(after)
  }

  fun testSeveralActions() {
    val before = """public class Main {
      |  public static void main(String[] args) {
      |    f<caret>inal Integer a = 0;
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
      |    <selection>final</selection> Integer b = 1;
      |    <selection>final</selection> Integer c = 2;
      |    <selection>final</selection> Integer d = 3;
      |    final Integer e = 5;
      |    <selection>final</selection> Integer f = 6;
      |    final Integer g = 7;
      |  }
      |}
    """.trimMargin()

    myFixture.checkResult(after)

    typeText(parseKeys("<Esc>"))
    val afterEscape = """public class Main {
      |  public static void main(String[] args) {
      |    final Integer a = 0;
      |    fina<caret>l Integer b = 1;
      |    fina<caret>l Integer c = 2;
      |    fina<caret>l Integer d = 3;
      |    final Integer e = 5;
      |    fina<caret>l Integer f = 6;
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

    val after = """<selection>qwe</selection>
      |asd
      |<selection>qwe</selection>
      |asd
      |qwe
    """.trimMargin()
    myFixture.checkResult(after)
  }


  fun testSkipSelectionSubstring() {
    val before = """qw<caret>e
      |asdqweasd
      |ads
      |asdqweasd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("g<A-n>", "<A-x>", "<A-n>".repeat(2)))

    val after = """qwe
      |asd<selection>qwe</selection>asd
      |ads
      |asd<selection>qwe</selection>asd
      |<selection>qwe</selection>
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testSkipSelectionVisualMode() {
    val before = """q<selection>we
      |dsgkldfjs ldfl gkjsdsl kj
      |dsfg dhjs</selection>dafkljgh
      |dfkjsg
    """.trimMargin()
    val editor = configureByText(before)
    CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER,
                                               MappingMode.VISUAL)

    typeText(parseKeys("<A-x>"))
    assertMode(CommandState.Mode.VISUAL)
    myFixture.checkResult(before)
  }

  fun testAddSelectionVisualMode() {
    val before = """jdfsg sdf<caret>dfkgjhfkgkldfjsg
      |dfkjghdfsgs
      |dflsgsdfgh
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("vjl", "<A-n>"))
    val after = """jdfsg sdf<caret>dfkgjhfkgkldfjsg
                        |dfkjghdfs<caret>gs
                        |dflsgsdfgh""".trimMargin()
    myFixture.checkResult(after)
  }

  fun testNextOccurrenceIgnorecase() {
    val before = """fun getCellType(<caret>pos: VisualPosition): CellType {
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
    val after = """fun getCellType(<selection>pos</selection>: Visual<selection>Pos</selection>ition): CellType {
    if (<selection>pos</selection> in snakeCells) {
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
}
