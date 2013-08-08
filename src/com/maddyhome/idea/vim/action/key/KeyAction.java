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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 */
public class KeyAction extends AnAction {
  public void actionPerformed(@NotNull AnActionEvent event) {
    if (logger.isDebugEnabled()) {
      logger.debug("actionPerformed=" + event);
    }
    if (!VimPlugin.isEnabled()) {
      return;
    }

    if (event.getInputEvent() instanceof KeyEvent) {
      KeyEvent ke = (KeyEvent)event.getInputEvent();
      Editor editor = event.getData(PlatformDataKeys.EDITOR);
      if (editor != null) {
        KeyStroke key = KeyStroke.getKeyStrokeForEvent(ke);
        KeyHandler.getInstance().handleKey(editor, key, event.getDataContext());
      }
      else {
        if (ExEntryPanel.getInstance().isActive()) {
          KeyEvent e = new KeyEvent(ke.getComponent(), ke.getID(), ke.getWhen(), ke.getModifiers(),
                                    ke.getKeyCode(), ke.getKeyChar(), ke.getKeyLocation());
          ExEntryPanel.getInstance().processKey(e);
        }
      }
    }
  }

  protected void setShortcutSet(ShortcutSet shortcutSet) {
    if (logger.isDebugEnabled()) {
      logger.debug("setShortcutSet=" + shortcutSet);
    }
    super.setShortcutSet(shortcutSet);
  }

  private static Logger logger = Logger.getInstance(KeyAction.class.getName());
}
