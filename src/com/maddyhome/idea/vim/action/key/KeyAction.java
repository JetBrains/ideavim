/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.action.key;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.helper.EditorData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class KeyAction extends AnAction implements DumbAware {
  // TODO: Always handle these keys if emulation is enabled and they aren't used in a special context
  public static KeyStroke[] VIM_ONLY_EDITOR_KEYS = new KeyStroke[] {
    KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
    KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
    KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),  // +C
    KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0),
    KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),      // +C
    KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),          // +C +S
    KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),        // +C +S
    KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),        // +C +S +CS
    KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),       // +C +S +CS
    KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0),        // +C +S +CS
    KeyStroke.getKeyStroke(KeyEvent.VK_END, 0),         // +C +S +CS
    KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0),     // +C +S +CS
    KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0),   // +C +S +CS
  };

  public void actionPerformed(@NotNull AnActionEvent e) {
    final Editor editor = getEditor(e);
    final KeyStroke keyStroke = getKeyStroke(e);
    if (editor != null && keyStroke != null) {
      // Should we use InjectedLanguageUtil.getTopLevelEditor(editor) here, as we did in former EditorKeyHandler?
      KeyHandler.getInstance().handleKey(editor, keyStroke, e.getDataContext());
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(isEnabled(e));
  }

  private boolean isEnabled(@NotNull AnActionEvent e) {
    if (VimPlugin.isEnabled()) {
      final Editor editor = getEditor(e);
      final KeyStroke keyStroke = getKeyStroke(e);
      if (editor != null && keyStroke != null) {
        final int keyCode = keyStroke.getKeyCode();
        if (LookupManager.getActiveLookup(editor) != null) {
          return keyCode == KeyEvent.VK_ESCAPE;
        }
        // Debug watch, Python console, etc.
        else if (!EditorData.isFileEditor(editor) && CommandState.inInsertMode(editor)) {
          return keyCode != KeyEvent.VK_ENTER && keyCode != KeyEvent.VK_ESCAPE && keyCode != KeyEvent.VK_TAB;
        }
        return true;
      }
    }
    return false;
  }

  @Nullable
  private KeyStroke getKeyStroke(@NotNull AnActionEvent e) {
    final InputEvent inputEvent = e.getInputEvent();
    if (inputEvent instanceof KeyEvent) {
      final KeyEvent keyEvent = (KeyEvent)inputEvent;
      return KeyStroke.getKeyStrokeForEvent(keyEvent);
    }
    return null;
  }

  @Nullable
  private Editor getEditor(@NotNull AnActionEvent e) {
    return e.getData(PlatformDataKeys.EDITOR);
  }
}
