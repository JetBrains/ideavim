package com.maddyhome.idea.vim.undo;

/*
* IdeaVim - A Vim emulator plugin for IntelliJ Idea
* Copyright (C) 2003 Rick Maddy
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
import java.util.ArrayList;

/**
 *
 */
public class EditorUndoList
{
    public void beginCommand(Editor editor)
    {
        logger.info("beginCommand");
        currentCommand = new UndoCommand(editor);
    }

    public void abortCommand()
    {
        logger.info("abortCommand");
        currentCommand = null;
    }

    public void endCommand(Editor editor)
    {
        if (currentCommand == null)
        {
            return;
        }
        else if (currentCommand.size() == 0)
        {
            currentCommand = null;
            return;
        }

        logger.info("endCommand");
        while (pointer < undos.size())
        {
            undos.remove(pointer);
        }

        currentCommand.complete(editor);
        undos.add(currentCommand);
        currentCommand = null;

        if (undos.size() > maxUndos)
        {
            undos.remove(0);
        }

        pointer = undos.size();
    }

    public void addChange(DocumentChange change)
    {
        if (currentCommand != null)
        {
            logger.info("addChange");
            currentCommand.addChange(change);
        }
    }

    public boolean redo(Editor editor, DataContext context)
    {
        if (pointer < undos.size())
        {
            UndoCommand cmd = (UndoCommand)undos.get(pointer);
            logger.debug("redo command " + pointer);
            pointer++;
            cmd.redo(editor, context);

            return true;
        }

        return false;
    }

    public boolean undo(Editor editor, DataContext context)
    {
        if (pointer > 0)
        {
            pointer--;
            UndoCommand cmd = (UndoCommand)undos.get(pointer);
            logger.debug("undo command " + pointer);
            cmd.undo(editor, context);

            return true;
        }

        return false;
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();
        res.append("EditorUndoList[");
        res.append("pointer=" + pointer);
        res.append(", undos=" + undos);
        res.append("]");

        return res.toString();
    }

    private UndoCommand currentCommand;
    private ArrayList undos = new ArrayList();
    private int pointer = 0;
    private int maxUndos = 1000;

    private static Logger logger = Logger.getInstance(EditorUndoList.class.getName());
}
