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
public class NumberOption extends TextOption
{
    NumberOption(String name, String abbrev, int dflt)
    {
        this(name, abbrev, dflt, 0, Integer.MAX_VALUE);
    }

    NumberOption(String name, String abbrev, int dflt, int min, int max)
    {
        super(name, abbrev);
        this.dflt = dflt;
        this.value = dflt;
        this.min = min;
        this.max = max;
    }

    public String getValue()
    {
        return Integer.toString(value);
    }

    public boolean set(String val)
    {
        Integer num = asNumber(val);
        if (num == null)
        {
            return false;
        }

        if (inRange(num.intValue()))
        {
            value = num.intValue();

            return true;
        }

        return false;
    }

    public boolean append(String val)
    {
        Integer num = asNumber(val);
        if (num == null)
        {
            return false;
        }

        if (inRange(value + num.intValue()))
        {
            value += num.intValue();

            return true;
        }

        return false;
    }

    public boolean prepend(String val)
    {
        Integer num = asNumber(val);
        if (num == null)
        {
            return false;
        }

        if (inRange(value * num.intValue()))
        {
            value *= num.intValue();

            return true;
        }

        return false;
    }

    public boolean remove(String val)
    {
        Integer num = asNumber(val);
        if (num == null)
        {
            return false;
        }

        if (inRange(value - num.intValue()))
        {
            value -= num.intValue();

            return true;
        }

        return false;
    }

    public boolean isDefault()
    {
        return value == dflt;
    }

    public void resetDefault()
    {
        value = dflt;
    }

    protected Integer asNumber(String val)
    {
        try
        {
            return Integer.decode(val);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    protected boolean inRange(int val)
    {
        return (val >= min && val <= max);
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

    private int dflt;
    private int value;
    private int min;
    private int max;
}
