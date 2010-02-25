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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.intellij.openapi.actionSystem.DataContext;

/**
 *
 */
public class DigraphHandler extends CommandHandler
{
    public DigraphHandler()
    {
        super("dig", "raphs", ARGUMENT_OPTIONAL | KEEP_FOCUS);
    }

    public boolean execute(Editor editor, DataContext context, ExCommand cmd) throws ExException
    {
        String arg = cmd.getArgument();
        if (logger.isDebugEnabled())
        {
            logger.debug("arg="+arg);
        }

        return CommandGroups.getInstance().getDigraph().parseCommandLine(editor, cmd.getArgument(), true);
    }

    private static Logger logger = Logger.getInstance(DigraphHandler.class.getName());
}
