package com.maddyhome.idea.vim.helper;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
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

import com.maddyhome.idea.vim.VimPlugin;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 */
public class MessageHelper
{
    public static void EMSG(String key)
    {
        VimPlugin.showMessage(getMsg(key));
    }

    public static void EMSG(String key, String val)
    {
        VimPlugin.showMessage(getMsg(key, val));
    }

    public static void EMSG(String key, String val, String val2)
    {
        VimPlugin.showMessage(getMsg(key, new Object[] { val, val2 }));
    }

    public static String getMsg(String key)
    {
        return bundle.getString(key);
    }

    public static String getMsg(String key, String val)
    {
        String msg = bundle.getString(key);

        msg = MessageFormat.format(msg, val);

        return msg;
    }

    public static String getMsg(String key, Object[] vals)
    {
        String msg = bundle.getString(key);

        msg = MessageFormat.format(msg, vals);

        return msg;
    }

    private static ResourceBundle bundle = ResourceBundle.getBundle("messages");
}
