package com.maddyhome.idea.vim.handler.change;

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
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.handler.AbstractEditorActionHandler;
import com.maddyhome.idea.vim.undo.UndoManager;

/**
 */
public class RepeatChangeHandler extends AbstractEditorActionHandler
{
    public boolean execute(Editor editor, DataContext context, Command command)
    {
        CommandState state = CommandState.getInstance();
        Command cmd = state.getLastChangeCommand();
        if (cmd != null)
        {
            if (command.getRawCount() > 0)
            {
                cmd.setCount(command.getCount());
                Command mot = cmd.getArgument().getMotion();
                if (mot != null)
                {
                    mot.setCount(0);
                }
            }
            Command save = state.getCommand();
            state.setCommand(cmd);
            int mode = state.getMode();
            state.setMode(CommandState.MODE_REPEAT);
            char reg = CommandGroups.getInstance().getRegister().getCurrentRegister();
            CommandGroups.getInstance().getRegister().selectRegister(state.getLastChangeRegister());
            try
            {
                KeyHandler.executeAction(cmd.getAction(), context);
                UndoManager.getInstance().endCommand(editor);
            }
            catch (Exception e)
            {
                // oops
            }
            state.setMode(mode);
            state.setCommand(save);
            state.saveLastChangeCommand(cmd);
            CommandGroups.getInstance().getRegister().selectRegister(reg);

            return true;
        }
        else
        {
            return false;
        }
    }
}
