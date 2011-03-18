package com.maddyhome.idea.vim.undo;

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
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.helper.DocumentManager;
import com.maddyhome.idea.vim.option.NumberOption;
import com.maddyhome.idea.vim.option.Options;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class EditorUndoList {
  public EditorUndoList(Editor editor) {
    beginCommand(editor);
  }

  /**
   * A document is restorable only if it hasn't been saved since changes were made.
   */
  public void documentSaved() {
    restorable = pointer == 0;
  }

  public boolean inCommand() {
    return currentCommand != null;
  }

  public void beginCommand(Editor editor) {
    logger.info("beginCommand");
    if (inCommand()) {
      endCommand();
    }
    currentCommand = new UndoCommand(editor);
  }

  public void abortCommand() {
    logger.info("abortCommand");
    currentCommand = null;
  }

  public void endCommand() {
    logger.info("endCommand");
    if (currentCommand != null && currentCommand.size() > 0) {
      logger.debug("ended");
      int max = getMaxUndos();
      if (max == 0) {
        undos.clear();
        undos.add(currentCommand);
      }
      else {
        while (pointer < undos.size()) {
          undos.remove(pointer);
        }

        undos.add(currentCommand);

        if (undos.size() > max) {
          undos.remove(0);
        }
      }

      currentCommand.complete();

      pointer = undos.size();

      if (logger.isDebugEnabled()) {
        logger.debug("this=" + this);
      }
    }

    currentCommand = null;
  }

  public int size() {
    return currentCommand == null ? 0 : currentCommand.size();
  }

  public void addChange(DocumentChange change) {
    logger.info("addChange");
    if (!inUndo && currentCommand != null) {
      logger.info("added");
      currentCommand.addChange(change);
    }
  }

  public boolean redo(Editor editor, DataContext context) {
    if (pointer < undos.size()) {
      UndoCommand cmd = undos.get(pointer);
      if (logger.isDebugEnabled()) logger.debug("redo command " + pointer);
      pointer++;
      inUndo = true;
      cmd.redo(editor, context);
      inUndo = false;

      return true;
    }

    return false;
  }

  public boolean undo(Editor editor, DataContext context) {
    if (pointer == 0 && getMaxUndos() == 0) {
      return redo(editor, context);
    }

    if (pointer > 0) {
      pointer--;
      UndoCommand cmd = undos.get(pointer);
      if (logger.isDebugEnabled()) logger.debug("undo command " + pointer);
      inUndo = true;
      cmd.undo(editor, context);
      inUndo = false;

      if (pointer == 0 && restorable) {
        Project p = PlatformDataKeys.PROJECT.getData(context);
        DocumentManager.getInstance().reloadDocument(editor.getDocument(), p);
      }

      return true;
    }

    return false;
  }

  public boolean undoLine(Editor editor, DataContext context) {
    if (pointer == 0 && getMaxUndos() == 0) {
      return redo(editor, context);
    }

    if (pointer > 0) {
      int lastLine = -1;

      pointer--;
      UndoCommand cmd = undos.get(pointer);
      if (logger.isDebugEnabled()) logger.debug("undo command " + pointer);
      while (cmd.isOneLine() && (lastLine == -1 || cmd.getLine() == lastLine)) {
        lastLine = cmd.getLine();

        inUndo = true;
        cmd.undo(editor, context);
        inUndo = false;

        if (pointer > 0) {
          pointer--;
          cmd = undos.get(pointer);
        }
        else {
          break;
        }
      }

      return true;
    }

    return false;
  }

  private int getMaxUndos() {
    return ((NumberOption)Options.getInstance().getOption("undolevels")).value();
  }

  public String toString() {
    StringBuffer res = new StringBuffer();
    res.append("EditorUndoList[");
    res.append("pointer=").append(pointer);
    res.append(", undos=").append(undos);
    res.append("]");

    return res.toString();
  }

  //private Editor editor;
  private UndoCommand currentCommand;
  private List<UndoCommand> undos = new ArrayList<UndoCommand>();
  private int pointer = 0;
  private boolean inUndo = false;
  private boolean restorable = true;

  private static Logger logger = Logger.getInstance(EditorUndoList.class.getName());
}
