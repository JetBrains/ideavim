/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.multiplecursors

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class VimMultipleCursorsExtensionJavaTest : VimJavaTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("multiple-cursors")
  }

  @Test
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

  @Test
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

  @Test
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

  @Test
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

  @Test
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

  @Test
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

    assertMode(Mode.NORMAL())
    assertState(afterEscape)

    typeText(injector.parser.parseKeys("I" + "@NotNull " + "<Esc>"))
    assertMode(Mode.NORMAL())
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

    assertMode(Mode.NORMAL())
    assertState(afterInsert)
  }
}