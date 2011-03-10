package com.maddyhome.idea.vim.action;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.undo.UndoManager;

/**
 *
 */
public class TxActionWrapper extends AbstractDelegateAction {
  public TxActionWrapper() {
  }

  public TxActionWrapper(AnAction origAction) {
    super(origAction);
  }

  public void actionPerformed(AnActionEvent event) {
    logger.debug("actionPerformed");

    final Editor editor = event.getData(PlatformDataKeys.EDITOR); // API change - don't merge
    if (editor == null || !VimPlugin.isEnabled()) {
      getOrigAction().actionPerformed(event);
    }
    else {
      boolean doTx = !UndoManager.getInstance().inCommand(editor);
      logger.debug("doTx = " + doTx);
      if (doTx) {
        UndoManager.getInstance().endCommand(editor);
        UndoManager.getInstance().beginCommand(editor);
      }

      getOrigAction().actionPerformed(event);

      if (doTx) {
        UndoManager.getInstance().endCommand(editor);
        UndoManager.getInstance().beginCommand(editor);
      }
    }
  }

  private static Logger logger = Logger.getInstance(TxActionWrapper.class.getName());
}
