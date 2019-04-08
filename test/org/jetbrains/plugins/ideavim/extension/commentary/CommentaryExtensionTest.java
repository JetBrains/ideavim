package org.jetbrains.plugins.ideavim.extension.commentary;

import com.intellij.ide.highlighter.HtmlFileType;
import org.jetbrains.plugins.ideavim.JavaVimTestCase;

import static com.maddyhome.idea.vim.command.CommandState.Mode.COMMAND;
import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author dhleong
 */
public class CommentaryExtensionTest extends JavaVimTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    enableExtensions("commentary");
  }

  // |gc| |l|
  public void testBlockCommentSingle() {
    doTest(parseKeys("gcll"),
           "<caret>if (condition) {\n" + "}\n",
           "/<caret>*i*/f (condition) {\n" + "}\n");
    assertMode(COMMAND);
    assertSelection(null);
  }


  // |gc| |iw|
  public void testBlockCommentInnerWord() {
    doTest(parseKeys("gciw"),
           "<caret>if (condition) {\n" + "}\n",
           "<caret>/*if*/ (condition) {\n" + "}\n");
    assertMode(COMMAND);
    assertSelection(null);
  }

  // |gc| |iw|
  public void testBlockCommentTillForward() {
    doTest(parseKeys("gct{"),
           "<caret>if (condition) {\n" + "}\n",
           "<caret>/*if (condition) */{\n" + "}\n");
  }

  // |gc| |ab|
  public void testBlockCommentOuterParens() {
    doTest(parseKeys("gcab"),
           "if (<caret>condition) {\n" + "}\n",
           "if <caret>/*(condition)*/ {\n" + "}\n");
  }

  /*
   * NB: linewise motions become linewise comments;
   *  otherwise, they are incredibly difficult to undo
   */

  // |gc| |j|
  public void testLineCommentDown() {
    doTest(parseKeys("gcj"),
           "<caret>if (condition) {\n" + "}\n",
           "//if (condition) {\n" +
           "//}\n");
  }

  // |gc| |ip|
  public void testLineCommentInnerParagraph() {
    doTest(parseKeys("gcip"),
           "<caret>if (condition) {\n" + "}\n",
           "//if (condition) {\n" +
           "//}\n");
  }

  // |gc| |ip|
  public void testLineCommentSingleLineInnerParagraph() {
    doTest(parseKeys("gcip"),
           "<caret>if (condition) {}",
           "//if (condition) {}");
  }

  /* Ensure uncommenting works as well */

  // |gc| |ip|
  public void testLineUncommentInnerParagraph() {
    doTest(parseKeys("gcip"),
           "<caret>//if (condition) {\n" + "//}\n",
           "if (condition) {\n" +
           "}\n");
    assertMode(COMMAND);
    assertSelection(null);
  }

  // |gc| |ip|
  public void testLineUncommentSingleLineInnerParagraph() {
    doTest(parseKeys("gcip"),
           "<caret>//if (condition) {}",
           "if (condition) {}");
  }

  /* Visual mode */

  // |gc| |ip|
  public void testLineCommentVisualInnerParagraph() {
    doTest(parseKeys("vipgc"),
           "<caret>if (condition) {\n" + "}\n",
           "//if (condition) {\n" +
           "//}\n");
  }

  // |gc| |ip|
  public void testLineUncommentVisualInnerParagraph() {
    doTest(parseKeys("vipgc"),
           "<caret>//if (condition) {\n" + "//}\n",
           "if (condition) {\n" +
           "}\n");
  }




  /* Special shortcut gcc is always linewise */

  // |gcc|
  public void testLineCommentShortcut() {
    doTest(parseKeys("gccj"),
           "<caret>if (condition) {\n" + "}\n",
           "//if (condition) {\n" +
           "<caret>}\n");
    assertMode(COMMAND);
    assertSelection(null);
  }

  // |gcc|
  public void testLineCommentShortcutPreservesCaret() {
    doTest(parseKeys("gcc"),
           "if (<caret>condition) {\n" + "}\n",
           "//if (<caret>condition) {\n" + "}\n");
    assertMode(COMMAND);
    assertSelection(null);
  }

  // |gcc|
  public void testLineUncommentShortcut() {
    doTest(parseKeys("gcc"),
           "<caret>//if (condition) {\n" + "}\n",
           "<caret>if (condition) {\n" +
           "}\n");
    assertMode(COMMAND);
    assertSelection(null);
  }

  // |gcc|
  public void testHTMLCommentShortcut() {
    myFixture.configureByText(HtmlFileType.INSTANCE, "<div />");
    typeText(parseKeys("gcc"));
    myFixture.checkResult("<!--<div />-->");
    assertMode(COMMAND);
    assertSelection(null);
  }
}
