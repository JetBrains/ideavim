package org.jetbrains.plugins.ideavim.ex;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import org.jetbrains.plugins.ideavim.VimTestCase;

/**
 * @author vlan
 */
public class SubstituteCommandTest extends VimTestCase {
  public void testOneLetter() {
    doTest("s/a/b/",
           "a<caret>baba\n" +
           "ab\n",
           "bbaba\n" +
           "ab\n");
  }

  public void testOneLetterMultiPerLine() {
    doTest("s/a/b/g",
           "a<caret>baba\n" +
           "ab\n",
           "bbbbb\n" +
           "ab\n");
  }

  public void testOneLetterMultiPerLineWholeFile() {
    doTest("%s/a/b/g",
           "a<caret>baba\n" +
           "ab\n",
           "bbbbb\n" +
           "bb\n");
  }

  // VIM-146
  public void testEOLtoQuote() {
    doTest("s/$/'/g",
           "<caret>one\n" +
           "two\n",
           "one'\n" +
           "two\n");
  }

  public void testSOLtoQuote() {
    doTest("s/^/'/g",
           "<caret>one\n" +
           "two\n",
           "'one\n" +
           "two\n");
  }

  public void testEmptyToQuote() {
    doTest("s//'/g",
           "<caret>one\n" +
           "two\n",
           "'one\n" +
           "two\n");
  }

  public void testDotToNul() {
    doTest("s/\\./\\n/g",
           "<caret>one.two.three\n",
           "one\u0000two\u0000three\n");
  }

  public void testToNL() {
    doTest("s/\\./\\r/g",
           "<caret>one.two.three\n",
           "one\ntwo\nthree\n");
  }

  // VIM-289
  public void testDotToNLDot() {
    doTest("s/\\./\\r\\./g",
           "<caret>one.two.three\n",
           "one\n.two\n.three\n");
  }

  private void doTest(final String command, String before, String after) {
    myFixture.configureByText("a.java", before);
    final Editor editor = myFixture.getEditor();
    final EditorDataContext dataContext = new EditorDataContext(editor);
    final Project project = myFixture.getProject();
    final CommandParser commandParser = CommandParser.getInstance();
    RunnableHelper.runWriteCommand(project, new Runnable() {
      @Override
      public void run() {
        try {
          commandParser.processCommand(editor, dataContext, command, 1);
        }
        catch (ExException e) {
          throw new RuntimeException(e);
        }
      }
    }, null, null);
    myFixture.checkResult(after);
  }
}
