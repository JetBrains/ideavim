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
import com.maddyhome.idea.vim.option.NumberOption;
import com.maddyhome.idea.vim.option.Options;
import java.util.ArrayList;

/**
 *
 */
public class EditorUndoList
{
    public EditorUndoList(Editor editor)
    {
        this.editor = editor;

        beginCommand();
    }

    public boolean inCommand()
    {
        return currentCommand != null;
    }

    public void beginCommand()
    {
        logger.info("beginCommand");
        if (inCommand())
        {
            endCommand();
        }
        currentCommand = new UndoCommand(editor);
    }

    public void abortCommand()
    {
        logger.info("abortCommand");
        currentCommand = null;
    }

    public void endCommand()
    {
        if (currentCommand != null && currentCommand.size() > 0)
        {
            logger.info("endCommand");
            int max = getMaxUndos();
            if (max == 0)
            {
                undos.clear();
                undos.add(currentCommand);
            }
            else
            {
                while (pointer < undos.size())
                {
                    undos.remove(pointer);
                }

                undos.add(currentCommand);

                if (undos.size() > max)
                {
                    undos.remove(0);
                }
            }

            currentCommand.complete();

            pointer = undos.size();
        }

        currentCommand = null;
    }

    public void addChange(DocumentChange change)
    {
        if (!inUndo && currentCommand != null)
        {
            logger.info("addChange");
            currentCommand.addChange(change);
        }
        /*
        else if (!inUndo)
        {
            beginCommand(editor);
            currentCommand.addChange(change);
            endCommand(editor);
        }
        */
    }

    public boolean redo(Editor editor, DataContext context)
    {
        if (pointer < undos.size())
        {
            UndoCommand cmd = (UndoCommand)undos.get(pointer);
            logger.debug("redo command " + pointer);
            pointer++;
            inUndo = true;
            cmd.redo(editor, context);
            inUndo = false;

            return true;
        }

        return false;
    }

    public boolean undo(Editor editor, DataContext context)
    {
        if (pointer == 0 && getMaxUndos() == 0)
        {
            return redo(editor, context);
        }

        if (pointer > 0)
        {
            pointer--;
            UndoCommand cmd = (UndoCommand)undos.get(pointer);
            logger.debug("undo command " + pointer);
            inUndo = true;
            cmd.undo(editor, context);
            inUndo = false;

            return true;
        }

        return false;
    }

    private int getMaxUndos()
    {
        return ((NumberOption)Options.getInstance().getOption("undolevels")).value();
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

    private Editor editor;
    private UndoCommand currentCommand;
    private ArrayList undos = new ArrayList();
    private int pointer = 0;
    private boolean inUndo = false;

    private static Logger logger = Logger.getInstance(EditorUndoList.class.getName());
}
