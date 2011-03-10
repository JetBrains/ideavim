package com.maddyhome.idea.vim.group;

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

import org.jdom.Element;

/**
 * This base class provides empty implemtations for the interface methods.
 */
public abstract class AbstractActionGroup implements ActionGroup
{
    /**
     * Allows the group to save its state and any configuration. This does nothing.
     * @param element The plugin's root XML element that this group can add a child to
     */
    public void saveData(Element element)
    {
        // no-op
    }

    /**
     * Allows the group to restore its state and any configuration. This does nothing.
     * @param element The plugin's root XML element that this group can add a child to
     */
    public void readData(Element element)
    {
        // no-op
    }
}
