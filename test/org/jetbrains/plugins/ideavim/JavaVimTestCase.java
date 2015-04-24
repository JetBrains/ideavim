package org.jetbrains.plugins.ideavim;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * NB: We need to extend from JavaCodeInsightFixtureTestCase so we
 *  can create PsiFiles with proper Java Language type
 * @author dhleong
 */
public class JavaVimTestCase extends JavaCodeInsightFixtureTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    KeyHandler.getInstance().fullReset(myFixture.getEditor());
    Options.getInstance().resetAllOptions();
    VimPlugin.getKey().resetKeyMappings();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    ExEntryPanel.getInstance().deactivate(false);
  }

  @NotNull
  protected Editor configureByJavaText(@NotNull String content) {
    myFixture.configureByText(JavaFileType.INSTANCE, content);
    return myFixture.getEditor();
  }

  public void doTest(final List<KeyStroke> keys, String before, String after) {

    configureByJavaText(before);
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
