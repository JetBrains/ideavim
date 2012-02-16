package com.maddyhome.idea.vim.action;

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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;

import javax.swing.*;

public class PassThruDelegateEditorAction extends AbstractDelegateEditorAction {
  public PassThruDelegateEditorAction(KeyStroke stroke, EditorActionHandler origHandler) {
    super(new MyHandler(stroke, origHandler));
  }

  private static Logger logger = Logger.getInstance(PassThruDelegateEditorAction.class.getName());

  private static class MyHandler extends EditorActionHandler {
    public MyHandler(KeyStroke stroke, EditorActionHandler handler) {
      this.stroke = stroke;
      this.origHandler = handler;
    }

    public void execute(Editor editor, DataContext dataContext) {
      if (logger.isDebugEnabled()) {
        logger.debug("actionPerformed key=" + stroke);
      }
      if (editor == null || !VimPlugin.isEnabled()) {
        origHandler.execute(editor, dataContext);
      }
      else {
        if (logger.isDebugEnabled()) {
          logger.debug("event = KeyEvent: " + stroke);
        }
        KeyHandler.getInstance().handleKey(InjectedLanguageUtil.getTopLevelEditor(editor), stroke, dataContext);
      }
    }

    private KeyStroke stroke;
    private EditorActionHandler origHandler;
  }
}