package com.maddyhome.idea.vim.key;

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

import com.intellij.openapi.actionSystem.AnAction;

/**
 * This represents a command argument node in the key/action tree. Currently arguments of argType character
 * and motion command are used.
 */
public class ArgumentNode implements Node
{
    /**
     * Creates a node for the given action
     * @param action The action this arguments is mapped to
     * @param cmdType The type of the command this argument is for
     * @param argType The type of the argument
     * @param flags Any special flags associated with this argument
     */
    public ArgumentNode(AnAction action, int cmdType, int argType, int flags)
    {
        this.action = action;
        this.argType = argType;
        this.cmdType = cmdType;
        this.flags = flags;
    }

    /**
     * Gets the action of the argument
     * @return The argument's action
     */
    public AnAction getAction()
    {
        return action;
    }

    /**
     * Gets the argument type
     * @return The argument's type
     */
    public int getArgType()
    {
        return argType;
    }

    /**
     * Gets the type of the command this arguments is for
     * @return The argument's command type
     */
    public int getCmdType()
    {
        return cmdType;
    }

    /**
     * Gets the argument flags
     * @return The argument's flags
     */
    public int getFlags()
    {
        return flags;
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();
        res.append("ArgumentNode[");
        res.append("action=");
        res.append(action);
        res.append("argType=");
        res.append(argType);
        res.append("flags=");
        res.append(flags);
        res.append("]");

        return res.toString();
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof ArgumentNode)) return false;

        final ArgumentNode node = (ArgumentNode)o;

        if (argType != node.argType) return false;
        if (cmdType != node.cmdType) return false;
        if (flags != node.flags) return false;
        if (!action.equals(node.action)) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = action.hashCode();
        result = 29 * result + argType;
        result = 29 * result + cmdType;
        result = 29 * result + flags;
        return result;
    }

    protected AnAction action;
    protected int argType;
    protected int cmdType;
    protected int flags;
}
