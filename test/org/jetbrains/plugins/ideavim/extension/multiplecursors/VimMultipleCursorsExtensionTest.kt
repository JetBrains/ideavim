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

    typeText(parseKeys("*"))
    typeText(parseKeys("<A-n>"))
    typeText(parseKeys("<A-n>"))
    typeText(parseKeys("<A-n>"))

    val after = """public class ChangeLineAction extends EditorAction {
  public ChangeLineAction() {
    super(new ChangeEditorActionHandler(true, CaretOrder.DECREASING_OFFSET) {
      @Override
      public boolean execute(@<selection>NotNull</selection> Editor editor,
                             @NotNull Caret caret,
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

    typeText(parseKeys("*"))
    typeText(parseKeys("<A-n>", "<A-x>", "<A-n>"))

    val after = """<selection>private</selection> int a = 0;
      |<selection>private</selection> int b = 1;
      |private int c = 2;
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


    typeText(parseKeys("*", "<A-n>", "<A-n>", "<A-n>", "<A-p>", "<A-n>"))
    val after = """private int a = 0;
      |private int b = 1;
      |private <selection>int</selection> c = 2;
      |private <selection>int</selection> d = 3;
      |private <selection>int</selection> e = 4;
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

    typeText(parseKeys("*", "<A-n>", "<A-x>", "<A-n>", "<A-n>", "<A-n>", "<A-p>", "<A-n>", "<A-x>"))
    typeText(parseKeys("<Esc>"))

    val after = """public class Main {
      |  public static void main(String[] args) {
      |    <selection>final</selection> Integer a = 0;
      |    final Integer b = 1;
      |    final Integer c = 2;
      |    <selection>final</selection> Integer d = 3;
      |    <selection>final</selection> Integer e = 5;
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
      |    fina<caret>l Integer a = 0;
      |    final Integer b = 1;
      |    final Integer c = 2;
      |    fina<caret>l Integer d = 3;
      |    fina<caret>l Integer e = 5;
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
      |    @NotNull final Integer a = 0;
      |    final Integer b = 1;
      |    final Integer c = 2;
      |    @NotNull final Integer d = 3;
      |    @NotNull final Integer e = 5;
      |    @NotNull final Integer f = 6;
      |    final Integer g = 7;
      |  }
      |}
    """.trimMargin()

    assertMode(CommandState.Mode.COMMAND)
    myFixture.checkResult(afterInsert)
  }
}
