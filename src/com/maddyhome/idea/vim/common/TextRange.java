package com.maddyhome.idea.vim.common;
/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2004 Rick Maddy
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

public class TextRange
{
    public TextRange(int start, int end)
    {
        this(new int[] { start }, new int[] { end });
    }

    public TextRange(int[] starts, int[] ends)
    {
        this.starts = starts;
        this.ends = ends;
    }

    public boolean isMultiple()
    {
        return starts != null && starts.length > 1;
    }

    public int getLength()
    {
        return getEndOffset() - getStartOffset();
    }

    public int getStartOffset()
    {
        return starts[0];
    }

    public int getEndOffset()
    {
        return ends[ends.length - 1];
    }

    public int[] getStartOffsets()
    {
        return starts;
    }

    public int[] getEndOffsets()
    {
        return ends;
    }

    private int[] starts;
    private int[] ends;
}