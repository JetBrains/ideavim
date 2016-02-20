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

    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>More"), new OperatorHandler(), false);
    putKeyMapping(MappingMode.N, parseKeys("gm"), parseKeys("<Plug>More"), true);
  }

  public void testPlugMapping() {
    final String before =
      "int foo = bar<caret>index;";
    final String after =
      "int foo = bar;";

    doTest(parseKeys("gme"), before, after);
  }

  public void testAmbiguousPlugMapping() {
    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>MoreMagic"), new OperatorHandler(), false);
    putKeyMapping(MappingMode.N, parseKeys("gn"), parseKeys("<Plug>MoreMagic"), true);

    final String before =
      "int foo = bar<caret>index;";
    final String after =
      "int foo = bar;";

    doTest(parseKeys("gne"), before, after);
    doTest(parseKeys("gme"), before, after);
  }

  public void testAmbiguousKeyToPlugMapping() {
    // this is a fairly common pattern of mappings;
    //  you have a basic map that accepts an operator---
    //  gm, in this case---then another mapping that
    //  repeats the last character to operate on the
    //  whole line.
    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>Line"), new LineHandler(), false);
    putKeyMapping(MappingMode.N, parseKeys("gmm"), parseKeys("<Plug>Line"), true);

    final String before =
      "int foo = bar<caret>index;";
    final String after =
      "int foo = bar;";

    doTest(parseKeys("gmm"), before, "");
    doTest(parseKeys("gme"), before, after);
  }

  public void testAmbiguousMapping() {
    // direct extension mappings also need special care;
    putExtensionHandlerMapping(MappingMode.N, parseKeys("gn"), new OperatorHandler(), false);
    putExtensionHandlerMapping(MappingMode.N, parseKeys("gnn"), new LineHandler(), false);

    final String before =
      "int foo = bar<caret>index;";
    final String after =
      "int foo = bar;";

    doTest(parseKeys("gnn"), before, "");
    doTest(parseKeys("gne"), before, after);
  }

  static class OperatorHandler implements VimExtensionHandler {
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

  static class LineHandler implements VimExtensionHandler {
    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
       final ChangeGroup change = VimPlugin.getChange();
       change.deleteLine(editor, 1);
    }
  }
}
