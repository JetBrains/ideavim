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
import com.intellij.openapi.editor.Editor;
import java.util.ArrayList;

/**
 *
 */
public class UndoCommand
{
    public UndoCommand(Editor editor)
    {
        startOffset = editor.getCaretModel().getOffset();
    }

    public void complete(Editor editor)
    {
        endOffset = editor.getCaretModel().getOffset();
    }

    public void addChange(DocumentChange change)
    {
        changes.add(change);
    }

    public void redo(Editor editor, DataContext context)
    {
        for (int i = 0; i < changes.size(); i++)
        {
            DocumentChange change = (DocumentChange)changes.get(i);
            change.redo(editor, context);
        }

        editor.getCaretModel().moveToOffset(endOffset);
    }

    public void undo(Editor editor, DataContext context)
    {
        for (int i = changes.size() - 1; i >= 0; i--)
        {
            DocumentChange change = (DocumentChange)changes.get(i);
            change.undo(editor, context);
        }

        editor.getCaretModel().moveToOffset(startOffset);
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();
        res.append("UndoCommand[");
        res.append("startOffset=" + startOffset);
        res.append(", endOffset=" + endOffset);
        res.append(", changes=" + changes);
        res.append("]");

        return res.toString();
    }

    public int size()
    {
        return changes.size();
    }

    private int startOffset;
    private int endOffset;
    private ArrayList changes = new ArrayList();
}
