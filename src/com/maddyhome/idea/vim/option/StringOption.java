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
public class StringOption extends TextOption
{
    StringOption(String name, String abbrev, String dflt)
    {
        super(name, abbrev);
        this.dflt = dflt;
        this.value = dflt;
    }

    public String getValue()
    {
        return value;
    }

    public boolean set(String val)
    {
        value = val;

        return true;
    }

    public boolean append(String val)
    {
        value += val;

        return true;
    }

    public boolean prepend(String val)
    {
        value = val + value;

        return true;
    }

    public boolean remove(String val)
    {
        int pos = value.indexOf(val);
        if (pos != -1)
        {
            value = value.substring(0, pos) + value.substring(pos + val.length());

            return true;
        }

        return false;
    }

    public boolean isDefault()
    {
        return dflt.equals(value);
    }

    public void resetDefault()
    {
        value = dflt;
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();
        res.append("  ");
        res.append(getName());
        res.append("=");
        res.append(value);

        return res.toString();
    }

    protected String dflt;
    protected String value;
}
