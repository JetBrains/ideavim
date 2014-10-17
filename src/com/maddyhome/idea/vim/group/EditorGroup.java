package com.maddyhome.idea.vim.group;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.maddyhome.idea.vim.EventFacade;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.helper.DocumentManager;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author vlan
 */
public class EditorGroup {
  private static final boolean BLOCK_CURSOR_VIM_VALUE = true;
  private static final boolean ANIMATED_SCROLLING_VIM_VALUE = false;
  private static final boolean REFRAIN_FROM_SCROLLING_VIM_VALUE = true;

  private boolean isBlockCursor = false;
  private boolean isAnimatedScrolling = false;
  private boolean isRefrainFromScrolling = false;

  public EditorGroup() {
    EventFacade.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
      @Override
      public void editorCreated(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        isBlockCursor = editor.getSettings().isBlockCursor();
        isAnimatedScrolling = editor.getSettings().isAnimatedScrolling();
        isRefrainFromScrolling = editor.getSettings().isRefrainFromScrolling();
        EditorData.initializeEditor(editor);
        DocumentManager.getInstance().addListeners(editor.getDocument());
        VimPlugin.getKey().registerRequiredShortcutKeys(editor);

        if (VimPlugin.isEnabled()) {
          // Turn on insert mode if editor doesn't have any file
          if (!EditorData.isFileEditor(editor) && editor.getDocument().isWritable() &&
              !CommandState.inInsertMode(editor)) {
            KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke('i'), new EditorDataContext(editor));
          }
          editor.getSettings().setBlockCursor(!CommandState.inInsertMode(editor));
          editor.getSettings().setAnimatedScrolling(ANIMATED_SCROLLING_VIM_VALUE);
          editor.getSettings().setRefrainFromScrolling(REFRAIN_FROM_SCROLLING_VIM_VALUE);
        }
      }

      @Override
      public void editorReleased(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        EditorData.uninitializeEditor(editor);
        VimPlugin.getKey().unregisterShortcutKeys(editor);
        editor.getSettings().setAnimatedScrolling(isAnimatedScrolling);
        editor.getSettings().setRefrainFromScrolling(isRefrainFromScrolling);
        DocumentManager.getInstance().removeListeners(editor.getDocument());
      }
    }, ApplicationManager.getApplication());
  }

  public void turnOn() {
    setCursors(BLOCK_CURSOR_VIM_VALUE);
    setAnimatedScrolling(ANIMATED_SCROLLING_VIM_VALUE);
    setRefrainFromScrolling(REFRAIN_FROM_SCROLLING_VIM_VALUE);

  }

  public void turnOff() {
    setCursors(isBlockCursor);
    setAnimatedScrolling(isAnimatedScrolling);
    setRefrainFromScrolling(isRefrainFromScrolling);

  }

  private void setCursors(boolean isBlock) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      // Vim plugin should be turned on in insert mode
      ((EditorEx)editor).setInsertMode(true);
      editor.getSettings().setBlockCursor(isBlock);
    }
  }

  private void setAnimatedScrolling(boolean isOn) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      editor.getSettings().setAnimatedScrolling(isOn);
    }
  }

  private void setRefrainFromScrolling(boolean isOn) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      editor.getSettings().setRefrainFromScrolling(isOn);
    }
  }
}
