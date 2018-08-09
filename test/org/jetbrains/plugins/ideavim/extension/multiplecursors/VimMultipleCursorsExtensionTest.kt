package org.jetbrains.plugins.ideavim.extension.multiplecursors

import com.maddyhome.idea.vim.command.CommandState
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

  fun testRemoveOccurrence() {
    val before = """private i<caret>nt a = 0;
      |private int b = 1;
      |private int c = 2;
      |private int d = 3;
      |private int e = 4;
    """.trimMargin()
    configureByJavaText(before)


    typeText(parseKeys("<A-n>", "<A-n>", "<A-n>", "<A-p>", "<A-n>"))
    val after = """private <selection>int</selection> a = 0;
      |private <selection>int</selection> b = 1;
      |private <selection>int</selection> c = 2;
      |private int d = 3;
      |private int e = 4;
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
    typeText(parseKeys("<Esc>"))

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

    assertMode(CommandState.Mode.VISUAL)
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

  fun testSkipSelectionVisualMode() {
    val before = """q<selection>we
      |dsgkldfjs ldfl gkjsdsl kj
      |dsfg dhjs</selection>dafkljgh
      |dfkjsg
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("<A-x>"))
    myFixture.checkResult(before)
  }

  fun testRemoveSelectionVisualMode() {
    val before = """q<selection>we
      |dsgkldfjs ldfl gkjsdsl kj
      |dsfg dhjs</selection>dafkljgh
      |dfkjsg
    """.trimMargin()
    configureByText(before)

    typeText(parseKeys("<A-p>"))
    myFixture.checkResult(before)
  }
}
