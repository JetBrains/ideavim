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
import com.intellij.openapi.util.TextRange;
import com.maddyhome.idea.vim.ex.range.LineNumberRange;
import com.maddyhome.idea.vim.ex.range.AbstractRange;
import com.maddyhome.idea.vim.helper.EditorHelper;
import java.util.ArrayList;

/**
 *
 */
public class Ranges
{
    public Ranges()
    {
        ranges = new ArrayList();
    }

    public void addRange(Range[] range)
    {
        for (int i = 0; i < range.length; i++)
        {
            ranges.add(range[i]);
        }
    }

    public int size()
    {
        return ranges.size();
    }

    public int getLine(Editor editor, DataContext context)
    {
        if (size() > 0)
        {
            Range last = (Range)ranges.get(size() - 1);

            return last.getLine(editor, context);
        }
        else
        {
            return (new LineNumberRange(0, false)).getLine(editor, context);
        }
    }

    public int getFirstLine(Editor editor, DataContext context)
    {
        if (size() > 0)
        {
            Range first = (Range)ranges.get(0);

            return first.getLine(editor, context);
        }
        else
        {
            return (new LineNumberRange(0, false)).getLine(editor, context);
        }
    }

    public int getCount(Editor editor, DataContext context, int count)
    {
        if (count == -1)
        {
            return getLine(editor, context);
        }
        else
        {
            return count;
        }
    }

    public LineRange getLineRange(Editor editor, DataContext context, int count)
    {
        Range end = null;
        Range start = null;
        if (count == -1)
        {
            if (size() >= 2)
            {
                end = (Range)ranges.get(size() - 1);
                start = (Range)ranges.get(size() - 2);
            }
            else if (size() == 1)
            {
                end = (Range)ranges.get(size() - 1);
                start = end;
            }
            else
            {
                end = new LineNumberRange(0, false);
                start = end;
            }
        }
        else
        {
            if (size() >= 1)
            {
                start = (Range)ranges.get(size() - 1);
            }
            else
            {
                start = new LineNumberRange(0, false);
            }

            end = new LineNumberRange(start.getLine(editor, context), count - 1, false);
        }

        return new LineRange(start.getLine(editor, context), end.getLine(editor, context));
    }

    public TextRange getTextRange(Editor editor, DataContext context, int count)
    {
        LineRange lr = getLineRange(editor, context, count);
        int start = editor.getDocument().getLineStartOffset(lr.getStartLine());
        int end = editor.getDocument().getLineEndOffset(lr.getEndLine()) + 1;

        return new TextRange(start, Math.min(end, EditorHelper.getFileSize(editor)));
    }

    public static TextRange getCurrentLineRange(Editor editor, DataContext context)
    {
        Ranges ranges = new Ranges();

        return ranges.getTextRange(editor, context, -1);
    }

    public static TextRange getFileTextRange(Editor editor, DataContext context)
    {
        Ranges ranges = new Ranges();
        ranges.addRange(AbstractRange.createRange("%", 0, false));

        return ranges.getTextRange(editor, context, -1);
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();
        res.append("Ranges[ranges=" + ranges);
        res.append("]");

        return res.toString();
    }

    private ArrayList ranges;
}
