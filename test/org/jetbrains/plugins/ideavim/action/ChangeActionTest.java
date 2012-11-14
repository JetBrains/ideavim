package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
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
