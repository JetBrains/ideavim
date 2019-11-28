package org.jetbrains.plugins.ideavim.extension.commentary

import com.intellij.ide.highlighter.HtmlFileType
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper
import org.jetbrains.plugins.ideavim.JavaVimTestCase

/**
 * @author dhleong
 */
class CommentaryExtensionTest : JavaVimTestCase() {
  override fun setUp() {
    super.setUp()
    enableExtensions("commentary")
  }

  // |gc| |l|
  fun testBlockCommentSingle() {
    doTest(StringHelper.parseKeys("gcll"),
      "<caret>if (condition) {\n" + "}\n",
      "/<caret>*i*/f (condition) {\n" + "}\n")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gc| |iw|
  fun testBlockCommentInnerWord() {
    doTest(StringHelper.parseKeys("gciw"),
      "<caret>if (condition) {\n" + "}\n",
      "<caret>/*if*/ (condition) {\n" + "}\n")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gc| |iw|
  fun testBlockCommentTillForward() {
    doTest(StringHelper.parseKeys("gct{"),
      "<caret>if (condition) {\n" + "}\n",
      "<caret>/*if (condition) */{\n" + "}\n")
  }

  // |gc| |ab|
  fun testBlockCommentOuterParens() {
    doTest(StringHelper.parseKeys("gcab"),
      "if (<caret>condition) {\n" + "}\n",
      "if <caret>/*(condition)*/ {\n" + "}\n")
  }

  /*
   * NB: linewise motions become linewise comments;
   *  otherwise, they are incredibly difficult to undo
   */
// |gc| |j|
  fun testLineCommentDown() {
    doTest(StringHelper.parseKeys("gcj"),
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "//}\n")
  }

  // |gc| |ip|
  fun testLineCommentInnerParagraph() {
    doTest(StringHelper.parseKeys("gcip"),
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "//}\n")
  }

  // |gc| |ip|
  fun testLineCommentSingleLineInnerParagraph() {
    doTest(StringHelper.parseKeys("gcip"),
      "<caret>if (condition) {}",
      "//if (condition) {}")
  }

  /* Ensure uncommenting works as well */ // |gc| |ip|
  fun testLineUncommentInnerParagraph() {
    doTest(StringHelper.parseKeys("gcip"),
      "<caret>//if (condition) {\n" + "//}\n",
      "if (condition) {\n" +
        "}\n")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gc| |ip|
  fun testLineUncommentSingleLineInnerParagraph() {
    doTest(StringHelper.parseKeys("gcip"),
      "<caret>//if (condition) {}",
      "if (condition) {}")
  }

  /* Visual mode */ // |gc| |ip|
  fun testLineCommentVisualInnerParagraph() {
    doTest(StringHelper.parseKeys("vipgc"),
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "//}\n")
  }

  // |gc| |ip|
  fun testLineUncommentVisualInnerParagraph() {
    doTest(StringHelper.parseKeys("vipgc"),
      "<caret>//if (condition) {\n" + "//}\n",
      "if (condition) {\n" +
        "}\n")
  }

  /* Special shortcut gcc is always linewise */ // |gcc|
  fun testLineCommentShortcut() {
    doTest(StringHelper.parseKeys("gccj"),
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "<caret>}\n")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gcc|
  fun testLineCommentShortcutPreservesCaret() {
    doTest(StringHelper.parseKeys("gcc"),
      "if (<caret>condition) {\n" + "}\n",
      "//if (<caret>condition) {\n" + "}\n")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gcc|
  fun testLineUncommentShortcut() {
    doTest(StringHelper.parseKeys("gcc"),
      "<caret>//if (condition) {\n" + "}\n",
      "<caret>if (condition) {\n" +
        "}\n")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gcc|
  fun testHTMLCommentShortcut() {
    myFixture.configureByText(HtmlFileType.INSTANCE, "<div />")
    typeText(StringHelper.parseKeys("gcc"))
    myFixture.checkResult("<!--<div />-->")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun `test comment motion repeat`() {
    doTest(StringHelper.parseKeys("gcj", "jj."),
          """
                 <caret>if (condition) {
                 }
                 if (condition) {
                 }
                 """.trimIndent(),
          """
                //if (condition) {
                //}
                //if (condition) {
                //}
                """.trimIndent())
  }

  fun `test comment motion right repeat`() {
    doTest(StringHelper.parseKeys("gciw", "jj."),
      """
                <caret>if (condition) {
                }
                if (condition) {
                }
                """.trimIndent(),
      """
                /*if*/ (condition) {
                }
                /*if*/ (condition) {
                }
                """.trimIndent())
  }

  fun `test comment line repeat`() {
    doTest(StringHelper.parseKeys("gcc", "j."),
      """
                 <caret>if (condition) {
                 }
                 """.trimIndent(),
      """
                //if (condition) {
                //}
                """.trimIndent())
  }
}
