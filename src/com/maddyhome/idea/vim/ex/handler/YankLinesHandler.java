package com.maddyhome.idea.vim.ex.handler;

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
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.RegisterGroup;
import com.intellij.openapi.actionSystem.DataContext;

/**
 *
 */
public class YankLinesHandler extends CommandHandler
{
    public YankLinesHandler()
    {
        super("y", "ank", RANGE_OPTIONAL | ARGUMENT_OPTIONAL);
    }

    public boolean execute(Editor editor, DataContext context, ExCommand cmd) throws ExException
    {
        StringBuffer arg = new StringBuffer(cmd.getArgument());
        char register = RegisterGroup.REGISTER_DEFAULT;
        if (arg.length() > 0 && (arg.charAt(0) < '0' || arg.charAt(0) > '9'))
        {
            register = arg.charAt(0);
            arg.deleteCharAt(0);
            cmd.setArgument(arg.toString());
        }

        if (!CommandGroups.getInstance().getRegister().selectRegister(register))
        {
            return false;
        }

        TextRange range = cmd.getTextRange(editor, context, true);

        return CommandGroups.getInstance().getCopy().yankRange(editor, context, range, Command.FLAG_MOT_LINEWISE, false);
    }
}
