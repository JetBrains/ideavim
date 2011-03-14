package com.maddyhome.idea.vim.handler.key;

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

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;

import javax.swing.*;

/**
 *
 */
public class EditorKeyHandler extends EditorActionHandler {
  //public EditorKeyHandler(EditorActionHandler origHandler, KeyStroke stroke)
  //{
  //    this(origHandler, stroke, false);
  //}

  public EditorKeyHandler(EditorActionHandler origHandler, KeyStroke stroke, boolean special) {
    this.origHandler = origHandler;
    this.stroke = stroke;
    this.special = special;
  }

  public void execute(Editor editor, DataContext context) {
    logger.debug("execute");
    //if (isEnabled(editor, context))

    // Do not launch vim actions in case of lookup enabled
    boolean isEnabled = editor != null && VimPlugin.isEnabled();
    if (isEnabled) {
      final Lookup lookup = LookupManager.getActiveLookup(editor);
      if (lookup != null && lookup.isCompletion()) {
        isEnabled = false;
      }
    }
    if (isEnabled) {
      handle(editor, context);
    }
    else {
      original(editor, context);
    }
  }

  protected void original(Editor editor, DataContext context) {

    if (logger.isDebugEnabled()) {
      logger.debug("original for " + stroke);
      logger.debug("original is " + origHandler.getClass().getName());
      logger.debug("editor viewer=" + editor.isViewer());
      logger.debug("project=" + PlatformDataKeys.PROJECT.getData(context)); // API change - don't merge
    }
    origHandler.execute(editor, context);
  }

  protected void handle(Editor editor, DataContext context) {
    KeyHandler.getInstance().handleKey(editor, stroke, context);
  }

  public boolean isEnabled(Editor editor, DataContext dataContext) {
    boolean res = true;
    if (editor == null || !VimPlugin.isEnabled()
        // Enable correct enter handler processing in Rename, Watches etc
        || (special && PlatformDataKeys.VIRTUAL_FILE.getData(dataContext) == null)) {
      logger.debug("no editor or disabled");
      res = origHandler.isEnabled(editor, dataContext);
    }
    else if (PlatformDataKeys.VIRTUAL_FILE.getData(dataContext) == null) // API change - don't merge
    {
      logger.debug("no file");
      if (!special) {
        logger.debug("not special");
        res = origHandler.isEnabled(editor, dataContext);
      }
      else {
        if (!CommandState.inInsertMode(editor)) {
          logger.debug("not insert or replace");
          res = origHandler.isEnabled(editor, dataContext);
        }
      }
    }

    if (logger.isDebugEnabled()) logger.debug("res=" + res);
    return res;
  }

  protected EditorActionHandler origHandler;
  protected KeyStroke stroke;
  protected boolean special;

  private static Logger logger = Logger.getInstance(EditorKeyHandler.class.getName());
}
