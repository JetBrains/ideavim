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
import com.maddyhome.idea.vim.helper.EditorHelper;

/**
 *
 */
public class LineNumberRange extends AbstractRange
{
    public LineNumberRange(int offset, boolean move)
    {
        super(offset, move);

        this.line = CURRENT_LINE;
    }

    public LineNumberRange(int line, int offset, boolean move)
    {
        super(offset, move);

        this.line = line;
    }

    public LineNumberRange(boolean last, int offset, boolean move)
    {
        super(offset, move);

        this.line = last ? LAST_LINE : CURRENT_LINE;
    }

    protected int getRangeLine(Editor editor, DataContext context)
    {
        if (line == CURRENT_LINE)
        {
            line = editor.getCaretModel().getLogicalPosition().line;
        }
        else if (line == LAST_LINE)
        {
            line = EditorHelper.getLineCount(editor) - 1;
        }

        return line;
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();
        res.append("LineNumberRange[");
        res.append("line=" + line);
        res.append(", ");
        res.append(super.toString());
        res.append("]");

        return res.toString();
    }

    private int line;

    private static final int CURRENT_LINE = -1;
    private static final int LAST_LINE = -2;
}
