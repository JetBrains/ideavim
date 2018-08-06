package org.jetbrains.plugins.ideavim.extension.multiplecursors

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class VimMultipleCursorsExtensionTest : VimTestCase() {

  override fun setUp() {
    super.setUp()
    enableExtensions("multiple-cursors")
  }

  fun testPrototype() {
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
}
