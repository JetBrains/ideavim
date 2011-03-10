package com.maddyhome.idea.vim.command;

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

public class VisualChange
{
    public VisualChange(int lines, int columns, int type)
    {
        this.lines = lines;
        this.columns = columns;
        this.type = type;
    }

    public int getLines()
    {
        return lines;
    }

    public int getColumns()
    {
        return columns;
    }

    public int getType()
    {
        return type;
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();
        res.append("VisualChange[");
        res.append("lines=").append(lines);
        res.append(", columns=").append(columns);
        res.append(", type=").append(type);
        res.append("]");

        return res.toString();
    }

    int lines;
    int columns;
    int type;
}
