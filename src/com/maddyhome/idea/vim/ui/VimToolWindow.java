package com.maddyhome.idea.vim.ui;

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

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 */
public class VimToolWindow extends JPanel
{
    public static VimToolWindow getInstance()
    {
        if (instance == null)
        {
            instance = new VimToolWindow();
        }

        return instance;
    }

    /**
     */
    private VimToolWindow()
    {
        add(new JLabel("Resize so just the window title is visible"));

        setSize(0,0);
    }

    private static VimToolWindow instance;
}
