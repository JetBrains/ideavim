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

/**
 *
 */
public class LineRange
{
    public LineRange(int startLine, int endLine)
    {
        if (endLine >= startLine)
        {
            this.startLine = startLine;
            this.endLine = endLine;
        }
        else
        {
            this.startLine = endLine;
            this.endLine = startLine;
        }
    }

    public int getStartLine()
    {
        return startLine;
    }

    public int getEndLine()
    {
        return endLine;
    }

    private int startLine;
    private int endLine;
}
