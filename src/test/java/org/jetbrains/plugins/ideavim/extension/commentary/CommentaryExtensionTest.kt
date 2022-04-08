/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.extension.commentary

import com.intellij.ide.highlighter.HtmlFileType
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.JavaVimTestCase
import org.jetbrains.plugins.ideavim.VimTestCase.Companion.c

class CommentaryExtensionTest : JavaVimTestCase() {
  override fun setUp() {
    super.setUp()
    enableExtensions("commentary")
  }

  // |gc| |l| + move caret
  fun testBlockCommentSingle() {
    doTest(
      parseKeys("gcll"),
      "<caret>if (condition) {\n" + "}\n",
      "/<caret>*i*/f (condition) {\n" + "}\n"
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gc| |iw|
  fun testBlockCommentInnerWord() {
    doTest(
      parseKeys("gciw"),
      "<caret>if (condition) {\n" + "}\n",
      "<caret>/*if*/ (condition) {\n" + "}\n"
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gc| |iw|
  fun testBlockCommentTillForward() {
    doTest(
      parseKeys("gct{"),
      "<caret>if (condition) {\n" + "}\n",
      "<caret>/*if (condition) */{\n" + "}\n"
    )
  }

  // |gc| |ab|
  fun testBlockCommentOuterParens() {
    doTest(
      parseKeys("gcab"),
      "if (<caret>condition) {\n" + "}\n",
      "if <caret>/*(condition)*/ {\n" + "}\n"
    )
  }

  /*
   * NB: linewise motions become linewise comments;
   *  otherwise, they are incredibly difficult to undo
   */
// |gc| |j|
  fun testLineCommentDown() {
    doTest(
      parseKeys("gcj"),
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "//}\n"
    )
  }

  // |gc| |ip|
  fun testLineCommentInnerParagraph() {
    doTest(
      parseKeys("gcip"),
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "//}\n"
    )
  }

  // |gc| |ip|
  fun testLineCommentSingleLineInnerParagraph() {
    doTest(
      parseKeys("gcip"),
      "${c}if (condition) {}",
      "//if (condition) {}"
    )
  }

  /* Ensure uncommenting works as well */ // |gc| |ip|
  fun testLineUncommentInnerParagraph() {
    doTest(
      parseKeys("gcip"),
      "<caret>//if (condition) {\n" + "//}\n",
      "if (condition) {\n" +
        "}\n"
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gc| |ip|
  fun testLineUncommentSingleLineInnerParagraph() {
    doTest(
      parseKeys("gcip"),
      "$c//if (condition) {}",
      "if (condition) {}"
    )
  }

  /* Visual mode */ // |gc| |ip|
  fun testLineCommentVisualInnerParagraph() {
    doTest(
      parseKeys("vipgc"),
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "//}\n"
    )
  }

  // |gc| |ip|
  fun testLineUncommentVisualInnerParagraph() {
    doTest(
      parseKeys("vipgc"),
      "<caret>//if (condition) {\n" + "//}\n",
      "if (condition) {\n" +
        "}\n"
    )
  }

  /* Special shortcut gcc is always linewise */ // |gcc|
  fun testLineCommentShortcut() {
    doTest(
      parseKeys("gccj"),
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "<caret>}\n"
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gcc|
  fun testLineCommentShortcutPreservesCaret() {
    doTest(
      parseKeys("gcc"),
      "if (<caret>condition) {\n" + "}\n",
      "//if (<caret>condition) {\n" + "}\n"
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gcc|
  fun testLineUncommentShortcut() {
    doTest(
      parseKeys("gcc"),
      "<caret>//if (condition) {\n" + "}\n",
      "<caret>if (condition) {\n" +
        "}\n"
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gcc|
  fun testHTMLCommentShortcut() {
    myFixture.configureByText(HtmlFileType.INSTANCE, "<div />")
    typeText(parseKeys("gcc"))
    myFixture.checkResult("<!--<div />-->")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun `test comment motion repeat`() {
    doTest(
      parseKeys("gcj", "jj."),
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
      """.trimIndent()
    )
  }

  fun `test comment motion right repeat`() {
    doTest(
      parseKeys("gciw", "jj."),
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
      """.trimIndent()
    )
  }

  fun `test comment line repeat`() {
    doTest(
      parseKeys("gcc", "j."),
      """
                 <caret>if (condition) {
                 }
      """.trimIndent(),
      """
                //if (condition) {
                //}
      """.trimIndent()
    )
  }

  @VimBehaviorDiffers(description = "IntelliJ's uncomment leaves the leading whitespace")
  fun `test uncomment with gcgc`() {
    doTest(
      parseKeys("gcgc"),
      """
        // final Int value1 = 42;
        // final Int value2 = 42;
        // final Int value3 = 42;
        final Int <caret>value4 = 42;
      """.trimIndent(),
      """
         final Int value1 = 42;
         final Int value2 = 42;
         final Int value3 = 42;
        final Int value4 = 42;
      """.trimIndent()
    )
  }

  @VimBehaviorDiffers(description = "IntelliJ's uncomment leaves the leading whitespace")
  fun `test uncomment with gcu`() {
    doTest(
      parseKeys("gcu"),
      """
        // final Int value1 = 42;
        // final Int value2 = 42;
        // final Int value3 = 42;
        final Int <caret>value4 = 42;
      """.trimIndent(),
      """
         final Int value1 = 42;
         final Int value2 = 42;
         final Int value3 = 42;
        final Int value4 = 42;
      """.trimIndent()
    )
  }

  fun `test comment line with count`() {
    doTest(
      parseKeys("4gcc"),
      """
        final Int value1 = 42;
        final Int <caret>value2 = 42;
        final Int value3 = 42;
        final Int value4 = 42;
        final Int value5 = 42;
        final Int value6 = 42;
      """.trimIndent(),
      """
        final Int value1 = 42;
        //final Int value2 = 42;
        //final Int value3 = 42;
        //final Int value4 = 42;
        //final Int value5 = 42;
        final Int value6 = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes single line comment`() {
    doTest(
      parseKeys("dgc"),
      """
        // <caret>Comment 1
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes multiple line comments`() {
    doTest(
      parseKeys("dgc"),
      """
        // <caret>Comment 1
        // Comment 2
        // Comment 3
        // Comment 4
        // Comment 5
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes multiple line comments 2`() {
    doTest(
      parseKeys("dgc"),
      """
        // Comment 1
        // Comment 2
        // <caret>Comment 3
        // Comment 4
        // Comment 5
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes single line comment from leading whitespace`() {
    doTest(
      parseKeys("dgc"),
      """
        <caret> // Comment 1
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes single line comment from leading whitespace 2`() {
    doTest(
      parseKeys("dgc"),
      """
        <caret>
        
        // Comment 1
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes single line comment from leading whitespace 3`() {
    doTest(
      parseKeys("dgc"),
      """
        final Int value1 = 42;
        <caret>
        
        // Comment 1
        final Int value2 = 42;
      """.trimIndent(),
      """
        final Int value1 = 42;
        final Int value2 = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes single line comment from trailing whitespace`() {
    doTest(
      parseKeys("dgc"),
      """
        
        // Comment 1
        <caret>
        
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes single line comments separated by whitespace`() {
    doTest(
      parseKeys("dgc"),
      """
        // <caret> Comment 1
        
        // Comment 2
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes disjointed single line comments from whitespace`() {
    doTest(
      parseKeys("dgc"),
      """
        // Comment 1
        <caret>
        // Comment 2
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes single line comment from current line`() {
    doTest(
      parseKeys("dgc"),
      """
        // Comment
        final Int <caret>value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes single line comment from current line 2`() {
    doTest(
      parseKeys("dgc"),
      """
        // Comment
        final Int <caret>value = 42;
        final Int value2 = 42;
      """.trimIndent(),
      """
        final Int value = 42;
        final Int value2 = 42;
      """.trimIndent()
    )
  }

  fun `test text object does not delete line with comment and text`() {
    doTest(
      parseKeys("dgc"),
      """
        final Int <caret>value = 42; // Comment
      """.trimIndent(),
      """
        final Int value = 42; // Comment
      """.trimIndent()
    )
  }

  fun `test text object deletes block comment`() {
    doTest(
      parseKeys("dgc"),
      """
        /* <caret>Comment 1 */
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes multi-line block comment`() {
    doTest(
      parseKeys("dgc"),
      """
        /* Comment 1
         * <caret>Comment 2
         * Comment 3 */
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes adjoining multi-line block comments`() {
    doTest(
      parseKeys("dgc"),
      """
        /* Comment 1
         * Comment 2
         * Comment 3 */
        /* Comment 1
         * <caret>Comment 2
         * Comment 3 */
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes adjoining multi-line block comments 2`() {
    doTest(
      parseKeys("dgc"),
      """
        /* Comment 1
         * <caret>Comment 2
         * Comment 3 */
         
        /* Comment 1
         * Comment 2
         * Comment 3 */
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent()
    )
  }

  fun `test text object does not delete line with text and block comment`() {
    doTest(
      parseKeys("dgc"),
      """
        final Int value /* Block comment */ = 42;
      """.trimIndent(),
      """
        final Int value /* Block comment */ = 42;
      """.trimIndent()
    )
  }

  fun `test text object deletes JavaDoc comment`() {
    doTest(
      parseKeys("dgc"),
      """
        /**
         * <caret>Cool summary, dude
         * @param value the value, innit
         * @param name what's your name?
         */
        public void something(int value, String name) {
        }
      """.trimIndent(),
      """
        public void something(int value, String name) {
        }
      """.trimIndent()
    )
  }

  fun `test text object deletes JavaDoc comment from leading whitespace`() {
    doTest(
      parseKeys("dgc"),
      """
        <caret>
        /**
         * Cool summary, dude
         * @param value the value, innit
         * @param name what's your name?
         */
        public void something(int value, String name) {
        }
      """.trimIndent(),
      """
        public void something(int value, String name) {
        }
      """.trimIndent()
    )
  }

  fun `test text object deletes JavaDoc comment and adjoining comments`() {
    doTest(
      parseKeys("dgc"),
      """
        // <caret>This should be deleted too
        /**
         * Cool summary, dude
         * @param value the value, innit
         * @param name what's your name?
         */
        public void something(int value, String name) {
        }
      """.trimIndent(),
      """
        public void something(int value, String name) {
        }
      """.trimIndent()
    )
  }

  fun `test text object deletes JavaDoc comment and adjoining comments separated by whitespace`() {
    doTest(
      parseKeys("dgc"),
      """
        // <caret>This should be deleted too
        
        /* Block comment */
        
        /**
         * Cool summary, dude
         * @param value the value, innit
         * @param name what's your name?
         */
        public void something(int value, String name) {
        }
      """.trimIndent(),
      """
        public void something(int value, String name) {
        }
      """.trimIndent()
    )
  }
}
