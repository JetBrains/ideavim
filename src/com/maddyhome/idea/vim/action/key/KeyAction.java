package com.maddyhome.idea.vim.action.key;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2006 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.ui.ExEntryPanel;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 */
public class KeyAction extends AnAction {
  public void actionPerformed(AnActionEvent event) {
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
