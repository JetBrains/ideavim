package com.maddyhome.idea.vim.option;

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
public class ToggleOption extends Option
{
    ToggleOption(String name, String abbrev, boolean dflt)
    {
        super(name, abbrev);

        this.dflt = dflt;
        this.value = dflt;
    }

    public boolean getValue()
    {
        return value;
    }

    public void set()
    {
        value = true;
    }

    public void reset()
    {
        value = false;
    }

    public void toggle()
    {
        value = !value;
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();
        if (!value)
        {
            res.append("no");
        }
        else
        {
            res.append("  ");
        }

        res.append(getName());

        return res.toString();
    }

    public boolean isDefault()
    {
        return value == dflt;
    }

    public void resetDefault()
    {
        value = dflt;
    }

    protected boolean dflt;
    protected boolean value;
}
