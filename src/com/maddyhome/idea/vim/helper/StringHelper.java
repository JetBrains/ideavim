package com.maddyhome.idea.vim.helper;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.KeyStroke;

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

    public static List stringToKeys(String str)
    {
        ArrayList res = new ArrayList();
        for (int i = 0; i < str.length(); i++)
        {
            res.add(KeyStroke.getKeyStroke(str.charAt(i)));
        }

        return res;
    }

    public static String keysToString(List keys)
    {
        StringBuffer res = new StringBuffer();
        for (int i = 0; i < keys.size(); i++)
        {
            KeyStroke key = (KeyStroke)keys.get(i);
            if (key.getKeyChar() != KeyEvent.CHAR_UNDEFINED)
            {
                res.append(key.getKeyChar());
            }
            else
            {
                switch (key.getKeyCode())
                {
                    case KeyEvent.VK_TAB:
                        res.append("\t");
                        break;
                    case KeyEvent.VK_ENTER:
                        res.append("\n");
                        break;
                    case KeyEvent.VK_BACK_SPACE:
                        res.append("\b");
                        break;
                    default:
                        res.append('<');
                        res.append(getModifiersText(key.getModifiers()));
                        res.append(KeyEvent.getKeyText(key.getKeyCode()));
                        res.append('>');
                }
            }
        }

        return res.toString();
    }

    public static String getModifiersText(int modifiers)
    {
        StringBuffer buf = new StringBuffer();
        if ((modifiers & KeyEvent.META_MASK) != 0)
        {
            buf.append("M-");
        }
        if ((modifiers & KeyEvent.CTRL_MASK) != 0)
        {
            buf.append("C-");
        }
        if ((modifiers & KeyEvent.ALT_MASK) != 0)
        {
            buf.append("A-");
        }
        if ((modifiers & KeyEvent.SHIFT_MASK) != 0)
        {
            buf.append("S-");
        }
        if ((modifiers & KeyEvent.ALT_GRAPH_MASK) != 0)
        {
            buf.append("G-");
        }

        return buf.toString();
    }

    public static boolean containsUpperCase(String text)
    {
        for (int i = 0; i < text.length(); i++)
        {
            if (Character.isUpperCase(text.charAt(i)) && (i == 0 || text.charAt(i - 1) == '\\'))
            {
                return true;
            }
        }

        return false;
    }

    private StringHelper() {}
}
