package com.maddyhome.idea.vim.ex.range;

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
import com.maddyhome.idea.vim.ex.Range;

/**
 *
 */
public abstract class AbstractRange implements Range
{
    public static Range[] createRange(String str, int offset, boolean move)
    {
        if (str.equals(".") || str.length() == 0)
        {
            return new Range[] { new LineNumberRange(offset, move) };
        }
        else if (str.equals("%"))
        {
            return new Range[] { new LineNumberRange(0, 0, move), new LineNumberRange(true, offset, move) };
        }
        else if (str.equals("$"))
        {
            return new Range[] { new LineNumberRange(true, offset, move) };
        }
        else if (str.startsWith("'") && str.length() == 2)
        {
            return new Range[] { new MarkRange(str.charAt(1), offset, move) };
        }
        else if (str.startsWith("/") || str.startsWith("\\/"))
        {
            // TODO
        }
        else if (str.startsWith("?") || str.startsWith("\\?"))
        {
            // TODO
        }
        else
        {
            try
            {
                int line = Integer.parseInt(str) - 1;

                return new Range[] { new LineNumberRange(line, offset, move) };
            }
            catch (NumberFormatException e)
            {
            }
        }

        return null;
    }

    public AbstractRange(int offset, boolean move)
    {
        this.offset = offset;
        this.move = move;
    }

    protected int getOffset()
    {
        return offset;
    }

    public int getLine(Editor editor, DataContext context)
    {
        int line = getRangeLine(editor, context);

        return line + offset;
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();
        res.append("offset=" + offset);
        res.append(", move=" + move);

        return res.toString();
    }
    
    protected abstract int getRangeLine(Editor editor, DataContext context);

    protected int offset;
    protected boolean move;
}
