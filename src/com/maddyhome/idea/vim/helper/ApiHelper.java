package com.maddyhome.idea.vim.helper;

/*
* IdeaVim - A Vim emulator plugin for IntelliJ Idea
* Copyright (C) 2004 Rick Maddy
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

public class ApiHelper
{
    public static boolean supportsColorSchemes()
    {
        return hasColorSchemes;
    }

    public static boolean supportsBlockSelection()
    {
        return hasBlockSelection;
    }

    private static boolean hasColorSchemes = true;
    private static boolean hasBlockSelection = true;

    static {
        try
        {
            Class.forName("com.intellij.openapi.editor.colors.EditorColors");
        }
        catch (ClassNotFoundException e)
        {
            hasColorSchemes = false;
        }

        try
        {
            Class sm = Class.forName("com.intellij.openapi.editor.SelectionModel");
            sm.getMethod("hasBlockSelection", null);
        }
        catch (Exception e)
        {
            hasBlockSelection = false;
        }
    }
}
