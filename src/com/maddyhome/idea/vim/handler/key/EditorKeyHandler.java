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

package com.maddyhome.idea.vim.handler.key;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 *
 */
public class EditorKeyHandler extends EditorActionHandler {

  public EditorKeyHandler(@NotNull EditorActionHandler origHandler, @NotNull KeyStroke stroke, boolean special) {
    this.origHandler = origHandler;
    this.stroke = stroke;
    this.special = special;
  }

  public void execute(@NotNull Editor editor, @NotNull DataContext context) {
    logger.debug("execute");

    // Do not launch vim actions in case of lookup enabled, except <Esc>
    boolean isEnabled = VimPlugin.isEnabled();
    if (isEnabled) {
      final Lookup lookup = LookupManager.getActiveLookup(editor);
      if (lookup != null && stroke.getKeyCode() != KeyEvent.VK_ESCAPE) {
        isEnabled = false;
      }
    }
    if (isEnabled) {
      handle(InjectedLanguageUtil.getTopLevelEditor(editor), context);
    }
    else {
      original(editor, context);
    }
  }

  protected void original(@NotNull Editor editor, @NotNull DataContext context) {

    if (logger.isDebugEnabled()) {
      logger.debug("original for " + stroke);
      logger.debug("original is " + origHandler.getClass().getName());
      logger.debug("editor viewer=" + editor.isViewer());
      logger.debug("project=" + PlatformDataKeys.PROJECT.getData(context)); // API change - don't merge
    }
    origHandler.execute(editor, context);
  }

  protected void handle(@NotNull Editor editor, @NotNull DataContext context) {
    KeyHandler.getInstance().handleKey(editor, stroke, context);
  }

  public boolean isEnabled(@Nullable Editor editor, @NotNull DataContext dataContext) {
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

  @NotNull protected EditorActionHandler origHandler;
  @NotNull protected KeyStroke stroke;
  protected boolean special;

  private static Logger logger = Logger.getInstance(EditorKeyHandler.class.getName());
}
