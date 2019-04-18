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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.extension.surround;

import com.maddyhome.idea.vim.command.CommandState;
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
      "if <caret>(condition) {\n" +
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

  public void testSurroundTag() {
    configureByText("Hello <caret>World!\n");
    typeText(parseKeys("ysiw\\<em>"));
    myFixture.checkResult("Hello <em>World</em>!\n");
  }

  // VIM-1569
  public void testSurroundTagWithAttributes() {
    configureByText("Hello <caret>World!");
    typeText(parseKeys("ysiw\\<span class=\"important\" data-foo=\"bar\">"));
    myFixture.checkResult("Hello <span class=\"important\" data-foo=\"bar\">World</span>!");
  }

  // VIM-1569
  public void testSurraungTagAsInIssue(){
    configureByText("<p><caret>Hello</p>");
    typeText(parseKeys("VS<div class = \"container\">"));
    myFixture.checkResult("<div class = \"container\"><p>Hello</p></div>");
  }

  /* visual surround */

  public void testVisualSurroundWordParens() {
    final String before =
      "if <caret>condition {\n" +
      "}\n";
    final String after =
      "if <caret>(condition) {\n" +
      "}\n";

    doTest(parseKeys("veSb"), before, after);
    assertMode(CommandState.Mode.COMMAND);
    doTest(parseKeys("veS)"), before, after);
    assertMode(CommandState.Mode.COMMAND);
    doTest(parseKeys("veS("), before,
           "if ( condition ) {\n" +
           "}\n");
    assertMode(CommandState.Mode.COMMAND);
  }

  /* Delete surroundings */

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

  public void testDeleteSurroundingQuote() {
    final String before =
      "if (\"<caret>foo\".equals(foo)) {\n" +
      "}\n";
    final String after =
      "if (<caret>foo.equals(foo)) {\n" +
      "}\n";

    doTest(parseKeys("ds\""), before, after);
  }

  public void testDeleteSurroundingBlock() {
    final String before =
      "if (condition) {<caret>return;}\n";
    final String after =
      "if (condition) return;\n";

    doTest(parseKeys("dsB"), before, after);
    doTest(parseKeys("ds}"), before, after);
    doTest(parseKeys("ds{"), before, after);
  }

  public void testDeleteSurroundingArray() {
    final String before =
      "int foo = bar[<caret>index];";
    final String after =
      "int foo = barindex;";

    doTest(parseKeys("dsr"), before, after);
    doTest(parseKeys("ds]"), before, after);
    doTest(parseKeys("ds["), before, after);
  }

  public void testDeleteSurroundingAngle() {
    final String before =
      "foo = new Bar<<caret>Baz>();";
    final String after =
      "foo = new BarBaz();";

    doTest(parseKeys("dsa"), before, after);
    doTest(parseKeys("ds>"), before, after);
    doTest(parseKeys("ds<"), before, after);
  }

  public void testDeleteSurroundingTag() {
     final String before =
      "<div><p><caret>Foo</p></div>";
    final String after =
      "<div><caret>Foo</div>";

    doTest(parseKeys("dst"), before, after);
  }

  // VIM-1085
  public void testDeleteSurroundingParamsAtLineEnd() {
    final String before =
      "Foo\n" +
      "Seq(\"-<caret>Yrangepos\")\n";
    final String after =
      "Foo\n" +
      "Seq\"-Yrangepos\"\n";

    doTest(parseKeys("dsb"), before, after);
  }

  // VIM-1085
  public void testDeleteMultiLineSurroundingParamsAtLineEnd() {
    final String before =
      "Foo\n" +
      "Bar\n" +
      "Seq(\"-<caret>Yrangepos\",\n" +
      "    other)\n" +
      "Baz\n";
    final String after =
      "Foo\n" +
      "Bar\n" +
      "Seq\"-Yrangepos\",\n" +
      "    other\n" +
      "Baz\n";

    doTest(parseKeys("dsb"), before, after);
  }


  // TODO if/when we add proper repeat support
  //public void testRepeatDeleteSurroundParens() {
  //  final String before =
  //    "if ((<caret>condition)) {\n" +
  //    "}\n";
  //  final String after =
  //    "if condition {\n" +
  //    "}\n";
  //
  //  doTest(parseKeys("dsb."), before, after);
  //}

  /* Change surroundings */

  public void testChangeSurroundingParens() {
    final String before =
      "if (<caret>condition) {\n" +
      "}\n";
    final String after =
      "if [condition] {\n" +
      "}\n";

    doTest(parseKeys("csbr"), before, after);
  }

  public void testChangeSurroundingBlock() {
    final String before =
      "if (condition) {<caret>return;}";
    final String after =
      "if (condition) (return;)";

    doTest(parseKeys("csBb"), before, after);
  }

  public void testChangeSurroundingTagSimple() {
    final String before =
      "<div><p><caret>Foo</p></div>";
    final String after =
      "<div><caret>(Foo)</div>";

    doTest(parseKeys("cstb"), before, after);
  }

  public void testChangeSurroundingTagAnotherTag() {
    final String before =
      "<div><p><caret>Foo</p></div>";
    final String after =
      "<div><caret><b>Foo</b></div>";

    doTest(parseKeys("cst\\<b>"), before, after);
  }

  // TODO if/when we add proper repeat support
  //public void testRepeatChangeSurroundingParens() {
  //  final String before =
  //    "foo(<caret>index)(index2) = bar;";
  //  final String after =
  //    "foo[index][index2] = bar;";
  //
  //  doTest(parseKeys("csbrE."), before, after);
  //}
}
