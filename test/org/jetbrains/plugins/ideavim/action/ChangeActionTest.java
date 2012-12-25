package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import org.jetbrains.plugins.ideavim.VimTestCase;

import javax.swing.*;
import java.util.List;

import static com.maddyhome.idea.vim.helper.StringHelper.stringToKeys;

/**
 * @author vlan
 */
public class ChangeActionTest extends VimTestCase {
  // |c| |t|
  public void testChangeLinesTillForwards() {
    doTest(stringToKeys("ct(for "),
           "<caret>if (condition) {\n" +
           "}\n",
           "for (condition) {\n" +
           "}\n");
  }

  // VIM-276 |c| |T|
  public void testChangeLinesTillBackwards() {
    doTest(stringToKeys("cT("),
           "if (condition) {<caret>\n" +
           "}\n",
           "if (\n" +
           "}\n");
  }

  // VIM-276 |c| |F|
  public void testChangeLinesToBackwards() {
    doTest(stringToKeys("cFc"),
           "if (condition) {<caret>\n" +
           "}\n",
           "if (\n" +
           "}\n");
  }

  // VIM-311 |i_CTRL-O|
  public void testInsertSingleCommand() {
    final List<KeyStroke> keys = stringToKeys("idef");
    keys.add(KeyStroke.getKeyStroke("control O"));
    keys.addAll(stringToKeys("d2hx"));
    doTest(keys,
           "abc<caret>.\n",
           "abcdx.\n");
  }

  // VIM-321 |d| |count|
  public void testDeleteEmptyRange() {
    doTest(stringToKeys("d0"),
           "<caret>hello\n",
           "hello\n");
  }

  // VIM-112 |i| |i_CTRL-W|
  public void testInsertDeletePreviousWord() {
    final List<KeyStroke> keys = stringToKeys("ione two three");
    keys.add(KeyStroke.getKeyStroke("control W"));
    typeTextInFile(keys,
                   "hello\n" +
                   "<caret>\n");
    myFixture.checkResult("hello\n" +
                          "one two \n");
  }

  // VIM-157 |~|
  public void testToggleCharCase() {
    doTest(stringToKeys("~~"),
           "<caret>hello world\n",
           "HEllo world\n");
  }

  // VIM-157 |~|
  public void testToggleCharCaseLineEnd() {
    doTest(stringToKeys("~~"),
           "hello wor<caret>ld\n",
           "hello worLD\n");
  }

  // VIM-85 |i| |gi| |gg|
  public void testInsertAtPreviousAction() {
    final List<KeyStroke> keys = stringToKeys("ihello");
    keys.add(KeyStroke.getKeyStroke("ESCAPE"));
    keys.addAll(stringToKeys("gggi world! "));
    doTest(keys,
           "one\n" +
           "two <caret>three\n" +
           "four\n",
           "one\n" +
           "two hello world! three\n" +
           "four\n");
  }

  // VIM-312 |d| |w|
  public void testDeleteLastWordInFile() {
    doTest(stringToKeys("dw"),
           "one\n" +
           "<caret>two\n",
           "one\n" +
           "\n");
    assertOffset(4);
  }

  // |d| |w|
  public void testDeleteLastWordBeforeEOL() {
    doTest(stringToKeys("dw"),
           "one <caret>two\n" +
           "three\n",
           "one \n" +
           "three\n");
  }

  // VIM-105 |d| |w|
  public void testDeleteLastWordBeforeEOLs() {
    doTest(stringToKeys("dw"),
           "one <caret>two\n" +
           "\n" +
           "three\n",
           "one \n" +
           "\n" +
           "three\n");
  }

  // VIM-105 |d| |w|
  public void testDeleteLastWordBeforeEOLAndWhitespace() {
    doTest(stringToKeys("dw"),
           "one <caret>two\n" +
           " three\n",
           "one \n" +
           " three\n");
    assertOffset(3);
  }

  // VIM-105 |d| |w| |count|
  public void testDeleteTwoWordsOnTwoLines() {
    doTest(stringToKeys("d2w"),
           "one <caret>two\n" +
           "three four\n",
           "one four\n");
  }

  // VIM-200 |c| |w|
  public void testChangeWordAtLastChar() {
    doTest(stringToKeys("cw"),
           "on<caret>e two three\n",
           "on two three\n");
  }

  // VIM-300 |c| |w|
  public void testChangeWordTwoWordsWithoutWhitespace() {
    doTest(stringToKeys("cw"),
           "<caret>$value\n",
           "value\n");
  }

  // VIM-296 |cc|
  public void testChangeLineAtLastLine() {
    doTest(stringToKeys("cc"),
           "foo\n" +
           "<caret>bar\n",
           "foo\n" +
           "\n");
    assertOffset(4);
  }

  // VIM-394 |d| |v_aw|
  public void testDeleteIndentedWordBeforePunctuation() {
    doTest(stringToKeys("daw"),
           "foo\n" +
           "  <caret>bar, baz\n",
           "foo\n" +
           "  , baz\n");
  }

  // |d| |v_aw|
  public void testDeleteLastWordAfterPunctuation() {
    doTest(stringToKeys("daw"),
           "foo(<caret>bar\n" +
           "baz\n",
           "foo(\n" +
           "baz\n");
  }

  // VIM-244 |d| |l|
  public void testDeleteLastCharInLine() {
    doTest(stringToKeys("dl"),
           "fo<caret>o\n" +
           "bar\n",
           "fo\n" +
           "bar\n");
    assertOffset(1);
  }

  // VIM-393 |d|
  public void testDeleteBadArgument() {
    doTest(stringToKeys("dDdd"),
           "one\n" +
           "two\n",
           "two\n");
  }

  // VIM-262 |i_CTRL-R|
  public void testInsertFromRegister() {
    CommandGroups.getInstance().getRegister().setKeys('a', stringToKeys("World"));
    final List<KeyStroke> keys = stringToKeys("A, ");
    keys.add(KeyStroke.getKeyStroke("control R"));
    keys.addAll(stringToKeys("a!"));
    doTest(keys,
           "<caret>Hello\n",
           "Hello, World!\n");
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
