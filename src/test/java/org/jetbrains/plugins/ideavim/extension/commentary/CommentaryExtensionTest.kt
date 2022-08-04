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
import com.intellij.ide.highlighter.JavaFileType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.yaml.YAMLFileType

@Suppress("SpellCheckingInspection")
class CommentaryExtensionTest : VimTestCase() {
  override fun setUp() {
    super.setUp()
    enableExtensions("commentary")
  }

  // |gc| |l| + move caret
  fun testBlockCommentSingle() {
    doTest(
      "gcll",
      "<caret>if (condition) {\n" + "}\n",
      "/<caret>*i*/f (condition) {\n" + "}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
    assertSelection(null)
  }

  // |gc| |iw|
  fun testBlockCommentInnerWord() {
    doTest(
      "gciw",
      "<caret>if (condition) {\n" + "}\n",
      "<caret>/*if*/ (condition) {\n" + "}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
    assertSelection(null)
  }

  // |gc| |iw|
  fun testBlockCommentTillForward() {
    doTest(
      "gct{",
      "<caret>if (condition) {\n" + "}\n",
      "<caret>/*if (condition) */{\n" + "}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  // |gc| |ab|
  fun testBlockCommentOuterParens() {
    doTest(
      "gcab",
      "if (<caret>condition) {\n" + "}\n",
      "if <caret>/*(condition)*/ {\n" + "}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  /*
   * NB: linewise motions become linewise comments;
   *  otherwise, they are incredibly difficult to undo
   */
// |gc| |j|
  fun testLineCommentDown() {
    doTest(
      "gcj",
      "<caret>if (condition) {\n" + "}\n",
      "<caret>//if (condition) {\n" +
        "//}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun testLineCommentDownPreservesAbsoluteCaretLocation() {
    doTest(
      "gcj",
      "if (<caret>condition) {\n" + "}\n",
      "//if<caret> (condition) {\n" +
        "//}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  // |gc| |ip|
  fun testLineCommentInnerParagraph() {
    doTest(
      "gcip",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "//}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  // |gc| |ip|
  fun testLineCommentSingleLineInnerParagraph() {
    doTest(
      "gcip",
      "${c}if (condition) {}",
      "//if (condition) {}",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  /* Ensure uncommenting works as well */ // |gc| |ip|
  fun testLineUncommentInnerParagraph() {
    doTest(
      "gcip",
      "<caret>//if (condition) {\n" + "//}\n",
      "if (condition) {\n" +
        "}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
    assertSelection(null)
  }

  // |gc| |ip|
  fun testLineUncommentSingleLineInnerParagraph() {
    doTest(
      "gcip",
      "$c//if (condition) {}",
      "if (condition) {}",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  /* Visual mode */ // |gc| |ip|
  fun testLineCommentVisualInnerParagraph() {
    doTest(
      "vipgc",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "//}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  // |gc| |ip|
  fun testLineUncommentVisualInnerParagraph() {
    doTest(
      "vipgc",
      "<caret>//if (condition) {\n" + "//}\n",
      "if (condition) {\n" +
        "}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  /* Special shortcut gcc is always linewise */ // |gcc|
  fun testLineCommentShortcut() {
    doTest(
      "gccj",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "<caret>}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
    assertSelection(null)
  }

  // |gcc|
  fun testLineCommentShortcutSetsCaretToMotionLocation() {
    doTest(
      "gcc",
      "if (<caret>condition) {\n" + "}\n",
      "<caret>//if (condition) {\n" + "}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
    assertSelection(null)
  }

  // |gcc|
  fun testLineUncommentShortcut() {
    doTest(
      "gcc",
      "<caret>//if (condition) {\n" + "}\n",
      "<caret>if (condition) {\n" +
        "}\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
    assertSelection(null)
  }

  // |gcc|
  fun testHTMLCommentShortcut() {
    doTest(
      "gcc",
      "<div />",
      "<!--<div />-->",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
      HtmlFileType.INSTANCE
    )
    assertSelection(null)
  }

  fun `test comment motion repeat`() {
    doTest(
      "gcj" + "jj.",
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
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test comment motion right repeat`() {
    doTest(
      "gciw" + "jj.",
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
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test comment line repeat`() {
    doTest(
      "gcc" + "j.",
      """
                 <caret>if (condition) {
                 }
      """.trimIndent(),
      """
                //if (condition) {
                //}
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  @VimBehaviorDiffers(description = "IntelliJ's uncomment leaves the leading whitespace")
  fun `test uncomment with gcgc`() {
    doTest(
      "gcgc",
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
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  @VimBehaviorDiffers(description = "IntelliJ's uncomment leaves the leading whitespace")
  fun `test uncomment with gcu`() {
    doTest(
      "gcu",
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
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test comment line with count`() {
    // Caret position is kept as the position *before* the commenting. This is how Vim works
    doTest(
      "4gcc",
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
        //final In<caret>t value2 = 42;
        //final Int value3 = 42;
        //final Int value4 = 42;
        //final Int value5 = 42;
        final Int value6 = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes single line comment`() {
    doTest(
      "dgc",
      """
        // <caret>Comment 1
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes multiple line comments`() {
    doTest(
      "dgc",
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
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes multiple line comments 2`() {
    doTest(
      "dgc",
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
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes single line comment from leading whitespace`() {
    doTest(
      "dgc",
      """
        <caret> // Comment 1
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes single line comment from leading whitespace 2`() {
    doTest(
      "dgc",
      """
        <caret>
        
        // Comment 1
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes single line comment from leading whitespace 3`() {
    doTest(
      "dgc",
      """
        final Int value1 = 42;
        <caret>
        
        // Comment 1
        final Int value2 = 42;
      """.trimIndent(),
      """
        final Int value1 = 42;
        final Int value2 = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes single line comment from trailing whitespace`() {
    doTest(
      "dgc",
      """
        
        // Comment 1
        <caret>
        
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes single line comments separated by whitespace`() {
    doTest(
      "dgc",
      """
        // <caret> Comment 1
        
        // Comment 2
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes disjointed single line comments from whitespace`() {
    doTest(
      "dgc",
      """
        // Comment 1
        <caret>
        // Comment 2
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
        final Int value = 42;
  """
  )
  fun `test text object deletes single line comment from current line`() {
    doTest(
      "dgc",
      """
        // Comment
        final Int <caret>value = 42;
      """.trimIndent(),
      """
        
        final Int value = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes single line comment from current line 2`() {
    doTest(
      "dgc",
      """
        // Comment
        final Int <caret>value = 42;
        final Int value2 = 42;
      """.trimIndent(),
      """
        final Int value = 42;
        final Int value2 = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object does not delete line with comment and text`() {
    doTest(
      "dgc",
      """
        final Int <caret>value = 42; // Comment
      """.trimIndent(),
      """
        final Int value = 42; // Comment
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes block comment`() {
    doTest(
      "dgc",
      """
        /* <caret>Comment 1 */
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes multi-line block comment`() {
    doTest(
      "dgc",
      """
        /* Comment 1
         * <caret>Comment 2
         * Comment 3 */
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes adjoining multi-line block comments`() {
    doTest(
      "dgc",
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
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes adjoining multi-line block comments 2`() {
    doTest(
      "dgc",
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
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object does not delete line with text and block comment`() {
    doTest(
      "dgc",
      """
        final Int value /* Block comment */ = 42;
      """.trimIndent(),
      """
        final Int value /* Block comment */ = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes JavaDoc comment`() {
    doTest(
      "dgc",
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
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes JavaDoc comment from leading whitespace`() {
    doTest(
      "dgc",
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
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes JavaDoc comment and adjoining comments`() {
    doTest(
      "dgc",
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
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test text object deletes JavaDoc comment and adjoining comments separated by whitespace`() {
    doTest(
      "dgc",
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
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test Commentary command comments current line`() {
    doTest(
      ":Commentary<CR>",
      """
        final int var value1 = 42;
        final int var <caret>value2 = 42;
        final int var value3 = 42;
      """.trimIndent(),
      """
        final int var value1 = 42;
        //final int var <caret>value2 = 42;
        final int var value3 = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test Commentary command comments simple line range`() {
    doTest(
      ":2Commentary<CR>",
      """
        final int var <caret>value1 = 42;
        final int var value2 = 42;
        final int var value3 = 42;
      """.trimIndent(),
      """
        final int var <caret>value1 = 42;
        //final int var value2 = 42;
        final int var value3 = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test Commentary command comments line range`() {
    doTest(
      ":1,3Commentary<CR>",
      """
        final int var <caret>value1 = 42;
        final int var value2 = 42;
        final int var value3 = 42;
      """.trimIndent(),
      """
        //final int var <caret>value1 = 42;
        //final int var value2 = 42;
        //final int var value3 = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  @VimBehaviorDiffers(
    """
        <caret>//final int var value1 = 42;
        //final int var value2 = 42;
        //final int var value3 = 42;
  """,
    description = "Vim exits Visual mode before entering Command mode, and resets the caret to the start of the visual selection." +
      "When executing the Commentary command, we don't move the caret, so it should be end up at the start of the visual selection." +
      "Note that Escape exits Visual mode, but leaves the caret where it is",
    shouldBeFixed = true
  )
  fun `test Commentary command comments visual range`() {
    doTest(
      "Vjj" + ":Commentary<CR>",
      """
        final int var <caret>value1 = 42;
        final int var value2 = 42;
        final int var value3 = 42;
      """.trimIndent(),
      """
        //final int var value1 = 42;
        //final int var value2 = 42;
        //final int var <caret>value3 = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test Commentary command comments search range`() {
    doTest(
      ":g/value2/Commentary<CR>",
      """
        final int var <caret>value1 = 42;
        final int var value2 = 42;
        final int var value3 = 42;
        final int var value21 = 42;
        final int var value22 = 42;
      """.trimIndent(),
      """
        final int var value1 = 42;
        //final int var value2 = 42;
        final int var value3 = 42;
        //final int var value21 = 42;
        <caret>//final int var value22 = 42;
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, JavaFileType.INSTANCE
    )
  }

  fun `test block comment falls back to line comment when not available`() {
    doTest(
      "gcw",
      """
      american:
      - <caret>Boston Red Sox
      - Detroit Tigers
      - New York Yankees
      national:
      - New York Mets
      - Chicago Cubs
      - Atlanta Braves
      """.trimIndent(),
      """
      american:
      #- Boston Red Sox
      - Detroit Tigers
      - New York Yankees
      national:
      - New York Mets
      - Chicago Cubs
      - Atlanta Braves
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE, YAMLFileType.YML
    )
  }
}
