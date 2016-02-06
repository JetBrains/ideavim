package org.jetbrains.plugins.ideavim.extension;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import com.maddyhome.idea.vim.group.ChangeGroup;
import com.maddyhome.idea.vim.key.OperatorFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.extension.VimExtensionFacade.*;
import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author dhleong
 */
public class PlugTest extends VimTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>More"), new Handler(), false);
    putKeyMapping(MappingMode.N, parseKeys("gm"), parseKeys("<Plug>More"), true);
  }

  public void testPlugMapping() {
    final String before =
      "int foo = bar<caret>index;";
    final String after =
      "int foo = bar;";

    //doTest(parseKeys("gme"), before, after);
  }

  public void testAmbiguousPlugMapping() {
    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>MoreMagic"), new Handler(), false);
    putKeyMapping(MappingMode.N, parseKeys("gn"), parseKeys("<Plug>MoreMagic"), true);

    final String before =
      "int foo = bar<caret>index;";
    final String after =
      "int foo = bar;";

    //doTest(parseKeys("gne"), before, after);
    doTest(parseKeys("gme"), before, after);
  }

  static class Handler implements VimExtensionHandler {
    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      setOperatorFunction(new Operator());
      executeNormal(parseKeys("g@"), editor);
    }

  }
  static class Operator implements OperatorFunction {
    @Override
    public boolean apply(@NotNull Editor editor, @NotNull DataContext context, @NotNull SelectionType selectionType) {
      TextRange range = VimPlugin.getMark().getChangeMarks(editor);
      if (range == null) return false;

      final ChangeGroup change = VimPlugin.getChange();
      change.deleteRange(editor, range, selectionType, true);
      return true;
    }
  }
}
