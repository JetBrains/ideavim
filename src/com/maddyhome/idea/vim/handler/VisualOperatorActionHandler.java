package com.maddyhome.idea.vim.handler;

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
import com.intellij.openapi.util.TextRange;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.VisualChange;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.undo.UndoManager;

/**
 *
 */
public abstract class VisualOperatorActionHandler extends AbstractEditorActionHandler
{
    protected final boolean execute(final Editor editor, DataContext context, Command cmd)
    {
        if (!Command.isReadType(cmd.getType()))
        {
            UndoManager.getInstance().endCommand(editor);
            UndoManager.getInstance().beginCommand(editor);
        }
        if (CommandState.getInstance().getMode() == CommandState.MODE_REPEAT)
        {
            CommandGroups.getInstance().getMotion().toggleVisual(editor, context, 1, 1, 0);
        }
        TextRange range = CommandGroups.getInstance().getMotion().getVisualRange(editor);
        VisualChange change = CommandGroups.getInstance().getMotion().getVisualOperatorRange(editor, cmd.getFlags());
        boolean res = execute(editor, context, cmd, range);

        if (res)
        {
            EditorData.setLastVisualOperatorRange(editor, change);
            if ((cmd.getFlags() & Command.FLAG_MULTIKEY_UNDO) == 0 && !Command.isReadType(cmd.getType()))
            {
                UndoManager.getInstance().endCommand(editor);
                UndoManager.getInstance().beginCommand(editor);
            }
            CommandState.getInstance().saveLastChangeCommand(cmd);
        }
        else
        {
            if (!Command.isReadType(cmd.getType()))
            {
                UndoManager.getInstance().abortCommand(editor);
                UndoManager.getInstance().beginCommand(editor);
            }
        }

        CommandGroups.getInstance().getMotion().exitVisual(editor);

        return res;
    }

    protected abstract boolean execute(Editor editor, DataContext context, Command cmd, TextRange range);
}
