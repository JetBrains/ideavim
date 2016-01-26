package org.jetbrains.plugins.ideavim.extension.surround;

import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author dhleong
 */
public class VimSurroundExtensionTest extends VimTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    enableExtensions("surround");
  }

  /* surround */

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
  }

  public void testSurroundQuotes() {
    final String before =
      "foo = <caret>new Bar.Baz;";
    final String after =
      "foo = \"new Bar.Baz\";";

    doTest(parseKeys("yst;\""), before, after);
    doTest(parseKeys("ys4w\""), before, after);
  }
}
