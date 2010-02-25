package com.maddyhome.idea.vim.action.change;

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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.handler.AbstractEditorActionHandler;
import com.intellij.openapi.actionSystem.DataContext;

/**
 */
public class RepeatExCommandAction extends EditorAction
{
    public RepeatExCommandAction()
    {
        super(new Handler());
    }

    private static class Handler extends AbstractEditorActionHandler
    {
        public boolean execute(Editor editor, DataContext context, Command command)
        {
            int count = command.getCount();
            try
            {
                return CommandParser.getInstance().processLastCommand(editor, context, count);
            }
            catch (ExException e)
            {
                return false;
            }
        }
    }
}
