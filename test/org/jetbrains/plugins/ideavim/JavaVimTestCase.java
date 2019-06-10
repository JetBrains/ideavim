package org.jetbrains.plugins.ideavim;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import com.maddyhome.idea.vim.helper.TestInputModel;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.option.ToggleOption;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * NB: We need to extend from JavaCodeInsightFixtureTestCase so we
 *  can create PsiFiles with proper Java Language type
 * @author dhleong
 */
public abstract class JavaVimTestCase extends JavaCodeInsightFixtureTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    KeyHandler.getInstance().fullReset(myFixture.getEditor());
    Options.getInstance().resetAllOptions();
    VimPlugin.getKey().resetKeyMappings();
  }

  @Override
  protected void tearDown() throws Exception {
    ExEntryPanel.getInstance().deactivate(false);
    VimScriptGlobalEnvironment.getInstance().getVariables().clear();
    super.tearDown();
  }

  protected void enableExtensions(@NotNull String... extensionNames) {
    for (String name : extensionNames) {
      ToggleOption option = (ToggleOption)Options.getInstance().getOption(name);
      option.set();
    }
  }

  @NotNull
  protected Editor configureByJavaText(@NotNull String content) {
    myFixture.configureByText(JavaFileType.INSTANCE, content);
    return myFixture.getEditor();
  }

  public void doTest(final List<KeyStroke> keys, String before, String after) {
    configureByJavaText(before);
    typeText(keys);
    myFixture.checkResult(after);
  }

  @NotNull
  protected Editor typeText(@NotNull List<KeyStroke> keys) {
    final Editor editor = myFixture.getEditor();
    final KeyHandler keyHandler = KeyHandler.getInstance();
    final EditorDataContext dataContext = new EditorDataContext(editor);
    final Project project = myFixture.getProject();
    TestInputModel.getInstance(editor).setKeyStrokes(keys);
    RunnableHelper.runWriteCommand(project, new Runnable() {
      @Override
      public void run() {
        final TestInputModel inputModel = TestInputModel.getInstance(editor);
        for (KeyStroke key = inputModel.nextKeyStroke(); key != null; key = inputModel.nextKeyStroke()) {
          final ExEntryPanel exEntryPanel = ExEntryPanel.getInstance();
          if (exEntryPanel.isActive()) {
            exEntryPanel.handleKey(key);
          }
          else {
            keyHandler.handleKey(editor, key, dataContext);
          }
        }
      }
    }, null, null);
    return editor;
  }

  public void assertMode(@NotNull CommandState.Mode expectedMode) {
    final CommandState.Mode mode = CommandState.getInstance(myFixture.getEditor()).getMode();
    assertEquals(expectedMode, mode);
  }

  public void assertSelection(@Nullable String expected) {
    final String selected = myFixture.getEditor().getSelectionModel().getSelectedText();
    assertEquals(expected, selected);
  }

}
