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

import java.util.HashMap;

/**
 *
 */
public class CommandNode
{
    public CommandNode()
    {
        command = null;
    }

    public CommandNode(CommandHandler command)
    {
        this.command = command;
    }

    public CommandNode addChild(char ch, CommandHandler command)
    {
        CommandNode res = new CommandNode(command);
        nodes.put(new Character(ch), res);

        return res;
    }

    public CommandNode getChild(char ch)
    {
        return (CommandNode)nodes.get(new Character(ch));
    }

    public CommandHandler getCommandHandler()
    {
        return command;
    }

    public void setCommandHandler(CommandHandler command)
    {
        this.command = command;
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();
        res.append("CommandNode{");
        res.append("command=" + command);
        res.append(",children=" + nodes);
        res.append("}");

        return res.toString();
    }

    private CommandHandler command;
    private HashMap nodes = new HashMap();
}
