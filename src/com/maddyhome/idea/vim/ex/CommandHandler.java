package com.maddyhome.idea.vim.ex;

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
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import com.maddyhome.idea.vim.undo.UndoManager;

/**
 *
 */
public abstract class CommandHandler
{
    public static final int RANGE_REQUIRED = 1;
    public static final int RANGE_OPTIONAL = 2;
    public static final int RANGE_FORBIDDEN = 4;
    public static final int ARGUMENT_REQUIRED = 8;
    public static final int ARGUMENT_OPTIONAL = 16;
    public static final int ARGUMENT_FORBIDDEN = 32;
    public static final int WRITABLE = 256;
    public static final int READ_ONLY = 512;

    public CommandHandler(CommandName[] names, int flags)
    {
        this.names = names;
        this.flags = flags;

        CommandParser.getInstance().addHandler(this);
    }

    public CommandHandler(String text, String optional, int flags)
    {
        this(new CommandName[] { new CommandName(text, optional) }, flags);
    }

    public String getRequired()
    {
        return names[0].getRequired();
    }

    public String getOptional()
    {
        return names[0].getOptional();
    }

    public CommandName[] getNames()
    {
        return names;
    }

    public int getFlags()
    {
        return flags;
    }

    public void process(final Editor editor, final DataContext context, final ExCommand cmd, final int count) throws
        ExException
    {
        if ((flags & RANGE_FORBIDDEN) != 0 && cmd.getRanges().size() != 0)
        {
            throw new NoRangeAllowedException();
        }

        if ((flags & ARGUMENT_REQUIRED) != 0 && cmd.getArgument().length() == 0)
        {
            throw new MissingArgumentException();
        }

        if ((getFlags() & WRITABLE) != 0)
        {
            RunnableHelper.runWriteCommand(new Runnable() {
                public void run()
                {
                    boolean res = true;
                    try
                    {
                        UndoManager.getInstance().beginCommand(editor);
                        for (int i = 0; i < count && res; i++)
                        {
                            res = execute(editor, context, cmd);
                        }
                    }
                    catch (ExException e)
                    {
                        // TODO - handle this
                        VimPlugin.indicateError();
                        res = false;
                    }
                    finally
                    {
                        if (res)
                        {
                            UndoManager.getInstance().endCommand(editor);
                        }
                        else
                        {
                            UndoManager.getInstance().abortCommand(editor);
                        }
                    }
                }
            });
        }
        else
        {
            RunnableHelper.runReadCommand(new Runnable() {
                public void run()
                {
                    try
                    {
                        for (int i = 0; i < count; i++)
                        {
                            execute(editor, context, cmd);
                        }
                    }
                    catch (ExException e)
                    {
                        // TODO - handle this
                        VimPlugin.indicateError();
                    }
                }
            });
        }
    }

    public abstract boolean execute(Editor editor, DataContext context, ExCommand cmd) throws ExException;

    protected CommandName[] names;
    protected int flags;
}
