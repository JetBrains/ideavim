/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.commentary

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.ide.highlighter.JavaFileType
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.jetbrains.yaml.YAMLFileType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@Suppress("SpellCheckingInspection")
class CommentaryExtensionTest : VimJavaTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("commentary")
  }

  // |gc| |l| + move caret
  @Test
  fun testBlockCommentSingle() {
    doTest(
      "gcll",
      "<caret>if (condition) {\n" + "}\n",
      "/<caret>*i*/f (condition) {\n" + "}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  // |gc| |iw|
  @Test
  fun testBlockCommentInnerWord() {
    doTest(
      "gciw",
      "<caret>if (condition) {\n" + "}\n",
      "<caret>/*if*/ (condition) {\n" + "}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  // |gc| |iw|
  @Test
  fun testBlockCommentTillForward() {
    doTest(
      "gct{",
      "<caret>if (condition) {\n" + "}\n",
      "<caret>/*if (condition) */{\n" + "}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  // |gc| |ab|
  @Test
  fun testBlockCommentOuterParens() {
    doTest(
      "gcab",
      "if (<caret>condition) {\n" + "}\n",
      "if <caret>/*(condition)*/ {\n" + "}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  /*
   * NB: linewise motions become linewise comments;
   *  otherwise, they are incredibly difficult to undo
   */
// |gc| |j|
  @Test
  fun testLineCommentDown() {
    doTest(
      "gcj",
      "<caret>if (condition) {\n" + "}\n",
      "<caret>//if (condition) {\n" +
        "//}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
  fun testLineCommentDownPreservesAbsoluteCaretLocation() {
    doTest(
      "gcj",
      "if (<caret>condition) {\n" + "}\n",
      "//if<caret> (condition) {\n" +
        "//}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  // |gc| |ip|
  @Test
  fun testLineCommentInnerParagraph() {
    doTest(
      "gcip",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "//}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  // |gc| |ip|
  @Test
  fun testLineCommentSingleLineInnerParagraph() {
    doTest(
      "gcip",
      "${c}if (condition) {}",
      "//if (condition) {}",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  /* Ensure uncommenting works as well */ // |gc| |ip|
  @Test
  fun testLineUncommentInnerParagraph() {
    doTest(
      "gcip",
      "<caret>//if (condition) {\n" + "//}\n",
      "if (condition) {\n" +
        "}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  // |gc| |ip|
  @Test
  fun testLineUncommentSingleLineInnerParagraph() {
    doTest(
      "gcip",
      "$c//if (condition) {}",
      "if (condition) {}",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  /* Visual mode */ // |gc| |ip|
  @Test
  fun testLineCommentVisualInnerParagraph() {
    doTest(
      "vipgc",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "//}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  // |gc| |ip|
  @Test
  fun testLineUncommentVisualInnerParagraph() {
    doTest(
      "vipgc",
      "<caret>//if (condition) {\n" + "//}\n",
      "if (condition) {\n" +
        "}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  /* Special shortcut gcc is always linewise */ // |gcc|
  @Test
  fun testLineCommentShortcut() {
    doTest(
      "gccj",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" +
        "<caret>}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  // |gcc|
  @Test
  fun testLineCommentShortcutSetsCaretToMotionLocation() {
    doTest(
      "gcc",
      "if (<caret>condition) {\n" + "}\n",
      "<caret>//if (condition) {\n" + "}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  // |gcc|
  @Test
  fun testLineUncommentShortcut() {
    doTest(
      "gcc",
      "<caret>//if (condition) {\n" + "}\n",
      "<caret>if (condition) {\n" +
        "}\n",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  // |gcc|
  @Test
  fun testHTMLCommentShortcut() {
    doTest(
      "gcc",
      "<div />",
      "<!--<div />-->",
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @VimBehaviorDiffers(description = "IntelliJ's uncomment leaves the leading whitespace")
  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @VimBehaviorDiffers(description = "IntelliJ's uncomment leaves the leading whitespace")
  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test text object deletes single line comments separated by whitespace`() {
    doTest(
      "dgc",
      """
        // <caret> Comment 1
        
        // Comment 2
        final Int value = 42;
      """.trimIndent(),
      """
        
        // Comment 2
        final Int value = 42;
      """.trimIndent(),
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
        final Int value = 42;
  """,
  )
  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test text object does not delete line with comment and text`() {
    doTest(
      "dgc",
      """
        final Int <caret>value = 42; // Comment
      """.trimIndent(),
      """
        final Int value = 42; // Comment
      """.trimIndent(),
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
         
        /* Comment 1
         * Comment 2
         * Comment 3 */
        final Int value = 42;
      """.trimIndent(),
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test text object does not delete line with text and block comment`() {
    doTest(
      "dgc",
      """
        final Int value /* Block comment */ = 42;
      """.trimIndent(),
      """
        final Int value /* Block comment */ = 42;
      """.trimIndent(),
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test text object deletes JavaDoc comment from leading whitespace`() {
    doTest(
      "dgc",
      """
        /**<caret>
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
        
        /* Block comment */
        
        /**
         * Cool summary, dude
         * @param value the value, innit
         * @param name what's your name?
         */
        public void something(int value, String name) {
        }
      """.trimIndent(),
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }


  @Test
  fun `test text object deletes JavaDoc comment and adjoining comments separated by whitespace 2`() {
    doTest(
      "dgc",
      """
        // This should be deleted too
        
        /* <caret>Block comment */
        
        /**
         * Cool summary, dude
         * @param value the value, innit
         * @param name what's your name?
         */
        public void something(int value, String name) {
        }
      """.trimIndent(),
      """
        // This should be deleted too
        
        
        /**
         * Cool summary, dude
         * @param value the value, innit
         * @param name what's your name?
         */
        public void something(int value, String name) {
        }
      """.trimIndent(),
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test text object deletes JavaDoc comment and adjoining comments separated by whitespace 3`() {
    doTest(
      "dgc",
      """
        // This should be deleted too
        
        /* Block comment */
        
        /**<caret>
         * Cool summary, dude
         * @param value the value, innit
         * @param name what's your name?
         */
        public void something(int value, String name) {
        }
      """.trimIndent(),
      """
        // This should be deleted too
        
        /* Block comment */
        
        public void something(int value, String name) {
        }
      """.trimIndent(),
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test Commentary command comments visual range`() {
    doTest(
      "Vjj" + ":Commentary<CR>",
      """
        final int var ${c}value1 = 42;
        final int var value2 = 42;
        final int var value3 = 42;
      """.trimIndent(),
      """
        ${c}//final int var value1 = 42;
        //final int var value2 = 42;
        //final int var value3 = 42;
      """.trimIndent(),
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
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
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
  }

  @Test
  @Disabled("Doesn't work with the new version of IntelliJ and gradle plugin")
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
      Mode.NORMAL(),
      YAMLFileType.YML,
    )
  }
}
