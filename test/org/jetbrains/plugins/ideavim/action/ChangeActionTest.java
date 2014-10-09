package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import org.jetbrains.plugins.ideavim.VimTestCase;

import javax.swing.*;
import java.util.List;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;
import static com.maddyhome.idea.vim.helper.StringHelper.stringToKeys;

/**
 * @author vlan
 */
public class ChangeActionTest extends VimTestCase {
  // |c| |t|
  public void testChangeLinesTillForwards() {
    doTest(parseKeys("ct(", "for "),
           "<caret>if (condition) {\n" +
           "}\n",
           "for (condition) {\n" +
           "}\n");
  }

  // VIM-276 |c| |T|
  public void testChangeLinesTillBackwards() {
    doTest(parseKeys("cT("),
           "if (condition) {<caret>\n" +
           "}\n",
           "if (\n" +
           "}\n");
  }

  // VIM-276 |c| |F|
  public void testChangeLinesToBackwards() {
    doTest(parseKeys("cFc"),
           "if (condition) {<caret>\n" +
           "}\n",
           "if (\n" +
           "}\n");
  }

  // VIM-311 |i_CTRL-O|
  public void testInsertSingleCommand() {
    doTest(parseKeys("i", "def", "<C-O>", "d2h", "x"),
           "abc<caret>.\n",
           "abcdx.\n");
  }

  // VIM-321 |d| |count|
  public void testDeleteEmptyRange() {
    doTest(parseKeys("d0"), "<caret>hello\n", "hello\n");
  }

  // VIM-112 |i| |i_CTRL-W|
  public void testInsertDeletePreviousWord() {
    typeTextInFile(parseKeys("i", "one two three", "<C-W>"),
                   "hello\n" +
                   "<caret>\n");
    myFixture.checkResult("hello\n" + "one two \n");
  }

  // VIM-157 |~|
  public void testToggleCharCase() {
    doTest(parseKeys("~~"),
           "<caret>hello world\n",
           "HEllo world\n");
  }

  // VIM-157 |~|
  public void testToggleCharCaseLineEnd() {
    doTest(parseKeys("~~"),
           "hello wor<caret>ld\n",
           "hello worLD\n");
  }

  // VIM-85 |i| |gi| |gg|
  public void testInsertAtPreviousAction() {
    doTest(parseKeys("i", "hello", "<Esc>", "gg", "gi", " world! "), "one\n" +
                                                                     "two <caret>three\n" +
                                                                     "four\n", "one\n" +
                                                                               "two hello world! three\n" +
                                                                               "four\n");
  }

  // VIM-312 |d| |w|
  public void testDeleteLastWordInFile() {
    doTest(parseKeys("dw"),
           "one\n" +
           "<caret>two\n",
           "one\n" +
           "\n");
    assertOffset(4);
  }

  // |d| |w|
  public void testDeleteLastWordBeforeEOL() {
    doTest(parseKeys("dw"),
           "one <caret>two\n" +
           "three\n",
           "one \n" +
           "three\n");
  }

  // VIM-105 |d| |w|
  public void testDeleteLastWordBeforeEOLs() {
    doTest(parseKeys("dw"), "one <caret>two\n" +
                            "\n" +
                            "three\n", "one \n" +
                                       "\n" +
                                       "three\n");
  }

  // VIM-105 |d| |w|
  public void testDeleteLastWordBeforeEOLAndWhitespace() {
    doTest(parseKeys("dw"),
           "one <caret>two\n" +
           " three\n",
           "one \n" +
           " three\n");
    assertOffset(3);
  }

  // VIM-105 |d| |w| |count|
  public void testDeleteTwoWordsOnTwoLines() {
    doTest(parseKeys("d2w"),
           "one <caret>two\n" +
           "three four\n",
           "one four\n");
  }

  // VIM-200 |c| |w|
  public void testChangeWordAtLastChar() {
    doTest(parseKeys("cw"),
           "on<caret>e two three\n",
           "on two three\n");
  }

  // VIM-300 |c| |w|
  public void testChangeWordTwoWordsWithoutWhitespace() {
    doTest(parseKeys("cw"), "<caret>$value\n", "value\n");
  }

  // VIM-296 |cc|
  public void testChangeLineAtLastLine() {
    doTest(parseKeys("cc"),
           "foo\n" +
           "<caret>bar\n",
           "foo\n" +
           "\n");
    assertOffset(4);
  }

  // VIM-394 |d| |v_aw|
  public void testDeleteIndentedWordBeforePunctuation() {
    doTest(parseKeys("daw"),
           "foo\n" +
           "  <caret>bar, baz\n",
           "foo\n" +
           "  , baz\n");
  }

  // |d| |v_aw|
  public void testDeleteLastWordAfterPunctuation() {
    doTest(parseKeys("daw"), "foo(<caret>bar\n" + "baz\n", "foo(\n" + "baz\n");
  }

  // VIM-244 |d| |l|
  public void testDeleteLastCharInLine() {
    doTest(parseKeys("dl"),
           "fo<caret>o\n" +
           "bar\n",
           "fo\n" +
           "bar\n");
    assertOffset(1);
  }

  // VIM-393 |d|
  public void testDeleteBadArgument() {
    doTest(parseKeys("dD", "dd"),
           "one\n" +
           "two\n",
           "two\n");
  }

  // VIM-262 |i_CTRL-R|
  public void testInsertFromRegister() {
    VimPlugin.getRegister().setKeys('a', stringToKeys("World"));
    doTest(parseKeys("A", ", ", "<C-R>", "a", "!"),
           "<caret>Hello\n",
           "Hello, World!\n");
  }

  // VIM-421 |c| |w|
  public void testChangeLastWordInLine() {
    doTest(parseKeys("cw"),
           "ab.<caret>cd\n",
           "ab.<caret>\n");
  }

  // VIM-421 |c| |iw|
  public void testChangeLastInnerWordInLine() {
    doTest(parseKeys("c", "iw", "baz"),
           "foo bar bo<caret>o\n",
           "foo bar baz\n");
  }

  // VIM-421 |c| |w|
  public void testChangeLastCharInLine() {
    doTest(parseKeys("cw"),
           "fo<caret>o\n",
           "fo<caret>\n");
  }

  // VIM-404 |O|
  public void testInsertNewLineAboveFirstLine() {
    doTest(parseKeys("O", "bar"),
           "fo<caret>o\n",
           "bar\nfoo\n");
  }

  // VIM-472 |v|
  public void testVisualSelectionRightMargin() {
    doTest(parseKeys("v", "k$d"),
           "foo\n<caret>bar\n",
           "fooar\n");
  }

  // VIM-569 |a| |i_CTRL-W|
  public void testDeletePreviousWordDotEOL() {
    doTest(parseKeys("a", "<C-W>"),
           "this is a sentence<caret>.\n",
           "this is a sentence<caret>\n");
  }

  // VIM-569 |a| |i_CTRL-W|
  public void testDeletePreviousWordLastAfterWhitespace() {
    doTest(parseKeys("A", "<C-W>"),
           "<caret>this is a sentence\n",
           "this is a <caret>\n");
  }

  // VIM-513 |A| |i_CTRL-W|
  public void testDeletePreviousWordEOL() {
    doTest(parseKeys("A", "<C-W>"),
           "<caret>$variable\n",
           "$<caret>\n");
  }

  // VIM-632 |CTRL-V| |v_b_I|
  public void testChangeVisualBlock() {
    doTest(parseKeys("<C-V>", "j", "I", "quux ", "<Esc>"),
           "foo bar\n" +
           "<caret>baz quux\n" +
           "spam eggs\n",
           "foo bar\n" +
           "<caret>quux baz quux\n" +
           "quux spam eggs\n");
  }

  // VIM-632 |CTRL-V| |v_d|
  public void testDeleteVisualBlock() {
    doTest(parseKeys("<C-V>", "jjl", "d"),
           "<caret>foo\n" +
           "bar\n" +
           "baz\n" +
           "quux\n",
           "<caret>oo\n" +
           "ar\n" +
           "az\n" +
           "quux\n");
  }

  private void doTest(final List<KeyStroke> keys, String before, String after) {
    myFixture.configureByText("a.java", before);
    final Editor editor = myFixture.getEditor();
    final KeyHandler keyHandler = KeyHandler.getInstance();
    final EditorDataContext dataContext = new EditorDataContext(editor);
    final Project project = myFixture.getProject();
    RunnableHelper.runWriteCommand(project, new Runnable() {
      @Override
      public void run() {
        for (KeyStroke key : keys) {
          keyHandler.handleKey(editor, key, dataContext);
        }
      }
    }, null, null);
    myFixture.checkResult(after);
  }
}
