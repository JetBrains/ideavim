package org.jetbrains.plugins.ideavim.action.plugin.surround;

import com.maddyhome.idea.vim.action.plugin.surround.SurroundPlugin;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.option.ToggleOption;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author dhleong
 */
public class SurroundPluginTest extends VimTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    ToggleOption option = (ToggleOption) Options.getInstance()
      .getOption(SurroundPlugin.NAME);
    option.set();

  }

  public void testSurroundWordParens() {
    final String before =
      "if <caret>condition {\n" +
      "}\n";
    final String after =
      "if (condition) {\n" +
      "}\n";

    doTest(parseKeys("yseb"), before, after);
    doTest(parseKeys("yse)"), before, after);
    doTest(parseKeys("yse("), before,
           "if ( condition ) {\n" +
            "}\n");
  }

  public void testSurroundWORDBlock() {
    final String before =
      "if (condition) <caret>return;\n";
    final String after =
      "if (condition) {return;}\n";

    doTest(parseKeys("ysEB"), before, after);
    doTest(parseKeys("ysE}"), before, after);
    doTest(parseKeys("ysE{"), before,
           "if (condition) { return; }\n");
  }

  public void testSurroundWordArray() {
    final String before =
      "int foo = bar<caret>index;";
    final String after =
      "int foo = bar[index];";

    doTest(parseKeys("yser"), before, after);
    doTest(parseKeys("yse]"), before, after);
    doTest(parseKeys("yse["), before,
           "int foo = bar[ index ];");
  }

  public void testSurroundWordAngle() {
    final String before =
      "foo = new Bar<caret>Baz();";
    final String after =
      "foo = new Bar<Baz>();";

    doTest(parseKeys("ysea"), before, after);
    doTest(parseKeys("yse>"), before, after);
    doTest(parseKeys("yse<"), before,
           "foo = new Bar< Baz >();");
  }

  public void testDeleteSurroundingParens() {
    final String before =
      "if (<caret>condition) {\n" +
      "}\n";
    final String after =
      "if condition {\n" +
      "}\n";

    doTest(parseKeys("dsb"), before, after);
    doTest(parseKeys("ds("), before, after);
    doTest(parseKeys("ds)"), before, after);
  }
}
