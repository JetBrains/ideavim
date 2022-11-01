/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.VimStateMachine;
import com.maddyhome.idea.vim.group.visual.VimVisualTimer;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import com.maddyhome.idea.vim.helper.TestInputModel;
import com.maddyhome.idea.vim.newapi.IjExecutionContext;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.options.OptionScope;
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * NB: We need to extend from JavaCodeInsightFixtureTestCase so we
 * can create PsiFiles with proper Java Language type
 *
 * @author dhleong
 */
public abstract class JavaVimTestCase extends JavaCodeInsightFixtureTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    Editor editor = myFixture.getEditor();
    if (editor != null) {
      KeyHandler.getInstance().fullReset(new IjVimEditor(editor));
    }
    VimPlugin.getOptionService().resetAllOptions();
    VimPlugin.getKey().resetKeyMappings();
    VimPlugin.clearError();
  }

  @Override
  protected void tearDown() throws Exception {
    ExEntryPanel.getInstance().deactivate(false);
    VimPlugin.getVariableService().clear();
    Timer swingTimer = VimVisualTimer.INSTANCE.getSwingTimer();
    if (swingTimer != null) {
      swingTimer.stop();
    }
    super.tearDown();
  }

  protected void enableExtensions(@NotNull String... extensionNames) {
    for (String name : extensionNames) {
      VimPlugin.getOptionService().setOption(OptionScope.GLOBAL.INSTANCE, name, name);
    }
  }

  public void doTest(final List<KeyStroke> keys, String before, String after) {
    //noinspection IdeaVimAssertState
    myFixture.configureByText(JavaFileType.INSTANCE, before);
    typeText(keys);
    //noinspection IdeaVimAssertState
    myFixture.checkResult(after);
  }

  @NotNull
  protected Editor typeText(@NotNull List<KeyStroke> keys) {
    final Editor editor = myFixture.getEditor();
    final KeyHandler keyHandler = KeyHandler.getInstance();
    final EditorDataContext dataContext = EditorDataContext.init(editor, null);
    final Project project = myFixture.getProject();
    TestInputModel.getInstance(editor).setKeyStrokes(keys);
    RunnableHelper.runWriteCommand(project, () -> {
      final TestInputModel inputModel = TestInputModel.getInstance(editor);
      for (KeyStroke key = inputModel.nextKeyStroke(); key != null; key = inputModel.nextKeyStroke()) {
        final ExEntryPanel exEntryPanel = ExEntryPanel.getInstance();
        if (exEntryPanel.isActive()) {
          exEntryPanel.handleKey(key);
        }
        else {
          keyHandler.handleKey(new IjVimEditor(editor), key, new IjExecutionContext(dataContext));
        }
      }
    }, null, null);
    return editor;
  }

  public void assertMode(@NotNull VimStateMachine.Mode expectedMode) {
    final VimStateMachine.Mode mode = VimStateMachine.getInstance(new IjVimEditor(myFixture.getEditor())).getMode();
    assertEquals(expectedMode, mode);
  }

  public void assertSelection(@Nullable String expected) {
    final String selected = myFixture.getEditor().getSelectionModel().getSelectedText();
    assertEquals(expected, selected);
  }

}
