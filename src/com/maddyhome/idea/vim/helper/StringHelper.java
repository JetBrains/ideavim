package com.maddyhome.idea.vim.helper;

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
public class StringHelper
{
    public static String escape(String text)
    {
        StringBuffer res = new StringBuffer(text.length());

        for (int i = 0; i < text.length(); i++)
        {
            char ch = text.charAt(i);
            if (ch < ' ')
            {
                res.append('^').append((char)(ch + 'A' - 1));
            }
            else if (ch == '\n')
            {
                res.append("^J");
            }
            else if (ch == '\t')
            {
                res.append("^I");
            }
            else
            {
                res.append(ch);
            }
        }

        return res.toString();
    }

    private StringHelper() {}
}
