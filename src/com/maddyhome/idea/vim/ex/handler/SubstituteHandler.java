package com.maddyhome.idea.vim.ex.handler;

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
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandName;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ex.LineRange;
import com.maddyhome.idea.vim.group.CommandGroups;

/**
 *
 */
public class SubstituteHandler extends CommandHandler
{
    public SubstituteHandler()
    {
        super(new CommandName[] {
            new CommandName("s", "ubstitute"),
            new CommandName("&", ""),
            new CommandName("~", "")
        }, WRITABLE);
    }

    public boolean execute(Editor editor, DataContext context, ExCommand cmd) throws ExException
    {
        /*
        String arg = cmd.getArgument();
        logger.debug("arg="+arg);
        String pattern = "";
        String replace = "~";
        String args = "";
        String count = "";
        // Are there any aruments at all?
        if (arg.length() > 0)
        {
            // Are there flags and possible count?
            if (Character.isLetter(arg.charAt(0)) || arg.charAt(0) == '&')
            {
                StringTokenizer tokenizer = new StringTokenizer(arg, " ");
                args = tokenizer.nextToken();
                if (tokenizer.hasMoreTokens())
                {
                    count = tokenizer.nextToken();
                }
            }
            // Is there just a count?
            else if (Character.isDigit(arg.charAt(0)))
            {
                count = arg;
            }
            // We have a pattern and maybe flags and a count
            else
            {
                StringTokenizer tokenizer = new StringTokenizer(arg.substring(1), Character.toString(arg.charAt(0)));
                try
                {
                    if (tokenizer.hasMoreTokens())
                    {
                        pattern = tokenizer.nextToken();
                    }
                    if (tokenizer.hasMoreTokens())
                    {
                        replace = tokenizer.nextToken();
                    }
                    if (tokenizer.hasMoreTokens())
                    {
                        args = tokenizer.nextToken(" " + arg.charAt(0));
                    }
                    if (tokenizer.hasMoreTokens())
                    {
                        count = tokenizer.nextToken(" ");
                    }
                }
                catch (NoSuchElementException e)
                {
                }

                if (args.length() > 0 && Character.isDigit(args.charAt(0)))
                {
                    args = "";
                    count = args;
                }
            }
        }

        logger.debug("pattern="+pattern);
        logger.debug("replace="+replace);
        logger.debug("args="+args);
        logger.debug("count="+count);

        cmd.setArgument(count);
        TextRange range = cmd.getTextRange(editor, context, count.length() > 0);

        int sflags = SearchGroup.argsToFlags(args);
        if (cmd.getCommand().equals("~"))
        {
            sflags |= SearchGroup.REUSE;
        }
        */

        LineRange range = cmd.getLineRange(editor, context, false);
        return CommandGroups.getInstance().getSearch().searchAndReplace(editor, context, range, cmd.getCommand(),
            cmd.getArgument());
    }
}
