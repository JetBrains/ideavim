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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 *
 */
public class ListOption extends TextOption
{
    ListOption(String name, String abbrev, String[] dflt)
    {
        super(name, abbrev);

        this.dflt = new ArrayList(Arrays.asList(dflt));
        this.value = new ArrayList(this.dflt);
    }

    public String getValue()
    {
        StringBuffer res = new StringBuffer();
        int cnt = 0;
        for (Iterator iterator = value.iterator(); iterator.hasNext(); cnt++)
        {
            String s = (String)iterator.next();
            if (cnt > 0)
            {
                res.append(",");
            }
            res.append(s);
        }

        return res.toString();
    }

    public boolean contains(String val)
    {
        return value.containsAll(parseVals(val));
    }

    public boolean set(String val)
    {
        return set(parseVals(val));
    }

    public boolean append(String val)
    {
        return append(parseVals(val));
    }

    public boolean prepend(String val)
    {
        return prepend(parseVals(val));
    }

    public boolean remove(String val)
    {
        return remove(parseVals(val));
    }

    protected boolean set(ArrayList vals)
    {
        value = vals;

        return true;
    }

    protected boolean append(ArrayList vals)
    {
        value.addAll(vals);

        return true;
    }

    protected boolean prepend(ArrayList vals)
    {
        value.addAll(0, vals);

        return true;
    }

    protected boolean remove(ArrayList vals)
    {
        value.removeAll(vals);

        return true;
    }

    protected ArrayList parseVals(String val)
    {
        ArrayList res = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(",");
        while (tokenizer.hasMoreTokens())
        {
            String token = tokenizer.nextToken().trim();
            res.add(token);
        }

        return res;
    }

    public boolean isDefault()
    {
        return dflt.equals(value);
    }

    public void resetDefault()
    {
        value = new ArrayList(dflt);
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();
        res.append("  ");
        res.append(getName());
        res.append("=");
        res.append(getValue());

        return res.toString();
    }

    protected ArrayList dflt;
    protected ArrayList value;
}
